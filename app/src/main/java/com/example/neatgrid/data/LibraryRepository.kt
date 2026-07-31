package com.example.neatgrid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDataStore by preferencesDataStore("library_prefs")

class LibraryRepository(private val context: Context) {
    private val LIBRARY_PACKAGE_NAMES = stringSetPreferencesKey("library_package_names")
    private val EXCLUDED_DETECTED_GAME_PACKAGES = stringSetPreferencesKey("excluded_detected_game_packages")
    private val HIDDEN_PACKAGE_NAMES = stringSetPreferencesKey("hidden_package_names")

    val savedPackageNames: Flow<Set<String>> = context.libraryDataStore.data
        .map { preferences ->
            preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
        }

    val excludedDetectedGamePackages: Flow<Set<String>> = context.libraryDataStore.data
        .map { preferences ->
            preferences[EXCLUDED_DETECTED_GAME_PACKAGES] ?: emptySet()
        }

    val hiddenPackageNames: Flow<Set<String>> = context.libraryDataStore.data
        .map { preferences ->
            preferences[HIDDEN_PACKAGE_NAMES] ?: emptySet()
        }

    suspend fun saveApps(apps: List<AppInfo>) {
        context.libraryDataStore.edit { preferences ->
            val existingPackages = preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
            val newPackages = apps.map { it.packageName }.toSet()
            preferences[LIBRARY_PACKAGE_NAMES] = existingPackages + newPackages
        }
    }

    suspend fun clearApps() {
        context.libraryDataStore.edit { preferences ->
            preferences[LIBRARY_PACKAGE_NAMES] = emptySet()
            preferences[HIDDEN_PACKAGE_NAMES] = emptySet()
        }
    }

    suspend fun removeApp(packageName: String) {
        removeApps(setOf(packageName))
    }

    suspend fun removeApps(packageNames: Set<String>) {
        if (packageNames.isEmpty()) return
        context.libraryDataStore.edit { preferences ->
            val current = preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
            preferences[LIBRARY_PACKAGE_NAMES] = current - packageNames
            val hidden = preferences[HIDDEN_PACKAGE_NAMES] ?: emptySet()
            preferences[HIDDEN_PACKAGE_NAMES] = hidden - packageNames
        }
    }

    suspend fun hideApp(packageName: String) {
        context.libraryDataStore.edit { preferences ->
            val saved = preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
            if (packageName in saved) {
                val hidden = preferences[HIDDEN_PACKAGE_NAMES] ?: emptySet()
                preferences[HIDDEN_PACKAGE_NAMES] = hidden + packageName
            }
        }
    }

    suspend fun unhideApp(packageName: String) {
        context.libraryDataStore.edit { preferences ->
            val hidden = preferences[HIDDEN_PACKAGE_NAMES] ?: emptySet()
            preferences[HIDDEN_PACKAGE_NAMES] = hidden - packageName
        }
    }

    suspend fun excludeDetectedGames(packageNames: Set<String>) {
        if (packageNames.isEmpty()) return
        context.libraryDataStore.edit { preferences ->
            val excluded = preferences[EXCLUDED_DETECTED_GAME_PACKAGES] ?: emptySet()
            val saved = preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
            val hidden = preferences[HIDDEN_PACKAGE_NAMES] ?: emptySet()
            preferences[EXCLUDED_DETECTED_GAME_PACKAGES] = excluded + packageNames
            preferences[LIBRARY_PACKAGE_NAMES] = saved + packageNames
            preferences[HIDDEN_PACKAGE_NAMES] = hidden + packageNames
        }
    }
}
