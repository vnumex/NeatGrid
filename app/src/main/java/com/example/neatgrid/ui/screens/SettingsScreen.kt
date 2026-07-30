package com.example.neatgrid.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedThemeIndex: Int,
    onThemeChange: (Int) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    darkThemeEnabled: Boolean,
    amoledBlackEnabled: Boolean,
    onAmoledBlackChange: (Boolean) -> Unit,
    selectedAppsPerRow: Int,
    onAppsPerRowChange: (Int) -> Unit,
    showGameNames: Boolean,
    onShowGameNamesChange: (Boolean) -> Unit,
    roundedCovers: Boolean,
    onRoundedCoversChange: (Boolean) -> Unit,
    selectedRomFolderUri: String,
    onRomFolderChange: (String) -> Unit,
    onDetectInstalledGames: () -> Unit,
    onScanRoms: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val themeOptions = listOf("System", "Light", "Dark")
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
                onRomFolderChange(uri.toString())
            } catch (_: SecurityException) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("Appearance")
            ListItem(
                modifier = Modifier.padding(start = 8.dp),
                headlineContent = { Text(text = "Theme") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        themeOptions.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                ),
                                onClick = { onThemeChange(index) },
                                selected = selectedThemeIndex == index,
                                label = { Text(text = label) },
                            )
                        }
                    }
                }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(enabled = !amoledBlackEnabled) {
                        onDynamicColorChange(!dynamicColorEnabled)
                    },
                headlineContent = {
                    Text(
                        text = "Dynamic Color",
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (amoledBlackEnabled) 0.38f else 1f
                        )
                    )
                },
                trailingContent = {
                    Checkbox(
                        checked = dynamicColorEnabled,
                        enabled = !amoledBlackEnabled,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(enabled = darkThemeEnabled) {
                        onAmoledBlackChange(!amoledBlackEnabled)
                    },
                headlineContent = {
                    Text(
                        text = "AMOLED Black",
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (darkThemeEnabled) 1f else 0.38f
                        )
                    )
                },
                trailingContent = {
                    Checkbox(
                        checked = amoledBlackEnabled,
                        enabled = darkThemeEnabled,
                        onCheckedChange = onAmoledBlackChange
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Library")
            ListItem(
                modifier = Modifier.padding(start = 8.dp),
                headlineContent = { Text(text = "Apps Per Row") },
                supportingContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                enabled = selectedAppsPerRow > 2,
                                onClick = { onAppsPerRowChange(selectedAppsPerRow - 1) }
                            ) {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = selectedAppsPerRow.toString(),
                                modifier = Modifier.width(64.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                enabled = selectedAppsPerRow < 8,
                                onClick = { onAppsPerRowChange(selectedAppsPerRow + 1) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase apps per row"
                                )
                            }
                        }
                        Text(
                            text = "Current: $selectedAppsPerRow apps",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onShowGameNamesChange(!showGameNames) },
                headlineContent = { Text(text = "Show Game Names") },
                trailingContent = {
                    Checkbox(
                        checked = showGameNames,
                        onCheckedChange = onShowGameNamesChange
                    )
                }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onRoundedCoversChange(!roundedCovers) },
                headlineContent = { Text(text = "Rounded Covers") },
                trailingContent = {
                    Checkbox(
                        checked = roundedCovers,
                        onCheckedChange = onRoundedCoversChange
                    )
                }
            )
            HorizontalDivider( modifier = Modifier.padding(vertical = 8.dp) )

            SectionHeader("Installed Games")
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onDetectInstalledGames() },
                headlineContent = { Text(text = "Detect Installed Games", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(text = "Import installed apps marked as games by Android") }
            )
            HorizontalDivider( modifier = Modifier.padding(vertical = 8.dp) )

            SectionHeader("ROMs")
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { folderPicker.launch(null) },
                headlineContent = { Text(text = "Set Folder") },
                supportingContent = {
                    if (selectedRomFolderUri.isNotEmpty()) Text(text = selectedRomFolderUri)
                    else Text(text = "No folder selected") }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(enabled = selectedRomFolderUri.isNotEmpty()) { onScanRoms() },
                headlineContent = { Text(text = "Scan Now",fontWeight = FontWeight.SemiBold) },
                supportingContent = {
                    Text(
                        text = if (selectedRomFolderUri.isNotEmpty()) {
                            "Import compatible ROM files from the selected folder"
                        } else {
                            "Select a ROM folder first"
                        }
                    )
                }
            )
            HorizontalDivider( modifier = Modifier.padding(vertical = 8.dp) )
        }
    }
}

    @Composable
    fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        )
    }
