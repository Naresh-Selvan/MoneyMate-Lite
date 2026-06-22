package com.moneymate.lite.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.ui.viewmodel.LoanViewModel
import com.moneymate.lite.ui.viewmodel.PaymentViewModel
import com.moneymate.lite.ui.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    navController: NavController,
    personViewModel: PersonViewModel = hiltViewModel(),
    loanViewModel: LoanViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    val activeLoan by loanViewModel.getActiveLoanFlow(personId).collectAsState()
    val allLoans by loanViewModel.getLoansByPerson(personId).collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var person by remember { mutableStateOf<Person?>(null) }
    var showNewLoanDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var expandedLoanId by remember { mutableStateOf<Long?>(null) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }

    // Fetch person data once
    LaunchedEffect(personId) {
        person = personViewModel.getPersonById(personId)
    }

    // Payments for active loan — keyed on the loan ID, not the entire Loan object,
    // so we don't restart the flow on every DB emission of the same loan.
    val activeLoanId = activeLoan?.id
    val activePayments by remember(activeLoanId) {
        if (activeLoanId != null) {
            loanViewModel.getPaymentsByLoan(activeLoanId)
        } else {
            kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        }
    }.collectAsState()

    val activeLoanTotalPaid = activePayments.sumOf { it.amount }
    val activeLoanBalance = if (activeLoan != null) activeLoan!!.totalAmount - activeLoanTotalPaid else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.name ?: "Person Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header section
            item {
                person?.let { p ->
                    Text(
                        text = p.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    p.mobileNumber?.let { mobile ->
                        Text(
                            text = mobile,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    p.place?.let { place ->
                        Text(
                            text = place,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            // Active loan card or No active loan section
            item {
                if (activeLoan != null) {
                    ActiveLoanCard(
                        totalAmount = activeLoan!!.totalAmount,
                        totalPaid = activeLoanTotalPaid,
                        balance = activeLoanBalance,
                        dateGiven = activeLoan!!.dateGiven,
                        dateFormat = dateFormat,
                        onRecordPayment = { showRecordPaymentDialog = true }
                    )
                } else {
                    NoActiveLoanCard(onCreateLoan = { showNewLoanDialog = true })
                }
            }

            // Loan History header
            if (allLoans.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Loan History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Loan history items
                items(allLoans, key = { it.id }) { loan ->
                    LoanHistoryItem(
                        loan = loan,
                        isExpanded = expandedLoanId == loan.id,
                        dateFormat = dateFormat,
                        onToggle = {
                            expandedLoanId = if (expandedLoanId == loan.id) null else loan.id
                        },
                        onDeletePayment = { paymentToDelete = it }
                    )
                }
            }
        }
    }

    // New Loan Dialog
    if (showNewLoanDialog) {
        NewLoanDialog(
            personId = personId,
            viewModel = loanViewModel,
            onDismiss = { showNewLoanDialog = false },
            onLoanCreated = {
                showNewLoanDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Loan created")
                }
            }
        )
    }

    // Record Payment Dialog
    if (showRecordPaymentDialog && activeLoan != null) {
        RecordPaymentDialog(
            loanId = activeLoan!!.id,
            currentBalance = activeLoanBalance,
            viewModel = paymentViewModel,
            onDismiss = { showRecordPaymentDialog = false },
            onPaymentRecorded = { wasCompleted ->
                showRecordPaymentDialog = false
                scope.launch {
                    val msg = if (wasCompleted) "Loan completed! 🎉" else "Payment recorded"
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    if (paymentToDelete != null) {
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Delete Payment") },
            text = { Text("Are you sure you want to delete this payment of ₹${"%.0f".format(paymentToDelete?.amount ?: 0.0)}? This will move it to recently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        paymentToDelete?.let { payment ->
                            loanViewModel.deletePayment(payment.id)
                        }
                        paymentToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActiveLoanCard(
    totalAmount: Double,
    totalPaid: Double,
    balance: Double,
    dateGiven: Long,
    dateFormat: SimpleDateFormat,
    onRecordPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Active Loan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total:", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    text = "₹${"%.0f".format(totalAmount)}",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Paid:", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    text = "₹${"%.0f".format(totalPaid)}",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Balance:", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                Text(
                    text = "₹${"%.0f".format(balance)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date Given:", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    text = dateFormat.format(Date(dateGiven)),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRecordPayment,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Payment")
            }
        }
    }
}

@Composable
private fun NoActiveLoanCard(
    onCreateLoan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No active loan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onCreateLoan) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Loan")
            }
        }
    }
}

@Composable
private fun LoanHistoryItem(
    loan: Loan,
    isExpanded: Boolean,
    dateFormat: SimpleDateFormat,
    onToggle: () -> Unit,
    onDeletePayment: (Payment) -> Unit,
    loanViewModel: LoanViewModel = hiltViewModel()
) {
    val payments by remember(loan.id) {
        loanViewModel.getPaymentsByLoan(loan.id)
    }.collectAsState()

    val totalPaid = payments.sumOf { it.amount }
    val balance = loan.totalAmount - totalPaid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "₹${"%.0f".format(loan.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(loan.dateGiven)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(isCompleted = loan.isCompleted)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (loan.isCompleted && loan.completedAt != null) {
                Text(
                    text = "Completed: ${dateFormat.format(Date(loan.completedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded payment details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Payments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (payments.isEmpty()) {
                        Text(
                            text = "No payments recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        payments.forEach { payment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(payment.date)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${"%.0f".format(payment.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeletePayment(payment) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete payment",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Paid",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "₹${"%.0f".format(totalPaid)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (balance > 0 && !loan.isCompleted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE53935)
                            )
                            Text(
                                text = "₹${"%.0f".format(balance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(isCompleted: Boolean) {
    val (label, bgColor, textColor) = if (isCompleted) {
        Triple("Completed", Color(0xFFF5F5F5), Color(0xFF616161))
    } else {
        Triple("Active", Color(0xFFE8F5E9), Color(0xFF2E7D32))
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
