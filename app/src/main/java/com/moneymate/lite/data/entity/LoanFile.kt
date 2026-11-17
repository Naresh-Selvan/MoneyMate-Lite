package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "loan_files")
data class LoanFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedId: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED"
)
