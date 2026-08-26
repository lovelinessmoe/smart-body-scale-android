package com.example.dianzicheng.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val PAIRING_COMPLETE = booleanPreferencesKey("pairing_complete")
    private val PAIRED_MAC = stringPreferencesKey("paired_mac")
    private val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
    private val WEBDAV_URL = stringPreferencesKey("webdav_url")
    private val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
    private val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
    private val LAST_BACKUP_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_backup_time")

    val isPairingComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PAIRING_COMPLETE] ?: false
        }

    val pairedMac: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PAIRED_MAC]
        }

    val healthConnectEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HEALTH_CONNECT_ENABLED] ?: false
        }

    val webdavUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[WEBDAV_URL] ?: ""
        }

    val webdavUsername: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[WEBDAV_USERNAME] ?: ""
        }

    val webdavPassword: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[WEBDAV_PASSWORD] ?: ""
        }

    val lastBackupTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_BACKUP_TIME] ?: 0L
        }

    suspend fun setPairingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PAIRING_COMPLETE] = complete
        }
    }

    suspend fun savePairedMac(mac: String) {
        context.dataStore.edit { preferences ->
            preferences[PAIRED_MAC] = mac
        }
    }

    suspend fun clearPairedMac() {
        context.dataStore.edit { preferences ->
            preferences.remove(PAIRED_MAC)
            preferences[PAIRING_COMPLETE] = false
        }
    }

    suspend fun setHealthConnectEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HEALTH_CONNECT_ENABLED] = enabled
        }
    }

    suspend fun saveWebdavConfig(url: String, user: String, pass: String) {
        context.dataStore.edit { preferences ->
            preferences[WEBDAV_URL] = url
            preferences[WEBDAV_USERNAME] = user
            preferences[WEBDAV_PASSWORD] = pass
        }
    }

    suspend fun saveLastBackupTime(timeMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_TIME] = timeMs
        }
    }
}
