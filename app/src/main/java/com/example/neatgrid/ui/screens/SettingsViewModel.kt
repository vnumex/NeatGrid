package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.SettingsManager
import com.example.neatgrid.data.IidbService
import com.example.neatgrid.data.MetadataSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val iidbService = IidbService()

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

    val scanRomSubfolders: StateFlow<Boolean> = settingsManager.scanRomSubfoldersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setScanRomSubfolders(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveScanRomSubfolders(enabled)
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

    val iidbApiKey: StateFlow<String> = settingsManager.iidbApiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun setIidbApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsManager.saveIidbApiKey(apiKey)
        }
    }

    fun verifyIidbKey(apiKey: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(iidbService.verifyKey(apiKey))
        }
    }

    val metadataSourceOrder: StateFlow<List<MetadataSource>> = settingsManager.metadataSourceOrderFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MetadataSource.DEFAULT_ORDER
        )

    fun moveMetadataSource(index: Int, delta: Int) {
        val current = metadataSourceOrder.value
        val targetIndex = index + delta
        if (index !in current.indices || targetIndex !in current.indices || delta == 0) return
        val updated = current.toMutableList()
        val item = updated[index]
        updated[index] = updated[targetIndex]
        updated[targetIndex] = item
        viewModelScope.launch {
            settingsManager.saveMetadataSourceOrder(updated)
        }
    }

    val emulatorSelections: StateFlow<Map<String, String>> = settingsManager.emulatorSelectionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun setEmulatorSelection(system: String, packageName: String) {
        viewModelScope.launch {
            settingsManager.saveEmulatorSelection(system, packageName)
        }
    }
}
