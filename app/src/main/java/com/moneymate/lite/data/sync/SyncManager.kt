package com.moneymate.lite.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moneymate.lite.data.dao.ActivityLogDao
import com.moneymate.lite.data.dao.ApprovalRequestDao
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.ActivityLogEntity
import com.moneymate.lite.data.entity.ApprovalRequestEntity
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paymentDao: PaymentDao,
    private val loanDao: LoanDao,
    private val loanFileDao: LoanFileDao,
    private val personDao: PersonDao,
    private val approvalRequestDao: ApprovalRequestDao,
    private val activityLogDao: ActivityLogDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _failedSyncCount = MutableStateFlow(0)
    val failedSyncCount: StateFlow<Int> = _failedSyncCount.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncErrors = MutableStateFlow<List<String>>(emptyList())
    val syncErrors: StateFlow<List<String>> = _syncErrors.asStateFlow()

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    scope.launch {
                        triggerSync()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            }
        )

        refreshPendingCounts()
    }

    private fun checkInitialConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refreshPendingCounts() {
        scope.launch {
            val pending = paymentDao.getPaymentsPendingSync()
            _pendingSyncCount.value = pending.count { it.syncStatus == "PENDING_SYNC" }
            _failedSyncCount.value = pending.count { it.syncStatus == "FAILED" }
            val errors = pending.mapNotNull { it.syncError }.distinct()
            _syncErrors.value = errors
        }
    }

    private val syncPrefs by lazy {
        context.getSharedPreferences("moneymate_sync_prefs", Context.MODE_PRIVATE)
    }

    suspend fun syncWithCloud(businessId: String? = null, role: UserRole? = null, forceFullSync: Boolean = false): Result<Unit> =
        triggerSync(businessId, role, forceFullSync)

    suspend fun triggerSync(
        businessId: String? = null,
        role: UserRole? = null,
        forceFullSync: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!_isOnline.value) {
            refreshPendingCounts()
            return@withContext Result.failure(IllegalStateException("Device is offline"))
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            refreshPendingCounts()
            return@withContext Result.failure(IllegalStateException("User not authenticated"))
        }

        if (_isSyncing.value) {
            return@withContext Result.success(Unit)
        }

        _isSyncing.value = true
        val errors = mutableListOf<String>()
        val syncStartTime = System.currentTimeMillis()

        try {
            val db = FirebaseFirestore.getInstance()

            // 1. Resolve businessId and verify worker status
            val userDoc = db.collection("users").document(user.uid).get().await()
            val activeBizId = businessId ?: userDoc.getString("businessId") ?: ""
            val isActive = userDoc.getBoolean("isActive") ?: true
            val userRole = role ?: if (userDoc.getString("role") == "OWNER") UserRole.OWNER else UserRole.WORKER

            if (activeBizId.isBlank()) {
                _isSyncing.value = false
                return@withContext Result.success(Unit)
            }

            // Auto-heal and mark any local unsynced records on Owner device for upload
            if (userRole == UserRole.OWNER) {
                loanDao.markUnsyncedLoansForSync(activeBizId, syncStartTime)
                paymentDao.markUnsyncedPaymentsForSync(activeBizId, syncStartTime)
            }

            // 2. Process pending offline payments
            val pendingPayments = paymentDao.getPaymentsPendingSync()
            for (payment in pendingPayments) {
                try {
                    // Revalidate worker status if recorded by worker
                    if (!isActive && payment.recordedByWorkerId == user.uid) {
                        val errMsg = "Worker account was disabled by owner. Offline payment rejected."
                        paymentDao.updateSyncStatus(payment.id, "FAILED", errMsg)
                        errors.add(errMsg)
                        continue
                    }

                    // Validate positive finite amount
                    if (payment.amount <= 0.0 || payment.amount.isInfinite() || payment.amount.isNaN()) {
                        val errMsg = "Invalid payment amount: ${payment.amount}"
                        paymentDao.updateSyncStatus(payment.id, "FAILED", errMsg)
                        errors.add(errMsg)
                        continue
                    }

                    // Check loan on server or locally
                    val loan = loanDao.getLoanById(payment.loanId)
                    if (loan == null || loan.isDeleted) {
                        val errMsg = "Associated loan not found or deleted."
                        paymentDao.updateSyncStatus(payment.id, "FAILED", errMsg)
                        errors.add(errMsg)
                        continue
                    }

                    // Write to Firestore with idempotent sharedId
                    val paymentRef = db.collection("businesses").document(activeBizId)
                        .collection("payments").document(payment.sharedId)

                    val paymentData = hashMapOf(
                        "sharedId" to payment.sharedId,
                        "businessId" to activeBizId,
                        "loanId" to payment.loanId,
                        "loanSharedId" to (payment.loanSharedId.ifBlank { loan.sharedId }),
                        "amount" to payment.amount,
                        "date" to payment.date,
                        "recordedByWorkerId" to payment.recordedByWorkerId,
                        "recordedByWorkerName" to payment.recordedByWorkerName,
                        "createdAt" to payment.createdAt,
                        "lastModified" to System.currentTimeMillis(),
                        "isDeleted" to payment.isDeleted
                    )
                    paymentRef.set(paymentData, SetOptions.merge()).await()

                    // Mark as synced locally
                    paymentDao.updateSyncStatus(payment.id, "SYNCED", null)

                    // Recheck loan completion
                    val totalPaid = paymentDao.getTotalPaidForLoan(loan.id)
                    if (totalPaid >= loan.totalAmount && !loan.isCompleted) {
                        loanDao.markCompleted(loan.id, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    val errMsg = "Sync failed for payment ${payment.sharedId}: ${e.message}"
                    paymentDao.updateSyncStatus(payment.id, "FAILED", errMsg)
                    errors.add(errMsg)
                }
            }

            // 2b. Process pending offline loans
            val pendingLoans = loanDao.getLoansPendingSync()
            for (loan in pendingLoans) {
                try {
                    val loanRef = db.collection("businesses").document(activeBizId)
                        .collection("loans").document(loan.sharedId)
                    val loanData = hashMapOf(
                        "sharedId" to loan.sharedId,
                        "businessId" to activeBizId,
                        "personId" to loan.personId,
                        "personSharedId" to loan.personSharedId,
                        "totalAmount" to loan.totalAmount,
                        "dateGiven" to loan.dateGiven,
                        "isCompleted" to loan.isCompleted,
                        "completedAt" to loan.completedAt,
                        "createdAt" to loan.createdAt,
                        "lastModified" to System.currentTimeMillis(),
                        "isDeleted" to loan.isDeleted
                    )
                    loanRef.set(loanData, SetOptions.merge()).await()
                    loanDao.updateSyncStatus(loan.id, "SYNCED")
                } catch (e: Exception) {
                    val errMsg = "Sync failed for loan ${loan.sharedId}: ${e.message}"
                    errors.add(errMsg)
                }
            }

            // 3. Pull updates from Firestore for authorized records (fast incremental sync)
            val lastSyncKey = "last_sync_$activeBizId"
            val lastSync = syncPrefs.getLong(lastSyncKey, 0L)
            val isSchemaUpgrade = syncPrefs.getInt("sync_schema_version", 0) < 37
            val isFirstSync = loanFileDao.getAllFilesIncludingDeleted().isEmpty() || lastSync == 0L
            val doFullSync = forceFullSync || isFirstSync || isSchemaUpgrade
            val cutoff = if (doFullSync) 0L else maxOf(0L, lastSync - 60_000L)

            pullUpdatesFromCloud(activeBizId, userDoc, userRole, cutoff)

            // Auto-heal relational foreign keys in local SQLite
            personDao.relinkPersonsToLocalFiles()
            loanDao.relinkLoansToLocalPersons()
            paymentDao.relinkPaymentsToLocalLoans()
            autoHealDuplicatePersons(activeBizId, db)

            if (isSchemaUpgrade) {
                syncPrefs.edit().putInt("sync_schema_version", 37).apply()
            }
            syncPrefs.edit().putLong(lastSyncKey, syncStartTime).apply()
            _lastSyncTime.value = syncStartTime
            _syncErrors.value = errors
            refreshPendingCounts()

            Result.success(Unit)
        } catch (e: Exception) {
            errors.add(e.message ?: "Unknown sync error")
            _syncErrors.value = errors
            refreshPendingCounts()
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun pullUpdatesFromCloud(
        businessId: String,
        userDoc: com.google.firebase.firestore.DocumentSnapshot,
        userRole: UserRole,
        cutoffTime: Long
    ) {
        val db = FirebaseFirestore.getInstance()
        val accessAll = userDoc.getBoolean("accessAllFiles") ?: true
        @Suppress("UNCHECKED_CAST")
        val allowedFileIds = (userDoc.get("allowedFileIds") as? List<String>) ?: emptyList()
        val bizRef = db.collection("businesses").document(businessId)

        // 1. Pull files
        val filesQuery = if (cutoffTime > 0L) {
            bizRef.collection("files").whereGreaterThan("lastModified", cutoffTime)
        } else {
            bizRef.collection("files")
        }
        val filesSnapshot = filesQuery.get().await()
        for (fileDoc in filesSnapshot.documents) {
            val fileSharedId = fileDoc.getString("sharedId") ?: fileDoc.id
            if (userRole == UserRole.WORKER && !accessAll && fileSharedId !in allowedFileIds) {
                continue
            }
            val name = fileDoc.getString("name") ?: ""
            val existing = loanFileDao.getFileBySharedId(fileSharedId)
                ?: loanFileDao.getAllFilesIncludingDeleted().firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }
            val sortOrder = fileDoc.getLong("sortOrder")?.toInt() ?: 0
            val isDeleted = fileDoc.getBoolean("isDeleted") ?: false
            val createdAt = fileDoc.getLong("createdAt") ?: System.currentTimeMillis()
            val lastModified = fileDoc.getLong("lastModified") ?: System.currentTimeMillis()

            if (existing == null) {
                loanFileDao.insert(
                    LoanFile(
                        sharedId = fileSharedId,
                        businessId = businessId,
                        name = name,
                        sortOrder = sortOrder,
                        createdAt = createdAt,
                        lastModified = lastModified,
                        isDeleted = isDeleted,
                        syncStatus = "SYNCED"
                    )
                )
            } else {
                loanFileDao.update(
                    existing.copy(
                        sharedId = fileSharedId,
                        businessId = businessId,
                        name = name,
                        sortOrder = sortOrder,
                        isDeleted = isDeleted,
                        lastModified = maxOf(lastModified, existing.lastModified),
                        syncStatus = "SYNCED"
                    )
                )
            }
        }

        // Cache local files by sharedId to correctly link local relational foreign keys
        val localFiles = loanFileDao.getAllFilesIncludingDeleted().associateBy { it.sharedId }
        val allowedLocalFileSharedIds = if (userRole == UserRole.WORKER && !accessAll) {
            allowedFileIds.toSet()
        } else {
            localFiles.keys
        }

        // 2. Pull persons / customers
        val personsQuery = if (cutoffTime > 0L) {
            bizRef.collection("persons").whereGreaterThan("lastModified", cutoffTime)
        } else {
            bizRef.collection("persons")
        }
        val personsSnapshot = personsQuery.get().await()
        for (pDoc in personsSnapshot.documents) {
            val pSharedId = pDoc.getString("sharedId") ?: pDoc.id
            val fileSharedId = pDoc.getString("fileSharedId") ?: ""
            if (fileSharedId.isNotBlank() && fileSharedId !in allowedLocalFileSharedIds) {
                continue
            }
            val localFile = if (fileSharedId.isNotBlank()) localFiles[fileSharedId] else null
            val localFileId = localFile?.id
                ?: (pDoc.getLong("fileId")?.takeIf { it > 0 })
                ?: localFiles.values.firstOrNull()?.id
                ?: 0L

            val rawName = pDoc.getString("name") ?: ""
            val cleanName = rawName.trim().removeSuffix("-").trim()
            val allLocalPersons = personDao.getAllPersonsIncludingDeleted()
            val existing = personDao.getPersonBySharedId(pSharedId)
                ?: (if (localFileId > 0) allLocalPersons.firstOrNull {
                    it.fileId == localFileId && it.name.trim().removeSuffix("-").trim().equals(cleanName, ignoreCase = true)
                } else null)
                ?: allLocalPersons.firstOrNull {
                    it.name.trim().removeSuffix("-").trim().equals(cleanName, ignoreCase = true)
                }
            val mobile = pDoc.getString("mobileNumber")
            val place = pDoc.getString("place")
            val notes = pDoc.getString("notes")
            val sortOrder = pDoc.getLong("sortOrder")?.toInt() ?: 0
            val isDeleted = pDoc.getBoolean("isDeleted") ?: false
            val createdAt = pDoc.getLong("createdAt") ?: System.currentTimeMillis()
            val lastModified = pDoc.getLong("lastModified") ?: System.currentTimeMillis()

            if (existing == null) {
                val maxSort = allLocalPersons.filter { it.fileId == localFileId && !it.isDeleted }.maxOfOrNull { it.sortOrder } ?: 0
                val finalSortOrder = if (sortOrder > 0) sortOrder else (maxSort + 1)
                personDao.insert(
                    Person(
                        sharedId = pSharedId,
                        businessId = businessId,
                        fileId = localFileId,
                        fileSharedId = fileSharedId,
                        name = rawName,
                        mobileNumber = mobile,
                        place = place,
                        notes = notes,
                        sortOrder = finalSortOrder,
                        createdAt = createdAt,
                        lastModified = lastModified,
                        isDeleted = isDeleted
                    )
                )
            } else {
                val finalSortOrder = when {
                    existing.sortOrder > 0 && sortOrder == 0 -> existing.sortOrder
                    sortOrder > 0 -> sortOrder
                    else -> existing.sortOrder
                }
                personDao.update(
                    existing.copy(
                        sharedId = pSharedId,
                        businessId = businessId,
                        fileId = if (localFileId > 0) localFileId else existing.fileId,
                        fileSharedId = if (fileSharedId.isNotBlank()) fileSharedId else existing.fileSharedId,
                        name = existing.name.ifBlank { rawName },
                        mobileNumber = mobile ?: existing.mobileNumber,
                        place = place ?: existing.place,
                        notes = notes ?: existing.notes,
                        sortOrder = finalSortOrder,
                        isDeleted = isDeleted,
                        lastModified = maxOf(lastModified, existing.lastModified)
                    )
                )
            }
        }

        // Cache local persons by sharedId
        val localPersons = personDao.getAllPersonsIncludingDeleted().associateBy { it.sharedId }

        // 3. Pull loans
        val loansQuery = if (cutoffTime > 0L) {
            bizRef.collection("loans").whereGreaterThan("lastModified", cutoffTime)
        } else {
            bizRef.collection("loans")
        }
        val loansSnapshot = loansQuery.get().await()
        for (lDoc in loansSnapshot.documents) {
            val lSharedId = lDoc.getString("sharedId") ?: lDoc.id
            val personSharedId = lDoc.getString("personSharedId") ?: ""
            val localPerson = localPersons[personSharedId]
            if (personSharedId.isNotBlank() && localPerson == null && userRole == UserRole.WORKER && !accessAll) {
                continue
            }
            val localPersonId = localPerson?.id ?: lDoc.getLong("personId") ?: 0L

            val existing = loanDao.getLoanBySharedId(lSharedId)
            val totalAmount = lDoc.getDouble("totalAmount") ?: 0.0
            val dateGiven = lDoc.getLong("dateGiven") ?: System.currentTimeMillis()
            val isCompleted = lDoc.getBoolean("isCompleted") ?: false
            val completedAt = lDoc.getLong("completedAt")
            val isDeleted = lDoc.getBoolean("isDeleted") ?: false
            val createdAt = lDoc.getLong("createdAt") ?: System.currentTimeMillis()
            val lastModified = lDoc.getLong("lastModified") ?: System.currentTimeMillis()

            if (existing == null) {
                loanDao.insert(
                    Loan(
                        sharedId = lSharedId,
                        businessId = businessId,
                        personId = localPersonId,
                        personSharedId = personSharedId,
                        totalAmount = totalAmount,
                        dateGiven = dateGiven,
                        isCompleted = isCompleted,
                        completedAt = completedAt,
                        createdAt = createdAt,
                        lastModified = lastModified,
                        isDeleted = isDeleted,
                        syncStatus = "SYNCED"
                    )
                )
            } else {
                loanDao.update(
                    existing.copy(
                        sharedId = lSharedId,
                        businessId = businessId,
                        personId = localPersonId,
                        personSharedId = personSharedId,
                        totalAmount = totalAmount,
                        dateGiven = dateGiven,
                        isCompleted = isCompleted,
                        completedAt = completedAt,
                        isDeleted = isDeleted,
                        lastModified = maxOf(lastModified, existing.lastModified),
                        syncStatus = "SYNCED"
                    )
                )
            }
        }

        // Cache local loans by sharedId
        val localLoans = loanDao.getAllLoansIncludingDeleted().associateBy { it.sharedId }

        // 4. Pull payments
        val paymentsQuery = if (cutoffTime > 0L) {
            bizRef.collection("payments").whereGreaterThan("lastModified", cutoffTime)
        } else {
            bizRef.collection("payments")
        }
        val paymentsSnapshot = paymentsQuery.get().await()
        for (payDoc in paymentsSnapshot.documents) {
            val paySharedId = payDoc.getString("sharedId") ?: payDoc.id
            val loanSharedId = payDoc.getString("loanSharedId") ?: ""
            val localLoan = localLoans[loanSharedId]
            if (loanSharedId.isNotBlank() && localLoan == null && userRole == UserRole.WORKER && !accessAll) {
                continue
            }
            val localLoanId = localLoan?.id ?: payDoc.getLong("loanId") ?: 0L

            val existing = paymentDao.getPaymentBySharedId(paySharedId)
            val amount = payDoc.getDouble("amount") ?: 0.0
            val date = payDoc.getLong("date") ?: System.currentTimeMillis()
            val recordedByWorkerId = payDoc.getString("recordedByWorkerId")
            val recordedByWorkerName = payDoc.getString("recordedByWorkerName")
            val isDeleted = payDoc.getBoolean("isDeleted") ?: false
            val createdAt = payDoc.getLong("createdAt") ?: System.currentTimeMillis()
            val lastModified = payDoc.getLong("lastModified") ?: System.currentTimeMillis()

            if (existing == null) {
                paymentDao.insert(
                    Payment(
                        sharedId = paySharedId,
                        businessId = businessId,
                        loanId = localLoanId,
                        loanSharedId = loanSharedId,
                        amount = amount,
                        date = date,
                        recordedByWorkerId = recordedByWorkerId,
                        recordedByWorkerName = recordedByWorkerName,
                        createdAt = createdAt,
                        lastModified = lastModified,
                        isDeleted = isDeleted,
                        syncStatus = "SYNCED"
                    )
                )
            } else {
                paymentDao.update(
                    existing.copy(
                        sharedId = paySharedId,
                        businessId = businessId,
                        loanId = localLoanId,
                        loanSharedId = loanSharedId,
                        amount = amount,
                        date = date,
                        recordedByWorkerId = recordedByWorkerId,
                        recordedByWorkerName = recordedByWorkerName,
                        isDeleted = isDeleted,
                        lastModified = maxOf(lastModified, existing.lastModified),
                        syncStatus = "SYNCED"
                    )
                )
            }
        }

        // 5. Pull approval requests
        try {
            val approvalsQuery = if (cutoffTime > 0L) {
                bizRef.collection("approval_requests").whereGreaterThan("requestedAt", cutoffTime)
            } else {
                bizRef.collection("approval_requests")
            }
            val approvalsSnapshot = approvalsQuery.get().await()
            for (aDoc in approvalsSnapshot.documents) {
                val req = ApprovalRequestEntity.fromDocument(aDoc, businessId)
                approvalRequestDao.insert(req)
            }
        } catch (e: Exception) {
            // Handled
        }

        // 6. Pull activity logs (Owner only)
        if (userRole == UserRole.OWNER) {
            try {
                val logsQuery = if (cutoffTime > 0L) {
                    bizRef.collection("activity_logs")
                        .whereGreaterThan("timestamp", cutoffTime)
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(100)
                } else {
                    bizRef.collection("activity_logs")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(100)
                }
                val logsSnapshot = logsQuery.get().await()
                for (lDoc in logsSnapshot.documents) {
                    val log = ActivityLogEntity.fromDocument(lDoc, businessId)
                    activityLogDao.insert(log)
                }
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    private suspend fun autoHealDuplicatePersons(businessId: String, db: FirebaseFirestore) {
        try {
            val allPersons = personDao.getAllPersonsIncludingDeleted().filter { !it.isDeleted }
            val allLoans = loanDao.getAllLoansIncludingDeleted().filter { !it.isDeleted }
            val now = System.currentTimeMillis()

            val grouped = allPersons.groupBy { Pair(it.fileId, it.name.trim().removeSuffix("-").trim().lowercase()) }
            for ((_, group) in grouped) {
                if (group.size <= 1) continue

                // Find keeper: prefer person with active loans, then sortOrder > 0, then highest sortOrder, then lowest ID
                val keeper = group.maxWithOrNull(
                    compareBy<Person> { p -> allLoans.any { it.personId == p.id } }
                        .thenBy { it.sortOrder > 0 }
                        .thenBy { it.sortOrder }
                        .thenByDescending { -it.id }
                ) ?: group.first()

                val duplicates = group.filter { it.id != keeper.id }
                for (dup in duplicates) {
                    val loansToRelink = allLoans.filter { it.personId == dup.id }
                    for (l in loansToRelink) {
                        loanDao.update(l.copy(personId = keeper.id, personSharedId = keeper.sharedId, lastModified = now))
                    }

                    personDao.softDelete(dup.id, now)

                    if (businessId.isNotBlank() && dup.sharedId.isNotBlank()) {
                        try {
                            db.collection("businesses").document(businessId)
                                .collection("persons").document(dup.sharedId)
                                .update(mapOf("isDeleted" to true, "lastModified" to now))
                        } catch (e: Exception) {
                            // Non-fatal if offline
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
