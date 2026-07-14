package com.example.dianzicheng.data.local

import com.example.dianzicheng.domain.BodyMeasurement
import com.example.dianzicheng.domain.FamilyMember
import com.example.dianzicheng.domain.Sex

fun MemberEntity.toDomain() = FamilyMember(
    id = id,
    name = name,
    sex = sex,
    heightCm = heightCm,
    birthDateEpochMs = birthDateEpochMs,
    referenceWeightKg = referenceWeightKg
)

fun FamilyMember.toEntity() = MemberEntity(
    id = id,
    name = name,
    sex = sex,
    heightCm = heightCm,
    birthDateEpochMs = birthDateEpochMs,
    referenceWeightKg = referenceWeightKg
)

fun MeasurementEntity.toDomain() = BodyMeasurement(
    id = id,
    measuredAtEpochMs = measuredAtEpochMs,
    weightKg = weightKg,
    impedanceOhm = impedanceOhm,
    bmi = bmi,
    bodyFatPct = bodyFatPct,
    muscleKg = muscleKg,
    waterPct = waterPct,
    proteinPct = proteinPct,
    boneMassKg = boneMassKg,
    memberId = memberId,
    memberNameSnapshot = memberNameSnapshot
)

fun BodyMeasurement.toEntity() = MeasurementEntity(
    id = id,
    measuredAtEpochMs = measuredAtEpochMs,
    weightKg = weightKg,
    impedanceOhm = impedanceOhm,
    bmi = bmi,
    bodyFatPct = bodyFatPct,
    muscleKg = muscleKg,
    waterPct = waterPct,
    proteinPct = proteinPct,
    boneMassKg = boneMassKg,
    memberId = memberId,
    memberNameSnapshot = memberNameSnapshot
)
