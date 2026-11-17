package com.moneymate.lite.data.firebase

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.moneymate.lite.data.AppDatabase
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.model.UserRole
import com.moneymate.lite.data.repository.ActivityLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreHelper @Inject constructor(
    private val database: AppDatabase,
    private val loanFileDao: LoanFileDao,
    private val personDao: PersonDao,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao,
    private val activityLogRepository: ActivityLogRepository
) {
    suspend fun restoreAll(
        businessId: String,
        userRole: UserRole
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Privilege check: Owner only!
            if (userRole != UserRole.OWNER) {
                return@withContext Result.failure(SecurityException("Only business owner has permission to perform restore."))
            }

            val user = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("User not authenticated")
            val db = FirebaseFirestore.getInstance()

            val userDoc = db.collection("users").document(user.uid).get().await()
            if (userDoc.getString("role") != "OWNER") {
                return@withContext Result.failure(SecurityException("Server validation failed: Account does not have owner privileges."))
            }

            var restoredFiles = 0
            var restoredPersons = 0
            var restoredLoans = 0
            var restoredPayments = 0

            // 1. First check shared business collections
            val bizRef = db.collection("businesses").document(businessId)
            val filesSnap = bizRef.collection("files").get().await()

            if (!filesSnap.isEmpty) {
                database.withTransaction {
                    for (doc in filesSnap.documents) {
                        val file = LoanFile(
                            sharedId = doc.getString("sharedId") ?: doc.id,
                            businessId = businessId,
                            name = doc.getString("name") ?: "",
                            sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis(),
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )
                        loanFileDao.insert(file)
                        restoredFiles++
                    }

                    // Persons
                    val personsSnap = bizRef.collection("persons").get().await()
                    for (doc in personsSnap.documents) {
                        val p = Person(
                            sharedId = doc.getString("sharedId") ?: doc.id,
                            businessId = businessId,
                            fileId = doc.getLong("fileId") ?: 0L,
                            fileSharedId = doc.getString("fileSharedId") ?: "",
                            name = doc.getString("name") ?: "",
                            mobileNumber = doc.getString("mobileNumber"),
                            place = doc.getString("place"),
                            notes = doc.getString("notes"),
                            sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis(),
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )
                        personDao.insert(p)
                        restoredPersons++
                    }

                    // Loans
                    val loansSnap = bizRef.collection("loans").get().await()
                    for (doc in loansSnap.documents) {
                        val l = Loan(
                            sharedId = doc.getString("sharedId") ?: doc.id,
                            businessId = businessId,
                            personId = doc.getLong("personId") ?: 0L,
                            personSharedId = doc.getString("personSharedId") ?: "",
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            dateGiven = doc.getLong("dateGiven") ?: System.currentTimeMillis(),
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            completedAt = doc.getLong("completedAt"),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis(),
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )
                        loanDao.insert(l)
                        restoredLoans++
                    }

                    // Payments
                    val paymentsSnap = bizRef.collection("payments").get().await()
                    for (doc in paymentsSnap.documents) {
                        val pay = Payment(
                            sharedId = doc.getString("sharedId") ?: doc.id,
                            businessId = businessId,
                            loanId = doc.getLong("loanId") ?: 0L,
                            loanSharedId = doc.getString("loanSharedId") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            date = doc.getLong("date") ?: System.currentTimeMillis(),
                            recordedByWorkerId = doc.getString("recordedByWorkerId"),
                            recordedByWorkerName = doc.getString("recordedByWorkerName"),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis(),
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )
                        paymentDao.insert(pay)
                        restoredPayments++
                    }
                }

                activityLogRepository.logActivity(
                    businessId = businessId,
                    actorId = user.uid,
                    actorName = user.displayName ?: "Owner",
                    actorRole = UserRole.OWNER,
                    action = "RESTORE_COMPLETED",
                    details = "Restored from business cloud: $restoredFiles files, $restoredPersons customers, $restoredLoans loans, $restoredPayments payments",
                    outcome = "SUCCESS"
                )

                return@withContext Result.success("Restored $restoredFiles files, $restoredPersons customers, $restoredLoans loans, $restoredPayments payments.")
            }

            // Fallback: Check legacy files/{uid}/loan_files
            val legacyFilesSnap = db.collection("files").document(user.uid)
                .collection("loan_files").get().await()

            if (!legacyFilesSnap.isEmpty) {
                database.withTransaction {
                    for (fileDoc in legacyFilesSnap.documents) {
                        val file = LoanFile(
                            sharedId = "file_${fileDoc.id}",
                            businessId = businessId,
                            name = fileDoc.getString("name") ?: "",
                            sortOrder = 0,
                            createdAt = fileDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                            isDeleted = fileDoc.getBoolean("isDeleted") ?: false
                        )
                        val fileId = loanFileDao.insert(file)
                        restoredFiles++

                        val personsSnap = fileDoc.reference.collection("persons").get().await()
                        for (pDoc in personsSnap.documents) {
                            val person = Person(
                                sharedId = "person_${pDoc.id}",
                                businessId = businessId,
                                fileId = fileId,
                                fileSharedId = file.sharedId,
                                name = pDoc.getString("name") ?: "",
                                mobileNumber = pDoc.getString("mobileNumber"),
                                place = pDoc.getString("place"),
                                notes = pDoc.getString("notes"),
                                sortOrder = pDoc.getLong("sortOrder")?.toInt() ?: 0,
                                createdAt = pDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                                isDeleted = pDoc.getBoolean("isDeleted") ?: false
                            )
                            val personId = personDao.insert(person)
                            restoredPersons++

                            val loansSnap = pDoc.reference.collection("loans").get().await()
                            for (lDoc in loansSnap.documents) {
                                val loan = Loan(
                                    sharedId = "loan_${lDoc.id}",
                                    businessId = businessId,
                                    personId = personId,
                                    personSharedId = person.sharedId,
                                    totalAmount = lDoc.getDouble("totalAmount") ?: 0.0,
                                    dateGiven = lDoc.getLong("dateGiven") ?: System.currentTimeMillis(),
                                    isCompleted = lDoc.getBoolean("isCompleted") ?: false,
                                    completedAt = lDoc.getLong("completedAt"),
                                    createdAt = lDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    isDeleted = lDoc.getBoolean("isDeleted") ?: false
                                )
                                val loanId = loanDao.insert(loan)
                                restoredLoans++

                                val paymentsSnap = lDoc.reference.collection("payments").get().await()
                                for (payDoc in paymentsSnap.documents) {
                                    val payment = Payment(
                                        sharedId = "payment_${payDoc.id}",
                                        businessId = businessId,
                                        loanId = loanId,
                                        loanSharedId = loan.sharedId,
                                        amount = payDoc.getDouble("amount") ?: 0.0,
                                        date = payDoc.getLong("date") ?: System.currentTimeMillis(),
                                        createdAt = payDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        isDeleted = payDoc.getBoolean("isDeleted") ?: false
                                    )
                                    paymentDao.insert(payment)
                                    restoredPayments++
                                }
                            }
                        }
                    }
                }

                activityLogRepository.logActivity(
                    businessId = businessId,
                    actorId = user.uid,
                    actorName = user.displayName ?: "Owner",
                    actorRole = UserRole.OWNER,
                    action = "RESTORE_COMPLETED",
                    details = "Restored from legacy cloud backup: $restoredFiles files, $restoredPersons customers, $restoredLoans loans, $restoredPayments payments",
                    outcome = "SUCCESS"
                )

                return@withContext Result.success("Restored $restoredFiles files, $restoredPersons customers, $restoredLoans loans, $restoredPayments payments from backup.")
            }

            Result.failure(Exception("No backup found in cloud for this account."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
