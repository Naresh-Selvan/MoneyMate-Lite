package com.moneymate.lite.data.migration

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class MigrationPreview(
    val fileCount: Int,
    val customerCount: Int,
    val loanCount: Int,
    val paymentCount: Int,
    val totalLoanAmount: Double,
    val totalPaidAmount: Double,
    val totalOutstandingBalance: Double,
    val isAlreadyMigrated: Boolean
)

@Singleton
class BusinessMigrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val loanFileDao: LoanFileDao,
    private val personDao: PersonDao,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao,
    private val activityLogRepository: ActivityLogRepository
) {
    suspend fun generatePreview(businessId: String): MigrationPreview = withContext(Dispatchers.IO) {
        val files = loanFileDao.getAllFilesIncludingDeleted()
        val customers = personDao.getAllPersonsIncludingDeleted()
        val loans = loanDao.getAllLoansIncludingDeleted()
        val payments = paymentDao.getAllPaymentsIncludingDeleted()

        val activeLoans = loans.filter { !it.isDeleted }
        val activePayments = payments.filter { !it.isDeleted }

        val totalLoan = activeLoans.sumOf { it.totalAmount }
        val totalPaid = activePayments.sumOf { it.amount }
        val totalBalance = paymentDao.getTotalOutstandingBalance()

        val alreadyMigrated = files.isNotEmpty() && files.all { it.businessId == businessId && it.sharedId.isNotBlank() }

        MigrationPreview(
            fileCount = files.size,
            customerCount = customers.size,
            loanCount = loans.size,
            paymentCount = payments.size,
            totalLoanAmount = totalLoan,
            totalPaidAmount = totalPaid,
            totalOutstandingBalance = totalBalance,
            isAlreadyMigrated = alreadyMigrated
        )
    }

    suspend fun createBackupSnapshot(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val files = loanFileDao.getAllFilesIncludingDeleted()
            val customers = personDao.getAllPersonsIncludingDeleted()
            val loans = loanDao.getAllLoansIncludingDeleted()
            val payments = paymentDao.getAllPaymentsIncludingDeleted()

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("createdAt", System.currentTimeMillis())
                put("fileCount", files.size)
                put("customerCount", customers.size)
                put("loanCount", loans.size)
                put("paymentCount", payments.size)
            }

            val snapshotFile = File(context.filesDir, "moneymate_migration_backup_${System.currentTimeMillis()}.json")
            snapshotFile.writeText(rootJson.toString(2))
            Result.success(snapshotFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeMigration(
        businessId: String,
        actorId: String,
        actorName: String,
        onProgress: (step: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Result<MigrationPreview> = withContext(Dispatchers.IO) {
        try {
            // 1. Create safety snapshot first
            createBackupSnapshot()

            onProgress("Analyzing local data...", 0, 100)
            val previewBefore = generatePreview(businessId)

            val fileMap = mutableMapOf<Long, LoanFile>()
            val personMap = mutableMapOf<Long, Person>()
            val loanMap = mutableMapOf<Long, Loan>()

            // 2. Database update inside transaction
            database.withTransaction {
                val files = loanFileDao.getAllFilesIncludingDeleted()
                for (file in files) {
                    val sharedId = file.sharedId.ifBlank { "file_${file.id}_${UUID.randomUUID()}" }
                    val updated = file.copy(
                        sharedId = sharedId,
                        businessId = businessId,
                        lastModified = System.currentTimeMillis()
                    )
                    loanFileDao.update(updated)
                    fileMap[file.id] = updated
                }

                val persons = personDao.getAllPersonsIncludingDeleted()
                for (p in persons) {
                    val sharedId = p.sharedId.ifBlank { "person_${p.id}_${UUID.randomUUID()}" }
                    val fileSharedId = fileMap[p.fileId]?.sharedId ?: p.fileSharedId.ifBlank { "file_${p.fileId}" }
                    val updated = p.copy(
                        sharedId = sharedId,
                        businessId = businessId,
                        fileSharedId = fileSharedId,
                        lastModified = System.currentTimeMillis()
                    )
                    personDao.update(updated)
                    personMap[p.id] = updated
                }

                val loans = loanDao.getAllLoansIncludingDeleted()
                for (l in loans) {
                    val sharedId = l.sharedId.ifBlank { "loan_${l.id}_${UUID.randomUUID()}" }
                    val personSharedId = personMap[l.personId]?.sharedId ?: l.personSharedId.ifBlank { "person_${l.personId}" }
                    val updated = l.copy(
                        sharedId = sharedId,
                        businessId = businessId,
                        personSharedId = personSharedId,
                        lastModified = System.currentTimeMillis()
                    )
                    loanDao.update(updated)
                    loanMap[l.id] = updated
                }

                val payments = paymentDao.getAllPaymentsIncludingDeleted()
                for (pay in payments) {
                    val sharedId = pay.sharedId.ifBlank { "payment_${pay.id}_${UUID.randomUUID()}" }
                    val loanSharedId = loanMap[pay.loanId]?.sharedId ?: pay.loanSharedId.ifBlank { "loan_${pay.loanId}" }
                    val updated = pay.copy(
                        sharedId = sharedId,
                        businessId = businessId,
                        loanSharedId = loanSharedId,
                        lastModified = System.currentTimeMillis()
                    )
                    paymentDao.update(updated)
                }
            }

            // 3. Financial total verification
            val previewAfter = generatePreview(businessId)
            if (kotlin.math.abs(previewBefore.totalOutstandingBalance - previewAfter.totalOutstandingBalance) > 0.01) {
                throw IllegalStateException("Financial integrity check failed during migration: balances do not match!")
            }

            // 4. Upload to Firestore under businesses/{businessId}
            if (FirebaseAuth.getInstance().currentUser != null && businessId.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()

                // Ensure business document exists first
                db.collection("businesses").document(businessId).set(
                    mapOf(
                        "businessId" to businessId,
                        "ownerUid" to actorId,
                        "migrationStatus" to "IN_PROGRESS",
                        "lastModified" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()

                val files = loanFileDao.getAllFilesIncludingDeleted()
                val persons = personDao.getAllPersonsIncludingDeleted()
                val loans = loanDao.getAllLoansIncludingDeleted()
                val payments = paymentDao.getAllPaymentsIncludingDeleted()

                val totalOps = files.size + persons.size + loans.size + payments.size
                var completedOps = 0

                // Write files in batches
                for (chunk in files.chunked(300)) {
                    val batch = db.batch()
                    for (f in chunk) {
                        val ref = db.collection("businesses").document(businessId)
                            .collection("files").document(f.sharedId)
                        batch.set(ref, f, SetOptions.merge())
                    }
                    batch.commit().await()
                    completedOps += chunk.size
                    onProgress("Syncing files to cloud...", completedOps, totalOps)
                }

                // Write persons in batches
                for (chunk in persons.chunked(300)) {
                    val batch = db.batch()
                    for (p in chunk) {
                        val ref = db.collection("businesses").document(businessId)
                            .collection("persons").document(p.sharedId)
                        batch.set(ref, p, SetOptions.merge())
                    }
                    batch.commit().await()
                    completedOps += chunk.size
                    onProgress("Syncing customers to cloud...", completedOps, totalOps)
                }

                // Write loans in batches
                for (chunk in loans.chunked(300)) {
                    val batch = db.batch()
                    for (l in chunk) {
                        val ref = db.collection("businesses").document(businessId)
                            .collection("loans").document(l.sharedId)
                        batch.set(ref, l, SetOptions.merge())
                    }
                    batch.commit().await()
                    completedOps += chunk.size
                    onProgress("Syncing loans to cloud...", completedOps, totalOps)
                }

                // Write payments in batches
                for (chunk in payments.chunked(300)) {
                    val batch = db.batch()
                    for (pay in chunk) {
                        val ref = db.collection("businesses").document(businessId)
                            .collection("payments").document(pay.sharedId)
                        batch.set(ref, pay, SetOptions.merge())
                    }
                    batch.commit().await()
                    completedOps += chunk.size
                    onProgress("Syncing payments to cloud...", completedOps, totalOps)
                }

                db.collection("businesses").document(businessId).set(
                    mapOf(
                        "migrationStatus" to "COMPLETED",
                        "migratedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // 5. Activity log
            activityLogRepository.logActivity(
                businessId = businessId,
                actorId = actorId,
                actorName = actorName,
                actorRole = UserRole.OWNER,
                action = "BUSINESS_MIGRATED",
                details = "Migrated ${previewAfter.fileCount} files, ${previewAfter.customerCount} customers, ${previewAfter.loanCount} loans, ${previewAfter.paymentCount} payments. Total balance: ₹${previewAfter.totalOutstandingBalance}",
                outcome = "SUCCESS"
            )

            Result.success(previewAfter)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
