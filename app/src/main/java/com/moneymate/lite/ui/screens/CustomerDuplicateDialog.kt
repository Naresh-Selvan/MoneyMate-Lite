package com.moneymate.lite.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymate.lite.data.sync.CustomerDuplicate
import com.moneymate.lite.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun CustomerDuplicateDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    var candidates by remember { mutableStateOf<List<CustomerDuplicate>?>(null) }
    var selected by remember { mutableStateOf<CustomerDuplicate?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        try {
            candidates = viewModel.reviewCustomerDuplicates()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not check customer records."
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val candidate = selected
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (candidate == null) "Possible duplicate customers" else "Archive this entry?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (candidate != null) {
                    Text("Archive the entry without loan history for ${candidate.extra.name} in ${candidate.fileName}? Its original record and payments will stay in place.")
                } else {
                    val rows = candidates
                    when {
                        rows == null && error == null -> Text("Checking loan files…")
                        rows != null && rows.isEmpty() -> Text("No candidates found in the loan files on this phone.")
                        rows != null -> LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rows, key = { it.extra.id }) { row ->
                                val extraDetails = listOfNotNull(row.extra.mobileNumber, row.extra.place)
                                    .filter { it.isNotBlank() }.joinToString(" · ")
                                val originalDetails = listOfNotNull(row.original.mobileNumber, row.original.place)
                                    .filter { it.isNotBlank() }.joinToString(" · ")
                                Text("${row.extra.name} · ${row.fileName}", style = MaterialTheme.typography.titleSmall)
                                Text("Without history: ${extraDetails.ifBlank { "No contact details" }}")
                                Text("With history: ${originalDetails.ifBlank { "No contact details" }}")
                                if (row.extra.sharedId != row.original.sharedId) {
                                    TextButton(onClick = { error = null; selected = row }) {
                                        Text("Review archive")
                                    }
                                } else {
                                    Text("These entries share an ID and need further review.")
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (candidate != null) {
                TextButton(enabled = !saving, onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.archiveCustomerDuplicate(candidate)
                            selected = null
                            refresh()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Could not archive this customer entry."
                        } finally {
                            saving = false
                        }
                    }
                }) {
                    Text(if (saving) "Archiving…" else "Archive")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (candidate != null) {
                TextButton(enabled = !saving, onClick = {
                    selected = null
                    error = null
                    scope.launch { refresh() }
                }) { Text("Back") }
            }
        }
    )
}
