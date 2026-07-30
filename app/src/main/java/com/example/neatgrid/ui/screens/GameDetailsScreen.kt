package com.example.neatgrid.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameDetailsScreen(
    packageName: String,
    onBack: () -> Unit,
    onMetadataChanged: () -> Unit,
    viewModel: GameDetailsViewModel = viewModel()
) {
    val metadata by viewModel.metadata.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    var showOverrideDialog by remember { mutableStateOf(false) }
    var showEditChoicesDialog by remember { mutableStateOf(false) }
    var showManualEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        viewModel.loadMetadata(packageName)
    }

    DisposableEffect(packageName) {
        onDispose(onMetadataChanged)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (metadata != null) {
                        IconButton(onClick = { showEditChoicesDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Metadata Match",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            enabled = !isRefreshing,
                            onClick = { viewModel.refreshMetadata(packageName) }
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh metadata",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null && metadata == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Could Not Load Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "Unknown error occurred.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.refreshMetadata(packageName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try Again")
                        }
                        OutlinedButton(
                            onClick = { showOverrideDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search LaunchBox Manually")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.launchGame(packageName) {
                                    Toast.makeText(context, "Cannot open game", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Game")
                        }
                    }
                }
            } else if (metadata != null) {
                val game = metadata!!
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // 1. Hero Backdrop Image Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        val backdropUrl = game.screenshotUrls.firstOrNull() ?: game.coverUrl
                        if (backdropUrl != null) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = "Backdrop",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                            )
                        }

                        // Gradient fading the backdrop into the surface background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface
                                        ),
                                        startY = 0.0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                )
                        )
                    }

                    // 2. Info Overlapping Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-60).dp)
                    ) {
                        if (error != null) {
                            Text(
                                text = error.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Cover Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(160.dp)
                            ) {
                                AsyncImage(
                                    model = game.coverUrl,
                                    contentDescription = "Cover Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Basic text details
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = game.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val releaseText = game.releaseDate?.split("-")?.firstOrNull() ?: "TBA"
                                Text(
                                    text = "Released: $releaseText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (game.rating != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Rating: %.1f%%".format(game.rating),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. Launch Action Button
                        Button(
                            onClick = {
                                viewModel.launchGame(packageName) {
                                    Toast.makeText(context, "Cannot open game", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. Quick tags: Genres & Platforms
                        if (game.genres.isNotEmpty()) {
                            Text(
                                text = "Genres",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                game.genres.forEach { genre ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(genre) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (game.platforms.isNotEmpty()) {
                            Text(
                                text = "Platforms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                game.platforms.forEach { platform ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(platform) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 5. Summary Text
                        if (!game.summary.isNullOrBlank()) {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = game.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // 6. Screenshots Carousel
                        if (game.screenshotUrls.isNotEmpty()) {
                            Text(
                                text = "Screenshots",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(game.screenshotUrls) { url ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(135.dp)
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Screenshot",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp + innerPadding.calculateBottomPadding()))
                    }
                }
            }
        }

        // Manual Override Search Dialog
        if (showOverrideDialog) {
            Dialog(onDismissRequest = { showOverrideDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        var searchQuery by remember { mutableStateOf(metadata?.title ?: "") }
                        val searchResults by viewModel.searchResults.collectAsState()
                        val isSearching by viewModel.isSearching.collectAsState()
                        val searchError by viewModel.searchError.collectAsState()
                        val keyboardController = LocalSoftwareKeyboardController.current

                        LaunchedEffect(Unit) {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.searchOverride(searchQuery)
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Search LaunchBox Game",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { showOverrideDialog = false },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Game Title") },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.searchOverride(searchQuery)
                                    keyboardController?.hide()
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { 
                                viewModel.searchOverride(searchQuery)
                                keyboardController?.hide()
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else if (searchError != null) {
                                Text(
                                    text = searchError.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (searchResults.isEmpty()) {
                                Text(
                                    text = "No results found. Search above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(searchResults) { gameResult ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.applyOverride(
                                                        packageName,
                                                        gameResult
                                                    )
                                                    showOverrideDialog = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = gameResult.coverUrl,
                                                    contentDescription = gameResult.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(50.dp, 70.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = gameResult.title,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val release = gameResult.releaseDate?.split("-")?.firstOrNull() ?: "TBA"
                                                    val platformText = gameResult.platforms.firstOrNull() ?: "Unknown Platform"
                                                    Text(
                                                        text = "$platformText • $release",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        }

        // Choice Dialog
        if (showEditChoicesDialog) {
            AlertDialog(
                onDismissRequest = { showEditChoicesDialog = false },
                title = { Text("Edit Game Metadata", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Text("Choose whether you want to search LaunchBox or manually enter game details.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                showEditChoicesDialog = false
                                showOverrideDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search LaunchBox Database")
                        }
                        Button(
                            onClick = {
                                showEditChoicesDialog = false
                                showManualEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Details Manually")
                        }
                        OutlinedButton(
                            onClick = { showEditChoicesDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                },
                dismissButton = null
            )
        }

        // Manual Edit Dialog
        if (showManualEditDialog && metadata != null) {
            val game = metadata!!
            var editTitle by remember { mutableStateOf(game.title) }
            var editReleaseDate by remember { mutableStateOf(game.releaseDate ?: "") }
            var editGenres by remember { mutableStateOf(game.genres.joinToString(", ")) }
            var editPlatforms by remember { mutableStateOf(game.platforms.joinToString(", ")) }
            var editSummary by remember { mutableStateOf(game.summary ?: "") }
            var editCoverUrl by remember { mutableStateOf(game.coverUrl ?: "") }
            var editBackdropUrl by remember { mutableStateOf(game.screenshotUrls.firstOrNull() ?: "") }

            AlertDialog(
                onDismissRequest = { showManualEditDialog = false },
                title = { Text("Edit Game Details") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editReleaseDate,
                            onValueChange = { editReleaseDate = it },
                            label = { Text("Release Date / Year") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGenres,
                            onValueChange = { editGenres = it },
                            label = { Text("Genres (comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editPlatforms,
                            onValueChange = { editPlatforms = it },
                            label = { Text("Platforms (comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editCoverUrl,
                            onValueChange = { editCoverUrl = it },
                            label = { Text("Cover Image URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editBackdropUrl,
                            onValueChange = { editBackdropUrl = it },
                            label = { Text("Backdrop Image URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editSummary,
                            onValueChange = { editSummary = it },
                            label = { Text("About / Description") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val genresList = editGenres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val platformsList = editPlatforms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val screenshotsList = if (editBackdropUrl.isNotEmpty()) listOf(editBackdropUrl) else emptyList()

                            val updatedGame = game.copy(
                                title = editTitle,
                                releaseDate = editReleaseDate.takeIf { it.isNotEmpty() },
                                genres = genresList,
                                platforms = platformsList,
                                summary = editSummary.takeIf { it.isNotEmpty() },
                                coverUrl = editCoverUrl.takeIf { it.isNotEmpty() },
                                screenshotUrls = screenshotsList
                            )
                            viewModel.updateMetadata(packageName, updatedGame)
                            showManualEditDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showManualEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
