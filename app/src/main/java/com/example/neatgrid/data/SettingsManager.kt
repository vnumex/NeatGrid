package com.example.neatgrid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val RAWG_API_KEY_KEY = stringPreferencesKey("rawg_api_key")
        val THEME_COLOR_KEY = intPreferencesKey("theme_color")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_enabled")
        val AMOLED_BLACK_KEY = booleanPreferencesKey("amoled_black_enabled")
        val LIBRARY_SORT_KEY = stringPreferencesKey("library_sort_mode")
        val SHOW_GAME_NAMES_KEY = booleanPreferencesKey("show_game_names")
        val ROUNDED_COVERS_KEY = booleanPreferencesKey("rounded_covers")
    }

    val appsPerRowFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            (preferences[APPS_PER_ROW_KEY] ?: 5).coerceIn(2, 8)
        }

    suspend fun saveAppsPerRow(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[APPS_PER_ROW_KEY] = count.coerceIn(2, 8)
        }
    }

    val showGameNamesFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_GAME_NAMES_KEY] ?: true
        }

    suspend fun saveShowGameNames(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_GAME_NAMES_KEY] = enabled
        }
    }

    val roundedCoversFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ROUNDED_COVERS_KEY] ?: true
        }

    suspend fun saveRoundedCovers(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ROUNDED_COVERS_KEY] = enabled
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

    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val amoledBlackEnabled = preferences[AMOLED_BLACK_KEY]
                ?: (preferences[THEME_COLOR_KEY] == 2)
            val dynamicColorEnabled = preferences[DYNAMIC_COLOR_KEY]
                ?: when (preferences[THEME_COLOR_KEY]) {
                    1, 2 -> false
                    else -> true
                }
            dynamicColorEnabled && !amoledBlackEnabled
        }

    suspend fun saveDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val amoledBlackEnabled = preferences[AMOLED_BLACK_KEY]
                ?: (preferences[THEME_COLOR_KEY] == 2)
            preferences[DYNAMIC_COLOR_KEY] = enabled && !amoledBlackEnabled
        }
    }

    val amoledBlackFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AMOLED_BLACK_KEY] ?: (preferences[THEME_COLOR_KEY] == 2)
        }

    suspend fun saveAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AMOLED_BLACK_KEY] = enabled
            preferences[DYNAMIC_COLOR_KEY] = false
        }
    }

    val librarySortModeFlow: Flow<LibrarySortMode> = context.dataStore.data
        .map { preferences ->
            LibrarySortMode.fromPreference(preferences[LIBRARY_SORT_KEY])
        }

    suspend fun saveLibrarySortMode(sortMode: LibrarySortMode) {
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_SORT_KEY] = sortMode.name
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

    val rawgApiKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[RAWG_API_KEY_KEY] ?: ""
        }

    suspend fun saveRawgApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[RAWG_API_KEY_KEY] = apiKey
        }
    }
}
