package com.wafflehq.infra.ui.home

import com.wafflehq.infra.data.scan.ScanStateRepository
import com.wafflehq.infra.ir.IrTransmitter
import com.wafflehq.infra.ir.NecCodec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var irTransmitter: IrTransmitter
    private lateinit var scanStateRepository: ScanStateRepository
    private var persistedIndex = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        irTransmitter = mockk(relaxed = true)
        every { irTransmitter.hasEmitter } returns true

        scanStateRepository = mockk()
        every { scanStateRepository.currentIndex } returns MutableStateFlow(0)
        val slot = slot<Int>()
        coEvery { scanStateRepository.setCurrentIndex(capture(slot)) } answers { persistedIndex = slot.captured }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(irTransmitter, scanStateRepository)

    @Test
    fun `starts paused at the persisted index`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.value.currentIndex)
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun `play transmits and advances the index over time`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPlayPauseClicked()
        assertTrue(vm.uiState.value.isRunning)

        // 3 loop iterations run: t=0, t=interval, t=2*interval — landing just after
        // the 3rd keeps the 4th (scheduled at t=3*interval) from firing.
        dispatcher.scheduler.advanceTimeBy(TRANSMIT_INTERVAL_MS * 2 + 1)
        dispatcher.scheduler.runCurrent()

        assertEquals(3, vm.uiState.value.currentIndex)
    }

    @Test
    fun `pause stops the loop and keeps the current index`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPlayPauseClicked()
        dispatcher.scheduler.advanceTimeBy(TRANSMIT_INTERVAL_MS * 2 + 1)
        dispatcher.scheduler.runCurrent()
        val indexAtPause = vm.uiState.value.currentIndex

        vm.onPlayPauseClicked()
        dispatcher.scheduler.advanceTimeBy(TRANSMIT_INTERVAL_MS * 5)
        dispatcher.scheduler.runCurrent()

        assertFalse(vm.uiState.value.isRunning)
        assertEquals(indexAtPause, vm.uiState.value.currentIndex)
    }

    @Test
    fun `start value confirmation jumps to the clamped index`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onStartValueTextChanged("99999")
        vm.onStartValueConfirmed()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NecCodec.MAX_INDEX, vm.uiState.value.currentIndex)
    }

    @Test
    fun `step buttons move the index and clamp at the bounds`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onStep(-5)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentIndex)

        vm.onStep(50)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(50, vm.uiState.value.currentIndex)

        vm.onStep(-10)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(40, vm.uiState.value.currentIndex)
    }

    @Test
    fun `step is ignored while running`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPlayPauseClicked()
        vm.onStep(500)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test
    fun `no emitter disables playback`() = runTest(dispatcher) {
        every { irTransmitter.hasEmitter } returns false
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.hasIrEmitter)

        vm.onPlayPauseClicked()
        dispatcher.scheduler.runCurrent()

        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun `interval change is clamped to the allowed range`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onIntervalChanged(-50)
        assertEquals(MIN_TRANSMIT_INTERVAL_MS, vm.uiState.value.transmitIntervalMs)

        vm.onIntervalChanged(MAX_TRANSMIT_INTERVAL_MS + 1000)
        assertEquals(MAX_TRANSMIT_INTERVAL_MS, vm.uiState.value.transmitIntervalMs)
    }

    @Test
    fun `interval change speeds up the running loop immediately`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPlayPauseClicked()
        vm.onIntervalChanged(MIN_TRANSMIT_INTERVAL_MS)

        dispatcher.scheduler.advanceTimeBy(MIN_TRANSMIT_INTERVAL_MS * 5 + 1)
        dispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.currentIndex > 3)
    }

    @Test
    fun `reaching the last index stops and marks the scan finished`() = runTest(dispatcher) {
        every { scanStateRepository.currentIndex } returns MutableStateFlow(NecCodec.MAX_INDEX)
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPlayPauseClicked()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isRunning)
        assertTrue(vm.uiState.value.isFinished)
        assertEquals(NecCodec.MAX_INDEX, vm.uiState.value.currentIndex)
    }
}
