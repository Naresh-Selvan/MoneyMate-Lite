package com.moneymate.lite.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.WorkerProfileEntity
import com.moneymate.lite.ui.viewmodel.WorkerViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWorkersScreen(
    navController: NavController,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val workers by viewModel.workers.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var workerToResetPassword by remember { mutableStateOf<WorkerProfileEntity?>(null) }
    var workerToEditAccess by remember { mutableStateOf<WorkerProfileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Workers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                Icon(Icons.Default.Add, contentDescription = "Add Worker")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading && workers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (workers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workers added yet.\nTap + to create a worker account.",
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workers, key = { it.uid }) { worker ->
                    WorkerCard(
                        worker = worker,
                        allFiles = allFiles,
                        onResetPassword = { workerToResetPassword = worker },
                        onEditAccess = { workerToEditAccess = worker },
                        onToggleStatus = {
                            val newStatus = !worker.isActive
                            viewModel.toggleWorkerStatus(
                                workerUid = worker.uid,
                                isActive = newStatus,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (newStatus) "Worker enabled" else "Worker disabled"
                                        )
                                    }
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWorkerDialog(
            allFiles = allFiles,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, username, tempPassword, accessAll, selectedFiles ->
                viewModel.createWorker(
                    displayName = name,
                    username = username,
                    tempPassword = tempPassword,
                    accessAllFiles = accessAll,
                    allowedFileIds = selectedFiles,
                    onSuccess = {
                        showAddDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Worker account created!") }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                    }
                )
            }
        )
    }

    workerToResetPassword?.let { worker ->
        ResetWorkerPasswordDialog(
            worker = worker,
            onDismiss = { workerToResetPassword = null },
            onConfirm = { tempPassword ->
                viewModel.resetPassword(
                    workerUid = worker.uid,
                    tempPassword = tempPassword,
                    onSuccess = {
                        workerToResetPassword = null
                        scope.launch { snackbarHostState.showSnackbar("Password reset successfully!") }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                    }
                )
            }
        )
    }

    workerToEditAccess?.let { worker ->
        EditFileAccessDialog(
            worker = worker,
            allFiles = allFiles,
            onDismiss = { workerToEditAccess = null },
            onConfirm = { accessAll, selectedFiles ->
                viewModel.updateWorkerFileAccess(
                    workerUid = worker.uid,
                    accessAllFiles = accessAll,
                    allowedFileIds = selectedFiles,
                    onSuccess = {
                        workerToEditAccess = null
                        scope.launch { snackbarHostState.showSnackbar("Permissions updated!") }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                    }
                )
            }
        )
    }
}

@Composable
private fun WorkerCard(
    worker: WorkerProfileEntity,
    allFiles: List<LoanFile>,
    onResetPassword: () -> Unit,
    onEditAccess: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val accessSummary = remember(worker.accessAllFiles, worker.allowedFileIdsJson, allFiles) {
        if (worker.accessAllFiles) {
            "All Files"
        } else {
            val count = try {
                JSONArray(worker.allowedFileIdsJson).length()
            } catch (e: Exception) {
                0
            }
            "$count specific file(s)"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (worker.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = worker.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    if (worker.isActive) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Text(
                            text = "Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${worker.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Access: $accessSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (worker.mustChangePassword) {
                    Text(
                        text = "Must change password on next login",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reset Password") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onResetPassword()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit File Access") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEditAccess()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (worker.isActive) "Disable Account" else "Enable Account") },
                        leadingIcon = {
                            Icon(
                                if (worker.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (worker.isActive) Color(0xFFE53935) else Color(0xFF2E7D32)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleStatus()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWorkerDialog(
    allFiles: List<LoanFile>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, username: String, tempPassword: String, accessAll: Boolean, selectedFiles: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var tempPassword by remember { mutableStateOf("") }
    var accessAll by remember { mutableStateOf(true) }
    val selectedFiles = remember { mutableStateListOf<String>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Worker Account") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() },
                    label = { Text("Username * (lowercase, letters/numbers)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = { tempPassword = it },
                    label = { Text("Temporary Password * (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("File Access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = accessAll, onClick = { accessAll = true })
                    Text("All Files", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !accessAll, onClick = { accessAll = false })
                    Text("Specific Files Only", style = MaterialTheme.typography.bodyMedium)
                }

                if (!accessAll) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        allFiles.forEach { file ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = file.sharedId in selectedFiles,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedFiles.add(file.sharedId)
                                        else selectedFiles.remove(file.sharedId)
                                    }
                                )
                                Text(file.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || username.isBlank() || tempPassword.isBlank()) {
                        errorMessage = "Please fill all required fields"
                    } else if (tempPassword.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                    } else if (!accessAll && selectedFiles.isEmpty()) {
                        errorMessage = "Please select at least one file"
                    } else {
                        onConfirm(name, username, tempPassword, accessAll, selectedFiles.toList())
                    }
                }
            ) {
                Text("Create Worker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ResetWorkerPasswordDialog(
    worker: WorkerProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: (tempPassword: String) -> Unit
) {
    var tempPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Set a new temporary password for @${worker.username}. The worker will be required to change it on their next login.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = { tempPassword = it },
                    label = { Text("New Temporary Password *") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tempPassword.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                    } else {
                        onConfirm(tempPassword)
                    }
                }
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditFileAccessDialog(
    worker: WorkerProfileEntity,
    allFiles: List<LoanFile>,
    onDismiss: () -> Unit,
    onConfirm: (accessAll: Boolean, selectedFiles: List<String>) -> Unit
) {
    var accessAll by remember { mutableStateOf(worker.accessAllFiles) }
    val selectedFiles = remember {
        val initial = try {
            val arr = JSONArray(worker.allowedFileIdsJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        mutableStateListOf<String>().apply { addAll(initial) }
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit File Access: @${worker.username}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = accessAll, onClick = { accessAll = true })
                    Text("All Files", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !accessAll, onClick = { accessAll = false })
                    Text("Specific Files Only", style = MaterialTheme.typography.bodyMedium)
                }

                if (!accessAll) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        allFiles.forEach { file ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = file.sharedId in selectedFiles,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedFiles.add(file.sharedId)
                                        else selectedFiles.remove(file.sharedId)
                                    }
                                )
                                Text(file.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!accessAll && selectedFiles.isEmpty()) {
                        errorMessage = "Please select at least one file"
                    } else {
                        onConfirm(accessAll, selectedFiles.toList())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
