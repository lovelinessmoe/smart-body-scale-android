package com.example.dianzicheng.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dianzicheng.data.ble.BleScaleClient
import com.example.dianzicheng.data.repository.ScaleRepository
import com.example.dianzicheng.domain.BodyAlgorithm
import com.example.dianzicheng.domain.Sex
import com.example.dianzicheng.domain.FamilyMember
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

    private var isMeasurementSaving = false

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
                if (!stable) {
                    // Weight became unstable again — reset guard so next stable event saves correctly
                    isMeasurementSaving = false
                } else {
                    completeMeasurement()
                }
            }
        }
        viewModelScope.launch {
            bleClient.impedance.collect { imp ->
                _uiState.update { it.copy(impedanceOhm = imp) }
            }
        }
    }

    private fun completeMeasurement() {
        val weight = bleClient.weight.value
        val impedance = bleClient.impedance.value

        if (isMeasurementSaving || weight <= 0.0) return
        isMeasurementSaving = true

        viewModelScope.launch {
            try {
                val matchedMember = repository.getBestMember(weight)
                val measurement = if (matchedMember != null) {
                    BodyAlgorithm.calculate(
                        weightKg = weight,
                        impedanceOhm = impedance,
                        sex = matchedMember.sex,
                        heightCm = matchedMember.heightCm,
                        birthDateEpochMs = matchedMember.birthDateEpochMs
                    )
                } else {
                    BodyAlgorithm.calculate(
                        weightKg = weight,
                        impedanceOhm = impedance,
                        sex = Sex.MALE,
                        heightCm = 175.0,
                        birthDateEpochMs = 631152000000L
                    )
                }

                var finalMatchedMember: FamilyMember? = null
                try {
                    finalMatchedMember = repository.saveMeasurement(measurement)
                } catch (e: Exception) {
                    android.util.Log.e("ScaleViewModel", "Database save failed", e)
                }

                val diff = finalMatchedMember?.let { kotlin.math.abs(it.referenceWeightKg - measurement.weightKg) } ?: 100.0
                _uiState.update {
                    it.copy(
                        currentMeasurement = measurement,
                        showNewMemberAlert = diff > 7.0
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ScaleViewModel", "Error in completeMeasurement", e)
            }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(showNewMemberAlert = false) }
    }

    fun startScanning() {
        android.util.Log.d("ScaleViewModel", "startScanning() called")
        isMeasurementSaving = false
        _uiState.update {
            it.copy(
                currentMeasurement = null,
                liveWeightKg = 0.0,
                impedanceOhm = 0.0,
                isStable = false
            )
        }
        bleClient.startScan()
    }
}
