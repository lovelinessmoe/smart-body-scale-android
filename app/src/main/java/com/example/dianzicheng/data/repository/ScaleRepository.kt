package com.example.dianzicheng.data.repository

import com.example.dianzicheng.data.local.ScaleDao
import com.example.dianzicheng.data.local.toDomain
import com.example.dianzicheng.data.local.toEntity
import com.example.dianzicheng.domain.BodyMeasurement
import com.example.dianzicheng.domain.FamilyMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.math.abs

class ScaleRepository(private val dao: ScaleDao) {
    fun getHistory(): Flow<List<BodyMeasurement>> =
        dao.getAllMeasurements().map { entities -> entities.map { it.toDomain() } }

    suspend fun saveMeasurement(measurement: BodyMeasurement): FamilyMember? {
        val members = dao.getAllMembers().first().map { it.toDomain() }
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

    private fun findBestMember(weightKg: Double, members: List<FamilyMember>): FamilyMember? {
        if (members.isEmpty()) return null
        return members.minByOrNull { abs(it.referenceWeightKg - weightKg) }
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurement) {
        dao.deleteMeasurement(measurement.toEntity())
    }
}
