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
    val currentIndex: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val hasIrEmitter: Boolean = true,
    val startValueText: String = "0",
    val hexText: String = NecCodec.hexOf(0),
    val transmitIntervalMs: Long = TRANSMIT_INTERVAL_MS,
)

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
            val saved = scanStateRepository.currentIndex.first().coerceIn(0, NecCodec.MAX_INDEX)
            _uiState.update {
                it.copy(currentIndex = saved, startValueText = saved.toString(), hexText = NecCodec.hexOf(saved))
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
                    val index = _uiState.value.currentIndex
                    irTransmitter.transmit(NecCodec.buildFrame(index))

                    val next = index + 1
                    if (next > NecCodec.MAX_INDEX) {
                        _uiState.update { it.copy(isRunning = false, isFinished = true) }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(currentIndex = next, startValueText = next.toString(), hexText = NecCodec.hexOf(next))
                    }
                    if (next % PERSIST_EVERY == 0) {
                        scanStateRepository.setCurrentIndex(next)
                    }
                    delay(_uiState.value.transmitIntervalMs)
                }
            } finally {
                withContext(NonCancellable) {
                    scanStateRepository.setCurrentIndex(_uiState.value.currentIndex)
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
        val parsed = NecCodec.indexFromHex(_uiState.value.hexText) ?: return
        setIndex(parsed)
    }

    fun onSendCurrentClicked() {
        val state = _uiState.value
        if (state.isRunning || !state.hasIrEmitter) return
        viewModelScope.launch {
            irTransmitter.transmit(NecCodec.buildFrame(state.currentIndex))
        }
    }

    private fun setIndex(target: Int) {
        val clamped = target.coerceIn(0, NecCodec.MAX_INDEX)
        _uiState.update {
            it.copy(
                currentIndex = clamped,
                startValueText = clamped.toString(),
                hexText = NecCodec.hexOf(clamped),
                isFinished = false,
            )
        }
        viewModelScope.launch { scanStateRepository.setCurrentIndex(clamped) }
    }

    override fun onCleared() {
        scanJob?.cancel()
    }
}
