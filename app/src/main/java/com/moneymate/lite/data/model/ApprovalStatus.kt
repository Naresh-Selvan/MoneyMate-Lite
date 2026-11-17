package com.moneymate.lite.data.model

enum class ApprovalStatus(val label: String) {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    WITHDRAWN("Withdrawn"),
    CONFLICT_STALE("Conflict / Stale")
}
