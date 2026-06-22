package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_files")
data class LoanFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
