package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val name: String,
    val mobileNumber: String?,
    val place: String?,
    val notes: String?,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
