package com.moneymate.lite.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.moneymate.lite.data.AppDatabase
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
import kotlinx.coroutines.sync.Mutex
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
    private val activityLogDao: ActivityLogDao,
    private val database: AppDatabase,
    private val customerDuplicates: CustomerDuplicates
) {
    private val syncLock = Mutex()
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

        if (!syncLock.tryLock()) {
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
            syncLock.unlock()
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

        // Network reads finish before Room begins publishing changes.
        suspend fun documents(collection: String) = bizRef.collection(collection).let { ref ->
            val query = if (cutoffTime > 0L) ref.whereGreaterThan("lastModified", cutoffTime) else ref
            query.get(Source.SERVER).await().documents
        }
        val files = documents("files")
        val persons = documents("persons")
        val loans = documents("loans")
        val payments = documents("payments")

        database.withTransaction {
            customerDuplicates.removeIdenticalLocalCopies(businessId)
            for (fileDoc in files) {
                val fileSharedId = fileDoc.getString("sharedId") ?: fileDoc.id
                if (userRole == UserRole.WORKER && !accessAll && fileSharedId !in allowedFileIds) {
                    continue
                }
                val name = fileDoc.getString("name") ?: ""
                val existing = loanFileDao.getFileBySharedId(fileSharedId)
                if (existing != null && existing.businessId.isNotBlank() && existing.businessId != businessId) {
                    continue
                }
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
            val localFiles = loanFileDao.getAllFilesIncludingDeleted().filter { it.businessId == businessId }
                .associateBy { it.sharedId }
            val localFilesById = localFiles.values.associateBy { it.id }
            val allowedLocalFileSharedIds = if (userRole == UserRole.WORKER && !accessAll) {
                allowedFileIds.toSet()
            } else {
                localFiles.keys
            }

            for (pDoc in persons) {
                val sharedId = pDoc.getString("sharedId")?.takeIf { it.isNotBlank() } ?: pDoc.id
                val existing = personDao.getPersonBySharedId(sharedId)
                if (existing != null && existing.businessId.isNotBlank() && existing.businessId != businessId) {
                    continue
                }
                val fileSharedId = pDoc.getString("fileSharedId")?.takeIf { it.isNotBlank() }
                    ?: existing?.fileSharedId?.takeIf { it.isNotBlank() }
                    ?: existing?.let { localFilesById[it.fileId]?.sharedId }
                    ?: pDoc.getLong("fileId")?.let { "file_$it" }
                    ?: continue
                if (fileSharedId !in allowedLocalFileSharedIds) continue
                val file = localFiles[fileSharedId] ?: continue
                val lastModified = pDoc.getLong("lastModified") ?: 0L
                val isDeleted = pDoc.getBoolean("isDeleted") ?: false
                if (existing != null && !existing.acceptsCloudVersion(lastModified, isDeleted)) continue

                val isNewer = existing == null || lastModified > existing.lastModified
                val sortOrder = pDoc.getLong("sortOrder")?.toInt()
                val position = existing?.sortOrder?.takeUnless { isNewer && sortOrder != null }
                    ?: sortOrder
                    ?: ((personDao.getActivePersonsList(file.id).maxOfOrNull { it.sortOrder } ?: -1) + 1)
                val customer = Person(
                    id = existing?.id ?: 0L,
                    sharedId = sharedId,
                    businessId = businessId,
                    fileId = file.id,
                    fileSharedId = fileSharedId,
                    name = pDoc.getString("name") ?: existing?.name.orEmpty(),
                    mobileNumber = if (pDoc.contains("mobileNumber")) pDoc.getString("mobileNumber") else existing?.mobileNumber,
                    place = if (pDoc.contains("place")) pDoc.getString("place") else existing?.place,
                    notes = if (pDoc.contains("notes")) pDoc.getString("notes") else existing?.notes,
                    sortOrder = position,
                    createdAt = existing?.createdAt ?: pDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastModified = lastModified,
                    isDeleted = isDeleted
                )
                if (existing == null) personDao.insert(customer) else personDao.update(customer)
            }

            // Cache local persons by sharedId
            val localPersons = personDao.getAllPersonsIncludingDeleted().groupBy { it.sharedId }

            for (lDoc in loans) {
                val lSharedId = lDoc.getString("sharedId") ?: lDoc.id
                val existing = loanDao.getLoanBySharedId(lSharedId)
                val cloudPersonId = lDoc.getString("personSharedId").orEmpty()
                if (existing != null && cloudPersonId.isNotBlank() &&
                    existing.personSharedId.isNotBlank() && existing.personSharedId != cloudPersonId
                ) {
                    continue
                }
                val personSharedId = cloudPersonId.ifBlank {
                    existing?.personSharedId.orEmpty().ifBlank {
                        lDoc.getLong("personId")?.let { "person_$it" }.orEmpty()
                    }
                }
                val matches = localPersons[personSharedId].orEmpty().filter {
                    it.businessId == businessId && it.fileSharedId in allowedLocalFileSharedIds
                }
                val localPerson = matches.firstOrNull { it.id == existing?.personId }
                    ?: matches.singleOrNull()
                // Numeric cloud IDs are local to the sending device.
                val localPersonId = localPerson?.id
                    ?: existing?.personId?.takeIf { personSharedId.isBlank() && userRole == UserRole.OWNER }
                    ?: continue
                val totalAmount = lDoc.getDouble("totalAmount") ?: 0.0
                val dateGiven = lDoc.getLong("dateGiven") ?: System.currentTimeMillis()
                val isCompleted = lDoc.getBoolean("isCompleted") ?: false
                val completedAt = lDoc.getLong("completedAt")
                val isDeleted = lDoc.getBoolean("isDeleted") ?: false
                val createdAt = lDoc.getLong("createdAt") ?: System.currentTimeMillis()
                val lastModified = lDoc.getLong("lastModified") ?: 0L
                if (existing != null && existing.lastModified > lastModified) continue

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
            val localLoans = loanDao.getAllLoansIncludingDeleted().filter { it.businessId == businessId }
                .groupBy { it.sharedId }

            for (payDoc in payments) {
                val paySharedId = payDoc.getString("sharedId") ?: payDoc.id
                val existing = paymentDao.getPaymentBySharedId(paySharedId)
                val cloudLoanId = payDoc.getString("loanSharedId").orEmpty()
                if (existing != null && cloudLoanId.isNotBlank() &&
                    existing.loanSharedId.isNotBlank() && existing.loanSharedId != cloudLoanId
                ) {
                    continue
                }
                val loanSharedId = cloudLoanId.ifBlank {
                    existing?.loanSharedId.orEmpty().ifBlank {
                        payDoc.getLong("loanId")?.let { "loan_$it" }.orEmpty()
                    }
                }
                val matches = localLoans[loanSharedId].orEmpty().filter { loan ->
                    localPersons[loan.personSharedId].orEmpty().any {
                        it.id == loan.personId && it.fileSharedId in allowedLocalFileSharedIds
                    }
                }
                val localLoan = matches.firstOrNull { it.id == existing?.loanId }
                    ?: matches.singleOrNull()
                    ?: continue

                val amount = payDoc.getDouble("amount") ?: 0.0
                val date = payDoc.getLong("date") ?: System.currentTimeMillis()
                val recordedByWorkerId = payDoc.getString("recordedByWorkerId")
                val recordedByWorkerName = payDoc.getString("recordedByWorkerName")
                val isDeleted = payDoc.getBoolean("isDeleted") ?: false
                val createdAt = payDoc.getLong("createdAt") ?: System.currentTimeMillis()
                val lastModified = payDoc.getLong("lastModified") ?: 0L
                if (existing != null && existing.lastModified > lastModified) continue

                if (existing == null) {
                    paymentDao.insert(
                        Payment(
                            sharedId = paySharedId,
                            businessId = businessId,
                            loanId = localLoan.id,
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
                            loanId = localLoan.id,
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
            customerDuplicates.removeIdenticalLocalCopies(businessId)
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
}
