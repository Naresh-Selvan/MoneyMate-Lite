package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "approval_requests")
data class ApprovalRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val version: Int = 1,
    val supersededBy: String? = null,
    val workerId: String,
    val workerName: String,
    val fileSharedId: String,
    val fileName: String,
    val recordType: String, // "CUSTOMER", "LOAN", "PAYMENT", "FILE"
    val recordSharedId: String? = null,
    val actionType: String, // from PendingActionType.name
    val reason: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // from ApprovalStatus.name
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val baseSnapshotJson: String? = null,
    val proposedPayloadJson: String,
    val syncStatus: String = "SUBMITTED",
    val syncError: String? = null,
    val clientOpId: String = UUID.randomUUID().toString()
)
