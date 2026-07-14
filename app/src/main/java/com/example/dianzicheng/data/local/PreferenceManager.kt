package com.example.dianzicheng.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val PAIRING_COMPLETE = booleanPreferencesKey("pairing_complete")

    val isPairingComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PAIRING_COMPLETE] ?: false
        }

    suspend fun setPairingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PAIRING_COMPLETE] = complete
        }
    }
}
