package com.wafflehq.infra.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wafflehq.infra.data.scan.ScanStateRepository
import com.wafflehq.infra.ir.IrTransmitter
import com.wafflehq.infra.ir.NecCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

val STEP_SIZES = listOf(1000, 500, 100, 50, 25, 10, 5, 2, 1)
const val TRANSMIT_INTERVAL_MS = 150L
const val MIN_TRANSMIT_INTERVAL_MS = 0L
const val MAX_TRANSMIT_INTERVAL_MS = 500L
private const val PERSIST_EVERY = 10

data class ScanUiState(
    val standardIndex: Int = 0,
    val extendedIndex: Int = 0,
    val isExtended: Boolean = false,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val hasIrEmitter: Boolean = true,
    val startValueText: String = "0",
    val hexText: String = NecCodec.hexOf(0),
    val transmitIntervalMs: Long = TRANSMIT_INTERVAL_MS,
) {
    val currentIndex: Int get() = if (isExtended) extendedIndex else standardIndex
    val maxIndex: Int get() = NecCodec.maxIndex(isExtended)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val irTransmitter: IrTransmitter,
    private val scanStateRepository: ScanStateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState(hasIrEmitter = irTransmitter.hasEmitter))
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            val standard = scanStateRepository.currentIndex.first().coerceIn(0, NecCodec.MAX_INDEX)
            val extended = scanStateRepository.currentExtendedIndex.first().coerceIn(0, NecCodec.EXTENDED_MAX_INDEX)
            val isExtended = scanStateRepository.isExtendedMode.first()
            _uiState.update {
                val current = if (isExtended) extended else standard
                it.copy(
                    standardIndex = standard,
                    extendedIndex = extended,
                    isExtended = isExtended,
                    startValueText = current.toString(),
                    hexText = NecCodec.hexOf(current, isExtended),
                )
            }
        }
    }

    fun onPlayPauseClicked() {
        if (_uiState.value.isRunning) pause() else play()
    }

    private fun play() {
        val state = _uiState.value
        if (state.isRunning || !state.hasIrEmitter) return

        _uiState.update { it.copy(isRunning = true, isFinished = false) }
        scanJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val state = _uiState.value
                    val extended = state.isExtended
                    val index = state.currentIndex
                    irTransmitter.transmit(NecCodec.buildTransmission(index, extended))

                    val next = index + 1
                    if (next > NecCodec.maxIndex(extended)) {
                        _uiState.update { it.copy(isRunning = false, isFinished = true) }
                        return@launch
                    }

                    _uiState.update {
                        withIndex(it, extended, next).copy(
                            startValueText = next.toString(),
                            hexText = NecCodec.hexOf(next, extended),
                        )
                    }
                    if (next % PERSIST_EVERY == 0) {
                        persistIndex(next, extended)
                    }
                    delay(_uiState.value.transmitIntervalMs)
                }
            } finally {
                withContext(NonCancellable) {
                    val finalState = _uiState.value
                    persistIndex(finalState.currentIndex, finalState.isExtended)
                }
            }
        }
    }

    private fun pause() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun onIntervalChanged(ms: Long) {
        val clamped = ms.coerceIn(MIN_TRANSMIT_INTERVAL_MS, MAX_TRANSMIT_INTERVAL_MS)
        _uiState.update { it.copy(transmitIntervalMs = clamped) }
    }

    fun onStartValueTextChanged(text: String) {
        if (text.isEmpty() || text.all { it.isDigit() }) {
            _uiState.update { it.copy(startValueText = text) }
        }
    }

    fun onStartValueConfirmed() {
        if (_uiState.value.isRunning) return
        val parsed = _uiState.value.startValueText.toIntOrNull() ?: _uiState.value.currentIndex
        setIndex(parsed)
    }

    fun onStep(delta: Int) {
        if (_uiState.value.isRunning) return
        setIndex(_uiState.value.currentIndex + delta)
    }

    fun onHexTextChanged(text: String) {
        val upper = text.uppercase()
        if (upper.length <= 8 && upper.all { it.isDigit() || it in 'A'..'F' }) {
            _uiState.update { it.copy(hexText = upper) }
        }
    }

    fun onHexConfirmed() {
        if (_uiState.value.isRunning) return
        val hex = _uiState.value.hexText
        val extended = _uiState.value.isExtended || NecCodec.requiresExtended(hex)
        val parsed = NecCodec.indexFromHex(hex, extended) ?: return
        if (extended && !_uiState.value.isExtended) onExtendedModeChanged(true)
        setIndex(parsed)
    }

    fun onExtendedModeChanged(enabled: Boolean) {
        if (_uiState.value.isRunning) return
        _uiState.update {
            val current = if (enabled) it.extendedIndex else it.standardIndex
            it.copy(
                isExtended = enabled,
                startValueText = current.toString(),
                hexText = NecCodec.hexOf(current, enabled),
                isFinished = false,
            )
        }
        viewModelScope.launch { scanStateRepository.setExtendedMode(enabled) }
    }

    fun onSendCurrentClicked() {
        val state = _uiState.value
        if (state.isRunning || !state.hasIrEmitter) return
        viewModelScope.launch {
            irTransmitter.transmit(NecCodec.buildTransmission(state.currentIndex, state.isExtended))
        }
    }

    private fun setIndex(target: Int) {
        val extended = _uiState.value.isExtended
        val clamped = target.coerceIn(0, NecCodec.maxIndex(extended))
        _uiState.update {
            withIndex(it, extended, clamped).copy(
                startValueText = clamped.toString(),
                hexText = NecCodec.hexOf(clamped, extended),
                isFinished = false,
            )
        }
        viewModelScope.launch { persistIndex(clamped, extended) }
    }

    private fun withIndex(state: ScanUiState, extended: Boolean, index: Int): ScanUiState =
        if (extended) state.copy(extendedIndex = index) else state.copy(standardIndex = index)

    private suspend fun persistIndex(index: Int, extended: Boolean) {
        if (extended) {
            scanStateRepository.setCurrentExtendedIndex(index)
        } else {
            scanStateRepository.setCurrentIndex(index)
        }
    }

    override fun onCleared() {
        scanJob?.cancel()
    }
}
