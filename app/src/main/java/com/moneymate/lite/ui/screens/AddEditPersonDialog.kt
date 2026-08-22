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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.moneymate.lite.data.entity.Person

data class AddEditPersonResult(
    val person: Person,
    val initialLoanAmount: Double = 0.0,
    val initialLoanDate: Long = System.currentTimeMillis(),
    val targetPosition: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonDialog(
    fileId: Long,
    existingPerson: Person? = null,
    existingLoanAmount: Double? = null,
    existingLoanDate: Long? = null,
    currentPersonsCount: Int,
    onDismiss: () -> Unit,
    onSave: (AddEditPersonResult) -> Unit
) {
    var name by remember { mutableStateOf(existingPerson?.name ?: "") }
    var mobileNumbers by remember { 
        val existing = existingPerson?.mobileNumber?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateOf(if (existing.isEmpty()) listOf("") else existing)
    }
    var mobileErrors by remember { mutableStateOf(List(mobileNumbers.size) { false }) }
    var activeContactIndex by remember { mutableStateOf<Int?>(null) }
    var place by remember { mutableStateOf(existingPerson?.place ?: "") }
    var notes by remember { mutableStateOf(existingPerson?.notes ?: "") }
    var loanDateMillis by remember { mutableStateOf(existingLoanDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var totalAmountText by remember {
        mutableStateOf(
            if (existingLoanAmount != null) {
                if (existingLoanAmount % 1 == 0.0) {
                    existingLoanAmount.toLong().toString()
                } else {
                    existingLoanAmount.toString()
                }
            } else {
                ""
            }
        )
    }
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
                                activeContactIndex?.let { index ->
                                    if (index in mobileNumbers.indices) {
                                        val newList = mobileNumbers.toMutableList()
                                        newList[index] = last10
                                        mobileNumbers = newList
                                        
                                        val newErrors = mobileErrors.toMutableList()
                                        if (index < newErrors.size) newErrors[index] = false
                                        mobileErrors = newErrors
                                    }
                                }
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
        var hasMobileError = false
        val newMobileErrors = mobileNumbers.map { num ->
            val isErr = num.isNotBlank() && !num.matches(Regex("^\\d{10}$"))
            if (isErr) hasMobileError = true
            isErr
        }
        mobileErrors = newMobileErrors
        if (hasMobileError) valid = false

        if (!isEditMode || existingLoanAmount != null) {
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = loanDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { loanDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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

                mobileNumbers.forEachIndexed { index, number ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = number,
                            onValueChange = { newVal ->
                                val newList = mobileNumbers.toMutableList()
                                newList[index] = newVal
                                mobileNumbers = newList
                                
                                val newErrors = mobileErrors.toMutableList()
                                if (index < newErrors.size) newErrors[index] = false
                                mobileErrors = newErrors
                            },
                            label = { Text(if (mobileNumbers.size == 1) "Mobile Number" else "Mobile Number ${index + 1}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            trailingIcon = {
                                IconButton(onClick = {
                                    activeContactIndex = index
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
                            isError = if (index < mobileErrors.size) mobileErrors[index] else false,
                            supportingText = if (index < mobileErrors.size && mobileErrors[index]) {
                                { Text("Must be 10 digits") }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        if (mobileNumbers.size > 1) {
                            IconButton(onClick = {
                                val newList = mobileNumbers.toMutableList()
                                newList.removeAt(index)
                                mobileNumbers = newList
                                
                                val newErrors = mobileErrors.toMutableList()
                                if (index < newErrors.size) newErrors.removeAt(index)
                                mobileErrors = newErrors
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Remove number")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TextButton(onClick = {
                    mobileNumbers = mobileNumbers + listOf("")
                    mobileErrors = mobileErrors + listOf(false)
                }) {
                    Text("Add Another Number")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Place") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Total amount field for new customers, or existing customers with an active loan
                if (!isEditMode || existingLoanAmount != null) {
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
                            if (isEditMode) {
                                { Text("Required. Updates the active loan amount.") }
                            } else {
                                { Text("Required. Creates a loan automatically.") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dateFormat.format(Date(loanDateMillis)),
                        onValueChange = {},
                        label = { Text("Date Given") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Change Date")
                    }

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
                    val joinedMobileNumbers = mobileNumbers.filter { it.isNotBlank() }.joinToString(",")
                    val person = if (isEditMode) {
                        existingPerson.copy(
                            name = name.trim(),
                            mobileNumber = joinedMobileNumbers.ifBlank { null },
                            place = place.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                    } else {
                        Person(
                            fileId = fileId,
                            name = name.trim(),
                            mobileNumber = joinedMobileNumbers.ifBlank { null },
                            place = place.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                    }
                    onSave(AddEditPersonResult(person, initialLoanAmount, loanDateMillis, targetPos))
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
