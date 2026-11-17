package com.moneymate.lite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedId: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val fileId: Long,
    val fileSharedId: String = "",
    val name: String,
    val mobileNumber: String?,
    val place: String?,
    val notes: String?,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "SYNCED"
)
