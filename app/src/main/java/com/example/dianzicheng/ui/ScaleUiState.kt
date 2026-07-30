package com.example.dianzicheng.ui

import com.example.dianzicheng.data.ble.BleScaleClient
import com.example.dianzicheng.domain.BodyMeasurement

data class ScaleUiState(
    val connection: BleScaleClient.ConnectionState = BleScaleClient.ConnectionState.IDLE,
    val liveWeightKg: Double = 0.0,
    val impedanceOhm: Double = 0.0,
    val isStable: Boolean = false,
    val currentMeasurement: BodyMeasurement? = null,
    val showNewMemberAlert: Boolean = false,
    val error: String? = null,
    val debugMessage: String? = null,
    val discoveredDeviceName: String? = null,
    val discoveredDeviceMac: String? = null
)
