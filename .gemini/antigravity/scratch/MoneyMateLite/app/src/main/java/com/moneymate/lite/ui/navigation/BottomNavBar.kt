package com.moneymate.lite.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit
)

private val navItems = listOf(
    BottomNavItem(
        label = "Files",
        route = "home",
        icon = { Icon(Icons.Default.Home, contentDescription = "Files") }
    ),
    BottomNavItem(
        label = "Reports",
        route = "reports",
        icon = { Icon(Icons.Default.DateRange, contentDescription = "Reports") }
    ),
    BottomNavItem(
        label = "Settings",
        route = "settings",
        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )
)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = item.icon,
                label = { Text(item.label) }
            )
        }
    }
}
