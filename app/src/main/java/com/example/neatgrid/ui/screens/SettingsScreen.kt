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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedThemeIndex: Int,
    onThemeChange: (Int) -> Unit,
    selectedAppsPerRow: Int,
    onAppsPerRowChange: (Int) -> Unit,
    selectedRomFolderUri: String,
    onRomFolderChange: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val themeOptions = listOf("System", "Light", "Dark")
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
            onRomFolderChange(uri.toString())
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
                modifier = Modifier.padding(start = 8.dp),
                headlineContent = { Text(text = "Apps Per Row") },
                supportingContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = selectedAppsPerRow.toFloat(),
                            onValueChange = { newValue ->
                                onAppsPerRowChange(newValue.toInt())
                            },
                            valueRange = 3f..8f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        Text(
                            text = "Current: $selectedAppsPerRow apps",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
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
                    .clickable{},
                headlineContent = { Text(text = "Scan Now",fontWeight = FontWeight.SemiBold) },
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
