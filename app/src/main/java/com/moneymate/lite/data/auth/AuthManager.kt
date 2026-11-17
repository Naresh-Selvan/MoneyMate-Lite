package com.moneymate.lite.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.moneymate.lite.data.dao.ActivityLogDao
import com.moneymate.lite.data.dao.ApprovalRequestDao
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.dao.WorkerProfileDao
import com.moneymate.lite.data.model.UserProfile
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
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loanFileDao: LoanFileDao,
    private val personDao: PersonDao,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao,
    private val approvalRequestDao: ApprovalRequestDao,
    private val activityLogDao: ActivityLogDao,
    private val workerProfileDao: WorkerProfileDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("moneymate_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(prefs.getBoolean("key_offline_mode", false))
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.GUEST)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _businessId = MutableStateFlow(prefs.getString("key_business_id", "") ?: "")
    val businessId: StateFlow<String> = _businessId.asStateFlow()

    private val _isPasswordChangeRequired = MutableStateFlow(false)
    val isPasswordChangeRequired: StateFlow<Boolean> = _isPasswordChangeRequired.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                setOfflineMode(false)
                scope.launch {
                    loadOrCreateUserProfile(user)
                }
            } else {
                _currentProfile.value = null
                _userRole.value = UserRole.GUEST
                _businessId.value = ""
            }
        }

        auth.currentUser?.let { user ->
            scope.launch {
                loadOrCreateUserProfile(user)
            }
        }
    }

    private suspend fun loadOrCreateUserProfile(user: FirebaseUser) = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(user.uid).get().await()

            if (userDoc.exists()) {
                val roleStr = userDoc.getString("role") ?: "WORKER"
                val role = if (roleStr == "OWNER") UserRole.OWNER else UserRole.WORKER
                val bizId = userDoc.getString("businessId") ?: "biz_${user.uid}"
                val active = userDoc.getBoolean("isActive") ?: true
                val mustChange = userDoc.getBoolean("mustChangePassword") ?: false

                if (!active) {
                    withContext(Dispatchers.Main) {
                        _authError.value = "Your account has been disabled by the owner."
                        signOut()
                    }
                    return@withContext
                }

                @Suppress("UNCHECKED_CAST")
                val allowedFiles = (userDoc.get("allowedFileIds") as? List<String>) ?: emptyList()
                val profile = UserProfile(
                    uid = user.uid,
                    businessId = bizId,
                    role = role,
                    displayName = userDoc.getString("displayName") ?: user.displayName ?: "",
                    username = userDoc.getString("username") ?: "",
                    email = userDoc.getString("email") ?: user.email ?: "",
                    accessAllFiles = userDoc.getBoolean("accessAllFiles") ?: (role == UserRole.OWNER),
                    allowedFileIds = allowedFiles,
                    isActive = active,
                    mustChangePassword = mustChange,
                    createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = userDoc.getLong("updatedAt") ?: System.currentTimeMillis()
                )

                _currentProfile.value = profile
                _userRole.value = role
                _businessId.value = bizId
                _isPasswordChangeRequired.value = mustChange
                prefs.edit().putString("key_business_id", bizId).apply()
            } else {
                val bizId = "biz_${user.uid}"
                val now = System.currentTimeMillis()
                val ownerProfile = UserProfile(
                    uid = user.uid,
                    businessId = bizId,
                    role = UserRole.OWNER,
                    displayName = user.displayName ?: "Owner",
                    username = "owner_${user.uid.take(6).lowercase()}",
                    email = user.email ?: "",
                    accessAllFiles = true,
                    allowedFileIds = emptyList(),
                    isActive = true,
                    mustChangePassword = false,
                    createdAt = now,
                    updatedAt = now
                )

                val data = hashMapOf(
                    "uid" to user.uid,
                    "businessId" to bizId,
                    "role" to "OWNER",
                    "displayName" to ownerProfile.displayName,
                    "username" to ownerProfile.username,
                    "email" to ownerProfile.email,
                    "accessAllFiles" to true,
                    "allowedFileIds" to emptyList<String>(),
                    "isActive" to true,
                    "mustChangePassword" to false,
                    "createdAt" to now,
                    "updatedAt" to now
                )

                val bizData = hashMapOf(
                    "businessId" to bizId,
                    "ownerUid" to user.uid,
                    "ownerEmail" to (user.email ?: ""),
                    "businessName" to "MoneyMate Business",
                    "createdAt" to now,
                    "migrationStatus" to "PENDING"
                )

                val batch = db.batch()
                batch.set(db.collection("users").document(user.uid), data)
                batch.set(db.collection("businesses").document(bizId), bizData)
                batch.commit().await()

                _currentProfile.value = ownerProfile
                _userRole.value = UserRole.OWNER
                _businessId.value = bizId
                _isPasswordChangeRequired.value = false
                prefs.edit().putString("key_business_id", bizId).apply()
            }
        } catch (e: Exception) {
            val cachedWorker = workerProfileDao.getWorkerById(user.uid)
            if (cachedWorker != null) {
                _userRole.value = UserRole.WORKER
                _businessId.value = cachedWorker.businessId
                _isPasswordChangeRequired.value = cachedWorker.mustChangePassword
            } else {
                _userRole.value = UserRole.OWNER
                _businessId.value = prefs.getString("key_business_id", "biz_${user.uid}") ?: "biz_${user.uid}"
            }
        }
    }

    suspend fun signInWorker(
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUsername = username.trim().lowercase()
            if (cleanUsername.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))
            }
            if (password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Password cannot be empty"))
            }

            val db = FirebaseFirestore.getInstance()
            val usernameDoc = db.collection("usernames").document(cleanUsername).get().await()

            if (!usernameDoc.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Worker username not found"))
            }

            val email = usernameDoc.getString("email")
                ?: return@withContext Result.failure(IllegalStateException("Worker email record not found"))

            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (newPassword.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
            }
            val user = auth.currentUser
                ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))

            user.updatePassword(newPassword).await()

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .update("mustChangePassword", false, "updatedAt", System.currentTimeMillis())
                .await()

            _isPasswordChangeRequired.value = false
            _currentProfile.value = _currentProfile.value?.copy(mustChangePassword = false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        prefs.edit().putBoolean("key_offline_mode", enabled).apply()
    }

    fun signOut() {
        auth.signOut()
        setOfflineMode(false)
        _currentProfile.value = null
        _userRole.value = UserRole.GUEST
        _businessId.value = ""
        _isPasswordChangeRequired.value = false
        prefs.edit().remove("key_business_id").apply()
    }

    fun clearAuthError() {
        _authError.value = null
    }
}
