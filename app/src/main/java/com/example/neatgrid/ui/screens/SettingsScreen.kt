package com.example.neatgrid.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neatgrid.data.Emulator
import com.example.neatgrid.data.LibraryCoverStyle
import com.example.neatgrid.data.LibraryDensity
import com.example.neatgrid.data.LibraryTitleStyle
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.ui.components.ApiKeyDialog
import com.example.neatgrid.ui.components.InformationCreditsDialog
import com.example.neatgrid.ui.components.MetadataSourceRow
import com.example.neatgrid.ui.components.SegmentedSettingItem
import java.util.Locale

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
    libraryDensity: LibraryDensity,
    onLibraryDensityChange: (LibraryDensity) -> Unit,
    libraryTitleStyle: LibraryTitleStyle,
    onLibraryTitleStyleChange: (LibraryTitleStyle) -> Unit,
    libraryCoverStyle: LibraryCoverStyle,
    onLibraryCoverStyleChange: (LibraryCoverStyle) -> Unit,
    roundedCovers: Boolean,
    onRoundedCoversChange: (Boolean) -> Unit,
    emulatorSelections: Map<String, String>,
    onEmulatorSelectionChange: (String, String) -> Unit,
    selectedRomFolderUri: String,
    onRomFolderChange: (String) -> Unit,
    scanRomSubfolders: Boolean,
    onScanRomSubfoldersChange: (Boolean) -> Unit,
    onDetectInstalledGames: () -> Unit,
    onScanRoms: () -> Unit,
    metadataSourceOrder: List<MetadataSource>,
    onMoveMetadataSource: (Int, Int) -> Unit,
    iidbApiKey: String,
    onIidbApiKeyChange: (String) -> Unit,
    onVerifyIidbKey: (String, (Result<String>) -> Unit) -> Unit,
    rawgApiKey: String,
    onRawgApiKeyChange: (String) -> Unit,
    steamGridDbApiKey: String,
    onSteamGridDbApiKeyChange: (String) -> Unit,
    onVerifySteamGridDbKey: (String, (Result<String>) -> Unit) -> Unit,
    disabledMetadataSources: Set<MetadataSource>,
    onMetadataSourceEnabledChange: (MetadataSource, Boolean) -> Unit,
    imageCacheBytes: Long?,
    onRefreshImageCacheSize: () -> Unit,
    onClearImageCache: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val themeOptions = listOf("System", "Light", "Dark")
    val context = LocalContext.current
    var selectedSystem by remember { mutableStateOf<String?>(null) }
    var showIidbKeyDialog by remember { mutableStateOf(false) }
    var showRawgKeyDialog by remember { mutableStateOf(false) }
    var showSteamGridDbKeyDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onRefreshImageCacheSize() }

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
            SegmentedSettingItem(
                modifier = Modifier.padding(start = 8.dp),
                title = "Theme",
                options = themeOptions.indices.toList(),
                selected = selectedThemeIndex,
                optionLabel = { themeOptions[it] },
                onSelect = onThemeChange
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
            SegmentedSettingItem(
                modifier = Modifier.padding(start = 8.dp),
                title = "Grid Density",
                options = LibraryDensity.entries,
                selected = libraryDensity,
                optionLabel = LibraryDensity::label,
                onSelect = onLibraryDensityChange
            )
            val iidbConfigured = iidbApiKey.isNotBlank()
            SegmentedSettingItem(
                modifier = Modifier.padding(start = 8.dp),
                title = "Cover Style",
                options = LibraryCoverStyle.entries,
                selected = libraryCoverStyle.effective(iidbConfigured),
                optionLabel = LibraryCoverStyle::label,
                onSelect = onLibraryCoverStyleChange,
                enabled = iidbConfigured,
                supportingText = if (iidbConfigured) null else "Add an iiDB API key to use game icons"
            )
            SegmentedSettingItem(
                modifier = Modifier.padding(start = 8.dp),
                title = "Game Titles",
                options = LibraryTitleStyle.entries,
                selected = libraryTitleStyle,
                optionLabel = LibraryTitleStyle::label,
                onSelect = onLibraryTitleStyleChange
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

            SectionHeader("Metadata")
            val keyConfigured = mapOf(
                MetadataSource.IIDB to iidbApiKey.isNotBlank(),
                MetadataSource.RAWG to rawgApiKey.isNotBlank(),
                MetadataSource.STEAMGRIDDB to steamGridDbApiKey.isNotBlank()
            )
            metadataSourceOrder.forEachIndexed { index, source ->
                MetadataSourceRow(
                    position = index,
                    source = source,
                    isLast = index == metadataSourceOrder.lastIndex,
                    enabled = source !in disabledMetadataSources,
                    keyConfigured = keyConfigured[source] ?: false,
                    onMove = { delta -> onMoveMetadataSource(index, delta) },
                    onEnabledChange = { enabled -> onMetadataSourceEnabledChange(source, enabled) }
                )
            }
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { showIidbKeyDialog = true },
                headlineContent = { Text(text = "iiDB API Key") },
                supportingContent = {
                    Text(text = if (iidbApiKey.isNotBlank()) "Configured" else "Not configured")
                }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { showRawgKeyDialog = true },
                headlineContent = { Text(text = "RAWG API Key") },
                supportingContent = {
                    Text(text = if (rawgApiKey.isNotBlank()) "Configured" else "Not configured")
                }
            )
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { showSteamGridDbKeyDialog = true },
                headlineContent = { Text(text = "SteamGridDB API Key") },
                supportingContent = {
                    Text(text = if (steamGridDbApiKey.isNotBlank()) "Configured" else "Not configured")
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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

            SectionHeader("Storage")
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(enabled = (imageCacheBytes ?: 0L) > 0L) { onClearImageCache() },
                headlineContent = { Text(text = "Clear Image Cache") },
                supportingContent = {
                    Text(
                        text = imageCacheBytes?.let { "${formatBytes(it)} of downloaded artwork" }
                            ?: "Calculating…"
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { showInfoDialog = true },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                },
                headlineContent = { Text(text = "Information & Credits", fontWeight = FontWeight.SemiBold) }
            )
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

    if (showIidbKeyDialog) {
        ApiKeyDialog(
            title = "iiDB API Key",
            currentKey = iidbApiKey,
            helperText = "Get a free API key at accounts.iisu.network",
            helperUrl = "https://accounts.iisu.network",
            onVerify = onVerifyIidbKey,
            onSave = { key ->
                onIidbApiKeyChange(key)
                showIidbKeyDialog = false
            },
            onDismiss = { showIidbKeyDialog = false }
        )
    }

    if (showSteamGridDbKeyDialog) {
        ApiKeyDialog(
            title = "SteamGridDB API Key",
            currentKey = steamGridDbApiKey,
            helperText = "Get a free API key at steamgriddb.com/profile/preferences/api",
            helperUrl = "https://www.steamgriddb.com/profile/preferences/api",
            onVerify = onVerifySteamGridDbKey,
            onSave = { key ->
                onSteamGridDbApiKeyChange(key)
                showSteamGridDbKeyDialog = false
            },
            onDismiss = { showSteamGridDbKeyDialog = false }
        )
    }

    if (showRawgKeyDialog) {
        ApiKeyDialog(
            title = "RAWG API Key",
            currentKey = rawgApiKey,
            helperText = "Get a free API key at rawg.io/apidocs",
            helperUrl = "https://rawg.io/apidocs",
            onVerify = null,
            onSave = { key ->
                onRawgApiKeyChange(key)
                showRawgKeyDialog = false
            },
            onDismiss = { showRawgKeyDialog = false }
        )
    }

    if (showInfoDialog) {
        val appVersion = remember(context) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            } catch (_: Exception) {
                "1.0"
            }
        }
        InformationCreditsDialog(
            appVersion = appVersion,
            onDismiss = { showInfoDialog = false }
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

private fun formatBytes(bytes: Long): String {
    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1.0) String.format(
        Locale.ROOT,
        "%.1f MB",
        megabytes
    ) else "${bytes / 1024} KB"
}

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
