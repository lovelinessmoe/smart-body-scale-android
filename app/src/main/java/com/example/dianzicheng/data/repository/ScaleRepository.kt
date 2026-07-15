package com.example.dianzicheng.data.repository

import com.example.dianzicheng.data.local.ScaleDao
import com.example.dianzicheng.data.local.toDomain
import com.example.dianzicheng.data.local.toEntity
import com.example.dianzicheng.domain.BodyMeasurement
import com.example.dianzicheng.domain.FamilyMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.math.abs

class ScaleRepository(private val dao: ScaleDao) {
    fun getHistory(): Flow<List<BodyMeasurement>> =
        dao.getAllMeasurements().map { entities -> entities.map { it.toDomain() } }

    suspend fun saveMeasurement(measurement: BodyMeasurement): FamilyMember? {
        val members = dao.getMembersList().map { it.toDomain() }
        val matchedMember = findBestMember(measurement.weightKg, members)
        
        val finalMeasurement = measurement.copy(
            id = UUID.randomUUID().toString(),
            memberId = matchedMember?.id,
            memberNameSnapshot = matchedMember?.name
        )
        dao.insertMeasurement(finalMeasurement.toEntity())
        
        // Update matched member's reference weight to keep matching accurate for next time
        matchedMember?.let { member ->
            dao.insertMember(member.copy(referenceWeightKg = measurement.weightKg).toEntity())
        }

        return matchedMember
    }

    suspend fun getBestMember(weightKg: Double): FamilyMember? {
        val members = dao.getMembersList().map { it.toDomain() }
        return findBestMember(weightKg, members)
    }

    private fun findBestMember(weightKg: Double, members: List<FamilyMember>): FamilyMember? {
        if (members.isEmpty()) return null
        
        // 1. Check if there are any members who have never measured (referenceWeightKg <= 0.0)
        // If so, match the first measurement to them!
        val neverMeasured = members.firstOrNull { it.referenceWeightKg <= 0.0 }
        if (neverMeasured != null) {
            return neverMeasured
        }

        // 2. Otherwise, find the closest member within 7.0 kg range
        val best = members.minByOrNull { abs(it.referenceWeightKg - weightKg) }
        return if (best != null && abs(best.referenceWeightKg - weightKg) <= 7.0) {
            best
        } else {
            null
        }
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurement) {
        dao.deleteMeasurement(measurement.toEntity())
    }
}
