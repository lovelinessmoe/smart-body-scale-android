package com.example.dianzicheng.data.repository

import com.example.dianzicheng.data.local.ScaleDao
import com.example.dianzicheng.data.local.toDomain
import com.example.dianzicheng.data.local.toEntity
import com.example.dianzicheng.domain.FamilyMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val dao: ScaleDao) {
    fun getMembers(): Flow<List<FamilyMember>> =
        dao.getAllMembers().map { entities -> entities.map { it.toDomain() } }

    suspend fun saveMember(member: FamilyMember) {
        dao.insertMember(member.toEntity())
    }

    suspend fun deleteMember(member: FamilyMember) {
        dao.deleteMember(member.toEntity())
    }
}
