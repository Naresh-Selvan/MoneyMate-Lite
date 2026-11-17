package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "worker_profiles")
data class WorkerProfileEntity(
    @PrimaryKey val uid: String,
    val businessId: String,
    val displayName: String,
    val username: String,
    val email: String,
    val accessAllFiles: Boolean = true,
    val allowedFileIdsJson: String = "[]",
    val isActive: Boolean = true,
    val mustChangePassword: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
