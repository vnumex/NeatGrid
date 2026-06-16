package com.example.neatgrid.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.RomFile
import com.example.neatgrid.data.RomRepository

enum class AddScreenMode {
    CHOOSE_TYPE,
    ADD_APPS,
    ADD_ROMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    libraryViewModel: LibraryViewModel,
    viewModel: AddGameViewModel = viewModel(),
    onAdded: () -> Unit
) {
    val apps by viewModel.appsList.collectAsState()
    val roms by viewModel.romsList.collectAsState()
    val romFolderUri by viewModel.romFolderUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val selectedApps by viewModel.selectedApps.collectAsState()
    val selectedRoms by viewModel.selectedRoms.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    var currentMode by rememberSaveable { mutableStateOf(AddScreenMode.CHOOSE_TYPE) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.setRomFolder(uri.toString())
            } catch (e: SecurityException) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when (currentMode) {
                        AddScreenMode.CHOOSE_TYPE -> "Add Games"
                        AddScreenMode.ADD_APPS -> "Installed Apps"
                        AddScreenMode.ADD_ROMS -> "ROM Files"
                    }
                    Text(text = titleText) 
                },
                navigationIcon = {
                    if (currentMode != AddScreenMode.CHOOSE_TYPE) {
                        IconButton(onClick = { currentMode = AddScreenMode.CHOOSE_TYPE }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Type Selection"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                actions = {
                    if (currentMode == AddScreenMode.ADD_APPS) {
                        if (selectedApps.isNotEmpty()) {
                            Text(
                                text = "Selected: ${selectedApps.size}",
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Checkbox(
                            modifier = Modifier.padding(end = 12.dp),
                            checked = apps.isNotEmpty() && selectedApps.size == apps.size,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    viewModel.selectAllApps(apps)
                                } else {
                                    viewModel.clearSelectedApps()
                                }
                            }
                        )
                    } else if (currentMode == AddScreenMode.ADD_ROMS) {
                        val selectableRoms = roms.filter { it.matchingEmulator != null }
                        if (selectedRoms.isNotEmpty()) {
                            Text(
                                text = "Selected: ${selectedRoms.size}",
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Checkbox(
                            modifier = Modifier.padding(end = 12.dp),
                            checked = selectableRoms.isNotEmpty() && selectedRoms.size == selectableRoms.size,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    viewModel.selectAllRoms(selectableRoms)
                                } else {
                                    viewModel.clearSelectedRoms()
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentMode != AddScreenMode.CHOOSE_TYPE) {
                val isFabVisible = if (currentMode == AddScreenMode.ADD_APPS) selectedApps.isNotEmpty() else selectedRoms.isNotEmpty()
                val fabText = if (currentMode == AddScreenMode.ADD_APPS) "Add Selected Apps" else "Add Selected ROMs"

                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (currentMode == AddScreenMode.ADD_APPS) {
                                libraryViewModel.addApps(apps.filter { selectedApps.contains(it.packageName) })
                            } else {
                                val selectedRomsList = roms.filter { selectedRoms.contains(it.uriString) }
                                val romApps = selectedRomsList.mapNotNull { rom ->
                                    val emulatorPackage = rom.matchingEmulator?.packageName ?: return@mapNotNull null
                                    val label = rom.name.substringBeforeLast('.')
                                    val syntheticPkg = RomRepository.buildPackageName(emulatorPackage, label, rom.uriString)
                                    
                                    val pm = context.packageManager
                                    val iconDrawable = try {
                                        pm.getApplicationIcon(emulatorPackage)
                                    } catch (e: Exception) {
                                        try {
                                            pm.getApplicationIcon(context.packageName)
                                        } catch (e2: Exception) {
                                            null
                                        }
                                    }
                                    if (iconDrawable != null) {
                                        AppInfo(
                                            label = label,
                                            packageName = syntheticPkg,
                                            icon = iconDrawable
                                        )
                                    } else {
                                        null
                                    }
                                }
                                libraryViewModel.addApps(romApps)
                            }
                            onAdded()
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(fabText) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (currentMode) {
                    AddScreenMode.CHOOSE_TYPE -> {
                        ChooseTypeLayout(
                            onChooseApps = {
                                currentMode = AddScreenMode.ADD_APPS
                                viewModel.scanApps()
                            },
                            onChooseRoms = {
                                currentMode = AddScreenMode.ADD_ROMS
                                viewModel.scanRoms()
                            }
                        )
                    }
                    AddScreenMode.ADD_APPS -> {
                        if (apps.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No launchable apps found.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(apps, key = { it.packageName }) { app ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            viewModel.toggleAppSelection(app.packageName)
                                        },
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
                    AddScreenMode.ADD_ROMS -> {
                        if (romFolderUri.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ROM folder is not configured.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Set a ROM directory to automatically scan for game files.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { folderPicker.launch(null) }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select ROM Folder")
                                }
                            }
                        } else if (roms.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No ROM files found.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "No compatible game files were detected in the configured directory.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = { folderPicker.launch(null) }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Change ROM Folder")
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(roms, key = { it.uriString }) { rom ->
                                    val pm = context.packageManager
                                    val emulatorIcon = rom.matchingEmulator?.let { emulator ->
                                        try {
                                            pm.getApplicationIcon(emulator.packageName)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    val isSelectable = rom.matchingEmulator != null
                                    val supportingText = if (isSelectable) {
                                        "${rom.extension.uppercase()} File • Runs via ${rom.matchingEmulator!!.label}"
                                    } else {
                                        "${rom.extension.uppercase()} File • No compatible emulator installed"
                                    }

                                    ListItem(
                                        modifier = Modifier.clickable(enabled = isSelectable) {
                                            viewModel.toggleRomSelection(rom.uriString)
                                        },
                                        leadingContent = {
                                            if (emulatorIcon != null) {
                                                AsyncImage(
                                                    model = emulatorIcon,
                                                    contentDescription = "Emulator Icon",
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = context.packageManager.getApplicationIcon(context.packageName),
                                                    contentDescription = "Generic Rom Icon",
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                        },
                                        headlineContent = {
                                            Text(
                                                text = rom.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = supportingText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelectable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                            )
                                        },
                                        trailingContent = {
                                            Checkbox(
                                                checked = selectedRoms.contains(rom.uriString),
                                                onCheckedChange = { viewModel.toggleRomSelection(rom.uriString) },
                                                enabled = isSelectable
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
            }
        }
    }
}

@Composable
fun ChooseTypeLayout(
    onChooseApps: () -> Unit,
    onChooseRoms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add Games to NeatGrid",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select the type of content you want to import into your library",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        Card(
            onClick = onChooseApps,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Installed App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan and import launcher applications installed on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            onClick = onChooseRoms,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add ROM File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan your configured directory for console emulator ROM games.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}