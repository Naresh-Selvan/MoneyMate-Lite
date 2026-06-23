package com.moneymate.lite.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.ui.viewmodel.AuthViewModel
import com.moneymate.lite.ui.viewmodel.LoanFileViewModel
import androidx.compose.runtime.LaunchedEffect
import java.net.URLEncoder
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: LoanFileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val files by viewModel.files.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<LoanFile?>(null) }
    var fileToRename by remember { mutableStateOf<LoanFile?>(null) }
    var renameText by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MoneyMate Lite") },
                actions = {
                    IconButton(onClick = {
                        showLogoutConfirm = true
                    }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add file")
            }
        }
    ) { paddingValues ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = files,
                    key = { it.id }
                ) { file ->
                    FileCard(
                        file = file,
                        onDelete = { fileToDelete = file },
                        onRename = {
                            fileToRename = file
                            renameText = file.name
                        },
                        onClick = {
                            navController.navigate("file_detail/${file.id}?name=${URLEncoder.encode(file.name, "UTF-8")}")
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newFileName = ""
            },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToSave = newFileName.trim()
                        if (nameToSave.isNotBlank()) {
                            scope.launch {
                                viewModel.addFile(nameToSave)
                            }
                        }
                        showAddDialog = false
                        newFileName = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newFileName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isOfflineMode by authViewModel.isOfflineMode.collectAsState()

    if (showLogoutConfirm) {
        val dialogTitle = if (isOfflineMode) "Exit Offline Mode" else "Sign Out"
        val dialogText = if (isOfflineMode) "Exit offline mode and return to the login screen?" else "Are you sure you want to sign out?"
        val confirmText = if (isOfflineMode) "Exit" else "Sign Out"

        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text(confirmText, color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete \"${fileToDelete?.name}\"? This will move it to recently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { file ->
                            scope.launch { viewModel.deleteFile(file.id) }
                        }
                        fileToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (fileToRename != null) {
        var customRenameText by remember { mutableStateOf(renameText) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = customRenameText,
                    onValueChange = { customRenameText = it },
                    label = { Text("New file name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToRename?.let { file ->
                            val newName = customRenameText.trim()
                            if (newName.isNotBlank()) {
                                scope.launch { viewModel.renameFile(file.id, newName) }
                            }
                        }
                        fileToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileCard(
    file: LoanFile,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = file.name.ifEmpty { "Unnamed File" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap to open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFE53935)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
