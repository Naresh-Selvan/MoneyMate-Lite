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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

    // Reactive file name — stable from first frame, no flicker
    val decodedName = remember(name) { java.net.URLDecoder.decode(name, "UTF-8") }
    val fileInfo by personViewModel.getFileByIdFlow(fileId).collectAsState()
    val displayName = fileInfo?.name ?: decodedName

    val activeLoanMap = remember(activeLoans) {
        activeLoans.associateBy { it.personId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
                ),
                actions = {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add customer")
            }
        }
    ) { paddingValues ->
        if (persons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(
                    items = persons,
                    key = { _, person -> person.id }
                ) { index, person ->
                    val activeLoan = activeLoanMap[person.id]

                    PersonCard(
                        index = index,
                        person = person,
                        activeLoan = activeLoan,
                        onEdit = { editingPerson = person },
                        onDelete = { deletingPerson = person },
                        onClick = {
                            navController.navigate("person_detail/${person.id}")
                        }
                    )
                }
            }
        }
    }

    // Add person dialog
    if (showAddDialog) {
        AddEditPersonDialog(
            fileId = fileId,
            onDismiss = {
                showAddDialog = false
            },
            onSave = { result ->
                scope.launch {
                    val personId = personViewModel.addPerson(result.person)
                    // Auto-create a loan if initial amount was provided
                    if (result.initialLoanAmount > 0 && personId > 0) {
                        loanViewModel.createLoan(
                            personId = personId,
                            totalAmount = result.initialLoanAmount,
                            dateGiven = System.currentTimeMillis()
                        )
                    }
                }
                showAddDialog = false
            }
        )
    }

    // Edit person dialog
    editingPerson?.let { person ->
        AddEditPersonDialog(
            fileId = fileId,
            existingPerson = person,
            onDismiss = {
                editingPerson = null
            },
            onSave = { result ->
                scope.launch {
                    personViewModel.updatePerson(result.person)
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
                Text("Are you sure you want to delete \"${person.name}\"? This action cannot be undone.")
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
}

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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false // snap back after revealing actions
        }
    )
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        animateColorAsState(
                            targetValue = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F)
                                else -> Color.Transparent
                            },
                            label = "swipe_bg"
                        ).value
                    )
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF1E88E5)
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
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
}
