package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.WorkerProfileDao
import com.moneymate.lite.data.entity.ActivityLogEntity
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.WorkerProfileEntity
import com.moneymate.lite.data.repository.ActivityLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    private val activityLogRepository: ActivityLogRepository,
    private val workerProfileDao: WorkerProfileDao,
    private val loanFileDao: LoanFileDao,
    private val authManager: AuthManager
) : ViewModel() {

    private val _selectedActorId = MutableStateFlow<String?>(null)
    val selectedActorId: StateFlow<String?> = _selectedActorId.asStateFlow()

    private val _selectedAction = MutableStateFlow<String?>(null)
    val selectedAction: StateFlow<String?> = _selectedAction.asStateFlow()

    private val _selectedFileSharedId = MutableStateFlow<String?>(null)
    val selectedFileSharedId: StateFlow<String?> = _selectedFileSharedId.asStateFlow()

    val workers: StateFlow<List<WorkerProfileEntity>> = authManager.businessId
        .flatMapLatest { bizId ->
            workerProfileDao.getAllWorkersFlow(bizId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<LoanFile>> = loanFileDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ActivityLogEntity>> = combine(
        authManager.businessId,
        _selectedActorId,
        _selectedAction,
        _selectedFileSharedId
    ) { bizId, actorId, action, fileId ->
        Quadruple(bizId, actorId, action, fileId)
    }.flatMapLatest { (bizId, actorId, action, fileId) ->
        activityLogRepository.getFilteredActivityLogsFlow(bizId, actorId, action, fileId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterActor(actorId: String?) {
        _selectedActorId.value = actorId
    }

    fun setFilterAction(action: String?) {
        _selectedAction.value = action
    }

    fun setFilterFile(fileSharedId: String?) {
        _selectedFileSharedId.value = fileSharedId
    }

    fun resetFilters() {
        _selectedActorId.value = null
        _selectedAction.value = null
        _selectedFileSharedId.value = null
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
