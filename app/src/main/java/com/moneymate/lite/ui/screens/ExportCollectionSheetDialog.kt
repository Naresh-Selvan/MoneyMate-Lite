package com.moneymate.lite.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymate.lite.ui.viewmodel.LoanViewModel
import com.moneymate.lite.ui.viewmodel.PersonViewModel
import com.moneymate.lite.util.CollectionSheetPdfGenerator
import com.moneymate.lite.util.CollectionSheetRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExportAction { PRINT, SHARE, DOWNLOAD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCollectionSheetDialog(
    fileId: Long,
    fileName: String,
    onDismiss: () -> Unit,
    onError: (String) -> Unit = {},
    personViewModel: PersonViewModel = hiltViewModel(),
    loanViewModel: LoanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportPdfDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var exportOnlyActiveBalances by rememberSaveable { mutableStateOf(true) }
    var showExportDatePicker by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (showExportDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = exportPdfDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showExportDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        exportPdfDateMillis = it
                    }
                    showExportDatePicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun exportSheet(action: ExportAction) {
        if (isGenerating) return
        isGenerating = true
        scope.launch {
            try {
                // Calculate end of selected day (23:59:59.999) to include all transactions up to that date
                val endOfDayMillis = Calendar.getInstance().apply {
                    timeInMillis = exportPdfDateMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val persons = personViewModel.getPersonsByFile(fileId).first()
                val balances = loanViewModel.getBalancesUpToDate(fileId, endOfDayMillis).first()
                val balancesMap = balances.associateBy { it.personId }

                val rows = persons.mapNotNull { p ->
                    val bal = balancesMap[p.id]?.balance ?: 0.0
                    if (!exportOnlyActiveBalances || bal > 0.0) {
                        CollectionSheetRow(customerName = p.name, balance = bal)
                    } else null
                }

                if (rows.isEmpty()) {
                    isGenerating = false
                    onError("No customers found for this file on the selected date.")
                    return@launch
                }

                val pdfFile = CollectionSheetPdfGenerator.generatePdf(
                    context = context,
                    fileName = fileName,
                    dateMillis = exportPdfDateMillis,
                    rows = rows
                )

                when (action) {
                    ExportAction.PRINT -> {
                        CollectionSheetPdfGenerator.printPdf(context, pdfFile, "Collection_Sheet_$fileName")
                        isGenerating = false
                        onDismiss()
                    }
                    ExportAction.SHARE -> {
                        CollectionSheetPdfGenerator.shareOrOpenPdf(context, pdfFile, "Collection Sheet - $fileName")
                        isGenerating = false
                        onDismiss()
                    }
                    ExportAction.DOWNLOAD -> {
                        val downloadResult = CollectionSheetPdfGenerator.downloadPdfToStorage(
                            context = context,
                            pdfFile = pdfFile,
                            displayName = fileName,
                            dateMillis = exportPdfDateMillis
                        )
                        isGenerating = false
                        if (downloadResult.isSuccess) {
                            onError(downloadResult.getOrNull() ?: "Downloaded successfully!")
                        } else {
                            onError("Download failed: ${downloadResult.exceptionOrNull()?.message}")
                        }
                        onDismiss()
                    }
                }
            } catch (e: Exception) {
                isGenerating = false
                onError("Failed to generate sheet: ${e.message}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        icon = {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Export Collection Sheet") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Formatted for US Legal paper (8.5 × 14 in) with 2 side-by-side tables. Shows individual customer balances up to the chosen date followed by 7 blank collection boxes. Overall outstanding total is excluded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = dateFormat.format(Date(exportPdfDateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Balance Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = { showExportDatePicker = true },
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isGenerating
                ) {
                    Text("Change Date")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Active balances only",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Only include customers with balance > ₹0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = exportOnlyActiveBalances,
                        onCheckedChange = { exportOnlyActiveBalances = it },
                        enabled = !isGenerating
                    )
                }

                if (isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Calculating balances & preparing PDF...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { exportSheet(ExportAction.PRINT) },
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Print")
                }
                OutlinedButton(
                    onClick = { exportSheet(ExportAction.SHARE) },
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Share")
                }
                Button(
                    onClick = { exportSheet(ExportAction.DOWNLOAD) },
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Download")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
}
