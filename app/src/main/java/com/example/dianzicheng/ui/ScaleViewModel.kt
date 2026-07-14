package com.example.dianzicheng.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dianzicheng.data.ble.BleScaleClient
import com.example.dianzicheng.data.repository.ScaleRepository
import com.example.dianzicheng.domain.BodyAlgorithm
import com.example.dianzicheng.domain.Sex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScaleViewModel(
    private val bleClient: BleScaleClient,
    private val repository: ScaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScaleUiState())
    val uiState: StateFlow<ScaleUiState> = _uiState.asStateFlow()

    init {
        observeBle()
    }

    private fun observeBle() {
        viewModelScope.launch {
            bleClient.connectionState.collect { state ->
                _uiState.update { it.copy(connection = state) }
            }
        }
        viewModelScope.launch {
            bleClient.weight.collect { weight ->
                _uiState.update { it.copy(liveWeightKg = weight) }
            }
        }
        viewModelScope.launch {
            bleClient.isStable.collect { stable ->
                _uiState.update { it.copy(isStable = stable) }
                if (stable && _uiState.value.impedanceOhm > 0) {
                    completeMeasurement()
                }
            }
        }
        viewModelScope.launch {
            bleClient.impedance.collect { imp ->
                _uiState.update { it.copy(impedanceOhm = imp) }
                if (_uiState.value.isStable && imp > 0) {
                    completeMeasurement()
                }
            }
        }
    }

    private fun completeMeasurement() {
        val state = _uiState.value
        // Logic: Get active profile or just use defaults for calculation
        // Real app would let user select active profile
        val measurement = BodyAlgorithm.calculate(
            weightKg = state.liveWeightKg,
            impedanceOhm = state.impedanceOhm,
            sex = Sex.MALE,
            heightCm = 175.0,
            birthDateEpochMs = 631152000000L
        )
        
        viewModelScope.launch {
            val matchedMember = repository.saveMeasurement(measurement)
            val diff = matchedMember?.let { kotlin.math.abs(it.referenceWeightKg - measurement.weightKg) } ?: 100.0
            
            _uiState.update { 
                it.copy(
                    currentMeasurement = measurement,
                    showNewMemberAlert = diff > 7.0
                ) 
            }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(showNewMemberAlert = false) }
    }

    fun startScanning() {
        bleClient.startScan()
    }
}
