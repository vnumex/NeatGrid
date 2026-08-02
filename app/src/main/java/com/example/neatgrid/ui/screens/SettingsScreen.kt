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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.neatgrid.data.Emulator

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
    emulatorSelections: Map<String, String>,
    onEmulatorSelectionChange: (String, String) -> Unit,
    selectedRomFolderUri: String,
    onRomFolderChange: (String) -> Unit,
    scanRomSubfolders: Boolean,
    onScanRomSubfoldersChange: (Boolean) -> Unit,
    onDetectInstalledGames: () -> Unit,
    onScanRoms: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val themeOptions = listOf("System", "Light", "Dark")
    val context = LocalContext.current
    var selectedSystem by remember { mutableStateOf<String?>(null) }

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
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

            SectionHeader("Emulators")
            EmulatorGroup(
                title = "Nintendo Consoles",
                systems = nintendoSystems,
                selections = emulatorSelections,
                onSystemClick = { selectedSystem = it }
            )
            EmulatorGroup(
                title = "Sony Consoles",
                systems = sonySystems,
                selections = emulatorSelections,
                onSystemClick = { selectedSystem = it }
            )
            EmulatorGroup(
                title = "Others",
                systems = otherSystems,
                selections = emulatorSelections,
                onSystemClick = { selectedSystem = it }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                    .clickable { onScanRomSubfoldersChange(!scanRomSubfolders) },
                headlineContent = { Text(text = "Scan Subfolders") },
                supportingContent = {
                    Text(text = "Include ROM files inside folders within the selected folder")
                },
                trailingContent = {
                    Checkbox(
                        checked = scanRomSubfolders,
                        onCheckedChange = onScanRomSubfoldersChange
                    )
                }
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

    selectedSystem?.let { system ->
        EmulatorSelectionDialog(
            system = system,
            installedEmulators = Emulator.getInstalledEmulators(context),
            selectedPackageName = emulatorSelections[system] ?: "automatic",
            onSelect = { packageName ->
                onEmulatorSelectionChange(system, packageName)
                selectedSystem = null
            },
            onDismiss = { selectedSystem = null }
        )
    }
}

@Composable
private fun EmulatorGroup(
    title: String,
    systems: List<String>,
    selections: Map<String, String>,
    onSystemClick: (String) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
    )
    systems.forEach { system ->
        val selectedPackageName = selections[system]
        val selectedLabel = Emulator.entries.firstOrNull {
            it.packageName == selectedPackageName
        }?.label ?: "Automatic"
        ListItem(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable { onSystemClick(system) },
            headlineContent = { Text(text = system) },
            supportingContent = { Text(text = selectedLabel) }
        )
    }
}

@Composable
private fun EmulatorSelectionDialog(
    system: String,
    installedEmulators: List<Emulator>,
    selectedPackageName: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val choices = buildList {
        add("automatic" to "Automatic")
        installedEmulators
            .filter { emulator ->
                emulator.systems.contains(system) ||
                        emulator == Emulator.RETROARCH ||
                        emulator == Emulator.RETROARCH_64
            }
            .distinctBy { it.packageName }
            .forEach { emulator ->
                add(emulator.packageName to emulator.label)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = system) },
        text = {
            Column {
                choices.forEach { (packageName, label) ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(packageName) },
                        headlineContent = { Text(text = label) },
                        trailingContent = {
                            RadioButton(
                                selected = selectedPackageName == packageName,
                                onClick = { onSelect(packageName) }
                            )
                        }
                    )
                }
                if (choices.size == 1) {
                    Text(
                        text = "No compatible emulator is installed yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done")
            }
        }
    )
}

private val nintendoSystems = listOf(
    "Nintendo 3DS",
    "Nintendo Switch",
    "Nintendo DS",
    "Game Boy Advance",
    "Super Nintendo",
    "Nintendo 64",
    "GameCube",
    "Wii"
)

private val sonySystems = listOf("PlayStation 1", "PlayStation 2", "PSP")

private val otherSystems = listOf("Multi-System")

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
