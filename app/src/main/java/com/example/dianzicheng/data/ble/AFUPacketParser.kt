package com.example.dianzicheng.data.ble

object AFUPacketParser {

    fun parseWeight(data: ByteArray): WeightData? {
        if (data.size < 5 || (data[0].toInt() and 0xFF) != 0xD5) return null
        val payload = u32le(data, 1)
        val isStable = (payload and 0x80000000L) != 0L
        val weight = (payload and 0x0003FFFFL) / 1000.0
        return WeightData(weight, isStable)
    }

    fun parseImpedance(data: ByteArray): Double? {
        if (data.size < 3 || (data[0].toInt() and 0xFF) != 0xD6) return null
        val adc = u16le(data, 1)
        return if (adc >= 1500) adc * 0.88 else adc.toDouble()
    }

    private fun u16le(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32le(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
                ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
                ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
                ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    data class WeightData(val weightKg: Double, val isStable: Boolean)
}
