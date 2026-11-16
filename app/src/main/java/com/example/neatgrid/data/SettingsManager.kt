package com.example.neatgrid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_KEY = intPreferencesKey("theme_index")
        val LEGACY_APPS_PER_ROW_KEY = intPreferencesKey("apps_per_row")
        val LIBRARY_DENSITY_KEY = stringPreferencesKey("library_density")
        val LIBRARY_TITLE_STYLE_KEY = stringPreferencesKey("library_title_style")
        val LIBRARY_COVER_STYLE_KEY = stringPreferencesKey("library_cover_style")
        val ROM_FOLDER_KEY = stringPreferencesKey("rom_folder")
        val RAWG_API_KEY_KEY = stringPreferencesKey("rawg_api_key")
        val THEME_COLOR_KEY = intPreferencesKey("theme_color")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_enabled")
        val AMOLED_BLACK_KEY = booleanPreferencesKey("amoled_black_enabled")
        val LIBRARY_SORT_KEY = stringPreferencesKey("library_sort_mode")
        val LEGACY_SHOW_GAME_NAMES_KEY = booleanPreferencesKey("show_game_names")
        val ROUNDED_COVERS_KEY = booleanPreferencesKey("rounded_covers")
        val EMULATOR_SELECTIONS_KEY = stringSetPreferencesKey("emulator_selections")
        val SCAN_ROM_SUBFOLDERS_KEY = booleanPreferencesKey("scan_rom_subfolders")
        val IIDB_API_KEY_KEY = stringPreferencesKey("iidb_api_key")
        val METADATA_SOURCE_ORDER_KEY = stringPreferencesKey("metadata_source_order")
        val DISABLED_METADATA_SOURCES_KEY = stringSetPreferencesKey("disabled_metadata_sources")
        val STEAMGRIDDB_API_KEY_KEY = stringPreferencesKey("steamgriddb_api_key")
    }

    val libraryDensityFlow: Flow<LibraryDensity> = context.dataStore.data
        .map { preferences ->
            LibraryDensity.fromPreference(
                preferences[LIBRARY_DENSITY_KEY],
                preferences[LEGACY_APPS_PER_ROW_KEY]
            )
        }

    suspend fun saveLibraryDensity(density: LibraryDensity) {
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_DENSITY_KEY] = density.name
        }
    }

    val libraryTitleStyleFlow: Flow<LibraryTitleStyle> = context.dataStore.data
        .map { preferences ->
            LibraryTitleStyle.fromPreference(
                preferences[LIBRARY_TITLE_STYLE_KEY],
                preferences[LEGACY_SHOW_GAME_NAMES_KEY]
            )
        }

    suspend fun saveLibraryTitleStyle(style: LibraryTitleStyle) {
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_TITLE_STYLE_KEY] = style.name
        }
    }

    val libraryCoverStyleFlow: Flow<LibraryCoverStyle> = context.dataStore.data
        .map { preferences ->
            LibraryCoverStyle.fromPreference(preferences[LIBRARY_COVER_STYLE_KEY])
        }

    suspend fun saveLibraryCoverStyle(style: LibraryCoverStyle) {
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_COVER_STYLE_KEY] = style.name
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

    val scanRomSubfoldersFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SCAN_ROM_SUBFOLDERS_KEY] ?: false }

    suspend fun saveScanRomSubfolders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_ROM_SUBFOLDERS_KEY] = enabled
        }
    }

    val rawgApiKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            ApiKeyCipher.decrypt(preferences[RAWG_API_KEY_KEY] ?: "")
        }

    suspend fun saveRawgApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[RAWG_API_KEY_KEY] = ApiKeyCipher.encrypt(apiKey)
        }
    }

    val iidbApiKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            ApiKeyCipher.decrypt(preferences[IIDB_API_KEY_KEY] ?: "")
        }

    suspend fun saveIidbApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[IIDB_API_KEY_KEY] = ApiKeyCipher.encrypt(apiKey)
        }
    }

    val steamGridDbApiKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            ApiKeyCipher.decrypt(preferences[STEAMGRIDDB_API_KEY_KEY] ?: "")
        }

    suspend fun saveSteamGridDbApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[STEAMGRIDDB_API_KEY_KEY] = ApiKeyCipher.encrypt(apiKey)
        }
    }

    val disabledMetadataSourcesFlow: Flow<Set<MetadataSource>> = context.dataStore.data
        .map { preferences ->
            MetadataSource.parseIds(preferences[DISABLED_METADATA_SOURCES_KEY].orEmpty())
        }

    suspend fun saveMetadataSourceEnabled(source: MetadataSource, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val disabled = preferences[DISABLED_METADATA_SOURCES_KEY].orEmpty()
            preferences[DISABLED_METADATA_SOURCES_KEY] =
                if (enabled) disabled - source.id else disabled + source.id
        }
    }

    val metadataSourceOrderFlow: Flow<List<MetadataSource>> = context.dataStore.data
        .map { preferences ->
            MetadataSource.parseOrder(preferences[METADATA_SOURCE_ORDER_KEY])
        }

    suspend fun saveMetadataSourceOrder(order: List<MetadataSource>) {
        context.dataStore.edit { preferences ->
            preferences[METADATA_SOURCE_ORDER_KEY] = MetadataSource.serializeOrder(order)
        }
    }

    val emulatorSelectionsFlow: Flow<Map<String, String>> = context.dataStore.data
        .map { preferences ->
            preferences[EMULATOR_SELECTIONS_KEY].orEmpty().mapNotNull { entry ->
                val separator = entry.indexOf('|')
                if (separator <= 0 || separator == entry.lastIndex) {
                    null
                } else {
                    entry.substring(0, separator) to entry.substring(separator + 1)
                }
            }.toMap()
        }

    suspend fun saveEmulatorSelection(system: String, packageName: String) {
        context.dataStore.edit { preferences ->
            val selections = preferences[EMULATOR_SELECTIONS_KEY].orEmpty()
                .filterNot { it.startsWith("$system|") }
                .toMutableSet()
            if (packageName == "automatic") {
                selections.removeIf { it.startsWith("$system|") }
            } else {
                selections += "$system|$packageName"
            }
            preferences[EMULATOR_SELECTIONS_KEY] = selections
        }
    }
}
