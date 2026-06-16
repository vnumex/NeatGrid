package com.example.neatgrid.ui.screens

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxService
import com.example.neatgrid.data.RomRepository
import com.example.neatgrid.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class GameDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val launchBoxService = LaunchBoxService()

    private val _metadata = MutableStateFlow<GameMetadata?>(null)
    val metadata: StateFlow<GameMetadata?> = _metadata.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GameMetadata>>(emptyList())
    val searchResults: StateFlow<List<GameMetadata>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _apiNotConfigured = MutableStateFlow(false)
    val apiNotConfigured: StateFlow<Boolean> = _apiNotConfigured.asStateFlow()

    private fun getCacheFile(packageName: String): File {
        val dir = File(getApplication<Application>().filesDir, "metadata")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$packageName.json")
    }

    private fun cleanSearchQuery(query: String): String {
        var clean = query.replace(Regex("\\s*\\([^)]*\\)"), "")
        clean = clean.replace(Regex("\\s*\\[[^]]*\\]"), "")
        return clean.trim()
    }

    fun loadMetadata(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _apiNotConfigured.value = false

            // 1. Try reading from cache
            val cacheFile = getCacheFile(packageName)
            if (cacheFile.exists()) {
                try {
                    val jsonStr = cacheFile.readText()
                    val json = JSONObject(jsonStr)
                    _metadata.value = GameMetadata.fromJson(json)
                    _isLoading.value = false
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Fetch automatically by app label
            try {
                val label = if (RomRepository.isRom(packageName)) {
                    RomRepository.parse(packageName)?.label ?: "ROM Game"
                } else {
                    val pm = getApplication<Application>().packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }

                val cleanedLabel = cleanSearchQuery(label)
                val results = launchBoxService.searchGames(cleanedLabel)
                val matchedGameWithDetails = if (results.isNotEmpty()) {
                    val matchedGame = results.find { it.title.equals(cleanedLabel, ignoreCase = true) }
                        ?: results.find { it.title.contains(cleanedLabel, ignoreCase = true) }
                        ?: results.first()
                    
                    if (matchedGame.summary?.startsWith("launchbox:") == true) {
                        val suffix = matchedGame.summary.substringAfter("launchbox:")
                        launchBoxService.fetchGameDetails(suffix, matchedGame)
                    } else {
                        matchedGame
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

                saveToCache(packageName, matchedGameWithDetails)
                _metadata.value = matchedGameWithDetails
            } catch (e: PackageManager.NameNotFoundException) {
                _error.value = "App not found on device."
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to fetch metadata."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchOverride(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) return@launch
            _isSearching.value = true
            try {
                val results = launchBoxService.searchGames(query)
                _searchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun applyOverride(packageName: String, game: GameMetadata) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val gameWithDetails = if (game.summary?.startsWith("launchbox:") == true) {
                    val suffix = game.summary.substringAfter("launchbox:")
                    launchBoxService.fetchGameDetails(suffix, game)
                } else {
                    game
                }
                saveToCache(packageName, gameWithDetails)
                _metadata.value = gameWithDetails
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveToCache(packageName: String, game: GameMetadata) {
        try {
            val cacheFile = getCacheFile(packageName)
            cacheFile.writeText(game.toJson().toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearOverride(packageName: String) {
        viewModelScope.launch {
            try {
                val cacheFile = getCacheFile(packageName)
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
                _metadata.value = null
                loadMetadata(packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMetadata(packageName: String, game: GameMetadata) {
        viewModelScope.launch {
            saveToCache(packageName, game)
            _metadata.value = game
        }
    }

    fun launchGame(packageName: String, onLaunchFailed: () -> Unit) {
        if (RomRepository.isRom(packageName)) {
            val romData = RomRepository.parse(packageName)
            if (romData != null) {
                val success = RomRepository.launchRom(getApplication(), romData)
                if (!success) {
                    onLaunchFailed()
                }
            } else {
                onLaunchFailed()
            }
        } else {
            val pm = getApplication<Application>().packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            } else {
                onLaunchFailed()
            }
        }
    }
}
