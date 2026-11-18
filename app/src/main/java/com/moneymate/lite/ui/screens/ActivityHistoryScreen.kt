package com.moneymate.lite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.entity.ActivityLogEntity
import com.moneymate.lite.ui.viewmodel.ActivityLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    navController: NavController,
    activityLogViewModel: ActivityLogViewModel = hiltViewModel()
) {
    val logs by activityLogViewModel.logs.collectAsState()
    val workers by activityLogViewModel.workers.collectAsState()
    val files by activityLogViewModel.files.collectAsState()

    val selectedActorId by activityLogViewModel.selectedActorId.collectAsState()
    val selectedAction by activityLogViewModel.selectedAction.collectAsState()
    val selectedFileSharedId by activityLogViewModel.selectedFileSharedId.collectAsState()

    var showActorMenu by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var selectedLogForDetails by remember { mutableStateOf<ActivityLogEntity?>(null) }

    val distinctActions = remember {
        listOf(
            "PAYMENT_RECORDED",
            "LOAN_CREATED",
            "LOAN_UPDATED",
            "LOAN_DELETED",
            "PERSON_CREATED",
            "PERSON_UPDATED",
            "PERSON_DELETED",
            "FILE_CREATED",
            "FILE_RENAMED",
            "FILE_DELETED",
            "REQUEST_APPROVED",
            "REQUEST_REJECTED",
            "REQUEST_WITHDRAWN",
            "WORKER_CREATED",
            "WORKER_STATUS_CHANGED",
            "MIGRATION_EXECUTED",
            "BACKUP_UPLOADED",
            "RESTORE_EXECUTED"
        )
    }

    val hasActiveFilter = selectedActorId != null || selectedAction != null || selectedFileSharedId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasActiveFilter) {
                        TextButton(onClick = { activityLogViewModel.resetFilters() }) {
                            Text("Clear Filters", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filters bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actor Filter
                Box {
                    FilterChip(
                        selected = selectedActorId != null,
                        onClick = { showActorMenu = true },
                        label = {
                            val actorName = workers.find { it.uid == selectedActorId }?.displayName
                            Text(if (actorName != null) "Actor: $actorName" else "Filter by Actor")
                        },
                        trailingIcon = if (selectedActorId != null) {
                            {
                                IconButton(
                                    onClick = { activityLogViewModel.setFilterActor(null) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    DropdownMenu(
                        expanded = showActorMenu,
                        onDismissRequest = { showActorMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Actors") },
                            onClick = {
                                activityLogViewModel.setFilterActor(null)
                                showActorMenu = false
                            }
                        )
                        workers.forEach { worker ->
                            DropdownMenuItem(
                                text = { Text("${worker.displayName} (${worker.username})") },
                                onClick = {
                                    activityLogViewModel.setFilterActor(worker.uid)
                                    showActorMenu = false
                                }
                            )
                        }
                    }
                }

                // Action Filter
                Box {
                    FilterChip(
                        selected = selectedAction != null,
                        onClick = { showActionMenu = true },
                        label = {
                            Text(if (selectedAction != null) "Action: $selectedAction" else "Filter by Action")
                        },
                        trailingIcon = if (selectedAction != null) {
                            {
                                IconButton(
                                    onClick = { activityLogViewModel.setFilterAction(null) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Actions") },
                            onClick = {
                                activityLogViewModel.setFilterAction(null)
                                showActionMenu = false
                            }
                        )
                        distinctActions.forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.replace("_", " ")) },
                                onClick = {
                                    activityLogViewModel.setFilterAction(act)
                                    showActionMenu = false
                                }
                            )
                        }
                    }
                }

                // File Filter
                Box {
                    FilterChip(
                        selected = selectedFileSharedId != null,
                        onClick = { showFileMenu = true },
                        label = {
                            val fName = files.find { it.sharedId == selectedFileSharedId }?.name
                            Text(if (fName != null) "File: $fName" else "Filter by File")
                        },
                        trailingIcon = if (selectedFileSharedId != null) {
                            {
                                IconButton(
                                    onClick = { activityLogViewModel.setFilterFile(null) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    DropdownMenu(
                        expanded = showFileMenu,
                        onDismissRequest = { showFileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Files") },
                            onClick = {
                                activityLogViewModel.setFilterFile(null)
                                showFileMenu = false
                            }
                        )
                        files.forEach { file ->
                            DropdownMenuItem(
                                text = { Text(file.name) },
                                onClick = {
                                    activityLogViewModel.setFilterFile(file.sharedId)
                                    showFileMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasActiveFilter) "No activities match your filters." else "No activity recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        ActivityLogCard(
                            log = log,
                            onClick = { selectedLogForDetails = log }
                        )
                    }
                }
            }
        }
    }

    if (selectedLogForDetails != null) {
        ActivityDetailsDialog(
            log = selectedLogForDetails!!,
            onDismiss = { selectedLogForDetails = null }
        )
    }
}

@Composable
private fun ActivityLogCard(
    log: ActivityLogEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) {
        dateFormat.format(Date(log.timestamp))
    }

    val actionBadgeColor = when {
        log.action.contains("APPROVED") || log.action.contains("RECORDED") || log.action.contains("CREATED") ->
            Color(0xFFE8F5E9)
        log.action.contains("REJECTED") || log.action.contains("DELETED") ->
            Color(0xFFFFEBEE)
        else -> Color(0xFFEDE7F6)
    }

    val actionTextColor = when {
        log.action.contains("APPROVED") || log.action.contains("RECORDED") || log.action.contains("CREATED") ->
            Color(0xFF2E7D32)
        log.action.contains("REJECTED") || log.action.contains("DELETED") ->
            Color(0xFFC62828)
        else -> Color(0xFF512DA8)
    }

    val friendlyTitle = remember(log.action) { formatActionTitle(log.action) }
    val friendlyDetails = remember(log.details, log.action, log.fileName, log.actorName, log.reason) {
        formatFriendlyDetails(log)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Action badge & time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionBadgeColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = friendlyTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = actionTextColor
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            // Actor line
            val roleText = if (log.actorRole.equals("OWNER", ignoreCase = true)) "Owner" else "Worker"
            Text(
                text = "👤 Done by: ${log.actorName} ($roleText)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // File & Record info if available
            if (!log.fileName.isNullOrBlank()) {
                val typeInfo = if (!log.recordType.isNullOrBlank()) " • ${log.recordType}" else ""
                Text(
                    text = "📁 File: ${log.fileName}$typeInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Prominent "What happened" section - anyone, including dad, understands instantly!
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "What happened:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = friendlyDetails,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!log.reason.isNullOrBlank() && !friendlyDetails.contains(log.reason)) {
                Text(
                    text = "💡 Reason: ${log.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.beforeSnapshotJson.isNullOrBlank() || !log.afterSnapshotJson.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (!log.beforeSnapshotJson.isNullOrBlank()) {
                            val cleanBefore = log.beforeSnapshotJson.removePrefix("{").removeSuffix("}").replace("\"", "")
                            Text(
                                text = "Previous: $cleanBefore",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!log.afterSnapshotJson.isNullOrBlank()) {
                            val cleanAfter = log.afterSnapshotJson.removePrefix("{").removeSuffix("}").replace("\"", "")
                            Text(
                                text = "Changed to: $cleanAfter",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${log.outcome}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (log.outcome == "SUCCESS") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!log.requestId.isNullOrBlank()) {
                        Text(
                            text = "Req: ${log.requestId.take(8)} • ",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "View details ➔",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatActionTitle(action: String): String {
    return when (action) {
        "WORKER_PRIVILEGES_UPDATED" -> "🛡️ Permissions Updated"
        "PAYMENT_RECORDED" -> "💵 Payment Recorded"
        "PAYMENT_DELETED" -> "🗑️ Payment Deleted"
        "PAYMENT_UPDATED" -> "✏️ Payment Updated"
        "LOAN_CREATED" -> "➕ Loan Created"
        "LOAN_UPDATED" -> "✏️ Loan Updated"
        "LOAN_DELETED" -> "🗑️ Loan Deleted"
        "WORKER_LOGIN" -> "🔑 Worker Logged In"
        "WORKER_CREATED" -> "👤 Worker Created"
        "WORKER_STATUS_CHANGED" -> "⚡ Worker Status Changed"
        "SUBMIT_REQUEST" -> "📩 Request Submitted"
        "REQUEST_APPROVED" -> "✅ Request Approved"
        "REQUEST_REJECTED" -> "❌ Request Rejected"
        "REQUEST_WITHDRAWN" -> "↩️ Request Withdrawn"
        "MIGRATION_EXECUTED" -> "🔄 Migration Executed"
        "BACKUP_UPLOADED" -> "☁️ Backup Uploaded"
        "RESTORE_EXECUTED" -> "📥 Backup Restored"
        else -> action.replace("_", " ")
    }
}

private fun formatFriendlyDetails(log: ActivityLogEntity): String {
    val raw = log.details?.trim().orEmpty()
    if (raw.isNotBlank()) {
        if (raw.contains("RecordPayments=")) {
            // Clean up legacy privilege logs
            val workerName = raw.substringAfter("worker '").substringBefore("':")
            val recordPayments = raw.contains("RecordPayments=true")
            val createLoans = raw.contains("DirectCreate=true")
            val editLoans = raw.contains("DirectEdit=true")
            val deleteRecords = raw.contains("Delete=true")
            val allFiles = raw.contains("AllFiles=true")

            return buildString {
                append("Permissions updated for '$workerName':\n")
                append("• Record Payments: ${if (recordPayments) "Allowed" else "Disabled"}\n")
                append("• Create Loans: ${if (createLoans) "Direct (No approval)" else "Requires Owner Approval"}\n")
                append("• Edit Loans: ${if (editLoans) "Direct (No approval)" else "Requires Owner Approval"}\n")
                append("• Delete Records: ${if (deleteRecords) "Allowed" else "Blocked"}\n")
                append("• File Access: ${if (allFiles) "All Files" else "Assigned Files Only"}")
            }
        }
        return raw
    }

    val filePart = if (!log.fileName.isNullOrBlank()) " in file '${log.fileName}'" else ""
    return when (log.action) {
        "WORKER_LOGIN" -> "Worker '${log.actorName}' signed into the app"
        "WORKER_PRIVILEGES_UPDATED" -> "Owner updated permissions and privileges for worker"
        "PAYMENT_RECORDED" -> "Payment was recorded by ${log.actorName}$filePart"
        "PAYMENT_DELETED" -> "Payment was deleted by ${log.actorName}$filePart (Moved to Recently Deleted)"
        "PAYMENT_UPDATED" -> "Payment was edited by ${log.actorName}$filePart"
        "LOAN_CREATED" -> "New loan was created by ${log.actorName}$filePart"
        "LOAN_UPDATED" -> "Loan was modified by ${log.actorName}$filePart"
        "LOAN_DELETED" -> "Loan was deleted by ${log.actorName}$filePart"
        "SUBMIT_REQUEST" -> "${log.actorName} submitted a change request for approval (Reason: ${log.reason ?: "Unspecified"})$filePart"
        "REQUEST_APPROVED" -> "Owner approved the pending change request$filePart"
        "REQUEST_REJECTED" -> "Owner rejected the change request (Reason: ${log.reason ?: "Unspecified"})$filePart"
        "WORKER_CREATED" -> "New worker account was created by ${log.actorName}"
        else -> "${log.action.replace("_", " ")}$filePart"
    }
}

@Composable
private fun ActivityDetailsDialog(
    log: ActivityLogEntity,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) {
        dateFormat.format(Date(log.timestamp))
    }
    val friendlyTitle = remember(log.action) { formatActionTitle(log.action) }
    val friendlyDetails = remember(log.details, log.action, log.fileName, log.actorName, log.reason) {
        formatFriendlyDetails(log)
    }

    // Try to extract customer name and amount if embedded in details
    val customerName = remember(log.details) {
        when {
            log.details?.contains("from '") == true -> log.details.substringAfter("from '").substringBefore("'")
            log.details?.contains("to '") == true -> log.details.substringAfter("to '").substringBefore("'")
            log.details?.contains("for customer '") == true -> log.details.substringAfter("for customer '").substringBefore("'")
            else -> null
        }
    }
    val amountStr = remember(log.details) {
        if (log.details?.contains("₹") == true) {
            "₹" + log.details.substringAfter("₹").takeWhile { it.isDigit() || it == '.' || it == ',' }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = friendlyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Actor info (Who collected it / performed action)
                val roleText = if (log.actorRole.equals("OWNER", ignoreCase = true)) "Owner" else "Worker"
                DetailRow(
                    icon = "👤",
                    label = "Performed By",
                    value = "${log.actorName} ($roleText)"
                )

                // Customer info (Whom it was collected from / given to)
                if (customerName != null) {
                    DetailRow(
                        icon = "🧑",
                        label = "Customer",
                        value = customerName
                    )
                }

                // Amount
                if (amountStr != null) {
                    DetailRow(
                        icon = "💰",
                        label = "Amount",
                        value = amountStr,
                        isBold = true,
                        valueColor = if (log.action.contains("PAYMENT")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }

                // File Name
                if (!log.fileName.isNullOrBlank()) {
                    DetailRow(
                        icon = "📁",
                        label = "File",
                        value = log.fileName
                    )
                }

                // Date & Time
                DetailRow(
                    icon = "📅",
                    label = "Date & Time",
                    value = formattedTime
                )

                // What happened / Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Full Summary",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = friendlyDetails,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Reason if present
                if (!log.reason.isNullOrBlank()) {
                    DetailRow(
                        icon = "💡",
                        label = "Reason",
                        value = log.reason
                    )
                }

                // Changes if present
                if (!log.beforeSnapshotJson.isNullOrBlank() || !log.afterSnapshotJson.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Changes Recorded",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!log.beforeSnapshotJson.isNullOrBlank()) {
                                Text(
                                    text = "Before: ${log.beforeSnapshotJson.removePrefix("{").removeSuffix("}").replace("\"", "")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!log.afterSnapshotJson.isNullOrBlank()) {
                                Text(
                                    text = "After: ${log.afterSnapshotJson.removePrefix("{").removeSuffix("}").replace("\"", "")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Status
                DetailRow(
                    icon = "📌",
                    label = "Outcome Status",
                    value = log.outcome,
                    valueColor = if (log.outcome == "SUCCESS") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    isBold = true
                )

                if (!log.requestId.isNullOrBlank()) {
                    DetailRow(
                        icon = "🏷️",
                        label = "Request ID",
                        value = log.requestId
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(
    icon: String,
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.bodyMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
