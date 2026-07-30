package com.example.neatgrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.ui.screens.MissingRomPrompt

@Composable
fun MissingRomDialog(
    prompt: MissingRomPrompt,
    onKeepFiles: () -> Unit,
    onDeleteFiles: () -> Unit,
    onDismiss: () -> Unit
) {
    val relatedFileCount = prompt.relatedFiles.size
    val message = if (relatedFileCount > 0) {
        "${prompt.label} is no longer in the ROM folder. NeatGrid found $relatedFileCount related save or state files it can access. Keep them?"
    } else {
        "${prompt.label} is no longer in the ROM folder. Its library entry will be removed. Emulator-private save data is not accessible to NeatGrid and will be kept."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "ROM Not Found") },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onKeepFiles) {
                Text(text = "Keep Save Files")
            }
        },
        dismissButton = {
            if (relatedFileCount > 0) {
                TextButton(onClick = onDeleteFiles) {
                    Text(text = "Delete Found Files")
                }
            }
            TextButton(onClick = onDismiss) {
                Text(text = "Not Now")
            }
        }
    )
}

@Composable
fun DetectedGamesDialog(
    games: List<AppInfo>,
    onConfirm: (keptPackageNames: Set<String>, excludedPackageNames: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val packageNames = games.map { it.packageName }
    var selectedPackages by remember(packageNames) {
        mutableStateOf(packageNames.toSet())
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
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Choose Games to Keep",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${selectedPackages.size} of ${games.size} selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(text = "Not Now")
                            }
                            TextButton(
                                onClick = { onConfirm(emptySet(), packageNames.toSet()) }
                            ) {
                                Text(text = "Exclude All")
                            }
                            Button(
                                onClick = {
                                    val excludedPackages = packageNames.toSet() - selectedPackages
                                    onConfirm(selectedPackages, excludedPackages)
                                }
                            ) {
                                Text(text = "Keep (${selectedPackages.size})")
                            }
                        }
                    }
                }
            ) { contentPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    item(key = "select_all") {
                        val allSelected = selectedPackages.size == packageNames.size
                        ListItem(
                            modifier = Modifier.clickable {
                                selectedPackages = if (allSelected) emptySet() else packageNames.toSet()
                            },
                            headlineContent = { Text(text = "Select All") },
                            supportingContent = { Text(text = "Toggle every detected game") },
                            trailingContent = {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { checked ->
                                        selectedPackages = if (checked) packageNames.toSet() else emptySet()
                                    }
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                    items(games, key = { it.packageName }) { game ->
                        val isSelected = game.packageName in selectedPackages
                        ListItem(
                            modifier = Modifier.clickable {
                                selectedPackages = if (isSelected) {
                                    selectedPackages - game.packageName
                                } else {
                                    selectedPackages + game.packageName
                                }
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = game.icon,
                                    contentDescription = "${game.label} icon",
                                    modifier = Modifier.size(48.dp)
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = game.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = game.packageName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedPackages = if (checked) {
                                            selectedPackages + game.packageName
                                        } else {
                                            selectedPackages - game.packageName
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
