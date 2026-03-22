package com.example.neatgrid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_KEY = intPreferencesKey("theme_index")
        val APPS_PER_ROW_KEY = intPreferencesKey("apps_per_row")
        val ROM_FOLDER_KEY = stringPreferencesKey("rom_folder")
    }

    val appsPerRowFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[APPS_PER_ROW_KEY] ?: 5
        }

    suspend fun saveAppsPerRow(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[APPS_PER_ROW_KEY] = count
        }
    }

    val themeIndexFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: 0
        }

    suspend fun saveThemeIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = index
        }
    }

    val romFolderFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ROM_FOLDER_KEY] ?: ""
        }
    suspend fun saveRomFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            preferences[ROM_FOLDER_KEY] = folderPath
        }
    }
}