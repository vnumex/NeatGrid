package com.example.neatgrid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neatgrid.ui.components.BottomNavItem
import com.example.neatgrid.ui.components.BottomNavigationBar
import com.example.neatgrid.ui.screens.AddGameScreen
import com.example.neatgrid.ui.screens.LibraryScreen
import com.example.neatgrid.ui.screens.LibraryViewModel
import com.example.neatgrid.ui.screens.SettingsScreen
import com.example.neatgrid.ui.screens.SettingsViewModel
import com.example.neatgrid.ui.theme.NeatGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val settingsViewModel: SettingsViewModel = viewModel()
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return LibraryViewModel(application) as T
                    }
                }
            )
            val appsPerRow by settingsViewModel.appsPerRow.collectAsStateWithLifecycle()
            val romFolderUri by settingsViewModel.romFolder.collectAsStateWithLifecycle()

            val selectedThemeIndex by settingsViewModel.themeIndex.collectAsStateWithLifecycle()
            val isDarkTheme = when (selectedThemeIndex) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            NeatGridTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = { BottomNavigationBar(navController = navController) }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = BottomNavItem.Library.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(BottomNavItem.Library.route) {
                                LibraryScreen(
                                    viewModel = libraryViewModel,
                                    columns = appsPerRow,
                                    onAppClick = { packageName ->
                                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                        if (launchIntent != null) {
                                            startActivity(launchIntent)
                                        }
                                        else {
                                            Toast.makeText(applicationContext, "App cannot be opened", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }
                            composable(BottomNavItem.AddGame.route) {
                                AddGameScreen(
                                    libraryViewModel = libraryViewModel,
                                    onAdded = { navController.navigate(BottomNavItem.Library.route) })
                            }
                            composable(BottomNavItem.Settings.route) {
                                SettingsScreen(
                                    selectedThemeIndex = selectedThemeIndex,
                                    onThemeChange = { settingsViewModel.setTheme(it) },
                                    selectedAppsPerRow = appsPerRow,
                                    onAppsPerRowChange = { settingsViewModel.setAppsPerRow(it) },
                                    selectedRomFolderUri = romFolderUri,
                                    onRomFolderChange = { settingsViewModel.setRomFolder(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}