package de.rezeptbuch.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.rezeptbuch.app.domain.model.ThemeMode
import de.rezeptbuch.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[KEY_THEME] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun isSeeded(): Boolean = dataStore.data.first()[KEY_SEEDED] == true

    suspend fun markSeeded() {
        dataStore.edit { it[KEY_SEEDED] = true }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_SEEDED = booleanPreferencesKey("seed_done")
    }
}
