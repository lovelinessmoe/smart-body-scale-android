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

    val isPairingComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PAIRING_COMPLETE] ?: false
        }

    val pairedMac: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PAIRED_MAC]
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
}
