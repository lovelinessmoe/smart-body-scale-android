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

    private var activeSessionId: String? = null
    private var hasAlertedForCurrentSession = false

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
                if (_uiState.value.isStable && weight > 0.0) {
                    saveOrUpdateMeasurement(isFinalLocked = false)
                }
            }
        }
        viewModelScope.launch {
            bleClient.isStable.collect { stable ->
                _uiState.update { it.copy(isStable = stable) }
                if (!stable) {
                    activeSessionId = null
                    hasAlertedForCurrentSession = false
                } else {
                    if (activeSessionId == null) {
                        activeSessionId = java.util.UUID.randomUUID().toString()
                    }
                    saveOrUpdateMeasurement(isFinalLocked = true)
                }
            }
        }
        viewModelScope.launch {
            bleClient.impedance.collect { imp ->
                _uiState.update { it.copy(impedanceOhm = imp) }
                if (_uiState.value.isStable && bleClient.weight.value > 0.0) {
                    saveOrUpdateMeasurement(isFinalLocked = false)
                }
            }
        }
        viewModelScope.launch {
            bleClient.discoveredDevice.collect { pair ->
                _uiState.update { 
                    it.copy(
                        discoveredDeviceName = pair?.first,
                        discoveredDeviceMac = pair?.second
                    )
                }
            }
        }
    }

    fun connectToMac(mac: String) {
        bleClient.connectMac(mac)
    }

    private fun saveOrUpdateMeasurement(isFinalLocked: Boolean = false) {
        val weight = bleClient.weight.value
        val impedance = bleClient.impedance.value

        if (weight <= 0.0) return
        val currentSessionId = activeSessionId ?: java.util.UUID.randomUUID().toString().also { activeSessionId = it }

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

                val finalMeasurementWithId = measurement.copy(id = currentSessionId)

                var finalMatchedMember: FamilyMember? = null
                try {
                    finalMatchedMember = repository.saveMeasurement(finalMeasurementWithId, existingId = currentSessionId)
                } catch (e: Exception) {
                    android.util.Log.e("ScaleViewModel", "Database save failed", e)
                }

                val diff = finalMatchedMember?.let { kotlin.math.abs(it.referenceWeightKg - weight) } ?: 100.0
                val shouldShowAlert = isFinalLocked && !hasAlertedForCurrentSession && (diff > 7.0)
                if (shouldShowAlert) {
                    hasAlertedForCurrentSession = true
                }

                _uiState.update {
                    it.copy(
                        currentMeasurement = finalMeasurementWithId,
                        showNewMemberAlert = if (shouldShowAlert) true else it.showNewMemberAlert
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ScaleViewModel", "Error in saveOrUpdateMeasurement", e)
            }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(showNewMemberAlert = false) }
    }

    fun startScanning() {
        android.util.Log.d("ScaleViewModel", "startScanning() called")
        activeSessionId = null
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
