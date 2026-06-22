package com.moneymate.lite.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymate.lite.data.entity.Person

data class AddEditPersonResult(
    val person: Person,
    val initialLoanAmount: Double = 0.0
)

@Composable
fun AddEditPersonDialog(
    fileId: Long,
    existingPerson: Person? = null,
    onDismiss: () -> Unit,
    onSave: (AddEditPersonResult) -> Unit
) {
    var name by remember { mutableStateOf(existingPerson?.name ?: "") }
    var mobileNumber by remember { mutableStateOf(existingPerson?.mobileNumber ?: "") }
    var place by remember { mutableStateOf(existingPerson?.place ?: "") }
    var notes by remember { mutableStateOf(existingPerson?.notes ?: "") }
    var totalAmountText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val isEditMode = existingPerson != null
    val title = if (isEditMode) "Edit Customer" else "Add Customer"

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) {
            nameError = true
            valid = false
        } else {
            nameError = false
        }
        if (mobileNumber.isNotBlank() && !mobileNumber.matches(Regex("^\\d{10}$"))) {
            mobileError = true
            valid = false
        } else {
            mobileError = false
        }
        if (!isEditMode && totalAmountText.isNotBlank()) {
            val amount = totalAmountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                amountError = true
                valid = false
            } else {
                amountError = false
            }
        }
        return valid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name *") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it; mobileError = false },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    isError = mobileError,
                    supportingText = if (mobileError) {
                        { Text("Must be 10 digits") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Place") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Total amount field for new customers only
                if (!isEditMode) {
                    OutlinedTextField(
                        value = totalAmountText,
                        onValueChange = {
                            totalAmountText = it
                            amountError = false
                        },
                        label = { Text("Total Amount Given (₹)") },
                        singleLine = true,
                        isError = amountError,
                        supportingText = if (amountError) {
                            { Text("Amount must be greater than 0") }
                        } else {
                            { Text("Optional. Creates a loan automatically.") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (validate()) {
                    val initialLoanAmount = totalAmountText.toDoubleOrNull() ?: 0.0
                    val person = if (isEditMode) {
                        existingPerson.copy(
                            name = name.trim(),
                            mobileNumber = mobileNumber.ifBlank { null },
                            place = place.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                    } else {
                        Person(
                            fileId = fileId,
                            name = name.trim(),
                            mobileNumber = mobileNumber.ifBlank { null },
                            place = place.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                    }
                    onSave(AddEditPersonResult(person, initialLoanAmount))
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
