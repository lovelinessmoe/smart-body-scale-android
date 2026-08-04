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

    fun getMembers(): Flow<List<FamilyMember>> =
        dao.getAllMembers().map { entities -> entities.map { it.toDomain() } }

    suspend fun saveMeasurement(
        measurement: BodyMeasurement,
        targetMember: FamilyMember? = null,
        existingId: String? = null
    ): FamilyMember? {
        val members = dao.getMembersList().map { it.toDomain() }
        val memberToBind = targetMember
            ?: members.firstOrNull { it.id == measurement.memberId }
            ?: findBestMember(measurement.weightKg, members)
        
        val finalMeasurement = measurement.copy(
            id = existingId ?: measurement.id.ifEmpty { UUID.randomUUID().toString() },
            memberId = memberToBind?.id,
            memberNameSnapshot = memberToBind?.name
        )
        dao.insertMeasurement(finalMeasurement.toEntity())
        
        // Update member's reference weight to keep matching accurate for next time
        memberToBind?.let { member ->
            dao.insertMember(member.copy(referenceWeightKg = measurement.weightKg).toEntity())
        }

        return memberToBind
    }

    suspend fun bindMeasurementToMember(measurement: BodyMeasurement, member: FamilyMember): BodyMeasurement {
        val updated = measurement.copy(
            memberId = member.id,
            memberNameSnapshot = member.name
        )
        dao.insertMeasurement(updated.toEntity())
        dao.insertMember(member.copy(referenceWeightKg = measurement.weightKg).toEntity())
        return updated
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
