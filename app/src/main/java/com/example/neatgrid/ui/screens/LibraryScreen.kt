package com.example.neatgrid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.neatgrid.data.LibraryCoverStyle
import com.example.neatgrid.data.LibraryDensity
import com.example.neatgrid.data.LibrarySortMode
import com.example.neatgrid.data.LibraryTitleStyle
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.ui.components.GameContextMenu
import com.example.neatgrid.ui.components.LibraryGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    density: LibraryDensity = LibraryDensity.COMPACT,
    titleStyle: LibraryTitleStyle = LibraryTitleStyle.BELOW,
    coverStyle: LibraryCoverStyle = LibraryCoverStyle.BOX_ART,
    roundedCovers: Boolean = true,
    onAppClick: (String) -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val library by viewModel.libraryList.collectAsState()
    val hiddenGames by viewModel.hiddenGames.collectAsState()
    val prefetchingStates by viewModel.prefetchingStates.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var expandedMenuPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var showLibraryOptions by rememberSaveable { mutableStateOf(false) }
    var showHiddenGames by rememberSaveable { mutableStateOf(false) }

    val coverShape = if (roundedCovers) RoundedCornerShape(12.dp) else RectangleShape

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "Library") },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                actions = {
                    IconButton(onClick = { showLibraryOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Library options"
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(density.columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(items = library, key = { it.packageName }) { app ->
                LibraryGridItem(
                    app = app,
                    isFetching = app.packageName in prefetchingStates,
                    density = density,
                    titleStyle = titleStyle,
                    coverStyle = coverStyle,
                    coverShape = coverShape,
                    onClick = onAppClick,
                    onLongPress = { expandedMenuPackage = it }
                )
            }
        }
    }

    if (showLibraryOptions) {
        LibraryOptionsSheet(
            hiddenGameCount = hiddenGames.size,
            selectedSortMode = sortMode,
            onDismiss = { showLibraryOptions = false },
            onShowHiddenGames = {
                showLibraryOptions = false
                showHiddenGames = true
            },
            onSortModeChange = { option ->
                viewModel.setSortMode(option)
                showLibraryOptions = false
            }
        )
    }

    library.firstOrNull { it.packageName == expandedMenuPackage }?.let { game ->
        GameContextMenu(
            game = game,
            roundedCovers = roundedCovers,
            onDismiss = { expandedMenuPackage = null },
            onLaunch = {
                expandedMenuPackage = null
                onLaunchApp(game.packageName)
            },
            onDetails = {
                expandedMenuPackage = null
                onAppClick(game.packageName)
            },
            onHide = {
                expandedMenuPackage = null
                viewModel.hideApp(game.packageName)
            },
            onDelete = {
                expandedMenuPackage = null
                viewModel.removeApp(game.packageName)
            }
        )
    }

    if (showHiddenGames) {
        HiddenGamesSheet(
            games = hiddenGames,
            roundedCovers = roundedCovers,
            onDismiss = { showHiddenGames = false },
            onUnhide = viewModel::unhideApp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryOptionsSheet(
    hiddenGameCount: Int,
    selectedSortMode: LibrarySortMode,
    onDismiss: () -> Unit,
    onShowHiddenGames: () -> Unit,
    onSortModeChange: (LibrarySortMode) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Text(
            text = "Library Options",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        ListItem(
            modifier = Modifier.clickable(onClick = onShowHiddenGames),
            headlineContent = { Text(text = "Hidden Games") },
            supportingContent = { Text(text = "$hiddenGameCount hidden") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "Sort Library",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LibrarySortMode.entries.forEach { option ->
            ListItem(
                modifier = Modifier.clickable { onSortModeChange(option) },
                headlineContent = { Text(text = option.label) },
                leadingContent = {
                    RadioButton(
                        selected = selectedSortMode == option,
                        onClick = { onSortModeChange(option) }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenGamesSheet(
    games: List<AppInfo>,
    roundedCovers: Boolean,
    onDismiss: () -> Unit,
    onUnhide: (String) -> Unit
) {
    val coverShape = if (roundedCovers) RoundedCornerShape(6.dp) else RectangleShape
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Text(
                text = "Hidden Games",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hidden games",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    lazyItems(games, key = { it.packageName }) { game ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = game.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = game.platform?.let { platform ->
                                {
                                    Text(
                                        text = platform,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = game.coverUrl ?: game.icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(coverShape)
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onUnhide(game.packageName) }) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Restore ${game.label}"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
