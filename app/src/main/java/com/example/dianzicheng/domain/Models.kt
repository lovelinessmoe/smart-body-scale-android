package com.example.dianzicheng.domain

enum class Sex { FEMALE, MALE }

data class FamilyMember(
    val id: String,
    val name: String,
    val sex: Sex,
    val heightCm: Double,
    val birthDateEpochMs: Long,
    val referenceWeightKg: Double,
)

data class BodyMeasurement(
    val id: String,
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
    val memberNameSnapshot: String?,
)
