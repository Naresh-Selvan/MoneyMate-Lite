package com.moneymate.lite.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneymate.lite.data.entity.Person

data class AddEditPersonResult(
    val person: Person,
    val initialLoanAmount: Double = 0.0,
    val targetPosition: Int
)

@Composable
fun AddEditPersonDialog(
    fileId: Long,
    existingPerson: Person? = null,
    currentPersonsCount: Int,
    onDismiss: () -> Unit,
    onSave: (AddEditPersonResult) -> Unit
) {
    var name by remember { mutableStateOf(existingPerson?.name ?: "") }
    var mobileNumber by remember { mutableStateOf(existingPerson?.mobileNumber ?: "") }
    var place by remember { mutableStateOf(existingPerson?.place ?: "") }
    var notes by remember { mutableStateOf(existingPerson?.notes ?: "") }
    var totalAmountText by remember { mutableStateOf("") }
    var serialNumberText by remember {
        mutableStateOf(
            if (existingPerson != null) {
                (existingPerson.sortOrder + 1).toString()
            } else {
                (currentPersonsCount + 1).toString()
            }
        )
    }
    var nameError by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var serialNumberError by remember { mutableStateOf(false) }

    val isEditMode = existingPerson != null
    val title = if (isEditMode) "Edit Customer" else "Add Customer"

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.data
            if (data != null) {
                try {
                    val cursor = context.contentResolver.query(
                        data,
                        arrayOf(
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null
                    )
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val numberIndex = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val nameIndex = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            
                            if (numberIndex >= 0) {
                                val rawNumber = c.getString(numberIndex) ?: ""
                                val digitsOnly = rawNumber.filter { it.isDigit() }
                                val last10 = if (digitsOnly.length >= 10) {
                                    digitsOnly.takeLast(10)
                                } else {
                                    digitsOnly
                                }
                                mobileNumber = last10
                                mobileError = false
                            }
                            
                            if (name.isBlank() && nameIndex >= 0) {
                                name = c.getString(nameIndex) ?: ""
                                nameError = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

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
        if (!isEditMode) {
            val amount = totalAmountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                amountError = true
                valid = false
            } else {
                amountError = false
            }
        }
        val targetPos = serialNumberText.toIntOrNull()
        val maxPos = if (isEditMode) currentPersonsCount else (currentPersonsCount + 1)
        if (targetPos == null || targetPos < 1 || targetPos > maxPos) {
            serialNumberError = true
            valid = false
        } else {
            serialNumberError = false
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
                    value = serialNumberText,
                    onValueChange = { serialNumberText = it; serialNumberError = false },
                    label = { Text("Serial Number (Position) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = serialNumberError,
                    supportingText = {
                        val maxPos = if (isEditMode) currentPersonsCount else (currentPersonsCount + 1)
                        if (serialNumberError) {
                            Text("Must be a number between 1 and $maxPos")
                        } else {
                            Text("Position in list (1 to $maxPos)")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it; mobileError = false },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    trailingIcon = {
                        IconButton(onClick = {
                            val intent = Intent(
                                Intent.ACTION_PICK,
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                            )
                            contactPickerLauncher.launch(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = "Select Contact"
                            )
                        }
                    },
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
                        label = { Text("Total Amount Given (₹) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError,
                        supportingText = if (amountError) {
                            { Text("Amount must be greater than 0") }
                        } else {
                            { Text("Required. Creates a loan automatically.") }
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
                    val targetPos = serialNumberText.toIntOrNull() ?: (currentPersonsCount + 1)
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
                    onSave(AddEditPersonResult(person, initialLoanAmount, targetPos))
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
