package com.saurabh.sar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class SarSettings(
    val warningMinutes: List<Int> = listOf(5, 1),
    val historyRetentionDays: Int = 30,
    val onboardingDone: Boolean = false,
    val defaultsCreated: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "sar_settings")

class SettingsStore(private val context: Context) {
    private val warnings = stringPreferencesKey("warning_minutes")
    private val retention = intPreferencesKey("history_retention_days")
    private val onboarding = booleanPreferencesKey("onboarding_done")
    private val defaults = booleanPreferencesKey("defaults_created")

    val settings: Flow<SarSettings> = context.dataStore.data.map { p ->
        SarSettings(
            warningMinutes = p[warnings]?.let { s -> s.split(',').mapNotNull { it.trim().toIntOrNull() } } ?: listOf(5, 1),
            historyRetentionDays = p[retention] ?: 30,
            onboardingDone = p[onboarding] ?: false,
            defaultsCreated = p[defaults] ?: false,
        )
    }

    suspend fun current(): SarSettings = settings.first()

    suspend fun setWarningMinutes(list: List<Int>) = context.dataStore.edit { it[warnings] = list.joinToString(",") }
    suspend fun setRetentionDays(days: Int) = context.dataStore.edit { it[retention] = days }
    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[onboarding] = done }
    suspend fun setDefaultsCreated(done: Boolean) = context.dataStore.edit { it[defaults] = done }
}
