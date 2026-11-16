package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.SettingsManager
import com.example.neatgrid.data.IidbService
import com.example.neatgrid.data.ImageCache
import com.example.neatgrid.data.LibraryCoverStyle
import com.example.neatgrid.data.LibraryDensity
import com.example.neatgrid.data.LibraryTitleStyle
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.SteamGridDbService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val iidbService = IidbService()
    private val steamGridDbService = SteamGridDbService()
    private val imageCache = ImageCache(application)

    private val _imageCacheBytes = MutableStateFlow<Long?>(null)
    val imageCacheBytes: StateFlow<Long?> = _imageCacheBytes.asStateFlow()

    fun refreshImageCacheSize() {
        viewModelScope.launch {
            _imageCacheBytes.value = imageCache.sizeBytes()
        }
    }

    fun clearImageCache(onCleared: () -> Unit) {
        viewModelScope.launch {
            imageCache.clear()
            _imageCacheBytes.value = imageCache.sizeBytes()
            onCleared()
        }
    }

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

    val libraryDensity: StateFlow<LibraryDensity> = settingsManager.libraryDensityFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryDensity.COMPACT
        )

    fun setLibraryDensity(density: LibraryDensity) {
        viewModelScope.launch {
            settingsManager.saveLibraryDensity(density)
        }
    }

    val libraryTitleStyle: StateFlow<LibraryTitleStyle> = settingsManager.libraryTitleStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryTitleStyle.BELOW
        )

    fun setLibraryTitleStyle(style: LibraryTitleStyle) {
        viewModelScope.launch {
            settingsManager.saveLibraryTitleStyle(style)
        }
    }

    val libraryCoverStyle: StateFlow<LibraryCoverStyle> = settingsManager.libraryCoverStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryCoverStyle.BOX_ART
        )

    fun setLibraryCoverStyle(style: LibraryCoverStyle) {
        viewModelScope.launch {
            settingsManager.saveLibraryCoverStyle(style)
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

    val steamGridDbApiKey: StateFlow<String> = settingsManager.steamGridDbApiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun setSteamGridDbApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsManager.saveSteamGridDbApiKey(apiKey)
        }
    }

    fun verifySteamGridDbKey(apiKey: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(steamGridDbService.verifyKey(apiKey))
        }
    }

    val disabledMetadataSources: StateFlow<Set<MetadataSource>> =
        settingsManager.disabledMetadataSourcesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    fun setMetadataSourceEnabled(source: MetadataSource, enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveMetadataSourceEnabled(source, enabled)
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
