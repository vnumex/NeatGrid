package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.AppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddGameViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository = AppsRepository(application)

    private val _appsList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appsList: StateFlow<List<AppInfo>> = _appsList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _appsList.value = repository.getLaunchableApps()
            _isLoading.value = false
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

    fun selectAllApp(apps: List<AppInfo>) {
        _selectedApps.value = apps.map { it.packageName }.toSet()
    }
    fun clearSelectedApps() {
        _selectedApps.value = emptySet()
    }

}