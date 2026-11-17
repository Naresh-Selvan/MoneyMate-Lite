package com.moneymate.lite.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
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
    private val personDao: PersonDao
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

    suspend fun triggerSync(businessId: String? = null, role: UserRole? = null): Result<Unit> = withContext(Dispatchers.IO) {
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

            // 3. Pull updates from Firestore for authorized records
            pullUpdatesFromCloud(activeBizId, userDoc, userRole)

            _lastSyncTime.value = System.currentTimeMillis()
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
        userRole: UserRole
    ) {
        val db = FirebaseFirestore.getInstance()
        val accessAll = userDoc.getBoolean("accessAllFiles") ?: true
        @Suppress("UNCHECKED_CAST")
        val allowedFileIds = (userDoc.get("allowedFileIds") as? List<String>) ?: emptyList()

        // Pull files
        val filesSnapshot = db.collection("businesses").document(businessId)
            .collection("files").get().await()

        for (fileDoc in filesSnapshot.documents) {
            val fileSharedId = fileDoc.getString("sharedId") ?: fileDoc.id
            if (userRole == UserRole.WORKER && !accessAll && fileSharedId !in allowedFileIds) {
                continue
            }
            val existing = loanFileDao.getFileBySharedId(fileSharedId)
            val name = fileDoc.getString("name") ?: ""
            val sortOrder = fileDoc.getLong("sortOrder")?.toInt() ?: 0
            val isDeleted = fileDoc.getBoolean("isDeleted") ?: false
            val createdAt = fileDoc.getLong("createdAt") ?: System.currentTimeMillis()
            val lastModified = fileDoc.getLong("lastModified") ?: System.currentTimeMillis()

            if (existing == null) {
                loanFileDao.insert(
                    com.moneymate.lite.data.entity.LoanFile(
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
            } else if (lastModified > existing.lastModified) {
                loanFileDao.update(
                    existing.copy(
                        name = name,
                        sortOrder = sortOrder,
                        isDeleted = isDeleted,
                        lastModified = lastModified
                    )
                )
            }
        }
    }
}
