package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.AppsRepository
import com.example.neatgrid.data.RomFile
import com.example.neatgrid.data.RomRepository
import com.example.neatgrid.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddGameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppsRepository(application)
    private val romRepository = RomRepository()
    private val settingsManager = SettingsManager(application)

    private val _appsList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appsList: StateFlow<List<AppInfo>> = _appsList.asStateFlow()

    private val _romsList = MutableStateFlow<List<RomFile>>(emptyList())
    val romsList: StateFlow<List<RomFile>> = _romsList.asStateFlow()

    private val _romFolderUri = MutableStateFlow("")
    val romFolderUri: StateFlow<String> = _romFolderUri.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    private val _selectedRoms = MutableStateFlow<Set<String>>(emptySet())
    val selectedRoms: StateFlow<Set<String>> = _selectedRoms.asStateFlow()

    init {
        viewModelScope.launch {
            val uri = settingsManager.romFolderFlow.first()
            _romFolderUri.value = uri
            _isLoading.value = false
        }
    }

    fun scanApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _appsList.value = repository.getLaunchableApps()
            _isLoading.value = false
        }
    }

    fun scanRoms() {
        viewModelScope.launch {
            _isLoading.value = true
            val uri = settingsManager.romFolderFlow.first()
            val scanSubfolders = settingsManager.scanRomSubfoldersFlow.first()
            _romFolderUri.value = uri
            if (uri.isNotEmpty()) {
                _romsList.value = romRepository.scanRomFolder(getApplication(), uri, scanSubfolders)
            } else {
                _romsList.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun setRomFolder(uriString: String) {
        viewModelScope.launch {
            settingsManager.saveRomFolder(uriString)
            _romFolderUri.value = uriString
            if (uriString.isNotEmpty()) {
                val scanSubfolders = settingsManager.scanRomSubfoldersFlow.first()
                _isLoading.value = true
                _romsList.value =
                    romRepository.scanRomFolder(getApplication(), uriString, scanSubfolders)
                _isLoading.value = false
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        val currentSelection = _selectedApps.value.toMutableSet()
        if (currentSelection.contains(packageName)) {
            currentSelection.remove(packageName)
        } else {
            currentSelection.add(packageName)
        }
        _selectedApps.value = currentSelection
    }

    fun selectAllApps(apps: List<AppInfo>) {
        _selectedApps.value = apps.map { it.packageName }.toSet()
    }

    fun clearSelectedApps() {
        _selectedApps.value = emptySet()
    }

    fun toggleRomSelection(uriString: String) {
        val current = _selectedRoms.value.toMutableSet()
        if (current.contains(uriString)) {
            current.remove(uriString)
        } else {
            current.add(uriString)
        }
        _selectedRoms.value = current
    }

    fun selectAllRoms(roms: List<RomFile>) {
        _selectedRoms.value = roms.map { it.uriString }.toSet()
    }

    fun clearSelectedRoms() {
        _selectedRoms.value = emptySet()
    }
}
