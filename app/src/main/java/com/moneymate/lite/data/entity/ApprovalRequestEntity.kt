package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import java.util.UUID

@Entity(tableName = "approval_requests")
data class ApprovalRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val version: Int = 1,
    val supersededBy: String? = null,
    val workerId: String = "",
    val workerName: String = "",
    val fileSharedId: String = "",
    val fileName: String = "",
    val recordType: String = "", // "CUSTOMER", "LOAN", "PAYMENT", "FILE"
    val recordSharedId: String? = null,
    val actionType: String = "", // from PendingActionType.name
    val reason: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // from ApprovalStatus.name
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val baseSnapshotJson: String? = null,
    val proposedPayloadJson: String = "{}",
    val syncStatus: String = "SUBMITTED",
    val syncError: String? = null,
    val clientOpId: String = UUID.randomUUID().toString()
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot, defaultBusinessId: String): ApprovalRequestEntity {
            return ApprovalRequestEntity(
                id = doc.getString("id") ?: doc.id,
                businessId = doc.getString("businessId")?.takeIf { it.isNotBlank() } ?: defaultBusinessId,
                version = doc.getLong("version")?.toInt() ?: 1,
                supersededBy = doc.getString("supersededBy"),
                workerId = doc.getString("workerId") ?: "",
                workerName = doc.getString("workerName") ?: "Worker",
                fileSharedId = doc.getString("fileSharedId") ?: "",
                fileName = doc.getString("fileName") ?: "File",
                recordType = doc.getString("recordType") ?: "RECORD",
                recordSharedId = doc.getString("recordSharedId"),
                actionType = doc.getString("actionType") ?: "",
                reason = doc.getString("reason") ?: "",
                requestedAt = doc.getLong("requestedAt") ?: System.currentTimeMillis(),
                status = doc.getString("status") ?: "PENDING",
                rejectionReason = doc.getString("rejectionReason"),
                reviewedBy = doc.getString("reviewedBy"),
                reviewedAt = doc.getLong("reviewedAt"),
                baseSnapshotJson = doc.getString("baseSnapshotJson"),
                proposedPayloadJson = doc.getString("proposedPayloadJson") ?: "{}",
                syncStatus = doc.getString("syncStatus") ?: "SYNCED",
                syncError = doc.getString("syncError"),
                clientOpId = doc.getString("clientOpId") ?: UUID.randomUUID().toString()
            )
        }
    }
}
