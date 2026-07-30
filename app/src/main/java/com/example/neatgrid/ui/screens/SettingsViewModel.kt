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

    val dynamicColorEnabled: StateFlow<Boolean> = settingsManager.dynamicColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveDynamicColor(enabled)
        }
    }

    val amoledBlackEnabled: StateFlow<Boolean> = settingsManager.amoledBlackFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveAmoledBlack(enabled)
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

    val showGameNames: StateFlow<Boolean> = settingsManager.showGameNamesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setShowGameNames(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveShowGameNames(enabled)
        }
    }

    val roundedCovers: StateFlow<Boolean> = settingsManager.roundedCoversFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setRoundedCovers(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveRoundedCovers(enabled)
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

    val rawgApiKey: StateFlow<String> = settingsManager.rawgApiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun setRawgApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsManager.saveRawgApiKey(apiKey)
        }
    }
}
