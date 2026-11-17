package com.moneymate.lite.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
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
                        ActivityLogCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogCard(log: ActivityLogEntity) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) {
        dateFormat.format(Date(log.timestamp))
    }

    val actionBadgeColor = when {
        log.action.contains("APPROVED") || log.action.contains("RECORDED") || log.action.contains("CREATED") ->
            Color(0xFFE8F5E9)
        log.action.contains("REJECTED") || log.action.contains("DELETED") ->
            Color(0xFFFFEBEE)
        else -> Color(0xFFE3F2FD)
    }

    val actionTextColor = when {
        log.action.contains("APPROVED") || log.action.contains("RECORDED") || log.action.contains("CREATED") ->
            Color(0xFF2E7D32)
        log.action.contains("REJECTED") || log.action.contains("DELETED") ->
            Color(0xFFC62828)
        else -> Color(0xFF1565C0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionBadgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = log.action.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = actionTextColor
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "Actor: ${log.actorName} (${log.actorRole})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!log.fileName.isNullOrBlank()) {
                Text(
                    text = "File: ${log.fileName} • ${log.recordType ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.reason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${log.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!log.beforeSnapshotJson.isNullOrBlank() || !log.afterSnapshotJson.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!log.beforeSnapshotJson.isNullOrBlank()) {
                            Text(
                                text = "Before: ${log.beforeSnapshotJson}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!log.afterSnapshotJson.isNullOrBlank()) {
                            Text(
                                text = "After: ${log.afterSnapshotJson}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Outcome: ${log.outcome}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (!log.requestId.isNullOrBlank()) {
                    Text(
                        text = "Req: ${log.requestId.take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
