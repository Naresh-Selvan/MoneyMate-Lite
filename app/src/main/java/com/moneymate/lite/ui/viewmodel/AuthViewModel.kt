package com.moneymate.lite.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("moneymate_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(prefs.getBoolean("key_offline_mode", false))
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        prefs.edit().putBoolean("key_offline_mode", enabled).apply()
    }

    fun signOut() {
        auth.signOut()
        setOfflineMode(false)
    }
}
