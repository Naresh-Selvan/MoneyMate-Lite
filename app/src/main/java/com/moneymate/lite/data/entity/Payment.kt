package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedId: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val loanId: Long,
    val loanSharedId: String = "",
    val amount: Double,
    val date: Long,
    val recordedByWorkerId: String? = null,
    val recordedByWorkerName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val syncError: String? = null
)
