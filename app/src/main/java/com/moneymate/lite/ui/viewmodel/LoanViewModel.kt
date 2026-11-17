package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.dao.LoanWithBalance
import com.moneymate.lite.data.dao.LoanWithBalanceAndFile
import com.moneymate.lite.data.dao.DateTransactionEntity
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.model.PendingActionType
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.data.repository.ActivityLogRepository
import com.moneymate.lite.data.repository.ApprovalRepository
import com.moneymate.lite.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val authManager: AuthManager,
    private val approvalRepository: ApprovalRepository,
    private val activityLogRepository: ActivityLogRepository
) : ViewModel() {

    // Caches to prevent creating new StateFlows on every recomposition
    // Use Eagerly so flows never stop and never reset to initial null/emptyList (prevents flickering)
    private val activeLoansInFileCache = mutableMapOf<Long, StateFlow<List<LoanWithBalance>>>()
    private val loansByPersonCache = mutableMapOf<Long, StateFlow<List<Loan>>>()
    private val activeLoanByPersonCache = mutableMapOf<Long, StateFlow<Loan?>>()
    private val paymentsByLoanCache = mutableMapOf<Long, StateFlow<List<Payment>>>()

    fun getActiveLoansInFile(fileId: Long): StateFlow<List<LoanWithBalance>> {
        return activeLoansInFileCache.getOrPut(fileId) {
            repository.getAllActiveLoansInFile(fileId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    val allActiveLoans: StateFlow<List<LoanWithBalanceAndFile>> = repository.getAllActiveLoansAcrossFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getLoansByPerson(personId: Long): StateFlow<List<Loan>> {
        return loansByPersonCache.getOrPut(personId) {
            repository.getLoansByPerson(personId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    fun getActiveLoanFlow(personId: Long): StateFlow<Loan?> {
        return activeLoanByPersonCache.getOrPut(personId) {
            repository.getActiveLoanByPersonFlow(personId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null
                )
        }
    }

    fun getPaymentsByLoan(loanId: Long): StateFlow<List<Payment>> {
        return paymentsByLoanCache.getOrPut(loanId) {
            repository.getPaymentsByLoan(loanId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    suspend fun createLoan(personId: Long, totalAmount: Double, dateGiven: Long): Result<Long> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canCreateLoansDirectly != true) {
            return Result.failure(IllegalStateException("REQUIRE_APPROVAL: Direct loan creation is restricted for workers. Please submit for owner approval."))
        }
        return try {
            val id = repository.createLoan(personId, totalAmount, dateGiven)
            val customerName = repository.getCustomerNameForPerson(personId) ?: "Customer"
            val fileName = repository.getFileNameForPerson(personId) ?: ""
            val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "LOAN_CREATED",
                    recordType = "LOAN",
                    recordSharedId = id.toString(),
                    fileName = fileName.takeIf { it.isNotBlank() },
                    details = "Created new loan of ₹${"%.0f".format(totalAmount)} for customer '$customerName'$fileInfo",
                    outcome = "SUCCESS"
                )
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deletePayment(id: Long, onResult: ((Result<Unit>) -> Unit)? = null) {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canDeleteRecords != true) {
            val err = "Workers cannot delete payments. Only the owner can reverse or delete payments."
            onResult?.invoke(Result.failure(IllegalStateException(err)))
            return
        }
        viewModelScope.launch {
            try {
                val payment = repository.getPaymentById(id)
                val customerName = repository.getCustomerNameForPayment(id) ?: "Customer"
                val fileName = repository.getFileNameForPayment(id) ?: ""
                val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
                repository.softDeletePayment(id)
                authManager.currentUser.value?.let { user ->
                    activityLogRepository.logActivity(
                        businessId = authManager.businessId.value,
                        actorId = user.uid,
                        actorName = authManager.getEffectiveUserName(),
                        actorRole = role,
                        action = "PAYMENT_DELETED",
                        recordType = "PAYMENT",
                        recordSharedId = payment?.sharedId ?: id.toString(),
                        fileName = fileName.takeIf { it.isNotBlank() },
                        details = "Deleted payment of ₹${"%.0f".format(payment?.amount ?: 0.0)} for customer '$customerName'$fileInfo (Moved to Recently Deleted)",
                        outcome = "SUCCESS"
                    )
                }
                onResult?.invoke(Result.success(Unit))
            } catch (e: Exception) {
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    fun restorePayment(id: Long) {
        viewModelScope.launch {
            repository.restorePayment(id)
        }
    }

    private val deletedPaymentsCache = mutableMapOf<Long, StateFlow<List<com.moneymate.lite.data.dao.DeletedPaymentWithPerson>>>()

    fun getDeletedPaymentsByFile(fileId: Long): StateFlow<List<com.moneymate.lite.data.dao.DeletedPaymentWithPerson>> {
        return deletedPaymentsCache.getOrPut(fileId) {
            repository.getDeletedPaymentsByFile(fileId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    fun getLoansGivenOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>> {
        return repository.getLoansGivenOnDate(fileId, startOfDay, endOfDay)
    }

    fun getPaymentsReceivedOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>> {
        return repository.getPaymentsReceivedOnDate(fileId, startOfDay, endOfDay)
    }

    fun getBalancesUpToDate(fileId: Long, upToDate: Long): Flow<List<com.moneymate.lite.data.dao.PersonBalanceUpToDate>> {
        return repository.getBalancesUpToDate(fileId, upToDate)
    }

    suspend fun addGivenTransaction(personId: Long, amount: Double, date: Long): Result<Unit> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canCreateLoansDirectly != true) {
            return Result.failure(IllegalStateException("REQUIRE_APPROVAL: Adding new loan amount requires owner approval."))
        }
        return try {
            repository.addGivenTransaction(personId, amount, date)
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "LOAN_GIVEN",
                    details = "Gave loan amount of ₹$amount",
                    outcome = "SUCCESS"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReceivedTransaction(personId: Long, amount: Double, date: Long): Result<Unit> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canRecordPayments == false) {
            return Result.failure(IllegalStateException("Payment recording is disabled for your worker account by the owner."))
        }
        return try {
            repository.addReceivedTransaction(personId, amount, date)
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "PAYMENT_RECORDED",
                    details = "Collected payment of ₹$amount",
                    outcome = "SUCCESS"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLoanAmount(loanId: Long, newAmount: Double): Result<Unit> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canEditLoansDirectly != true) {
            return Result.failure(IllegalStateException("REQUIRE_APPROVAL: Loan modification requires owner approval."))
        }
        return try {
            val oldLoan = repository.getLoanById(loanId)
            val customerName = repository.getCustomerNameForLoan(loanId) ?: "Customer"
            val fileName = repository.getFileNameForLoan(loanId) ?: ""
            val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
            repository.updateLoanAmount(loanId, newAmount)
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "LOAN_UPDATED",
                    recordType = "LOAN",
                    recordSharedId = oldLoan?.sharedId ?: loanId.toString(),
                    fileName = fileName.takeIf { it.isNotBlank() },
                    details = "Updated loan amount from ₹${"%.0f".format(oldLoan?.totalAmount ?: 0.0)} to ₹${"%.0f".format(newAmount)} for customer '$customerName'$fileInfo",
                    outcome = "SUCCESS"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLoan(loanId: Long, newAmount: Double, newDate: Long): Result<Unit> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canEditLoansDirectly != true) {
            return Result.failure(IllegalStateException("REQUIRE_APPROVAL: Loan modification requires owner approval."))
        }
        return try {
            val oldLoan = repository.getLoanById(loanId)
            val customerName = repository.getCustomerNameForLoan(loanId) ?: "Customer"
            val fileName = repository.getFileNameForLoan(loanId) ?: ""
            val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
            repository.updateLoan(loanId, newAmount, newDate)
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "LOAN_UPDATED",
                    recordType = "LOAN",
                    recordSharedId = oldLoan?.sharedId ?: loanId.toString(),
                    fileName = fileName.takeIf { it.isNotBlank() },
                    details = "Updated loan from ₹${"%.0f".format(oldLoan?.totalAmount ?: 0.0)} to ₹${"%.0f".format(newAmount)} for customer '$customerName'$fileInfo",
                    outcome = "SUCCESS"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentAmount(paymentId: Long, newAmount: Double): Result<Unit> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canEditLoansDirectly != true) {
            return Result.failure(IllegalStateException("REQUIRE_APPROVAL: Editing payment requires owner approval."))
        }
        return try {
            val oldPayment = repository.getPaymentById(paymentId)
            val customerName = repository.getCustomerNameForPayment(paymentId) ?: "Customer"
            val fileName = repository.getFileNameForPayment(paymentId) ?: ""
            val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
            repository.updatePaymentAmount(paymentId, newAmount)
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "PAYMENT_UPDATED",
                    recordType = "PAYMENT",
                    recordSharedId = oldPayment?.sharedId ?: paymentId.toString(),
                    fileName = fileName.takeIf { it.isNotBlank() },
                    details = "Updated payment from ₹${"%.0f".format(oldPayment?.amount ?: 0.0)} to ₹${"%.0f".format(newAmount)} for customer '$customerName'$fileInfo",
                    outcome = "SUCCESS"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePayment(paymentId: Long, newAmount: Double, newDate: Long): Result<Unit> {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canEditLoansDirectly != true) {
            return Result.failure(IllegalStateException("REQUIRE_APPROVAL: Editing payment requires owner approval."))
        }
        return try {
            val oldPayment = repository.getPaymentById(paymentId)
            val customerName = repository.getCustomerNameForPayment(paymentId) ?: "Customer"
            val fileName = repository.getFileNameForPayment(paymentId) ?: ""
            val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
            repository.updatePayment(paymentId, newAmount, newDate)
            authManager.currentUser.value?.let { user ->
                activityLogRepository.logActivity(
                    businessId = authManager.businessId.value,
                    actorId = user.uid,
                    actorName = authManager.getEffectiveUserName(),
                    actorRole = role,
                    action = "PAYMENT_UPDATED",
                    recordType = "PAYMENT",
                    recordSharedId = oldPayment?.sharedId ?: paymentId.toString(),
                    fileName = fileName.takeIf { it.isNotBlank() },
                    details = "Updated payment from ₹${"%.0f".format(oldPayment?.amount ?: 0.0)} to ₹${"%.0f".format(newAmount)} for customer '$customerName'$fileInfo",
                    outcome = "SUCCESS"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteLoan(id: Long, onResult: ((Result<Unit>) -> Unit)? = null) {
        val role = authManager.userRole.value
        val profile = authManager.currentProfile.value
        if (role == UserRole.WORKER && profile?.canDeleteRecords != true) {
            val err = "Workers cannot delete loans. Deletion requires owner approval."
            onResult?.invoke(Result.failure(IllegalStateException(err)))
            return
        }
        viewModelScope.launch {
            try {
                val loan = repository.getLoanById(id)
                val customerName = repository.getCustomerNameForLoan(id) ?: "Customer"
                val fileName = repository.getFileNameForLoan(id) ?: ""
                val fileInfo = if (fileName.isNotBlank()) " in file '$fileName'" else ""
                repository.softDeleteLoan(id)
                authManager.currentUser.value?.let { user ->
                    activityLogRepository.logActivity(
                        businessId = authManager.businessId.value,
                        actorId = user.uid,
                        actorName = authManager.getEffectiveUserName(),
                        actorRole = role,
                        action = "LOAN_DELETED",
                        recordType = "LOAN",
                        recordSharedId = loan?.sharedId ?: id.toString(),
                        fileName = fileName.takeIf { it.isNotBlank() },
                        details = "Deleted loan of ₹${"%.0f".format(loan?.totalAmount ?: 0.0)} for customer '$customerName'$fileInfo (Moved to Recently Deleted)",
                        outcome = "SUCCESS"
                    )
                }
                onResult?.invoke(Result.success(Unit))
            } catch (e: Exception) {
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    suspend fun submitLoanChangeProposal(
        fileSharedId: String,
        fileName: String,
        loanId: Long,
        customerName: String = "",
        oldAmount: Double,
        newAmount: Double,
        newDate: Long,
        reason: String
    ): Result<Unit> {
        val user = authManager.currentUser.value
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val bizId = authManager.businessId.value
        val loan = repository.getLoanById(loanId)
        val loanSharedId = loan?.sharedId ?: "loan_$loanId"
        val effectiveWorkerName = authManager.getEffectiveUserName()
        val resolvedCustomer = if (customerName.isNotBlank()) {
            customerName
        } else {
            approvalRepository.resolveCustomerNameForRecord("LOAN", loanSharedId) ?: "Customer"
        }

        val payload = JSONObject().apply {
            put("amount", newAmount)
            put("date", newDate)
            put("oldAmount", oldAmount)
            put("customerName", resolvedCustomer)
        }.toString()
        val base = JSONObject().apply {
            put("amount", oldAmount)
            put("date", loan?.dateGiven ?: newDate)
            put("customerName", resolvedCustomer)
        }.toString()

        return try {
            approvalRepository.submitProposal(
                businessId = bizId,
                workerId = user.uid,
                workerName = effectiveWorkerName,
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = "LOAN",
                recordSharedId = loanSharedId,
                actionType = PendingActionType.EDIT_LOAN,
                reason = reason,
                baseSnapshotJson = base,
                proposedPayloadJson = payload
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitPaymentEditProposal(
        fileSharedId: String,
        fileName: String,
        paymentId: Long,
        customerName: String = "",
        oldAmount: Double,
        newAmount: Double,
        newDate: Long,
        reason: String
    ): Result<Unit> {
        val user = authManager.currentUser.value
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val bizId = authManager.businessId.value
        val payment = repository.getPaymentById(paymentId)
        val paymentSharedId = payment?.sharedId ?: "payment_$paymentId"
        val effectiveWorkerName = authManager.getEffectiveUserName()
        val resolvedCustomer = if (customerName.isNotBlank()) {
            customerName
        } else {
            approvalRepository.resolveCustomerNameForRecord("PAYMENT", paymentSharedId) ?: "Customer"
        }

        val payload = JSONObject().apply {
            put("amount", newAmount)
            put("date", newDate)
            put("oldAmount", oldAmount)
            put("customerName", resolvedCustomer)
        }.toString()
        val base = JSONObject().apply {
            put("amount", oldAmount)
            put("date", payment?.date ?: newDate)
            put("customerName", resolvedCustomer)
        }.toString()

        return try {
            approvalRepository.submitProposal(
                businessId = bizId,
                workerId = user.uid,
                workerName = effectiveWorkerName,
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = "PAYMENT",
                recordSharedId = paymentSharedId,
                actionType = PendingActionType.EDIT_PAYMENT,
                reason = reason,
                baseSnapshotJson = base,
                proposedPayloadJson = payload
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitNewLoanProposal(
        fileSharedId: String,
        fileName: String,
        personId: Long,
        customerName: String = "",
        amount: Double,
        date: Long,
        reason: String
    ): Result<Unit> {
        val user = authManager.currentUser.value
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val bizId = authManager.businessId.value
        val effectiveWorkerName = authManager.getEffectiveUserName()
        val personSharedId = approvalRepository.getPersonSharedId(personId) ?: "person_$personId"
        val resolvedCustomer = if (customerName.isNotBlank()) {
            customerName
        } else {
            approvalRepository.resolveCustomerNameForRecord("CUSTOMER", personSharedId) ?: "Customer"
        }

        val payload = JSONObject().apply {
            put("amount", amount)
            put("date", date)
            put("customerName", resolvedCustomer)
        }.toString()
        val base = JSONObject().apply {
            put("customerName", resolvedCustomer)
        }.toString()

        return try {
            approvalRepository.submitProposal(
                businessId = bizId,
                workerId = user.uid,
                workerName = effectiveWorkerName,
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = "CUSTOMER",
                recordSharedId = personSharedId,
                actionType = PendingActionType.CREATE_LOAN,
                reason = reason,
                baseSnapshotJson = base,
                proposedPayloadJson = payload
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitPaymentReversalProposal(
        fileSharedId: String,
        fileName: String,
        paymentSharedId: String,
        amount: Double,
        reason: String
    ): Result<Unit> {
        val user = authManager.currentUser.value
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val bizId = authManager.businessId.value
        val base = JSONObject().apply {
            put("amount", amount)
        }.toString()

        return try {
            approvalRepository.submitProposal(
                businessId = bizId,
                workerId = user.uid,
                workerName = authManager.getEffectiveUserName(),
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = "PAYMENT",
                recordSharedId = paymentSharedId,
                actionType = PendingActionType.DELETE_PAYMENT,
                reason = reason,
                baseSnapshotJson = base,
                proposedPayloadJson = "{}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
