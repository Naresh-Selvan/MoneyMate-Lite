package com.moneymate.lite.data.model

data class UserProfile(
    val uid: String = "",
    val businessId: String = "",
    val role: UserRole = UserRole.GUEST,
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val accessAllFiles: Boolean = true,
    val allowedFileIds: List<String> = emptyList(),
    val canRecordPayments: Boolean = true,
    val canCreateLoansDirectly: Boolean = false,
    val canEditLoansDirectly: Boolean = false,
    val canDeleteRecords: Boolean = false,
    val isActive: Boolean = true,
    val mustChangePassword: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
