package com.example.neatgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.neatgrid.ui.theme.NeatGridTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neatgrid.ui.components.BottomNavItem
import com.example.neatgrid.ui.components.BottomNavigationBar
import com.example.neatgrid.ui.screens.AddGameScreen
import com.example.neatgrid.ui.screens.LibraryScreen
import com.example.neatgrid.ui.screens.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NeatGridTheme {
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
                        ){
                            composable(BottomNavItem.Library.route){
                                LibraryScreen()
                            }
                            composable(BottomNavItem.AddGame.route) {
                                AddGameScreen()
                            }
                            composable(BottomNavItem.Settings.route) {
                                SettingsScreen()
                            }

                        }

                    }
                }
            }
        }
    }
}