package com.example.neatgrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.neatgrid.data.GameMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameMetadataEditor(
    metadata: GameMetadata,
    searchResults: List<GameMetadata>,
    isSearching: Boolean,
    searchError: String?,
    onSearch: (String) -> Unit,
    onApplyMatch: (GameMetadata) -> Unit,
    onSave: (GameMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember(metadata.title) { mutableStateOf(metadata.title) }
    var editTitle by remember(metadata) { mutableStateOf(metadata.title) }
    var editReleaseDate by remember(metadata) { mutableStateOf(metadata.releaseDate.orEmpty()) }
    var editGenres by remember(metadata) { mutableStateOf(metadata.genres.joinToString(", ")) }
    var editPlatforms by remember(metadata) { mutableStateOf(metadata.platforms.joinToString(", ")) }
    var editSummary by remember(metadata) { mutableStateOf(metadata.summary.orEmpty()) }
    var editCoverUrl by remember(metadata) { mutableStateOf(metadata.coverUrl.orEmpty()) }
    var editBackdropUrl by remember(metadata) { mutableStateOf(metadata.backdropUrl.orEmpty()) }

    LaunchedEffect(metadata.title) {
        if (searchQuery.isNotBlank()) onSearch(searchQuery)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Column {
                        TopAppBar(
                            modifier = Modifier.statusBarsPadding(),
                            title = { Text(text = "Edit Game Metadata") },
                            actions = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text(text = "Find Match") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text(text = "Manual Edit") }
                            )
                        }
                    }
                },
                bottomBar = {
                    if (selectedTab == 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding()
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Button(
                                    enabled = editTitle.isNotBlank(),
                                    onClick = {
                                        onSave(
                                            metadata.copy(
                                                title = editTitle.trim(),
                                                releaseDate = editReleaseDate.trim().takeIf { it.isNotEmpty() },
                                                genres = commaSeparatedValues(editGenres),
                                                platforms = commaSeparatedValues(editPlatforms),
                                                summary = editSummary.trim().takeIf { it.isNotEmpty() },
                                                coverUrl = editCoverUrl.trim().takeIf { it.isNotEmpty() },
                                                backdropUrl = editBackdropUrl.trim().takeIf { it.isNotEmpty() }
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Save Changes")
                                }
                            }
                        }
                    }
                }
            ) { contentPadding ->
                if (selectedTab == 0) {
                    MetadataSearchPanel(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        results = searchResults,
                        isSearching = isSearching,
                        error = searchError,
                        onSearch = { onSearch(searchQuery) },
                        onSelect = onApplyMatch,
                        modifier = Modifier.padding(contentPadding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.size(4.dp))
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text(text = "Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editReleaseDate,
                                onValueChange = { editReleaseDate = it },
                                label = { Text(text = "Release Date / Year") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editGenres,
                                onValueChange = { editGenres = it },
                                label = { Text(text = "Genres") },
                                supportingText = { Text(text = "Separate multiple values with commas") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editPlatforms,
                                onValueChange = { editPlatforms = it },
                                label = { Text(text = "Platforms") },
                                supportingText = { Text(text = "Separate multiple values with commas") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editCoverUrl,
                                onValueChange = { editCoverUrl = it },
                                label = { Text(text = "Cover Image URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editBackdropUrl,
                                onValueChange = { editBackdropUrl = it },
                                label = { Text(text = "Backdrop Image URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editSummary,
                                onValueChange = { editSummary = it },
                                label = { Text(text = "Description") },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<GameMetadata>,
    isSearching: Boolean,
    error: String?,
    onSearch: () -> Unit,
    onSelect: (GameMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(text = "Game Title") },
            trailingIcon = {
                IconButton(
                    enabled = query.isNotBlank() && !isSearching,
                    onClick = {
                        onSearch()
                        keyboardController?.hide()
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotBlank() && !isSearching) onSearch()
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(12.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isSearching -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
                results.isEmpty() -> Text(
                    text = "No matches found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.sourceId ?: "${it.title}-${it.platforms.firstOrNull()}" }) { game ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(game) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = game.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp, 70.dp)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            game.platforms.firstOrNull(),
                                            game.releaseDate?.substringBefore('-')
                                        ).joinToString(" · ").ifEmpty { "No release details" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun commaSeparatedValues(value: String): List<String> {
    return value.split(',').map(String::trim).filter(String::isNotEmpty)
}
