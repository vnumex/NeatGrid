package com.example.neatgrid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.data.ArtworkOptions
import com.example.neatgrid.data.CachedGameMetadata
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.withArtwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ArtworkPickerState(
    val title: String = "",
    val isLoading: Boolean = true,
    val options: ArtworkOptions = ArtworkOptions(),
    val selected: Map<ArtworkKind, String?> = emptyMap(),
    val isSaving: Boolean = false,
    val error: String? = null
)

class ArtworkPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val metadataRepository = MetadataRepository(application)
    private var cached: CachedGameMetadata? = null
    private var loadedPackage: String? = null

    private val _state = MutableStateFlow(ArtworkPickerState())
    val state: StateFlow<ArtworkPickerState> = _state.asStateFlow()

    fun load(packageName: String) {
        if (loadedPackage == packageName) return
        loadedPackage = packageName
        viewModelScope.launch {
            _state.value = ArtworkPickerState()
            val current = withContext(Dispatchers.IO) { metadataRepository.readCached(packageName) }
            cached = current
            val metadata = current?.metadata
            _state.value = ArtworkPickerState(
                title = metadata?.title.orEmpty(),
                isLoading = metadata != null,
                selected = mapOf(
                    ArtworkKind.COVER to metadata?.coverUrl,
                    ArtworkKind.BANNER to metadata?.backdropUrl,
                    ArtworkKind.ICON to metadata?.iconUrl
                )
            )
            if (metadata == null) return@launch
            val options = try {
                metadataRepository.findArtworkOptions(metadata)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ArtworkOptions()
            }
            _state.update { it.copy(isLoading = false, options = options) }
        }
    }

    fun reset() {
        loadedPackage = null
        cached = null
        _state.value = ArtworkPickerState()
    }

    fun select(kind: ArtworkKind, url: String) {
        _state.update { it.copy(selected = it.selected + (kind to url)) }
    }

    fun save(packageName: String, onSaved: () -> Unit) {
        val current = cached ?: return onSaved()
        val selected = _state.value.selected
        val updated = current.metadata.withArtwork(
            cover = selected[ArtworkKind.COVER],
            banner = selected[ArtworkKind.BANNER],
            icon = selected[ArtworkKind.ICON]
        )
        if (updated == current.metadata) return onSaved()
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                metadataRepository.save(
                    packageName,
                    CachedGameMetadata(updated, MetadataCacheStatus.CUSTOM)
                )
                _state.update { it.copy(isSaving = false) }
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, error = "Couldn't save artwork") }
            }
        }
    }
}
