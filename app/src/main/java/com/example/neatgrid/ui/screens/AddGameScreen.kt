package com.example.neatgrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(libraryViewModel: LibraryViewModel, viewModel: AddGameViewModel = viewModel(), onAdded: () -> Unit) {
    val apps by viewModel.appsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Games") },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                actions = {
                    if (selectedApps.isNotEmpty()) {
                        Text(
                            text = "Selected: ${selectedApps.size}",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Checkbox(
                        modifier = Modifier.padding(end = 12.dp),
                        checked = selectedApps.size == apps.size,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                viewModel.selectAllApp(apps)
                            }
                            else {
                                viewModel.clearSelectedApps()
                            }
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedApps.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            libraryViewModel.addApps(apps.filter { selectedApps.contains(it.packageName) } )
                            onAdded()
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Selected Games") }
                    )

            }
        }

    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    ListItem(
                        leadingContent = {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = "${app.label} icon",
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        headlineContent = {
                            Text(
                                text = app.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = {
                            Text(
                                text = app.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Checkbox(
                                checked = selectedApps.contains(app.packageName),
                                onCheckedChange = { viewModel.toggleAppSelection(app.packageName) }
                            )
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}