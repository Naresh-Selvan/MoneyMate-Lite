package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.data.repository.ActivityLogRepository
import com.moneymate.lite.data.repository.LoanRepository
import com.moneymate.lite.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val authManager: AuthManager,
    private val activityLogRepository: ActivityLogRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _todayCollectionTotal = MutableStateFlow(0.0)
    val todayCollectionTotal: StateFlow<Double> = _todayCollectionTotal.asStateFlow()

    private val _totalOutstandingBalance = MutableStateFlow(0.0)
    val totalOutstandingBalance: StateFlow<Double> = _totalOutstandingBalance.asStateFlow()

    init {
        refreshTotals()
    }

    suspend fun recordPayment(loanId: Long, amount: Double, date: Long) {
        val user = authManager.currentUser.value
        val role = authManager.userRole.value
        val bizId = authManager.businessId.value
        val isOnline = syncManager.isOnline.value

        val customerName = loanRepository.getCustomerNameForLoan(loanId) ?: "Customer"
        val fileName = loanRepository.getFileNameForLoan(loanId) ?: ""
        val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""

        if (role == UserRole.WORKER && user != null) {
            val syncStatus = if (isOnline) "SYNCED" else "PENDING_SYNC"
            val effectiveName = authManager.getEffectiveUserName()
            loanRepository.recordWorkerPayment(
                loanId = loanId,
                amount = amount,
                date = date,
                workerId = user.uid,
                workerName = effectiveName,
                syncStatus = syncStatus
            )
            activityLogRepository.logActivity(
                businessId = bizId,
                actorId = user.uid,
                actorName = effectiveName,
                actorRole = UserRole.WORKER,
                action = "PAYMENT_RECORDED",
                details = "Recorded payment of ₹${"%.0f".format(amount)} for customer '$customerName'$fileInfo",
                fileName = fileName.takeIf { it.isNotBlank() },
                recordType = "PAYMENT",
                recordSharedId = loanId.toString(),
                outcome = "SUCCESS"
            )
            if (isOnline) {
                syncManager.triggerSync()
            } else {
                syncManager.refreshPendingCounts()
            }
        } else {
            loanRepository.recordPayment(loanId, amount, date)
            if (user != null) {
                activityLogRepository.logActivity(
                    businessId = bizId,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = UserRole.OWNER,
                    action = "PAYMENT_RECORDED",
                    details = "Recorded payment of ₹${"%.0f".format(amount)} for customer '$customerName'$fileInfo",
                    fileName = fileName.takeIf { it.isNotBlank() },
                    recordType = "PAYMENT",
                    recordSharedId = loanId.toString(),
                    outcome = "SUCCESS"
                )
            }
        }
        refreshTotals()
    }

    fun refresh() {
        refreshTotals()
    }

    private fun refreshTotals() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val todayEnd = calendar.timeInMillis

            _todayCollectionTotal.value = loanRepository.getTodayCollectionTotal(todayStart, todayEnd)
            _totalOutstandingBalance.value = loanRepository.getTotalOutstandingBalance()
        }
    }
}
