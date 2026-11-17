package com.moneymate.lite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.ui.theme.LocalThemeManager
import com.moneymate.lite.ui.viewmodel.ActivityLogViewModel
import com.moneymate.lite.ui.viewmodel.ApprovalViewModel
import com.moneymate.lite.ui.viewmodel.AppUpdateInfo
import com.moneymate.lite.ui.viewmodel.AuthViewModel
import com.moneymate.lite.ui.viewmodel.SettingsViewModel
import com.moneymate.lite.ui.viewmodel.UpdateViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val isBackingUp by settingsViewModel.isBackingUp.collectAsState()
    val isRestoring by settingsViewModel.isRestoring.collectAsState()
    val isMigrating by settingsViewModel.isMigrating.collectAsState()
    val migrationProgress by settingsViewModel.migrationProgress.collectAsState()
    val migrationPreview by settingsViewModel.migrationPreview.collectAsState()

    val pendingCount by approvalViewModel.pendingCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showMigrationDialog by remember { mutableStateOf(false) }

    val themeManager = LocalThemeManager.current
    val isDarkMode by themeManager.isDarkMode.collectAsState()
    var activeUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val isCheckingUpdates by updateViewModel.isChecking.collectAsState()

    val currentUser by authViewModel.currentUser.collectAsState()
    val currentProfile by authViewModel.currentProfile.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    val businessId by authViewModel.businessId.collectAsState()
    val isOfflineMode by authViewModel.isOfflineMode.collectAsState()
    val isUserLoggedIn = currentUser != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account & Business Info Section
            Text(
                text = "Account & Role",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOfflineMode) "Offline Guest" else (currentUser?.displayName ?: currentUser?.email ?: "User"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val roleColor = when (userRole) {
                            UserRole.OWNER -> Color(0xFF2E7D32)
                            UserRole.WORKER -> Color(0xFF1565C0)
                            UserRole.GUEST -> Color(0xFF757575)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(roleColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = userRole.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = roleColor
                            )
                        }
                    }

                    if (businessId.isNotBlank()) {
                        Text(
                            text = "Business ID: $businessId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (userRole == UserRole.WORKER && currentProfile != null) {
                        val accessText = if (currentProfile?.accessAllFiles == true) "All Files" else "${currentProfile?.allowedFileIds?.size ?: 0} selected files"
                        Text(
                            text = "File Access: $accessText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Owner Management Section
            if (userRole == UserRole.OWNER) {
                Text(
                    text = "Business Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Manage Workers Card
                Card(
                    onClick = { navController.navigate("manage_workers") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Manage Workers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Provision accounts, assign file access, reset passwords", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Pending Approvals Card with Count Badge
                Card(
                    onClick = { navController.navigate("pending_approvals") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Pending Approvals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Review and approve worker proposals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (pendingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = pendingCount.toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Activity History Card
                Card(
                    onClick = { navController.navigate("activity_history") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Activity History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Audit trail of payments, approvals, and edits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Business Migration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Business Data Migration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Assign existing personal data to your business ID and generate UUIDs for worker sharing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Button(
                            onClick = {
                                settingsViewModel.loadMigrationPreview()
                                showMigrationDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Preview & Migrate Business Data")
                        }
                    }
                }
            }

            // Worker Section: My Requests
            if (userRole == UserRole.WORKER) {
                Text(
                    text = "Worker Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    onClick = { navController.navigate("my_requests") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("My Submitted Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Check pending approval status, rejection reasons, or withdraw", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Backup & Restore Section (Owner Only per Requirement 7)
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (userRole != UserRole.OWNER) {
                        Text(
                            text = "Manual Cloud Backup & Restore is restricted to the Business Owner. Worker data syncs automatically in the background.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isOfflineMode || !isUserLoggedIn) {
                        Text(
                            text = "Please sign in with your Owner Google account for backup functions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Backup business data to cloud snapshot or restore from a previous backup. Historical deletion markers will be preserved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val backupProgressText by settingsViewModel.backupProgress.collectAsState()

                        Button(
                            onClick = {
                                settingsViewModel.backup { result ->
                                    scope.launch {
                                        val msg = if (result.isSuccess) "Backup complete"
                                        else "Backup failed: ${result.exceptionOrNull()?.message}"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBackingUp && !isRestoring
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp).size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(if (isBackingUp) backupProgressText ?: "Backing up..." else "Backup Now")
                        }

                        Button(
                            onClick = { showRestoreConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBackingUp && !isRestoring,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp).size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                            Text(if (isRestoring) "Restoring..." else "Restore from Cloud")
                        }
                    }
                }
            }

            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = { themeManager.toggleDarkMode() },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDarkMode) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { themeManager.toggleDarkMode() }
                    )
                }
            }

            // Trash & Utilities
            Text(
                text = "Trash & Utilities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = { navController.navigate("recently_deleted") },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Recently Deleted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View and restore deleted files or customers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Updates Section
            Text(
                text = "Updates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Current Version: v${updateViewModel.getAppVersionName()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            updateViewModel.checkForUpdates { info ->
                                scope.launch {
                                    if (info != null) {
                                        activeUpdateInfo = info
                                    } else {
                                        snackbarHostState.showSnackbar("App is up to date!")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCheckingUpdates
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp).size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(if (isCheckingUpdates) "Checking..." else "Check for Update")
                    }
                }
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

    // Owner Migration Dialog
    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { if (!isMigrating) showMigrationDialog = false },
            title = { Text("Business Data Migration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This process creates a recoverable local snapshot, assigns all existing files, customers, loans, and payments to your Business ID ($businessId), and assigns UUIDs for multi-device sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    if (migrationPreview != null) {
                        val p = migrationPreview!!
                        Text("Files: ${p.fileCount}", style = MaterialTheme.typography.bodyMedium)
                        Text("Customers: ${p.customerCount}", style = MaterialTheme.typography.bodyMedium)
                        Text("Loans: ${p.loanCount} (Total: ₹${String.format(Locale.getDefault(), "%,.0f", p.totalLoanAmount)})", style = MaterialTheme.typography.bodyMedium)
                        Text("Payments: ${p.paymentCount} (Total: ₹${String.format(Locale.getDefault(), "%,.0f", p.totalPaidAmount)})", style = MaterialTheme.typography.bodyMedium)
                        Text("Outstanding: ₹${String.format(Locale.getDefault(), "%,.0f", p.totalOutstandingBalance)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                        if (p.isAlreadyMigrated) {
                            Text(
                                "All records already belong to this business ID.",
                                color = Color(0xFF2E7D32),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    if (isMigrating && migrationProgress != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = migrationProgress!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.executeMigration { result ->
                            scope.launch {
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("Migration completed successfully!")
                                    showMigrationDialog = false
                                } else {
                                    snackbarHostState.showSnackbar("Migration failed: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    },
                    enabled = !isMigrating && migrationPreview != null
                ) {
                    if (isMigrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp).size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(if (isMigrating) "Migrating..." else "Run Migration")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMigrationDialog = false },
                    enabled = !isMigrating
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from Cloud") },
            text = {
                Text("This will add cloud data to your local database without overwriting existing records. Continue?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    settingsViewModel.restore { result ->
                        scope.launch {
                            val msg = if (result.isSuccess) {
                                "Restore complete: ${result.getOrNull()}"
                            } else {
                                "Restore failed: ${result.exceptionOrNull()?.message}"
                            }
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
