package com.moneymate.lite.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.data.repository.ActivityLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadHelper @Inject constructor(
    private val loanFileDao: LoanFileDao,
    private val personDao: PersonDao,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao,
    private val activityLogRepository: ActivityLogRepository
) {
    suspend fun uploadAll(
        businessId: String,
        userRole: UserRole,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Privilege check: Owner only!
            if (userRole != UserRole.OWNER) {
                return@withContext Result.failure(SecurityException("Only business owner has permission to perform manual backup."))
            }

            val user = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("User not authenticated")
            val db = FirebaseFirestore.getInstance()

            // Verify owner identity in Firestore
            val userDoc = db.collection("users").document(user.uid).get().await()
            if (userDoc.getString("role") != "OWNER") {
                return@withContext Result.failure(SecurityException("Server validation failed: Account does not have owner privileges."))
            }

            val files = loanFileDao.getAllFilesIncludingDeleted()
            val persons = personDao.getAllPersonsIncludingDeleted()
            val loans = loanDao.getAllLoansIncludingDeleted()
            val payments = paymentDao.getAllPaymentsIncludingDeleted()

            val totalOps = files.size + persons.size + loans.size + payments.size
            if (totalOps == 0) {
                return@withContext Result.success(Unit)
            }

            var completedOps = 0
            onProgress(0, totalOps)

            // 1. Upload files with deletion markers preserved
            for (chunk in files.chunked(300)) {
                val batch = db.batch()
                for (file in chunk) {
                    val ref = db.collection("businesses").document(businessId)
                        .collection("files").document(file.sharedId)
                    batch.set(ref, file, SetOptions.merge())
                }
                batch.commit().await()
                completedOps += chunk.size
                onProgress(completedOps, totalOps)
            }

            // 2. Upload persons with deletion markers preserved
            for (chunk in persons.chunked(300)) {
                val batch = db.batch()
                for (person in chunk) {
                    val ref = db.collection("businesses").document(businessId)
                        .collection("persons").document(person.sharedId)
                    batch.set(ref, person, SetOptions.merge())
                }
                batch.commit().await()
                completedOps += chunk.size
                onProgress(completedOps, totalOps)
            }

            // 3. Upload loans with deletion markers preserved
            for (chunk in loans.chunked(300)) {
                val batch = db.batch()
                for (loan in chunk) {
                    val ref = db.collection("businesses").document(businessId)
                        .collection("loans").document(loan.sharedId)
                    batch.set(ref, loan, SetOptions.merge())
                }
                batch.commit().await()
                completedOps += chunk.size
                onProgress(completedOps, totalOps)
            }

            // 4. Upload payments with deletion markers preserved
            for (chunk in payments.chunked(300)) {
                val batch = db.batch()
                for (payment in chunk) {
                    val ref = db.collection("businesses").document(businessId)
                        .collection("payments").document(payment.sharedId)
                    batch.set(ref, payment, SetOptions.merge())
                }
                batch.commit().await()
                completedOps += chunk.size
                onProgress(completedOps, totalOps)
            }

            // Update business backup timestamp
            db.collection("businesses").document(businessId).update(
                mapOf(
                    "lastBackupAt" to System.currentTimeMillis(),
                    "lastBackupBy" to user.uid
                )
            ).await()

            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = user.uid,
                actorName = user.displayName ?: "Owner",
                actorRole = UserRole.OWNER,
                action = "BACKUP_COMPLETED",
                details = "Owner completed cloud backup of $totalOps records (with deletion markers)",
                outcome = "SUCCESS"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
