package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.entity.ApprovalRequestEntity
import com.moneymate.lite.data.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
class ApprovalViewModel @Inject constructor(
    private val approvalRepository: ApprovalRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val pendingRequests: StateFlow<List<ApprovalRequestEntity>> = authManager.businessId
        .flatMapLatest { bizId ->
            approvalRepository.getPendingRequestsFlow(bizId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = authManager.businessId
        .flatMapLatest { bizId ->
            approvalRepository.getPendingRequestsCountFlow(bizId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val workerRequests: StateFlow<List<ApprovalRequestEntity>> = authManager.currentUser
        .flatMapLatest { user ->
            val uid = user?.uid ?: ""
            val bizId = authManager.businessId.value
            approvalRepository.getRequestsByWorkerFlow(bizId, uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun approve(
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = authManager.currentUser.value
        if (user == null) {
            onError("Authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = approvalRepository.approveRequest(
                requestId = requestId,
                reviewerId = user.uid,
                reviewerName = user.displayName ?: "Owner"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Approval failed")
            }
        }
    }

    fun reject(
        requestId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = authManager.currentUser.value
        if (user == null) {
            onError("Authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = approvalRepository.rejectRequest(
                requestId = requestId,
                reviewerId = user.uid,
                reviewerName = user.displayName ?: "Owner",
                rejectionReason = reason
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Rejection failed")
            }
        }
    }

    fun withdraw(
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = authManager.currentUser.value
        if (user == null) {
            onError("Authentication required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = approvalRepository.withdrawProposal(
                requestId = requestId,
                workerId = user.uid,
                workerName = user.displayName ?: "Worker"
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Withdrawal failed")
            }
        }
    }

    fun submitProposal(
        fileSharedId: String,
        fileName: String,
        recordType: String,
        recordSharedId: String?,
        actionType: com.moneymate.lite.data.model.PendingActionType,
        reason: String,
        baseSnapshotJson: String?,
        proposedPayloadJson: String,
        onSuccess: (ApprovalRequestEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = authManager.currentUser.value
        val bizId = authManager.businessId.value
        if (user == null || bizId.isBlank()) {
            onError("Authentication and active business required")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = approvalRepository.submitProposal(
                businessId = bizId,
                workerId = user.uid,
                workerName = user.displayName?.ifBlank { "Worker" } ?: "Worker",
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = recordType,
                recordSharedId = recordSharedId,
                actionType = actionType,
                reason = reason,
                baseSnapshotJson = baseSnapshotJson,
                proposedPayloadJson = proposedPayloadJson
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                onSuccess(result.getOrThrow())
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to submit request")
            }
        }
    }
}
