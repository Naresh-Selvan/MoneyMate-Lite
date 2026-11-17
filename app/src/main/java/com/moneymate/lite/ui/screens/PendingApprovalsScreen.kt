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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingApprovalsScreen(
    navController: NavController,
    viewModel: ApprovalViewModel = hiltViewModel()
) {
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    var requestToReject by remember { mutableStateOf<ApprovalRequestEntity?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var rejectionError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Approvals (${pendingRequests.size})") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (pendingRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pending approvals.\nAll worker actions are up to date.",
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
                items(pendingRequests, key = { it.id }) { request ->
                    ApprovalCard(
                        request = request,
                        dateFormat = dateFormat,
                        isProcessing = isProcessing,
                        onApprove = {
                            viewModel.approve(
                                requestId = request.id,
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("Request approved!") }
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            )
                        },
                        onReject = {
                            rejectionReasonText = ""
                            rejectionError = null
                            requestToReject = request
                        }
                    )
                }
            }
        }
    }

    requestToReject?.let { request ->
        AlertDialog(
            onDismissRequest = { requestToReject = null },
            title = { Text("Reject Request") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Please provide a reason for rejecting this ${request.actionType} request by ${request.workerName}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = {
                            rejectionReasonText = it
                            rejectionError = null
                        },
                        label = { Text("Rejection Reason *") },
                        isError = rejectionError != null,
                        supportingText = rejectionError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonText.trim().isBlank()) {
                            rejectionError = "Rejection reason is required"
                        } else {
                            viewModel.reject(
                                requestId = request.id,
                                reason = rejectionReasonText.trim(),
                                onSuccess = {
                                    requestToReject = null
                                    scope.launch { snackbarHostState.showSnackbar("Request rejected.") }
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToReject = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ApprovalCard(
    request: ApprovalRequestEntity,
    dateFormat: SimpleDateFormat,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val payload = remember(request.proposedPayloadJson) {
        try { JSONObject(request.proposedPayloadJson) } catch (e: Exception) { JSONObject() }
    }
    val base = remember(request.baseSnapshotJson) {
        try { if (!request.baseSnapshotJson.isNullOrBlank()) JSONObject(request.baseSnapshotJson) else null } catch (e: Exception) { null }
    }

    val isConflict = request.status == ApprovalStatus.CONFLICT_STALE.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConflict) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "v${request.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Requested by: ${request.workerName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "File: ${request.fileName} • ${dateFormat.format(Date(request.requestedAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reason: ${request.reason}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))
            DiffContent(request = request, base = base, payload = payload)

            if (isConflict) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "CONFLICT: Record was modified since submission. Fresh review required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Reject")
                }

                Spacer(modifier = Modifier.size(8.dp))

                Button(
                    onClick = onApprove,
                    enabled = !isProcessing && !isConflict,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun DiffContent(
    request: ApprovalRequestEntity,
    base: JSONObject?,
    payload: JSONObject
) {
    Column {
        when (request.actionType) {
            "CREATE_CUSTOMER_WITH_LOAN" -> {
                Text("New Customer: ${payload.optString("customerName")}", style = MaterialTheme.typography.bodySmall)
                val initialLoan = payload.optDouble("initialLoanAmount", 0.0)
                if (initialLoan > 0) {
                    Text("Initial Loan Amount: ₹${"%.0f".format(initialLoan)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
            "CREATE_LOAN", "INCREASE_LOAN" -> {
                val amt = payload.optDouble("amount", payload.optDouble("additionalAmount", 0.0))
                Text("Proposed Loan Amount: ₹${"%.0f".format(amt)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            "EDIT_CUSTOMER" -> {
                base?.let { b ->
                    Text("Current Name: ${b.optString("name")}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Proposed Name: ${payload.optString("name")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (payload.has("mobileNumber")) {
                    Text("Phone: ${payload.optString("mobileNumber")}", style = MaterialTheme.typography.bodySmall)
                }
            }
            "EDIT_PAYMENT" -> {
                base?.let { b ->
                    Text("Current Amount: ₹${"%.0f".format(b.optDouble("amount", 0.0))}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Proposed Amount: ₹${"%.0f".format(payload.optDouble("amount", 0.0))}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            "DELETE_CUSTOMER", "DELETE_LOAN", "DELETE_PAYMENT", "DELETE_FILE" -> {
                Text("Action: Permanent soft deletion of record", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Text("Proposed Changes: ${payload.toString()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
