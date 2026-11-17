package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val actorId: String,
    val actorName: String,
    val actorRole: String, // "OWNER", "WORKER"
    val action: String,
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
)
