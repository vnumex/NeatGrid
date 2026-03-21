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

    val savedPackageNames: Flow<Set<String>> = context.libraryDataStore.data
        .map { preferences ->
            preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
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
        }
    }

    suspend fun removeApp(packageName: String) {
        context.libraryDataStore.edit { preferences ->
            val current = preferences[LIBRARY_PACKAGE_NAMES] ?: emptySet()
            preferences[LIBRARY_PACKAGE_NAMES] = current - packageName
        }
    }
}