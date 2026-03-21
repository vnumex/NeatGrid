package com.example.neatgrid.ui.screens

import androidx.lifecycle.ViewModel
import com.example.neatgrid.data.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LibraryViewModel : ViewModel() {
    private val _libraryList = MutableStateFlow<List<AppInfo>>(emptyList())
    val libraryList: StateFlow<List<AppInfo>> = _libraryList.asStateFlow()

    fun addApps(apps: List<AppInfo>) {
        _libraryList.update { current ->
            val existing = current.map { it.packageName }.toHashSet()
            current + apps.filter { !existing.contains(it.packageName) }
        }
    }
}