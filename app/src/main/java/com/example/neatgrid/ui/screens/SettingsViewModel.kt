package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    val themeIndex: StateFlow<Int> = settingsManager.themeIndexFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setTheme(index: Int) {
        viewModelScope.launch {
            settingsManager.saveThemeIndex(index)
        }
    }

    val appsPerRow: StateFlow<Int> = settingsManager.appsPerRowFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5
        )

    fun setAppsPerRow(count: Int) {
        viewModelScope.launch {
            settingsManager.saveAppsPerRow(count)
        }
    }

    val romFolder: StateFlow<String> = settingsManager.romFolderFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun setRomFolder(folderPath: String) {
        viewModelScope.launch {
            settingsManager.saveRomFolder(folderPath)
        }
    }
}