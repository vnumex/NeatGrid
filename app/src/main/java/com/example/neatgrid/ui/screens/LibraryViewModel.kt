package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.AppsRepository
import com.example.neatgrid.data.LibraryRepository
import com.example.neatgrid.data.RomRepository
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appsRepository = AppsRepository(application)
    private val libraryRepository = LibraryRepository(application)

    private val _libraryList = MutableStateFlow<List<AppInfo>>(emptyList())
    val libraryList: StateFlow<List<AppInfo>> = _libraryList.asStateFlow()

    private val _prefetchingStates = MutableStateFlow<Set<String>>(emptySet())
    val prefetchingStates: StateFlow<Set<String>> = _prefetchingStates.asStateFlow()

    private val prefetchingPackages = ConcurrentHashMap.newKeySet<String>()

    init {
        observeSavedPackages()
    }

    private fun observeSavedPackages() {
        viewModelScope.launch {
            libraryRepository.savedPackageNames.collect { savedPackages ->
                reloadLibrary(savedPackages)
            }
        }
    }

    private fun getCacheFile(packageName: String): File {
        val dir = File(getApplication<Application>().filesDir, "metadata")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$packageName.json")
    }

    private fun reloadLibrary(savedPackageNames: Set<String>) {
        viewModelScope.launch {
            val apps = savedPackageNames.filter { !RomRepository.isRom(it) }.mapNotNull { pkg ->
                val app = appsRepository.getAppInfo(pkg) ?: return@mapNotNull null
                val cacheFile = getCacheFile(pkg)
                val coverUrl = if (cacheFile.exists()) {
                    try {
                        val jsonStr = cacheFile.readText()
                        val json = JSONObject(jsonStr)
                        json.optString("coverUrl", "").takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                app.copy(coverUrl = coverUrl)
            }
            
            val roms = savedPackageNames.filter { RomRepository.isRom(it) }.mapNotNull { romPackage ->
                val romData = RomRepository.parse(romPackage) ?: return@mapNotNull null
                val pm = getApplication<Application>().packageManager
                val iconDrawable = try {
                    pm.getApplicationIcon(romData.emulatorPackage)
                } catch (e: Exception) {
                    try {
                        pm.getApplicationIcon(getApplication<Application>().packageName)
                    } catch (e2: Exception) {
                        null
                    }
                }
                val cacheFile = getCacheFile(romPackage)
                val coverUrl = if (cacheFile.exists()) {
                    try {
                        val jsonStr = cacheFile.readText()
                        val json = JSONObject(jsonStr)
                        json.optString("coverUrl", "").takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                if (iconDrawable != null) {
                    AppInfo(
                        label = romData.label,
                        packageName = romPackage,
                        icon = iconDrawable,
                        coverUrl = coverUrl
                    )
                } else {
                    null
                }
            }

            _libraryList.value = apps + roms

            // Trigger background prefetching for uncached items
            (apps + roms).forEach { appInfo ->
                if (!getCacheFile(appInfo.packageName).exists() && prefetchingPackages.add(appInfo.packageName)) {
                    _prefetchingStates.update { it + appInfo.packageName }
                    launch {
                        prefetchMetadata(appInfo.packageName, appInfo.label)
                    }
                }
            }
        }
    }

    private suspend fun prefetchMetadata(packageName: String, label: String) {
        try {
            val cleanedLabel = cleanSearchQuery(label)
            val launchBoxService = LaunchBoxService()
            val results = launchBoxService.searchGames(cleanedLabel)
            val matchedGame = if (results.isNotEmpty()) {
                val bestMatch = results.find { it.title.equals(cleanedLabel, ignoreCase = true) }
                    ?: results.find { it.title.contains(cleanedLabel, ignoreCase = true) }
                    ?: results.first()

                if (bestMatch.summary?.startsWith("launchbox:") == true) {
                    val suffix = bestMatch.summary.substringAfter("launchbox:")
                    launchBoxService.fetchGameDetails(suffix, bestMatch)
                } else {
                    bestMatch
                }
            } else {
                GameMetadata(
                    title = label,
                    summary = null,
                    rating = null,
                    releaseDate = null,
                    genres = emptyList(),
                    platforms = emptyList(),
                    coverUrl = null,
                    screenshotUrls = emptyList()
                )
            }

            val cacheFile = getCacheFile(packageName)
            cacheFile.writeText(matchedGame.toJson().toString())
            
            // Reload manually to refresh this specific cover art
            val savedPackages = libraryRepository.savedPackageNames.first()
            reloadLibrary(savedPackages)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            prefetchingPackages.remove(packageName)
            _prefetchingStates.update { it - packageName }
        }
    }

    private fun cleanSearchQuery(query: String): String {
        var clean = query.replace(Regex("\\s*\\([^)]*\\)"), "")
        clean = clean.replace(Regex("\\s*\\[[^]]*\\]"), "")
        return clean.trim()
    }

    fun addApps(apps: List<AppInfo>) {
        viewModelScope.launch {
            libraryRepository.saveApps(apps)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            libraryRepository.removeApp(packageName)
        }
    }
}