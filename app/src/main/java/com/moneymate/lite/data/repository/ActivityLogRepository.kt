package com.moneymate.lite.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.moneymate.lite.data.dao.ActivityLogDao
import com.moneymate.lite.data.entity.ActivityLogEntity
import com.moneymate.lite.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogRepository @Inject constructor(
    private val activityLogDao: ActivityLogDao
) {
    fun getActivityLogsFlow(businessId: String): Flow<List<ActivityLogEntity>> =
        activityLogDao.getActivityLogsFlow(businessId)

    fun getFilteredActivityLogsFlow(
        businessId: String,
        actorId: String?,
        action: String?,
        fileSharedId: String?
    ): Flow<List<ActivityLogEntity>> =
        activityLogDao.getFilteredActivityLogsFlow(businessId, actorId, action, fileSharedId)

    suspend fun logActivity(
        businessId: String,
        actorId: String,
        actorName: String,
        actorRole: UserRole,
        action: String,
        fileSharedId: String? = null,
        fileName: String? = null,
        recordType: String? = null,
        recordSharedId: String? = null,
        reason: String? = null,
        details: String? = null,
        beforeSnapshotJson: String? = null,
        afterSnapshotJson: String? = null,
        requestId: String? = null,
        outcome: String = "SUCCESS"
    ): ActivityLogEntity = withContext(Dispatchers.IO) {
        val log = ActivityLogEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            actorId = actorId,
            actorName = actorName,
            actorRole = actorRole.name,
            action = action,
            fileSharedId = fileSharedId,
            fileName = fileName,
            recordType = recordType,
            recordSharedId = recordSharedId,
            timestamp = System.currentTimeMillis(),
            reason = reason,
            details = details,
            beforeSnapshotJson = beforeSnapshotJson,
            afterSnapshotJson = afterSnapshotJson,
            requestId = requestId,
            outcome = outcome,
            syncStatus = "SYNCED"
        )

        // Save locally
        activityLogDao.insert(log)

        // Try writing to Firestore if authenticated
        try {
            if (FirebaseAuth.getInstance().currentUser != null && businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("businesses").document(businessId)
                    .collection("activity_logs").document(log.id)
                    .set(log)
                    .await()
            }
        } catch (e: Exception) {
            // Offline or firestore error - logged locally
        }

        log
    }

    suspend fun insertAll(logs: List<ActivityLogEntity>) = withContext(Dispatchers.IO) {
        activityLogDao.insertAll(logs)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        activityLogDao.clearAll()
    }
}
