package com.example.dianzicheng.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dianzicheng.data.backup.WebDavManager
import com.example.dianzicheng.data.health.HealthConnectManager
import com.example.dianzicheng.data.local.PreferenceManager
import com.example.dianzicheng.data.repository.ProfileRepository
import com.example.dianzicheng.domain.FamilyMember
import com.example.dianzicheng.domain.Sex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val webDavManager: WebDavManager? = null,
    private val healthConnectManager: HealthConnectManager? = null
) : ViewModel() {

    val members: StateFlow<List<FamilyMember>> = repository.getMembers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val healthConnectEnabled: StateFlow<Boolean> = preferenceManager.healthConnectEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val webdavUrl: StateFlow<String> = preferenceManager.webdavUrl
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val webdavUsername: StateFlow<String> = preferenceManager.webdavUsername
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val webdavPassword: StateFlow<String> = preferenceManager.webdavPassword
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val lastBackupTime: StateFlow<Long> = preferenceManager.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

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

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setHealthConnectEnabled(enabled)
        }
    }

    fun saveWebdavConfig(url: String, user: String, pass: String) {
        viewModelScope.launch {
            preferenceManager.saveWebdavConfig(url, user, pass)
        }
    }

    fun testWebdavConnection(url: String, user: String, pass: String) {
        val manager = webDavManager ?: return
        viewModelScope.launch {
            _isOperating.value = true
            val result = manager.testConnection(url, user, pass)
            _isOperating.value = false
            _statusMessage.value = result.getOrElse { it.localizedMessage }
        }
    }

    fun backupData() {
        val manager = webDavManager ?: return
        viewModelScope.launch {
            _isOperating.value = true
            val url = webdavUrl.value
            val user = webdavUsername.value
            val pass = webdavPassword.value
            if (url.isBlank()) {
                _isOperating.value = false
                _statusMessage.value = "请先配置 WebDAV 服务器地址"
                return@launch
            }
            val result = manager.backupData(url, user, pass)
            _isOperating.value = false
            result.onSuccess { msg ->
                val now = System.currentTimeMillis()
                preferenceManager.saveLastBackupTime(now)
                _statusMessage.value = msg
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage
            }
        }
    }

    fun restoreData() {
        val manager = webDavManager ?: return
        viewModelScope.launch {
            _isOperating.value = true
            val url = webdavUrl.value
            val user = webdavUsername.value
            val pass = webdavPassword.value
            if (url.isBlank()) {
                _isOperating.value = false
                _statusMessage.value = "请先配置 WebDAV 服务器地址"
                return@launch
            }
            val result = manager.restoreData(url, user, pass)
            _isOperating.value = false
            _statusMessage.value = result.getOrElse { it.localizedMessage }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun isHealthConnectAvailable(): Boolean {
        return healthConnectManager?.isAvailable() == true
    }
}
