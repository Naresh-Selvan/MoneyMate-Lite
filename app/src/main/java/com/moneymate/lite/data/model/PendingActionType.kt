package com.moneymate.lite.data.model

enum class PendingActionType(val displayName: String) {
    CREATE_CUSTOMER_WITH_LOAN("New Customer & Loan"),
    CREATE_LOAN("New Loan"),
    INCREASE_LOAN("Increase Loan"),
    EDIT_CUSTOMER("Edit Customer"),
    EDIT_LOAN("Edit Loan"),
    EDIT_PAYMENT("Edit Payment"),
    DELETE_FILE("Delete File"),
    DELETE_CUSTOMER("Delete Customer"),
    DELETE_LOAN("Delete Loan"),
    DELETE_PAYMENT("Delete Payment")
}
