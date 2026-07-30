package com.example.dianzicheng.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dianzicheng.data.local.PreferenceManager
import com.example.dianzicheng.data.repository.ProfileRepository
import com.example.dianzicheng.domain.FamilyMember
import com.example.dianzicheng.domain.Sex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val members: StateFlow<List<FamilyMember>> = repository.getMembers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addMember(name: String, sex: Sex, heightCm: Double, birthDateEpochMs: Long, weightKg: Double) {
        viewModelScope.launch {
            val member = FamilyMember(
                id = UUID.randomUUID().toString(),
                name = name,
                sex = sex,
                heightCm = heightCm,
                birthDateEpochMs = birthDateEpochMs,
                referenceWeightKg = weightKg
            )
            repository.saveMember(member)
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun resetPairing() {
        viewModelScope.launch {
            preferenceManager.clearPairedMac()
        }
    }
}
