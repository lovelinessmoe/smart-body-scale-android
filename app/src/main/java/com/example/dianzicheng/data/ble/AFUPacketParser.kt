package com.example.dianzicheng.data.ble

object AFUPacketParser {

    fun parseWeight(data: ByteArray): WeightData? {
        if (data.size < 10 || (data[0].toInt() and 0xFF) != 0xAC) return null
        val w3 = data[3].toInt() and 0xFF
        val w4 = data[4].toInt() and 0xFF
        val w5 = data[5].toInt() and 0xFF
        val isStable = (data[6].toInt() and 0xFF) == 0x02
        val rawWeight = (w3 - 0x68) * 65536 + w4 * 256 + w5
        val weight = if (rawWeight < 0) 0.0 else rawWeight / 1000.0
        return WeightData(weight, isStable)
    }

    fun parseImpedance(data: ByteArray): Double? {
        if (data.size < 10 || (data[0].toInt() and 0xFF) != 0xAC) return null
        val imp8 = data[8].toInt() and 0xFF
        val imp9 = data[9].toInt() and 0xFF
        val impedance = (imp8 shl 8) or imp9
        return impedance.toDouble()
    }

    data class WeightData(val weightKg: Double, val isStable: Boolean)
}
