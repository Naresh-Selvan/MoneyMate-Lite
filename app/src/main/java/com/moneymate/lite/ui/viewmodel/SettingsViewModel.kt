package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.firebase.RestoreHelper
import com.moneymate.lite.data.firebase.UploadHelper
import com.moneymate.lite.data.migration.BusinessMigrationManager
import com.moneymate.lite.data.migration.MigrationPreview
import com.moneymate.lite.data.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val uploadHelper: UploadHelper,
    private val restoreHelper: RestoreHelper,
    private val migrationManager: BusinessMigrationManager,
    private val authManager: AuthManager
) : ViewModel() {

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupProgress = MutableStateFlow<String?>(null)
    val backupProgress: StateFlow<String?> = _backupProgress.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _isMigrating = MutableStateFlow(false)
    val isMigrating: StateFlow<Boolean> = _isMigrating.asStateFlow()

    private val _migrationProgress = MutableStateFlow<String?>(null)
    val migrationProgress: StateFlow<String?> = _migrationProgress.asStateFlow()

    private val _migrationPreview = MutableStateFlow<MigrationPreview?>(null)
    val migrationPreview: StateFlow<MigrationPreview?> = _migrationPreview.asStateFlow()

    fun loadMigrationPreview() {
        val bizId = authManager.businessId.value
        viewModelScope.launch {
            val preview = migrationManager.generatePreview(bizId)
            _migrationPreview.value = preview
        }
    }

    fun executeMigration(onResult: (Result<MigrationPreview>) -> Unit) {
        val bizId = authManager.businessId.value
        val user = authManager.currentUser.value
        if (bizId.isBlank() || user == null) {
            onResult(Result.failure(IllegalStateException("Owner authentication required")))
            return
        }

        if (_isMigrating.value) return
        _isMigrating.value = true
        _migrationProgress.value = "Starting migration..."

        viewModelScope.launch {
            val result = migrationManager.executeMigration(
                businessId = bizId,
                actorId = user.uid,
                actorName = user.displayName ?: "Owner",
                onProgress = { step, current, total ->
                    _migrationProgress.value = if (total > 0) "$step ($current/$total)" else step
                }
            )
            _isMigrating.value = false
            _migrationProgress.value = null
            if (result.isSuccess) {
                _migrationPreview.value = result.getOrNull()
            }
            onResult(result)
        }
    }

    fun backup(onResult: (Result<Unit>) -> Unit) {
        val bizId = authManager.businessId.value
        val role = authManager.userRole.value
        if (_isBackingUp.value || _isRestoring.value) return
        _isBackingUp.value = true
        _backupProgress.value = "Preparing data..."
        viewModelScope.launch {
            val result = uploadHelper.uploadAll(
                businessId = bizId,
                userRole = role,
                onProgress = { current, total ->
                    _backupProgress.value = "Uploading $current / $total"
                }
            )
            _isBackingUp.value = false
            _backupProgress.value = null
            onResult(result)
        }
    }

    fun restore(onResult: (Result<String>) -> Unit) {
        val bizId = authManager.businessId.value
        val role = authManager.userRole.value
        if (_isBackingUp.value || _isRestoring.value) return
        _isRestoring.value = true
        viewModelScope.launch {
            val result = restoreHelper.restoreAll(
                businessId = bizId,
                userRole = role
            )
            _isRestoring.value = false
            onResult(result)
        }
    }
}
