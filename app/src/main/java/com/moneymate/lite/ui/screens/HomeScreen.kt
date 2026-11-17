package com.moneymate.lite.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.ui.viewmodel.ApprovalViewModel
import com.moneymate.lite.ui.viewmodel.AuthViewModel
import com.moneymate.lite.ui.viewmodel.LoanFileViewModel
import java.net.URLEncoder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: LoanFileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val files by viewModel.files.collectAsState()
    val sortedFiles = remember(files) {
        files.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
    val userRole by authViewModel.userRole.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val failedSyncCount by viewModel.failedSyncCount.collectAsState()
    val pendingApprovalsCount by approvalViewModel.pendingCount.collectAsState()

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
                    if (userRole == UserRole.OWNER) {
                        IconButton(onClick = { navController.navigate("pending_approvals") }) {
                            Box {
                                Icon(
                                    Icons.Default.PendingActions,
                                    contentDescription = "Pending Approvals",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (pendingApprovalsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = pendingApprovalsCount.toString(),
                                            color = MaterialTheme.colorScheme.onError,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else if (userRole == UserRole.WORKER) {
                        IconButton(onClick = { navController.navigate("my_requests") }) {
                            Icon(
                                Icons.Default.AssignmentTurnedIn,
                                contentDescription = "My Requests",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(onClick = { showLogoutConfirm = true }) {
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
            // Workers cannot create files
            if (userRole != UserRole.WORKER) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add file")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Live Connectivity & Sync Status Banner
            SyncStatusBanner(
                isOnline = isOnline,
                isSyncing = isSyncing,
                pendingSyncCount = pendingSyncCount,
                failedSyncCount = failedSyncCount,
                onRetry = { viewModel.refreshSync() }
            )

            if (sortedFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userRole == UserRole.WORKER) "No files assigned to your account yet." else "No files yet. Tap + to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(
                        items = sortedFiles,
                        key = { it.id }
                    ) { file ->
                        FileCard(
                            file = file,
                            isWorker = userRole == UserRole.WORKER,
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
private fun SyncStatusBanner(
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingSyncCount: Int,
    failedSyncCount: Int,
    onRetry: () -> Unit
) {
    if (!isOnline) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                Text(
                    text = if (pendingSyncCount > 0) "Offline mode • $pendingSyncCount payment(s) pending sync" else "Offline mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE65100)
                )
            }
        }
    } else if (failedSyncCount > 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFEBEE))
                .clickable { onRetry() }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SyncProblem, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                    Text(
                        text = "$failedSyncCount sync item(s) failed. Tap to retry.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    } else if (isSyncing || pendingSyncCount > 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1565C0)
                )
                Text(
                    text = if (isSyncing) "Syncing with cloud..." else "$pendingSyncCount change(s) queued for sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

@Composable
private fun FileCard(
    file: LoanFile,
    isWorker: Boolean,
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
                        text = file.name.ifBlank { "Unnamed File" },
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

            // Workers cannot rename or delete files
            if (!isWorker) {
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
}
