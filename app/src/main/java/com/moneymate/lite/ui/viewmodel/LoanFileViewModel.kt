package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.data.repository.ActivityLogRepository
import com.moneymate.lite.data.repository.LoanFileRepository
import com.moneymate.lite.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LoanFileViewModel @Inject constructor(
    private val repository: LoanFileRepository,
    private val authManager: AuthManager,
    private val activityLogRepository: ActivityLogRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val pendingSyncCount: StateFlow<Int> = syncManager.pendingSyncCount
    val failedSyncCount: StateFlow<Int> = syncManager.failedSyncCount
    val lastSyncTime: StateFlow<Long?> = syncManager.lastSyncTime
    val syncErrors: StateFlow<List<String>> = syncManager.syncErrors

    fun refreshSync() {
        viewModelScope.launch {
            syncManager.triggerSync()
        }
    }

    val files: StateFlow<List<LoanFile>> = combine(
        repository.getAllFiles(),
        authManager.currentProfile,
        authManager.userRole
    ) { allFiles, profile, role ->
        if (role == UserRole.WORKER && profile != null && !profile.accessAllFiles) {
            allFiles.filter { it.sharedId in profile.allowedFileIds }
        } else {
            allFiles
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    suspend fun addFile(name: String) {
        val bizId = authManager.businessId.value
        val user = authManager.currentUser.value
        val role = authManager.userRole.value

        if (role == UserRole.WORKER) {
            throw IllegalStateException("Workers cannot create files.")
        }

        val file = LoanFile(name = name, businessId = bizId)
        val id = repository.insert(file)

        if (user != null) {
            activityLogRepository.logActivity(
                businessId = bizId,
                actorId = user.uid,
                actorName = user.displayName ?: "Owner",
                actorRole = UserRole.OWNER,
                action = "OWNER_CREATE_FILE",
                fileName = name,
                recordType = "FILE",
                details = "Created file '$name'",
                outcome = "SUCCESS"
            )
        }
    }

    suspend fun renameFile(id: Long, newName: String) {
        val bizId = authManager.businessId.value
        val user = authManager.currentUser.value
        val role = authManager.userRole.value

        if (role == UserRole.WORKER) {
            throw IllegalStateException("Workers cannot rename files.")
        }

        val file = repository.getFileById(id)
        if (file != null) {
            val oldName = file.name
            repository.update(file.copy(name = newName, lastModified = System.currentTimeMillis()))

            if (user != null) {
                activityLogRepository.logActivity(
                    businessId = bizId,
                    actorId = user.uid,
                    actorName = user.displayName ?: "Owner",
                    actorRole = UserRole.OWNER,
                    action = "OWNER_RENAME_FILE",
                    fileSharedId = file.sharedId,
                    fileName = newName,
                    recordType = "FILE",
                    details = "Renamed file from '$oldName' to '$newName'",
                    outcome = "SUCCESS"
                )
            }
        }
    }

    suspend fun deleteFile(id: Long) {
        val bizId = authManager.businessId.value
        val user = authManager.currentUser.value
        val role = authManager.userRole.value

        if (role == UserRole.WORKER) {
            throw IllegalStateException("Workers cannot delete files directly.")
        }

        val file = repository.getFileById(id)
        repository.softDelete(id)

        if (user != null && file != null) {
            activityLogRepository.logActivity(
                businessId = bizId,
                actorId = user.uid,
                actorName = user.displayName ?: "Owner",
                actorRole = UserRole.OWNER,
                action = "OWNER_DELETE_FILE",
                fileSharedId = file.sharedId,
                fileName = file.name,
                recordType = "FILE",
                details = "Deleted file '${file.name}'",
                outcome = "SUCCESS"
            )
        }
    }
}
