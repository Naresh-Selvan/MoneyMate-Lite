package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
