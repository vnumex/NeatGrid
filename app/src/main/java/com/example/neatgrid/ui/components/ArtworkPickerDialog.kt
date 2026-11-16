package com.example.neatgrid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.ui.screens.ArtworkPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkPickerDialog(
    packageName: String,
    showIcons: Boolean,
    onDone: (saved: Boolean) -> Unit,
    progressLabel: String? = null,
    onSkipAll: (() -> Unit)? = null
) {
    val viewModel: ArtworkPickerViewModel = viewModel(key = "artwork-picker")
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(packageName) { viewModel.load(packageName) }
    val finish: (Boolean) -> Unit = { saved ->
        viewModel.reset()
        onDone(saved)
    }
    val kinds = ArtworkKind.entries.filter { it != ArtworkKind.ICON || showIcons }

    Dialog(
        onDismissRequest = { finish(false) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(text = "Choose Artwork")
                                Text(
                                    text = state.error
                                        ?: listOfNotNull(
                                            state.title.ifBlank { null },
                                            progressLabel
                                        ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.error != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish(false) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Keep current artwork"
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                enabled = !state.isLoading && !state.isSaving,
                                onClick = { viewModel.save(packageName) { finish(true) } }
                            ) {
                                Text(text = "Save")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (onSkipAll != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                viewModel.reset()
                                onSkipAll()
                            }) {
                                Text(text = "Skip all")
                            }
                            TextButton(onClick = { finish(false) }) {
                                Text(text = "Skip")
                            }
                        }
                    }
                }
            ) { contentPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    when {
                        state.isLoading -> LoadingArtwork()
                        state.options.isEmpty() -> NoArtwork()
                        else -> LazyColumn(
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            items(kinds) { kind ->
                                ArtworkSection(
                                    kind = kind,
                                    groups = state.options.of(kind),
                                    selectedUrl = state.selected[kind],
                                    onSelect = { url -> viewModel.select(kind, url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingArtwork() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(text = "Looking for artwork…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoArtwork() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No artwork found in your metadata sources",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
