package com.moneymate.lite.data.sync

import com.moneymate.lite.data.entity.Person

internal fun Person.acceptsCloudVersion(timestamp: Long, deleted: Boolean): Boolean =
    timestamp > lastModified || (timestamp == lastModified && (deleted || !isDeleted))

internal fun Person.isRedundantCopyOf(original: Person): Boolean =
    sharedId == original.sharedId && businessId == original.businessId &&
        fileSharedId == original.fileSharedId && name == original.name &&
        mobileNumber == original.mobileNumber && place == original.place &&
        notes == original.notes && syncStatus == "SYNCED" && lastModified <= original.lastModified

internal fun findCustomerDuplicates(
    customers: List<Person>,
    hasLoanHistory: Set<Long>,
    businessId: String
): List<Pair<Person, Person>> = customers
    .filter {
        !it.isDeleted && it.businessId == businessId && it.sharedId.isNotBlank() &&
            it.name.isNotBlank()
    }
    .groupBy { it.fileId to it.name.trim().lowercase() }
    .values
    .flatMap { group ->
        val original = group.singleOrNull { it.id in hasLoanHistory } ?: return@flatMap emptyList()
        group.filter { it.id != original.id }.map { it to original }
    }
