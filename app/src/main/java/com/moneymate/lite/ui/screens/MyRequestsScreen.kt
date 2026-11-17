package com.moneymate.lite.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.moneymate.lite.data.entity.ApprovalRequestEntity
import com.moneymate.lite.data.model.ApprovalStatus
import com.moneymate.lite.ui.viewmodel.ApprovalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    navController: NavController,
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val requests by approvalViewModel.workerRequests.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var requestToWithdraw by remember { mutableStateOf<ApprovalRequestEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Submitted Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You haven't submitted any approval requests yet.",
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
                items(requests, key = { it.id }) { request ->
                    WorkerRequestCard(
                        request = request,
                        onWithdraw = { requestToWithdraw = request }
                    )
                }
            }
        }
    }

    requestToWithdraw?.let { req ->
        AlertDialog(
            onDismissRequest = { requestToWithdraw = null },
            title = { Text("Withdraw Request") },
            text = { Text("Are you sure you want to withdraw this approval request? It will be marked as withdrawn.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toWithdraw = req
                        requestToWithdraw = null
                        approvalViewModel.withdraw(
                            requestId = toWithdraw.id,
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Request withdrawn successfully")
                                }
                            },
                            onError = { err ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error: $err")
                                }
                            }
                        )
                    }
                ) {
                    Text("Withdraw", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToWithdraw = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WorkerRequestCard(
    request: ApprovalRequestEntity,
    onWithdraw: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(request.requestedAt) {
        dateFormat.format(Date(request.requestedAt))
    }

    val (badgeColor, badgeTextColor, statusLabel, statusIcon) = when (request.status) {
        ApprovalStatus.PENDING.name -> {
            if (request.syncStatus == "NOT_SUBMITTED") {
                Tuple4(Color(0xFFEEEEEE), Color(0xFF616161), "Not submitted", Icons.Default.Info)
            } else {
                Tuple4(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending approval", Icons.Default.HourglassTop)
            }
        }
        ApprovalStatus.APPROVED.name ->
            Tuple4(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Approved", Icons.Default.CheckCircle)
        ApprovalStatus.REJECTED.name ->
            Tuple4(Color(0xFFFFEBEE), Color(0xFFC62828), "Rejected", Icons.Default.Cancel)
        ApprovalStatus.WITHDRAWN.name ->
            Tuple4(Color(0xFFEEEEEE), Color(0xFF757575), "Withdrawn", Icons.Default.Cancel)
        ApprovalStatus.CONFLICT_STALE.name ->
            Tuple4(Color(0xFFFFF8E1), Color(0xFFF57F17), "Stale Conflict", Icons.Default.Warning)
        else ->
            Tuple4(Color(0xFFEEEEEE), Color(0xFF616161), request.status, Icons.Default.Info)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = request.actionType.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            val payloadJson = remember(request.proposedPayloadJson) {
                try { org.json.JSONObject(request.proposedPayloadJson) } catch (e: Exception) { null }
            }
            val baseJson = remember(request.baseSnapshotJson) {
                try { request.baseSnapshotJson?.let { org.json.JSONObject(it) } } catch (e: Exception) { null }
            }
            val customerName = payloadJson?.optString("customerName")?.takeIf { it.isNotBlank() }
                ?: baseJson?.optString("customerName")?.takeIf { it.isNotBlank() }

            if (!customerName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Customer: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val oldAmt = baseJson?.optDouble("amount") ?: payloadJson?.optDouble("oldAmount")
            val newAmt = payloadJson?.optDouble("amount")
            if (newAmt != null && oldAmt != null && oldAmt > 0 && oldAmt != newAmt) {
                Text(
                    text = "Amount: ₹${"%.0f".format(oldAmt)} ➔ ₹${"%.0f".format(newAmt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (newAmt != null && newAmt > 0) {
                Text(
                    text = "Proposed Amount: ₹${"%.0f".format(newAmt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "File: ${request.fileName} • ${request.recordType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Text(
                text = "Submitted: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Text(
                text = "Reason: ${request.reason}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!request.rejectionReason.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Owner Rejection Reason: ${request.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (request.status == ApprovalStatus.PENDING.name) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Withdraw Request")
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
