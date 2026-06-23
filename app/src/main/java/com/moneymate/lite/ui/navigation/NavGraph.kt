package com.moneymate.lite.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moneymate.lite.ui.screens.FileDetailScreen
import com.moneymate.lite.ui.screens.HomeScreen
import com.moneymate.lite.ui.screens.LoginScreen
import com.moneymate.lite.ui.screens.PersonDetailScreen
import com.moneymate.lite.ui.screens.ReportsScreen
import com.moneymate.lite.ui.screens.SettingsScreen
import com.moneymate.lite.ui.screens.RecentlyDeletedScreen
import com.moneymate.lite.ui.screens.FileTrashScreen
import com.moneymate.lite.ui.screens.UpdateDialog
import com.moneymate.lite.ui.viewmodel.AuthViewModel
import com.moneymate.lite.ui.viewmodel.UpdateViewModel
import com.moneymate.lite.ui.viewmodel.AppUpdateInfo

private val bottomNavRoutes = setOf("home", "reports", "settings")

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    var activeUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    val startDestination = remember {
        if (authViewModel.currentUser.value != null || authViewModel.isOfflineMode.value) "home" else "login"
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates { info ->
            if (info != null) {
                activeUpdateInfo = info
            }
        }
    }

    activeUpdateInfo?.let { updateInfo ->
        UpdateDialog(
            updateInfo = updateInfo,
            updateViewModel = updateViewModel,
            onDismiss = { activeUpdateInfo = null }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginScreen(navController = navController)
            }

            composable("home") {
                HomeScreen(navController = navController)
            }

            composable("reports") {
                ReportsScreen()
            }

            composable("settings") {
                SettingsScreen(navController = navController)
            }

            composable("recently_deleted") {
                RecentlyDeletedScreen(navController = navController)
            }

            composable(
                route = "file_detail/{fileId}?name={name}",
                arguments = listOf(
                    navArgument("fileId") { type = NavType.LongType },
                    navArgument("name") {
                        type = NavType.StringType
                        defaultValue = "File Details"
                    }
                )
            ) { backStackEntry ->
                val fileId = backStackEntry.arguments?.getLong("fileId") ?: return@composable
                val name = backStackEntry.arguments?.getString("name") ?: "File Details"
                FileDetailScreen(
                    fileId = fileId,
                    name = name,
                    navController = navController
                )
            }

            composable(
                route = "person_detail/{personId}",
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
                PersonDetailScreen(
                    personId = personId,
                    navController = navController
                )
            }

            composable(
                route = "file_trash/{fileId}",
                arguments = listOf(navArgument("fileId") { type = NavType.LongType })
            ) { backStackEntry ->
                val fileId = backStackEntry.arguments?.getLong("fileId") ?: return@composable
                FileTrashScreen(
                    fileId = fileId,
                    navController = navController
                )
            }
        }
    }
}
