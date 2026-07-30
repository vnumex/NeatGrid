package com.example.neatgrid.ui.screens

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.CachedGameMetadata
import com.example.neatgrid.data.Emulator
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.RomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class GameDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val metadataRepository = MetadataRepository(application)
    private var metadataJob: Job? = null
    private var metadataRequestId = 0
    private var searchJob: Job? = null
    private var searchRequestId = 0

    private val _metadata = MutableStateFlow<GameMetadata?>(null)
    val metadata: StateFlow<GameMetadata?> = _metadata.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GameMetadata>>(emptyList())
    val searchResults: StateFlow<List<GameMetadata>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMetadata(packageName: String) {
        metadataJob?.cancel()
        val requestId = ++metadataRequestId
        metadataJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val cached = withContext(Dispatchers.IO) {
                    metadataRepository.readCached(packageName)
                }
                if (cached != null) {
                    applyMetadata(cached)
                } else {
                    refresh(packageName)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = errorMessage(e)
            } finally {
                if (requestId == metadataRequestId) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun refreshMetadata(packageName: String) {
        metadataJob?.cancel()
        val requestId = ++metadataRequestId
        metadataJob = viewModelScope.launch {
            val hasMetadata = _metadata.value != null
            _isLoading.value = !hasMetadata
            _isRefreshing.value = hasMetadata
            _error.value = null
            try {
                refresh(packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = errorMessage(e)
            } finally {
                if (requestId == metadataRequestId) {
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
            }
        }
    }

    private suspend fun refresh(packageName: String) {
        val result = metadataRepository.lookup(
            label = resolveLabel(packageName),
            preferredPlatforms = resolvePlatforms(packageName)
        )
        metadataRepository.save(packageName, result)
        applyMetadata(result)
    }

    private fun applyMetadata(cachedMetadata: CachedGameMetadata) {
        _metadata.value = cachedMetadata.metadata
        _error.value = if (cachedMetadata.status == MetadataCacheStatus.NOT_FOUND) {
            "No confident metadata match was found. You can search manually or edit the details."
        } else {
            null
        }
    }

    private fun resolveLabel(packageName: String): String {
        if (RomRepository.isRom(packageName)) {
            return RomRepository.parse(packageName)?.label ?: "ROM Game"
        }
        val packageManager = getApplication<Application>().packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        return packageManager.getApplicationLabel(appInfo).toString()
    }

    private fun resolvePlatforms(packageName: String): Set<String> {
        if (!RomRepository.isRom(packageName)) return setOf("Android")
        val emulatorPackage = RomRepository.parse(packageName)?.emulatorPackage ?: return emptySet()
        return Emulator.entries
            .firstOrNull { it.packageName == emulatorPackage }
            ?.systems
            ?.filterNot { it == "Multi-System" }
            ?.toSet()
            .orEmpty()
    }

    fun searchOverride(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        val requestId = ++searchRequestId
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResults.value = emptyList()
            try {
                _searchResults.value = metadataRepository.search(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _searchError.value = errorMessage(e)
            } finally {
                if (requestId == searchRequestId) {
                    _isSearching.value = false
                }
            }
        }
    }

    fun applyOverride(packageName: String, game: GameMetadata) {
        metadataJob?.cancel()
        val requestId = ++metadataRequestId
        metadataJob = viewModelScope.launch {
            val hasMetadata = _metadata.value != null
            _isLoading.value = !hasMetadata
            _isRefreshing.value = hasMetadata
            _error.value = null
            try {
                val gameWithDetails = metadataRepository.resolve(game)
                metadataRepository.save(
                    packageName,
                    CachedGameMetadata(gameWithDetails, MetadataCacheStatus.CUSTOM)
                )
                _metadata.value = gameWithDetails
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = errorMessage(e)
            } finally {
                if (requestId == metadataRequestId) {
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun updateMetadata(packageName: String, game: GameMetadata) {
        metadataJob?.cancel()
        ++metadataRequestId
        metadataJob = viewModelScope.launch {
            try {
                metadataRepository.save(
                    packageName,
                    CachedGameMetadata(game, MetadataCacheStatus.CUSTOM)
                )
                _metadata.value = game
                _error.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = errorMessage(e)
            }
        }
    }

    private fun errorMessage(error: Throwable): String {
        return when (error) {
            is PackageManager.NameNotFoundException -> "App not found on this device."
            is IOException -> "Could not connect to the metadata service. Check your connection and try again."
            else -> error.localizedMessage ?: "Could not load game metadata."
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
