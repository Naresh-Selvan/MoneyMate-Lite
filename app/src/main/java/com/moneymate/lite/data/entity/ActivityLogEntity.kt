package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import java.util.UUID

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorRole: String = "", // "OWNER", "WORKER"
    val action: String = "",
    val fileSharedId: String? = null,
    val fileName: String? = null,
    val recordType: String? = null,
    val recordSharedId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val details: String? = null,
    val beforeSnapshotJson: String? = null,
    val afterSnapshotJson: String? = null,
    val requestId: String? = null,
    val outcome: String = "SUCCESS", // "SUCCESS", "REJECTED", "CONFLICT"
    val syncStatus: String = "SYNCED"
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot, defaultBusinessId: String): ActivityLogEntity {
            return ActivityLogEntity(
                id = doc.getString("id") ?: doc.id,
                businessId = doc.getString("businessId")?.takeIf { it.isNotBlank() } ?: defaultBusinessId,
                actorId = doc.getString("actorId") ?: "",
                actorName = doc.getString("actorName") ?: "User",
                actorRole = doc.getString("actorRole") ?: "WORKER",
                action = doc.getString("action") ?: "",
                fileSharedId = doc.getString("fileSharedId"),
                fileName = doc.getString("fileName"),
                recordType = doc.getString("recordType"),
                recordSharedId = doc.getString("recordSharedId"),
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                reason = doc.getString("reason"),
                details = doc.getString("details"),
                beforeSnapshotJson = doc.getString("beforeSnapshotJson"),
                afterSnapshotJson = doc.getString("afterSnapshotJson"),
                requestId = doc.getString("requestId"),
                outcome = doc.getString("outcome") ?: "SUCCESS",
                syncStatus = "SYNCED"
            )
        }
    }
}
