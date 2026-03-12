package com.example.neatgrid.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(var title: String, var icon: ImageVector, var route: String) {
    object Library : BottomNavItem("Library", Icons.Filled.Home, "library")
    object AddGame : BottomNavItem("Add Games", Icons.Filled.Add, "addGame")
    object Settings : BottomNavItem("Settings", Icons.Filled.Settings, "settings")
}

    @Composable
    fun BottomNavigationBar(navController: NavController) {
        val items = listOf(
            BottomNavItem.Library,
            BottomNavItem.AddGame,
            BottomNavItem.Settings
        )

        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                }
            }
        }



