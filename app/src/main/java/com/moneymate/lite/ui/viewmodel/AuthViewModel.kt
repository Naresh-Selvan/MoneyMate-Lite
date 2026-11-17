package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.moneymate.lite.data.auth.AuthManager
import com.moneymate.lite.data.model.UserProfile
import com.moneymate.lite.data.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val isOfflineMode: StateFlow<Boolean> = authManager.isOfflineMode
    val currentProfile: StateFlow<UserProfile?> = authManager.currentProfile
    val userRole: StateFlow<UserRole> = authManager.userRole
    val businessId: StateFlow<String> = authManager.businessId
    val isPasswordChangeRequired: StateFlow<Boolean> = authManager.isPasswordChangeRequired
    val authError: StateFlow<String?> = authManager.authError

    fun signInWorker(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.signInWorker(username, password)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Worker login failed")
            }
        }
    }

    fun changePassword(
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.changePassword(newPassword)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to update password")
            }
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        authManager.setOfflineMode(enabled)
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearAuthError() {
        authManager.clearAuthError()
    }
}
