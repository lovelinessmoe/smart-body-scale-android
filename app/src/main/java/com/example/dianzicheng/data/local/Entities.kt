package com.example.dianzicheng.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dianzicheng.domain.Sex

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sex: Sex,
    val heightCm: Double,
    val birthDateEpochMs: Long,
    val referenceWeightKg: Double
)

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val measuredAtEpochMs: Long,
    val weightKg: Double,
    val impedanceOhm: Double,
    val bmi: Double,
    val bodyFatPct: Double,
    val muscleKg: Double,
    val waterPct: Double,
    val proteinPct: Double,
    val boneMassKg: Double,
    val memberId: String?,
    val memberNameSnapshot: String?
)
