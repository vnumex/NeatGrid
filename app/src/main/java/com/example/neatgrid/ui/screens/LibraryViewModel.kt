package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.AppsRepository
import com.example.neatgrid.data.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appsRepository = AppsRepository(application)
    private val libraryRepository = LibraryRepository(application)

    private val _libraryList = MutableStateFlow<List<AppInfo>>(emptyList())
    val libraryList: StateFlow<List<AppInfo>> = _libraryList.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            val savedPackageNames = libraryRepository.savedPackageNames.first()
            val allApps = appsRepository.getLaunchableApps()
            _libraryList.value = allApps.filter { it.packageName in savedPackageNames }
        }
    }

    fun addApps(apps: List<AppInfo>) {
        viewModelScope.launch {
            libraryRepository.saveApps(apps)
            _libraryList.update { current ->
                val existing = current.map { it.packageName }.toHashSet()
                current + apps.filter { !existing.contains(it.packageName) }
            }
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            libraryRepository.removeApp(packageName)
            _libraryList.update { current ->
                current.filter { it.packageName != packageName }
            }
        }
    }
}