package com.moneymate.lite.data.repository

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.moneymate.lite.data.AppDatabase
import com.moneymate.lite.data.dao.ApprovalRequestDao
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.ApprovalRequestEntity
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.model.ApprovalStatus
import com.moneymate.lite.data.model.PendingActionType
import com.moneymate.lite.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApprovalRepository @Inject constructor(
    private val database: AppDatabase,
    private val approvalRequestDao: ApprovalRequestDao,
    private val loanFileDao: LoanFileDao,
    private val personDao: PersonDao,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao,
    private val activityLogRepository: ActivityLogRepository
) {
    fun getPendingRequestsFlow(businessId: String): Flow<List<ApprovalRequestEntity>> =
        approvalRequestDao.getPendingRequestsFlow(businessId)

    fun getPendingRequestsCountFlow(businessId: String): Flow<Int> =
        approvalRequestDao.getPendingRequestsCountFlow(businessId)

    fun getRequestsByWorkerFlow(businessId: String, workerId: String): Flow<List<ApprovalRequestEntity>> =
        approvalRequestDao.getRequestsByWorkerFlow(businessId, workerId)

    fun getPendingRequestsForRecordFlow(businessId: String, recordSharedId: String): Flow<List<ApprovalRequestEntity>> =
        approvalRequestDao.getPendingRequestsForRecordFlow(businessId, recordSharedId)

    suspend fun getRequestById(id: String): ApprovalRequestEntity? = withContext(Dispatchers.IO) {
        approvalRequestDao.getRequestById(id)
    }

    suspend fun getPersonSharedId(personId: Long): String? = withContext(Dispatchers.IO) {
        personDao.getPersonById(personId)?.sharedId
    }

    suspend fun resolveCustomerNameForRecord(recordType: String, recordSharedId: String?): String? = withContext(Dispatchers.IO) {
        if (recordSharedId.isNullOrBlank()) return@withContext null
        try {
            when (recordType.uppercase()) {
                "CUSTOMER", "PERSON" -> {
                    val p = personDao.getPersonBySharedId(recordSharedId)
                        ?: recordSharedId.toLongOrNull()?.let { personDao.getPersonById(it) }
                    p?.name
                }
                "LOAN" -> {
                    val l = loanDao.getLoanBySharedId(recordSharedId)
                        ?: recordSharedId.removePrefix("loan_").toLongOrNull()?.let { loanDao.getLoanById(it) }
                    l?.let { personDao.getPersonById(it.personId)?.name }
                }
                "PAYMENT" -> {
                    val p = paymentDao.getPaymentBySharedId(recordSharedId)
                        ?: recordSharedId.removePrefix("payment_").toLongOrNull()?.let { paymentDao.getPaymentById(it) }
                    if (p != null) {
                        val l = loanDao.getLoanById(p.loanId)
                        l?.let { personDao.getPersonById(it.personId)?.name }
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitProposal(
        businessId: String,
        workerId: String,
        workerName: String,
        fileSharedId: String,
        fileName: String,
        recordType: String,
        recordSharedId: String?,
        actionType: PendingActionType,
        reason: String,
        baseSnapshotJson: String?,
        proposedPayloadJson: String
    ): Result<ApprovalRequestEntity> = withContext(Dispatchers.IO) {
        try {
            if (reason.trim().isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("A reason is required for this request."))
            }

            val request = ApprovalRequestEntity(
                id = UUID.randomUUID().toString(),
                businessId = businessId,
                version = 1,
                supersededBy = null,
                workerId = workerId,
                workerName = workerName,
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = recordType,
                recordSharedId = recordSharedId,
                actionType = actionType.name,
                reason = reason.trim(),
                requestedAt = System.currentTimeMillis(),
                status = ApprovalStatus.PENDING.name,
                rejectionReason = null,
                reviewedBy = null,
                reviewedAt = null,
                baseSnapshotJson = baseSnapshotJson,
                proposedPayloadJson = proposedPayloadJson,
                syncStatus = "SUBMITTED",
                syncError = null,
                clientOpId = UUID.randomUUID().toString()
            )

            approvalRequestDao.insert(request)

            // Sync to Firestore
            if (FirebaseAuth.getInstance().currentUser != null && businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("businesses").document(businessId)
                    .collection("approval_requests").document(request.id)
                    .set(request)
                    .await()
            }

            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = workerId,
                actorName = workerName,
                actorRole = UserRole.WORKER,
                action = "SUBMIT_REQUEST",
                fileSharedId = fileSharedId,
                fileName = fileName,
                recordType = recordType,
                recordSharedId = recordSharedId,
                reason = reason.trim(),
                details = "Submitted ${actionType.displayName} for approval",
                requestId = request.id,
                outcome = "SUCCESS"
            )

            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdrawProposal(
        requestId: String,
        workerId: String,
        workerName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = approvalRequestDao.getRequestById(requestId)
                ?: return@withContext Result.failure(IllegalStateException("Request not found"))

            if (request.status != ApprovalStatus.PENDING.name) {
                return@withContext Result.failure(IllegalStateException("Request is no longer pending"))
            }

            val updated = request.copy(status = ApprovalStatus.WITHDRAWN.name)
            approvalRequestDao.update(updated)

            if (FirebaseAuth.getInstance().currentUser != null && request.businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("businesses").document(request.businessId)
                    .collection("approval_requests").document(request.id)
                    .update("status", ApprovalStatus.WITHDRAWN.name)
                    .await()
            }

            activityLogRepository.logActivity(
                businessId = request.businessId,
                actorId = workerId,
                actorName = workerName,
                actorRole = UserRole.WORKER,
                action = "WITHDRAW_REQUEST",
                fileSharedId = request.fileSharedId,
                fileName = request.fileName,
                recordType = request.recordType,
                recordSharedId = request.recordSharedId,
                details = "Withdrew request for ${request.actionType}",
                requestId = request.id,
                outcome = "SUCCESS"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supersedeProposal(
        oldRequestId: String,
        workerId: String,
        workerName: String,
        newReason: String,
        newProposedPayloadJson: String
    ): Result<ApprovalRequestEntity> = withContext(Dispatchers.IO) {
        try {
            val oldRequest = approvalRequestDao.getRequestById(oldRequestId)
                ?: return@withContext Result.failure(IllegalStateException("Original request not found"))

            val newId = UUID.randomUUID().toString()

            // 1. Create new immutable version
            val newRequest = oldRequest.copy(
                id = newId,
                version = oldRequest.version + 1,
                reason = newReason.trim(),
                proposedPayloadJson = newProposedPayloadJson,
                requestedAt = System.currentTimeMillis(),
                status = ApprovalStatus.PENDING.name,
                clientOpId = UUID.randomUUID().toString()
            )

            // 2. Mark old request as superseded
            val markedOld = oldRequest.copy(
                status = ApprovalStatus.WITHDRAWN.name,
                supersededBy = newId
            )

            database.withTransaction {
                approvalRequestDao.update(markedOld)
                approvalRequestDao.insert(newRequest)
            }

            if (FirebaseAuth.getInstance().currentUser != null && oldRequest.businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                val oldRef = db.collection("businesses").document(oldRequest.businessId)
                    .collection("approval_requests").document(oldRequest.id)
                val newRef = db.collection("businesses").document(newRequest.businessId)
                    .collection("approval_requests").document(newRequest.id)

                batch.update(oldRef, mapOf("status" to ApprovalStatus.WITHDRAWN.name, "supersededBy" to newId))
                batch.set(newRef, newRequest)
                batch.commit().await()
            }

            Result.success(newRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveRequest(
        requestId: String,
        reviewerId: String,
        reviewerName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = approvalRequestDao.getRequestById(requestId)
                ?: return@withContext Result.failure(IllegalStateException("Request not found"))

            // Idempotency: Do not re-apply if already approved
            if (request.status == ApprovalStatus.APPROVED.name) {
                return@withContext Result.success(Unit)
            }
            if (request.status != ApprovalStatus.PENDING.name) {
                return@withContext Result.failure(IllegalStateException("Request is not pending (status: ${request.status})"))
            }

            val payload = JSONObject(request.proposedPayloadJson)

            // Conflict detection: verify if underlying record was modified since submission
            if (!request.baseSnapshotJson.isNullOrBlank() && request.recordSharedId != null) {
                val base = JSONObject(request.baseSnapshotJson)
                val isStale = checkIfRecordIsStale(request.recordType, request.recordSharedId, base)
                if (isStale) {
                    val conflictRequest = request.copy(
                        status = ApprovalStatus.CONFLICT_STALE.name,
                        reviewedBy = reviewerId,
                        reviewedAt = System.currentTimeMillis(),
                        rejectionReason = "Conflict: Record was modified since submission. Fresh review required."
                    )
                    approvalRequestDao.update(conflictRequest)
                    if (FirebaseAuth.getInstance().currentUser != null && request.businessId.isNotBlank()) {
                        FirebaseFirestore.getInstance().collection("businesses").document(request.businessId)
                            .collection("approval_requests").document(request.id)
                            .set(conflictRequest).await()
                    }
                    return@withContext Result.failure(IllegalStateException("Record was modified since submission. Marked as Conflict."))
                }
            }

            val now = System.currentTimeMillis()

            // Atomically apply the official change in Room
            database.withTransaction {
                when (request.actionType) {
                    PendingActionType.CREATE_CUSTOMER_WITH_LOAN.name -> {
                        val file = loanFileDao.getFileBySharedId(request.fileSharedId)
                            ?: throw IllegalStateException("Target file not found")
                        val customerName = payload.getString("customerName")
                        val phone = payload.optString("phone").takeIf { it.isNotBlank() }
                        val place = payload.optString("place").takeIf { it.isNotBlank() }
                        val notes = payload.optString("notes").takeIf { it.isNotBlank() }
                        val initialLoanAmount = payload.optDouble("initialLoanAmount", 0.0)
                        val initialLoanDate = payload.optLong("initialLoanDate", now)

                        val personSharedId = UUID.randomUUID().toString()
                        val person = Person(
                            sharedId = personSharedId,
                            businessId = request.businessId,
                            fileId = file.id,
                            fileSharedId = file.sharedId,
                            name = customerName,
                            mobileNumber = phone,
                            place = place,
                            notes = notes,
                            createdAt = now,
                            lastModified = now
                        )
                        val personId = personDao.insert(person)

                        if (initialLoanAmount > 0.0) {
                            val loan = Loan(
                                sharedId = UUID.randomUUID().toString(),
                                businessId = request.businessId,
                                personId = personId,
                                personSharedId = personSharedId,
                                totalAmount = initialLoanAmount,
                                dateGiven = initialLoanDate,
                                createdAt = now,
                                lastModified = now
                            )
                            loanDao.insert(loan)
                        }
                    }

                    PendingActionType.CREATE_LOAN.name -> {
                        val person = personDao.getPersonBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Customer not found")
                        val amount = payload.getDouble("amount")
                        if (amount <= 0.0) throw IllegalArgumentException("Amount must be positive")
                        val date = payload.optLong("date", now)

                        val loan = Loan(
                            sharedId = UUID.randomUUID().toString(),
                            businessId = request.businessId,
                            personId = person.id,
                            personSharedId = person.sharedId,
                            totalAmount = amount,
                            dateGiven = date,
                            createdAt = now,
                            lastModified = now
                        )
                        loanDao.insert(loan)
                    }

                    PendingActionType.INCREASE_LOAN.name -> {
                        val loan = loanDao.getLoanBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Loan not found")
                        val additionalAmount = payload.getDouble("additionalAmount")
                        if (additionalAmount <= 0.0) throw IllegalArgumentException("Additional amount must be positive")
                        val newTotal = loan.totalAmount + additionalAmount
                        val updated = loan.copy(
                            totalAmount = newTotal,
                            lastModified = now,
                            isCompleted = false,
                            completedAt = null
                        )
                        loanDao.update(updated)
                    }

                    PendingActionType.EDIT_CUSTOMER.name -> {
                        val person = personDao.getPersonBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Customer not found")
                        val updated = person.copy(
                            name = payload.optString("name", person.name),
                            mobileNumber = payload.optString("mobileNumber", person.mobileNumber ?: ""),
                            place = payload.optString("place", person.place ?: ""),
                            notes = payload.optString("notes", person.notes ?: ""),
                            lastModified = now
                        )
                        personDao.update(updated)
                    }

                    PendingActionType.EDIT_LOAN.name -> {
                        val loan = loanDao.getLoanBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Loan not found")
                        val newAmount = payload.optDouble("amount", loan.totalAmount)
                        val newDate = payload.optLong("date", loan.dateGiven)
                        if (newAmount <= 0.0) throw IllegalArgumentException("Amount must be positive")

                        val totalPaid = paymentDao.getTotalPaidForLoan(loan.id)
                        val isComp = totalPaid >= newAmount
                        val updated = loan.copy(
                            totalAmount = newAmount,
                            dateGiven = newDate,
                            isCompleted = isComp,
                            completedAt = if (isComp && !loan.isCompleted) now else loan.completedAt,
                            lastModified = now
                        )
                        loanDao.update(updated)
                    }

                    PendingActionType.EDIT_PAYMENT.name -> {
                        val payment = paymentDao.getPaymentBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Payment not found")
                        val newAmount = payload.optDouble("amount", payment.amount)
                        val newDate = payload.optLong("date", payment.date)
                        if (newAmount <= 0.0) throw IllegalArgumentException("Amount must be positive")

                        val updatedPayment = payment.copy(
                            amount = newAmount,
                            date = newDate,
                            lastModified = now
                        )
                        paymentDao.update(updatedPayment)

                        val loan = loanDao.getLoanById(payment.loanId)
                        if (loan != null) {
                            val totalPaid = paymentDao.getTotalPaidForLoan(loan.id)
                            if (totalPaid >= loan.totalAmount && !loan.isCompleted) {
                                loanDao.markCompleted(loan.id, now)
                            } else if (totalPaid < loan.totalAmount && loan.isCompleted) {
                                loanDao.markIncomplete(loan.id)
                            }
                        }
                    }

                    PendingActionType.DELETE_CUSTOMER.name -> {
                        val person = personDao.getPersonBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Customer not found")
                        personDao.softDelete(person.id)
                        loanDao.softDeleteLoansForPerson(person.id)
                    }

                    PendingActionType.DELETE_LOAN.name -> {
                        val loan = loanDao.getLoanBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Loan not found")
                        loanDao.softDeleteLoan(loan.id)
                        paymentDao.softDeletePaymentsForLoan(loan.id)
                    }

                    PendingActionType.DELETE_PAYMENT.name -> {
                        val payment = paymentDao.getPaymentBySharedId(request.recordSharedId ?: "")
                            ?: throw IllegalStateException("Payment not found")
                        paymentDao.softDelete(payment.id)
                        val loan = loanDao.getLoanById(payment.loanId)
                        if (loan != null && loan.isCompleted) {
                            val totalPaid = paymentDao.getTotalPaidForLoan(loan.id)
                            if (totalPaid < loan.totalAmount) {
                                loanDao.markIncomplete(loan.id)
                            }
                        }
                    }

                    PendingActionType.DELETE_FILE.name -> {
                        val file = loanFileDao.getFileBySharedId(request.fileSharedId)
                            ?: throw IllegalStateException("File not found")
                        loanFileDao.softDelete(file.id)
                    }
                }

                // Update request state
                val approvedRequest = request.copy(
                    status = ApprovalStatus.APPROVED.name,
                    reviewedBy = reviewerId,
                    reviewedAt = now
                )
                approvalRequestDao.update(approvedRequest)
            }

            // Sync updated approval request to Firestore
            if (FirebaseAuth.getInstance().currentUser != null && request.businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("businesses").document(request.businessId)
                    .collection("approval_requests").document(request.id)
                    .update(
                        mapOf(
                            "status" to ApprovalStatus.APPROVED.name,
                            "reviewedBy" to reviewerId,
                            "reviewedAt" to now
                        )
                    ).await()
            }

            activityLogRepository.logActivity(
                businessId = request.businessId,
                actorId = reviewerId,
                actorName = reviewerName,
                actorRole = UserRole.OWNER,
                action = "APPROVE_REQUEST",
                fileSharedId = request.fileSharedId,
                fileName = request.fileName,
                recordType = request.recordType,
                recordSharedId = request.recordSharedId,
                details = "Approved ${request.actionType} requested by ${request.workerName}",
                requestId = request.id,
                afterSnapshotJson = request.proposedPayloadJson,
                outcome = "SUCCESS"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectRequest(
        requestId: String,
        reviewerId: String,
        reviewerName: String,
        rejectionReason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (rejectionReason.trim().isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Rejection reason is required."))
            }

            val request = approvalRequestDao.getRequestById(requestId)
                ?: return@withContext Result.failure(IllegalStateException("Request not found"))

            if (request.status != ApprovalStatus.PENDING.name) {
                return@withContext Result.failure(IllegalStateException("Request is no longer pending"))
            }

            val now = System.currentTimeMillis()
            val rejectedRequest = request.copy(
                status = ApprovalStatus.REJECTED.name,
                rejectionReason = rejectionReason.trim(),
                reviewedBy = reviewerId,
                reviewedAt = now
            )
            approvalRequestDao.update(rejectedRequest)

            if (FirebaseAuth.getInstance().currentUser != null && request.businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("businesses").document(request.businessId)
                    .collection("approval_requests").document(request.id)
                    .update(
                        mapOf(
                            "status" to ApprovalStatus.REJECTED.name,
                            "rejectionReason" to rejectionReason.trim(),
                            "reviewedBy" to reviewerId,
                            "reviewedAt" to now
                        )
                    ).await()
            }

            activityLogRepository.logActivity(
                businessId = request.businessId,
                actorId = reviewerId,
                actorName = reviewerName,
                actorRole = UserRole.OWNER,
                action = "REJECT_REQUEST",
                fileSharedId = request.fileSharedId,
                fileName = request.fileName,
                recordType = request.recordType,
                recordSharedId = request.recordSharedId,
                reason = rejectionReason.trim(),
                details = "Rejected ${request.actionType} requested by ${request.workerName}",
                requestId = request.id,
                outcome = "REJECTED"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun checkIfRecordIsStale(
        recordType: String,
        recordSharedId: String,
        baseSnapshot: JSONObject
    ): Boolean {
        return try {
            when (recordType) {
                "CUSTOMER" -> {
                    val p = personDao.getPersonBySharedId(recordSharedId) ?: return true
                    if (p.isDeleted != baseSnapshot.optBoolean("isDeleted", false)) return true
                    if (p.name != baseSnapshot.optString("name", p.name)) return true
                    false
                }
                "LOAN" -> {
                    val l = loanDao.getLoanBySharedId(recordSharedId) ?: return true
                    if (l.isDeleted != baseSnapshot.optBoolean("isDeleted", false)) return true
                    if (l.totalAmount != baseSnapshot.optDouble("totalAmount", l.totalAmount)) return true
                    false
                }
                "PAYMENT" -> {
                    val pay = paymentDao.getPaymentBySharedId(recordSharedId) ?: return true
                    if (pay.isDeleted != baseSnapshot.optBoolean("isDeleted", false)) return true
                    if (pay.amount != baseSnapshot.optDouble("amount", pay.amount)) return true
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        approvalRequestDao.clearAll()
    }
}
