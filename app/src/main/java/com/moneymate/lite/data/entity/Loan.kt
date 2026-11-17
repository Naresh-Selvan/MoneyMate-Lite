package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedId: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val personId: Long,
    val personSharedId: String = "",
    val totalAmount: Double,
    val dateGiven: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED"
)
