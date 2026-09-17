package com.eetu.duolingoapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "duolingo_settings")

class DuolingoSettingsManager(private val context: Context) {

    companion object {
        val BLOCK_TELEMETRY = booleanPreferencesKey("block_telemetry")
        val DESKTOP_USER_AGENT = booleanPreferencesKey("desktop_user_agent")
        val ENABLE_SPEAKING = booleanPreferencesKey("enable_speaking")
    }

    val blockTelemetryFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BLOCK_TELEMETRY] ?: true
    }

    val desktopUserAgentFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DESKTOP_USER_AGENT] ?: false
    }

    val enableSpeakingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_SPEAKING] ?: true
    }

    suspend fun setBlockTelemetry(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLOCK_TELEMETRY] = enabled
        }
    }

    suspend fun setDesktopUserAgent(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DESKTOP_USER_AGENT] = enabled
        }
    }

    suspend fun setEnableSpeaking(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SPEAKING] = enabled
        }
    }
}
