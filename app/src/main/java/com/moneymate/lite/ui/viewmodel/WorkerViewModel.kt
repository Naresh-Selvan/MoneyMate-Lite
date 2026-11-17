package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.WorkerProfileEntity
import com.moneymate.lite.data.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkerViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val loanFileDao: LoanFileDao,
    private val authManager: AuthManager
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val workers: StateFlow<List<WorkerProfileEntity>> = authManager.businessId
        .flatMapLatest { bizId: String ->
            workerRepository.getAllWorkersFlow(bizId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<LoanFile>> = loanFileDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createWorker(
        displayName: String,
        username: String,
        tempPassword: String,
        accessAllFiles: Boolean,
        allowedFileIds: List<String>,
        onSuccess: (WorkerProfileEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val bizId = authManager.businessId.value
        val owner = authManager.currentUser.value
        if (bizId.isBlank() || owner == null) {
            onError("Owner authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = workerRepository.createWorker(
                businessId = bizId,
                displayName = displayName,
                username = username,
                tempPassword = tempPassword,
                accessAllFiles = accessAllFiles,
                allowedFileIds = allowedFileIds,
                ownerActorId = owner.uid,
                ownerActorName = owner.displayName ?: "Owner"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess(result.getOrThrow())
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to create worker")
            }
        }
    }

    fun resetPassword(
        workerUid: String,
        tempPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val bizId = authManager.businessId.value
        val owner = authManager.currentUser.value
        if (bizId.isBlank() || owner == null) {
            onError("Owner authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = workerRepository.resetWorkerPassword(
                businessId = bizId,
                workerUid = workerUid,
                tempPassword = tempPassword,
                ownerActorId = owner.uid,
                ownerActorName = owner.displayName ?: "Owner"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to reset password")
            }
        }
    }

    fun toggleWorkerStatus(
        workerUid: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val bizId = authManager.businessId.value
        val owner = authManager.currentUser.value
        if (bizId.isBlank() || owner == null) {
            onError("Owner authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = workerRepository.toggleWorkerStatus(
                businessId = bizId,
                workerUid = workerUid,
                isActive = isActive,
                ownerActorId = owner.uid,
                ownerActorName = owner.displayName ?: "Owner"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to update worker status")
            }
        }
    }

    fun getTodayCollectionsFlow(workerUid: String): Flow<Double> {
        val startOfDay = getStartOfTodayMillis()
        return workerRepository.getTodayCollectionsForWorker(workerUid, startOfDay)
    }

    fun getTodayCountFlow(workerUid: String): Flow<Int> {
        val startOfDay = getStartOfTodayMillis()
        return workerRepository.getTodayPaymentCountForWorker(workerUid, startOfDay)
    }

    private fun getStartOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun updateWorkerPrivileges(
        workerUid: String,
        accessAllFiles: Boolean,
        allowedFileIds: List<String>,
        canRecordPayments: Boolean,
        canCreateLoansDirectly: Boolean,
        canEditLoansDirectly: Boolean,
        canDeleteRecords: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val bizId = authManager.businessId.value
        val owner = authManager.currentUser.value
        if (bizId.isBlank() || owner == null) {
            onError("Owner authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = workerRepository.updateWorkerPrivileges(
                businessId = bizId,
                workerUid = workerUid,
                accessAllFiles = accessAllFiles,
                allowedFileIds = allowedFileIds,
                canRecordPayments = canRecordPayments,
                canCreateLoansDirectly = canCreateLoansDirectly,
                canEditLoansDirectly = canEditLoansDirectly,
                canDeleteRecords = canDeleteRecords,
                ownerActorId = owner.uid,
                ownerActorName = owner.displayName ?: "Owner"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to update privileges")
            }
        }
    }

    fun updateWorkerFileAccess(
        workerUid: String,
        accessAllFiles: Boolean,
        allowedFileIds: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val bizId = authManager.businessId.value
        val owner = authManager.currentUser.value
        if (bizId.isBlank() || owner == null) {
            onError("Owner authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = workerRepository.updateWorkerFileAccess(
                businessId = bizId,
                workerUid = workerUid,
                accessAllFiles = accessAllFiles,
                allowedFileIds = allowedFileIds,
                ownerActorId = owner.uid,
                ownerActorName = owner.displayName ?: "Owner"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to update permissions")
            }
        }
    }
}
