package com.moneymate.lite.data.sync

import androidx.room.withTransaction
import com.moneymate.lite.data.AppDatabase
import com.moneymate.lite.data.entity.Person
import javax.inject.Inject

data class CustomerDuplicate(val extra: Person, val original: Person, val fileName: String)

class CustomerDuplicates @Inject constructor(private val database: AppDatabase) {
    private val persons = database.personDao()

    private suspend fun candidates(businessId: String): List<Pair<Person, Person>> {
        val customers = persons.getAllPersonsIncludingDeleted()
        val businessFiles = database.loanFileDao().getAllFilesIncludingDeleted()
            .filter { !it.isDeleted && it.businessId == businessId }
            .associateBy { it.id }
        val loans = database.loanDao().getAllLoansIncludingDeleted()
        val hasHistory = loans.mapTo(mutableSetOf()) { it.personId }
        val customersById = customers.associateBy { it.id }
        val ownerBySharedId = customers.groupBy { it.sharedId }
        for (loan in loans) {
            val localOwner = customersById[loan.personId]
            // A missing or mismatched local reference may still point at a cloud customer.
            if (loan.personSharedId.isNotBlank() && localOwner?.sharedId != loan.personSharedId) {
                ownerBySharedId[loan.personSharedId]?.forEach { hasHistory.add(it.id) }
            }
        }
        val scopedCustomers = customers.mapNotNull { customer ->
            val file = businessFiles[customer.fileId] ?: return@mapNotNull null
            if (customer.fileSharedId.isNotBlank() && customer.fileSharedId != file.sharedId) {
                return@mapNotNull null
            }
            if (customer.businessId.isBlank()) customer.copy(businessId = businessId) else customer
        }
        return findCustomerDuplicates(scopedCustomers, hasHistory, businessId).filter { (extra, original) ->
            val approvals = database.approvalRequestDao()
            approvals.getPendingRequestsForRecord(businessId, extra.id.toString()).isEmpty() &&
                approvals.getPendingRequestsForRecord(businessId, "person_${extra.id}").isEmpty() &&
                (extra.sharedId == original.sharedId ||
                    approvals.getPendingRequestsForRecord(businessId, extra.sharedId).isEmpty())
        }.map { (extra, original) -> customersById.getValue(extra.id) to customersById.getValue(original.id) }
    }

    // Caller holds the same database transaction as the sync pull.
    internal suspend fun removeIdenticalLocalCopies(businessId: String) {
        for ((extra, original) in candidates(businessId)) {
            if (extra.isRedundantCopyOf(original)) {
                // Only the redundant local row is removed. The shared cloud document is the original.
                persons.removeRedundantLocalCopy(extra.id)
            }
        }
    }

    suspend fun review(businessId: String): List<CustomerDuplicate> = database.withTransaction {
        val files = database.loanFileDao().getAllFilesIncludingDeleted().associateBy { it.id }
        candidates(businessId).mapNotNull { (extra, original) ->
            val file = files[extra.fileId]?.takeIf { !it.isDeleted && it.businessId == businessId }
                ?: return@mapNotNull null
            CustomerDuplicate(extra, original, file.name)
        }.sortedWith(compareBy({ it.fileName }, { it.original.sortOrder }, { it.extra.sortOrder }))
    }

    suspend fun archive(businessId: String, reviewed: CustomerDuplicate) = database.withTransaction {
        val (extra, original) = candidates(businessId).firstOrNull {
            it.first == reviewed.extra && it.second == reviewed.original
        } ?: error("These records changed. Refresh the list and review them again.")
        val file = database.loanFileDao().getFileById(extra.fileId)
        check(file != null && !file.isDeleted && file.businessId == businessId) {
            "This loan file is no longer available."
        }
        check(extra.sharedId != original.sharedId) {
            "These entries share the same ID. They need to be checked before archiving."
        }
        persons.softDelete(extra.id, maxOf(System.currentTimeMillis(), extra.lastModified))
    }
}
