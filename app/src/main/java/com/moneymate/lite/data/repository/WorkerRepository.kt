package com.moneymate.lite.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.WorkerProfileDao
import com.moneymate.lite.data.entity.WorkerProfileEntity
import com.moneymate.lite.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workerProfileDao: WorkerProfileDao,
    private val paymentDao: PaymentDao,
    private val activityLogRepository: ActivityLogRepository
) {
    fun getAllWorkersFlow(businessId: String): Flow<List<WorkerProfileEntity>> =
        workerProfileDao.getAllWorkersFlow(businessId)

    fun getTodayCollectionsForWorker(workerUid: String, startOfDay: Long): Flow<Double> =
        paymentDao.getTodayCollectionsByWorker(workerUid, startOfDay)

    fun getTodayPaymentCountForWorker(workerUid: String, startOfDay: Long): Flow<Int> =
        paymentDao.getTodayPaymentCountByWorker(workerUid, startOfDay)

    suspend fun getWorkerById(uid: String): WorkerProfileEntity? = withContext(Dispatchers.IO) {
        workerProfileDao.getWorkerById(uid)
    }

    suspend fun createWorker(
        businessId: String,
        displayName: String,
        username: String,
        tempPassword: String,
        accessAllFiles: Boolean,
        allowedFileIds: List<String>,
        ownerActorId: String,
        ownerActorName: String
    ): Result<WorkerProfileEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanUsername = username.trim().lowercase()
            if (cleanUsername.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))
            }
            if (tempPassword.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
            }

            val db = FirebaseFirestore.getInstance()

            // 1. Check if username is already taken globally or in business
            val usernameDoc = db.collection("usernames").document(cleanUsername).get().await()
            if (usernameDoc.exists()) {
                return@withContext Result.failure(IllegalStateException("Username '$cleanUsername' is already taken. Please choose another."))
            }

            // 2. Synthetic email for worker Firebase Auth account (must follow standard RFC 5322 format)
            val cleanBizId = businessId.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase().ifBlank { "biz" }
            val workerEmail = "${cleanUsername}.${cleanBizId}@moneymate.com"

            // 3. Provision worker in Firebase Auth via secondary FirebaseApp to avoid signing out the owner
            val secondaryApp = try {
                FirebaseApp.getInstance("WorkerProvisioningApp")
            } catch (e: Exception) {
                FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "WorkerProvisioningApp")
            }
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            val workerUid = try {
                val authResult = secondaryAuth.createUserWithEmailAndPassword(workerEmail, tempPassword).await()
                authResult.user?.uid ?: throw IllegalStateException("Failed to create worker authentication account")
            } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                // If the auth user was already created in an earlier partial attempt, reuse and reset password
                try {
                    val signInResult = secondaryAuth.signInWithEmailAndPassword(workerEmail, tempPassword).await()
                    signInResult.user?.uid ?: throw e
                } catch (signInErr: Exception) {
                    throw IllegalStateException("Username '$cleanUsername' is already registered in Firebase. Please choose another username or delete it from Firebase Authentication.", e)
                }
            }

            // 4. Save UserProfile in Firestore
            val allowedJson = JSONArray(allowedFileIds).toString()
            val now = System.currentTimeMillis()

            val firestoreData = hashMapOf(
                "uid" to workerUid,
                "businessId" to businessId,
                "role" to "WORKER",
                "displayName" to displayName.trim(),
                "username" to cleanUsername,
                "email" to workerEmail,
                "accessAllFiles" to accessAllFiles,
                "allowedFileIds" to allowedFileIds,
                "canRecordPayments" to true,
                "canCreateLoansDirectly" to false,
                "canEditLoansDirectly" to false,
                "canDeleteRecords" to false,
                "isActive" to true,
                "mustChangePassword" to true,
                "createdAt" to now,
                "updatedAt" to now
            )

            val batch = db.batch()
            batch.set(db.collection("users").document(workerUid), firestoreData)
            batch.set(
                db.collection("usernames").document(cleanUsername),
                hashMapOf(
                    "uid" to workerUid,
                    "businessId" to businessId,
                    "email" to workerEmail
                )
            )
            batch.set(
                db.collection("businesses").document(businessId)
                    .collection("workers").document(workerUid),
                firestoreData
            )
            batch.commit().await()
            secondaryAuth.signOut()

            // 5. Save locally in Room
            val entity = WorkerProfileEntity(
                uid = workerUid,
                businessId = businessId,
                displayName = displayName.trim(),
                username = cleanUsername,
                email = workerEmail,
                accessAllFiles = accessAllFiles,
                allowedFileIdsJson = allowedJson,
                canRecordPayments = true,
                canCreateLoansDirectly = false,
                canEditLoansDirectly = false,
                canDeleteRecords = false,
                isActive = true,
                mustChangePassword = true,
                createdAt = now,
                updatedAt = now
            )
            workerProfileDao.insert(entity)

            // 6. Log Activity
            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = ownerActorId,
                actorName = ownerActorName,
                actorRole = UserRole.OWNER,
                action = "WORKER_CREATED",
                details = "Created worker account '$cleanUsername' ($displayName)",
                recordType = "WORKER",
                recordSharedId = workerUid,
                outcome = "SUCCESS"
            )

            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetWorkerPassword(
        businessId: String,
        workerUid: String,
        tempPassword: String,
        ownerActorId: String,
        ownerActorName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (tempPassword.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
            }

            val worker = workerProfileDao.getWorkerById(workerUid)
                ?: return@withContext Result.failure(IllegalStateException("Worker not found"))

            // Update in secondary app if possible or update password flag
            try {
                val secondaryApp = try {
                    FirebaseApp.getInstance("WorkerProvisioningApp")
                } catch (e: Exception) {
                    FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "WorkerProvisioningApp")
                }
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
                // Sign in as worker and update password
                val workerLogin = secondaryAuth.signInWithEmailAndPassword(worker.email, "OLD_OR_TEMP_IGNORED").await()
                workerLogin.user?.updatePassword(tempPassword)?.await()
                secondaryAuth.signOut()
            } catch (e: Exception) {
                // If sign in fails, update mustChangePassword in Firestore so worker will be required to change
            }

            val db = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            val updates = hashMapOf<String, Any>(
                "mustChangePassword" to true,
                "updatedAt" to now
            )
            db.collection("users").document(workerUid).update(updates).await()
            db.collection("businesses").document(businessId)
                .collection("workers").document(workerUid).update(updates).await()

            workerProfileDao.update(worker.copy(mustChangePassword = true, updatedAt = now))

            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = ownerActorId,
                actorName = ownerActorName,
                actorRole = UserRole.OWNER,
                action = "WORKER_PASSWORD_RESET",
                details = "Reset password for worker '${worker.username}'",
                recordType = "WORKER",
                recordSharedId = workerUid,
                outcome = "SUCCESS"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleWorkerStatus(
        businessId: String,
        workerUid: String,
        isActive: Boolean,
        ownerActorId: String,
        ownerActorName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val worker = workerProfileDao.getWorkerById(workerUid)
                ?: return@withContext Result.failure(IllegalStateException("Worker not found"))

            val db = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            val updates = hashMapOf<String, Any>(
                "isActive" to isActive,
                "updatedAt" to now
            )

            db.collection("users").document(workerUid).update(updates).await()
            db.collection("businesses").document(businessId)
                .collection("workers").document(workerUid).update(updates).await()

            workerProfileDao.update(worker.copy(isActive = isActive, updatedAt = now))

            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = ownerActorId,
                actorName = ownerActorName,
                actorRole = UserRole.OWNER,
                action = if (isActive) "WORKER_ENABLED" else "WORKER_DISABLED",
                details = "${if (isActive) "Enabled" else "Disabled"} worker account '${worker.username}'",
                recordType = "WORKER",
                recordSharedId = workerUid,
                outcome = "SUCCESS"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkerPrivileges(
        businessId: String,
        workerUid: String,
        accessAllFiles: Boolean,
        allowedFileIds: List<String>,
        canRecordPayments: Boolean,
        canCreateLoansDirectly: Boolean,
        canEditLoansDirectly: Boolean,
        canDeleteRecords: Boolean,
        ownerActorId: String,
        ownerActorName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val worker = workerProfileDao.getWorkerById(workerUid)
                ?: return@withContext Result.failure(IllegalStateException("Worker not found"))

            val db = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            val updates = hashMapOf<String, Any>(
                "accessAllFiles" to accessAllFiles,
                "allowedFileIds" to allowedFileIds,
                "canRecordPayments" to canRecordPayments,
                "canCreateLoansDirectly" to canCreateLoansDirectly,
                "canEditLoansDirectly" to canEditLoansDirectly,
                "canDeleteRecords" to canDeleteRecords,
                "updatedAt" to now
            )

            db.collection("users").document(workerUid).update(updates).await()
            db.collection("businesses").document(businessId)
                .collection("workers").document(workerUid).update(updates).await()

            val allowedJson = JSONArray(allowedFileIds).toString()
            workerProfileDao.update(
                worker.copy(
                    accessAllFiles = accessAllFiles,
                    allowedFileIdsJson = allowedJson,
                    canRecordPayments = canRecordPayments,
                    canCreateLoansDirectly = canCreateLoansDirectly,
                    canEditLoansDirectly = canEditLoansDirectly,
                    canDeleteRecords = canDeleteRecords,
                    updatedAt = now
                )
            )

            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = ownerActorId,
                actorName = ownerActorName,
                actorRole = UserRole.OWNER,
                action = "WORKER_PRIVILEGES_UPDATED",
                details = "Updated privileges for worker '${worker.username}': RecordPayments=$canRecordPayments, DirectCreate=$canCreateLoansDirectly, DirectEdit=$canEditLoansDirectly, Delete=$canDeleteRecords, AllFiles=$accessAllFiles",
                recordType = "WORKER",
                recordSharedId = workerUid,
                outcome = "SUCCESS"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkerFileAccess(
        businessId: String,
        workerUid: String,
        accessAllFiles: Boolean,
        allowedFileIds: List<String>,
        ownerActorId: String,
        ownerActorName: String
    ): Result<Unit> = updateWorkerPrivileges(
        businessId = businessId,
        workerUid = workerUid,
        accessAllFiles = accessAllFiles,
        allowedFileIds = allowedFileIds,
        canRecordPayments = workerProfileDao.getWorkerById(workerUid)?.canRecordPayments ?: true,
        canCreateLoansDirectly = workerProfileDao.getWorkerById(workerUid)?.canCreateLoansDirectly ?: false,
        canEditLoansDirectly = workerProfileDao.getWorkerById(workerUid)?.canEditLoansDirectly ?: false,
        canDeleteRecords = workerProfileDao.getWorkerById(workerUid)?.canDeleteRecords ?: false,
        ownerActorId = ownerActorId,
        ownerActorName = ownerActorName
    )

    suspend fun refreshWorkersFromCloud(businessId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (businessId.isBlank()) return@withContext Result.success(Unit)
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("businesses").document(businessId)
                .collection("workers").get().await()

            val list = mutableListOf<WorkerProfileEntity>()
            for (doc in snapshot.documents) {
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("displayName") ?: ""
                val username = doc.getString("username") ?: ""
                val email = doc.getString("email") ?: ""
                val accessAll = doc.getBoolean("accessAllFiles") ?: true
                @Suppress("UNCHECKED_CAST")
                val files = doc.get("allowedFileIds") as? List<String> ?: emptyList()
                val canRecord = doc.getBoolean("canRecordPayments") ?: true
                val canCreate = doc.getBoolean("canCreateLoansDirectly") ?: false
                val canEdit = doc.getBoolean("canEditLoansDirectly") ?: false
                val canDelete = doc.getBoolean("canDeleteRecords") ?: false
                val active = doc.getBoolean("isActive") ?: true
                val mustChange = doc.getBoolean("mustChangePassword") ?: false
                val created = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                list.add(
                    WorkerProfileEntity(
                        uid = uid,
                        businessId = businessId,
                        displayName = name,
                        username = username,
                        email = email,
                        accessAllFiles = accessAll,
                        allowedFileIdsJson = JSONArray(files).toString(),
                        canRecordPayments = canRecord,
                        canCreateLoansDirectly = canCreate,
                        canEditLoansDirectly = canEdit,
                        canDeleteRecords = canDelete,
                        isActive = active,
                        mustChangePassword = mustChange,
                        createdAt = created,
                        updatedAt = updated
                    )
                )
            }
            if (list.isNotEmpty()) {
                workerProfileDao.insertAll(list)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
