package com.moneymate.lite.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.activity.compose.BackHandler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.data.dao.LoanWithBalance
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.ui.viewmodel.LoanViewModel
import com.moneymate.lite.ui.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.filled.CalendarMonth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    fileId: Long,
    name: String,
    navController: NavController,
    personViewModel: PersonViewModel = hiltViewModel(),
    loanViewModel: LoanViewModel = hiltViewModel()
) {
    val persons by personViewModel.getPersonsByFile(fileId).collectAsState()
    val activeLoans by loanViewModel.getActiveLoansInFile(fileId).collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var deletingPerson by remember { mutableStateOf<Person?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var isDateFilterActive by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerForFilter by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<DateTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<DateTransaction?>(null) }
    var transactionPersonTarget by remember { mutableStateOf<Person?>(null) }


    // Pagination states
    var currentPage by remember { mutableStateOf(1) }
    val pageSize = 20

    // Date formatting helper
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateString = remember(selectedDateMillis) {
        dateFormat.format(Date(selectedDateMillis))
    }

    BackHandler(enabled = isSearchActive || isDateFilterActive) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else if (isDateFilterActive) {
            isDateFilterActive = false
        }
    }


    // Reactive file name — stable from first frame, no flicker
    val decodedName = remember(name) { java.net.URLDecoder.decode(name, "UTF-8") }
    val fileInfo by personViewModel.getFileByIdFlow(fileId).collectAsState()
    val displayName = fileInfo?.name ?: decodedName

    val activeLoanMap = remember(activeLoans) {
        activeLoans.associateBy { it.personId }
    }

    // Combine queries for date-based transaction listing
    val dateRange = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val end = cal.timeInMillis
        Pair(start, end)
    }

    val dateLoansFlow = remember(fileId, dateRange) {
        loanViewModel.getLoansGivenOnDate(fileId, dateRange.first, dateRange.second)
    }
    val datePaymentsFlow = remember(fileId, dateRange) {
        loanViewModel.getPaymentsReceivedOnDate(fileId, dateRange.first, dateRange.second)
    }

    val transactions by remember(dateLoansFlow, datePaymentsFlow) {
        combine(dateLoansFlow, datePaymentsFlow) { loans, payments ->
            val list = mutableListOf<DateTransaction>()
            list.addAll(loans.map { DateTransaction(it.id, DateTransactionType.GIVEN, it.personId, it.personName, it.amount, it.date) })
            list.addAll(payments.map { DateTransaction(it.id, DateTransactionType.RECEIVED, it.personId, it.personName, it.amount, it.date) })
            list.sortByDescending { it.date }
            list
        }
    }.collectAsState(initial = emptyList())

    val totalGiven = remember(transactions) {
        transactions.filter { it.type == DateTransactionType.GIVEN }.sumOf { it.amount }
    }
    val totalReceived = remember(transactions) {
        transactions.filter { it.type == DateTransactionType.RECEIVED }.sumOf { it.amount }
    }

    val balancesUpToDateFlow = remember(fileId, dateRange) {
        loanViewModel.getBalancesUpToDate(fileId, dateRange.second)
    }
    val balancesUpToDate by balancesUpToDateFlow.collectAsState(initial = emptyList())

    val balancesUpToDateMap = remember(balancesUpToDate) {
        balancesUpToDate.associateBy { it.personId }
    }

    val totalBalance = remember(balancesUpToDate) {
        balancesUpToDate.sumOf { it.balance }
    }


    val filteredPersons = remember(persons, searchQuery) {
        val baseList = if (searchQuery.isBlank()) {
            persons
        } else {
            persons.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                (it.mobileNumber?.contains(searchQuery) == true) ||
                (it.place?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        baseList.sortedBy { it.sortOrder }
    }

    val totalPages = remember(filteredPersons) {
        maxOf(1, kotlin.math.ceil(filteredPersons.size.toDouble() / pageSize).toInt())
    }

    LaunchedEffect(filteredPersons.size) {
        if (currentPage > totalPages) {
            currentPage = 1
        }
    }

    val paginatedPersons = remember(filteredPersons, currentPage) {
        val startIndex = (currentPage - 1) * pageSize
        filteredPersons.drop(startIndex).take(pageSize)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customer...", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isDateFilterActive) {
                        Text(
                            text = "Tx: $dateString",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Search",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else if (isDateFilterActive) {
                        IconButton(onClick = {
                            isDateFilterActive = false
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Date Filter",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (!isSearchActive) {
                        if (!isDateFilterActive) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Customer",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        IconButton(onClick = { showDatePickerForFilter = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Filter by Date",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            isSearchActive = true
                            isDateFilterActive = false
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recently Deleted") },
                            onClick = {
                                showMenu = false
                                navController.navigate("file_trash/$fileId")
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isDateFilterActive) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    TransactionSummaryCard(
                        totalGiven = totalGiven,
                        totalReceived = totalReceived,
                        totalBalance = totalBalance
                    )
                }


                if (paginatedPersons.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No customers found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val personGivenTx = transactions.filter { it.type == DateTransactionType.GIVEN }.associateBy { it.personId }
                    val personReceivedTx = transactions.filter { it.type == DateTransactionType.RECEIVED }.associateBy { it.personId }

                    itemsIndexed(
                        items = paginatedPersons,
                        key = { _, person -> person.id }
                    ) { index, person ->
                        val globalIndex = (currentPage - 1) * pageSize + index
                        val givenTx = personGivenTx[person.id]
                        val receivedTx = personReceivedTx[person.id]
                        val personBalance = balancesUpToDateMap[person.id]?.balance ?: 0.0

                        DatePersonCard(
                            index = globalIndex,
                            person = person,
                            givenTx = givenTx,
                            receivedTx = receivedTx,
                            balance = personBalance,
                            onAddTransaction = {
                                transactionPersonTarget = person
                                showAddTransactionDialog = true
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                            },
                            onDeleteTransaction = { tx ->
                                deletingTransaction = tx
                            },
                            onClick = {
                                navController.navigate("person_detail/${person.id}")
                            }
                        )

                    }

                    if (totalPages > 1) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { if (currentPage > 1) currentPage-- },
                                    enabled = currentPage > 1
                                ) {
                                    Text("Previous")
                                }
                                Text(
                                    text = "Page $currentPage of $totalPages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = { if (currentPage < totalPages) currentPage++ },
                                    enabled = currentPage < totalPages
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            var offsetX by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(totalPages) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX < -100f) {
                                    if (currentPage < totalPages) {
                                        currentPage++
                                    }
                                } else if (offsetX > 100f) {
                                    if (currentPage > 1) {
                                        currentPage--
                                    }
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount
                            }
                        )
                    }
            ) {
                if (persons.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No customers yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (paginatedPersons.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No results found for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = paginatedPersons,
                                key = { _, person -> person.id }
                            ) { index, person ->
                                val activeLoan = activeLoanMap[person.id]
                                val globalIndex = (currentPage - 1) * pageSize + index

                                PersonCard(
                                    index = globalIndex,
                                    person = person,
                                    activeLoan = activeLoan,
                                    onEdit = { editingPerson = person },
                                    onDelete = { deletingPerson = person },
                                    onClick = {
                                        navController.navigate("person_detail/${person.id}")
                                    }
                                )
                            }

                            if (totalPages > 1) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { if (currentPage > 1) currentPage-- },
                                            enabled = currentPage > 1
                                        ) {
                                            Text("Previous")
                                        }
                                        Text(
                                            text = "Page $currentPage of $totalPages",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TextButton(
                                            onClick = { if (currentPage < totalPages) currentPage++ },
                                            enabled = currentPage < totalPages
                                        ) {
                                            Text("Next")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add person dialog
    if (showAddDialog) {
        AddEditPersonDialog(
            fileId = fileId,
            currentPersonsCount = persons.size,
            onDismiss = {
                showAddDialog = false
            },
            onSave = { result ->
                scope.launch {
                    val personId = personViewModel.addPerson(result.person, result.targetPosition)
                    // Auto-create a loan if initial amount was provided
                    if (result.initialLoanAmount > 0 && personId > 0) {
                        loanViewModel.createLoan(
                            personId = personId,
                            totalAmount = result.initialLoanAmount,
                            dateGiven = result.initialLoanDate
                        )
                    }
                }
                showAddDialog = false
            }
        )
    }

    // Edit person dialog
    editingPerson?.let { person ->
        val activeLoan = activeLoanMap[person.id]
        AddEditPersonDialog(
            fileId = fileId,
            existingPerson = person,
            existingLoanAmount = activeLoan?.totalAmount,
            existingLoanDate = activeLoan?.dateGiven,
            currentPersonsCount = persons.size,
            onDismiss = {
                editingPerson = null
            },
            onSave = { result ->
                scope.launch {
                    personViewModel.updatePerson(result.person, person.sortOrder, result.targetPosition - 1)
                    if (activeLoan != null && result.initialLoanAmount > 0) {
                        if (result.initialLoanAmount != activeLoan.totalAmount || result.initialLoanDate != activeLoan.dateGiven) {
                            loanViewModel.updateLoan(activeLoan.loanId, result.initialLoanAmount, result.initialLoanDate)
                        }
                    }
                }
                editingPerson = null
            }
        )
    }

    // Delete confirmation dialog
    deletingPerson?.let { person ->
        AlertDialog(
            onDismissRequest = { deletingPerson = null },
            title = { Text("Delete Customer") },
            text = {
                Text("Are you sure you want to delete \"${person.name}\"? This will move them to Recently Deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            personViewModel.deletePerson(person.id)
                        }
                        deletingPerson = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPerson = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date filter selection dialog
    if (showDatePickerForFilter) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerForFilter = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                        isDateFilterActive = true
                    }
                    showDatePickerForFilter = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForFilter = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Add transaction dialog
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            persons = persons,
            selectedDate = selectedDateMillis,
            loanViewModel = loanViewModel,
            preSelectedPerson = transactionPersonTarget,
            onDismiss = { 
                showAddTransactionDialog = false
                transactionPersonTarget = null
            },
            onSaved = { 
                showAddTransactionDialog = false
                transactionPersonTarget = null
            }
        )
    }

    // Edit transaction dialog
    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            loanViewModel = loanViewModel,
            onDismiss = { editingTransaction = null },
            onSaved = { editingTransaction = null }
        )
    }

    // Delete transaction dialog
    deletingTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Delete Transaction") },
            text = {
                val typeStr = if (tx.type == DateTransactionType.GIVEN) "Given (Loan)" else "Received (Payment)"
                Text("Are you sure you want to delete the $typeStr transaction of ₹${"%.0f".format(tx.amount)} for \"${tx.personName}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tx.type == DateTransactionType.GIVEN) {
                            loanViewModel.deleteLoan(tx.id)
                        } else {
                            loanViewModel.deletePayment(tx.id)
                        }
                        deletingTransaction = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    persons: List<Person>,
    selectedDate: Long,
    loanViewModel: LoanViewModel,
    preSelectedPerson: Person? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var selectedPerson by remember { mutableStateOf<Person?>(preSelectedPerson) }
    var showPersonSelector by remember { mutableStateOf(false) }
    var isGiven by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (showPersonSelector) {
        CustomerSelectDialog(
            persons = persons,
            onDismiss = { showPersonSelector = false },
            onSelect = {
                selectedPerson = it
                showPersonSelector = false
                errorMessage = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedPerson?.name ?: "Click to select customer *",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPersonSelector = true },
                    enabled = false,
                    colors = TextFieldDefaults.colors(
                        disabledTextColor = if (selectedPerson != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        disabledContainerColor = Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                TextButton(onClick = { showPersonSelector = true }) {
                    Text(if (selectedPerson == null) "Select Customer" else "Change Customer")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterChip(
                        selected = isGiven,
                        onClick = { isGiven = true; errorMessage = null },
                        label = { Text("Given (Loan)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isGiven,
                        onClick = { isGiven = false; errorMessage = null },
                        label = { Text("Received (Pay)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                        errorMessage = null
                    },
                    label = { Text("Amount (₹) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Amount must be greater than 0") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val person = selectedPerson
                if (person == null) {
                    errorMessage = "Please select a customer"
                    return@TextButton
                }
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    amountError = true
                    return@TextButton
                }

                scope.launch {
                    if (isGiven) {
                        val result = loanViewModel.addGivenTransaction(person.id, amount, selectedDate)
                        result.fold(
                            onSuccess = { onSaved() },
                            onFailure = { errorMessage = it.message ?: "Failed to record transaction" }
                        )
                    } else {
                        val result = loanViewModel.addReceivedTransaction(person.id, amount, selectedDate)
                        result.fold(
                            onSuccess = { onSaved() },
                            onFailure = { errorMessage = it.message ?: "Failed to record transaction" }
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelectDialog(
    persons: List<Person>,
    onDismiss: () -> Unit,
    onSelect: (Person) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(persons, query) {
        if (query.isBlank()) {
            persons
        } else {
            persons.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Customer") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search customer...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No customers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(filtered) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(p) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionDialog(
    transaction: DateTransaction,
    loanViewModel: LoanViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var amountText by remember { mutableStateOf("%.0f".format(transaction.amount)) }
    var amountError by remember { mutableStateOf(false) }
    var transactionDate by remember { mutableStateOf(transaction.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = transactionDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { transactionDate = it }
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
        title = { Text("Edit Transaction") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = transaction.personName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = if (transaction.type == DateTransactionType.GIVEN) "Given (Loan)" else "Received (Payment)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                        errorMessage = null
                    },
                    label = { Text("Amount (₹) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Amount must be greater than 0") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dateFormat.format(Date(transactionDate)),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(onClick = { showDatePicker = true }) {
                    Text("Change Date")
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    amountError = true
                    return@TextButton
                }

                scope.launch {
                    val result = if (transaction.type == DateTransactionType.GIVEN) {
                        loanViewModel.updateLoan(transaction.id, amount, transactionDate)
                    } else {
                        loanViewModel.updatePayment(transaction.id, amount, transactionDate)
                    }
                    result.fold(
                        onSuccess = { onSaved() },
                        onFailure = { errorMessage = it.message ?: "Failed to save transaction" }
                    )
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

@Composable
private fun TransactionSummaryCard(
    totalGiven: Double,
    totalReceived: Double,
    totalBalance: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Given",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "₹${"%.0f".format(totalGiven)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Received",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "₹${"%.0f".format(totalReceived)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "₹${"%.0f".format(totalBalance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
private fun TransactionRow(
    transaction: DateTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.personName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                val typeLabel = if (transaction.type == DateTransactionType.GIVEN) "Money Given" else "Money Received"
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val isGiven = transaction.type == DateTransactionType.GIVEN
            val amountColor = if (isGiven) Color(0xFFD32F2F) else Color(0xFF388E3C)
            val prefix = if (isGiven) "-₹" else "+₹"
            
            Text(
                text = "$prefix${"%.0f".format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Transaction",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

enum class DateTransactionType {
    GIVEN, RECEIVED
}

data class DateTransaction(
    val id: Long,
    val type: DateTransactionType,
    val personId: Long,
    val personName: String,
    val amount: Double,
    val date: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonCard(
    index: Int,
    person: Person,
    activeLoan: LoanWithBalance?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${person.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                person.mobileNumber?.let { mobile ->
                    Text(
                        text = mobile,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                person.place?.let { place ->
                    Text(
                        text = place,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (activeLoan != null) {
                    Text(
                        text = "Balance: ₹${"%.0f".format(activeLoan.balance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE53935)
                    )
                    Text(
                        text = "Total: ₹${"%.0f".format(activeLoan.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No active loan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Call") },
                        onClick = {
                            showMenu = false
                            if (!person.mobileNumber.isNullOrBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:${person.mobileNumber}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                        enabled = !person.mobileNumber.isNullOrBlank()
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePersonCard(
    index: Int,
    person: Person,
    givenTx: DateTransaction?,
    receivedTx: DateTransaction?,
    balance: Double,
    onAddTransaction: () -> Unit,
    onEditTransaction: (DateTransaction) -> Unit,
    onDeleteTransaction: (DateTransaction) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${person.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Given Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (givenTx != null) "Given: ₹${"%.0f".format(givenTx.amount)}" else "Given: nil",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (givenTx != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (givenTx != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (givenTx != null) {
                        Row {
                            IconButton(
                                onClick = { onEditTransaction(givenTx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Given", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDeleteTransaction(givenTx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Given", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Received Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (receivedTx != null) "Received: ₹${"%.0f".format(receivedTx.amount)}" else "Received: nil",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (receivedTx != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (receivedTx != null) Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (receivedTx != null) {
                        Row {
                            IconButton(
                                onClick = { onEditTransaction(receivedTx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Received", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDeleteTransaction(receivedTx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Received", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                // Balance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balance: ₹${"%.0f".format(balance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance > 0) Color(0xFFE53935) else if (balance < 0) Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            
            IconButton(
                onClick = onAddTransaction,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
