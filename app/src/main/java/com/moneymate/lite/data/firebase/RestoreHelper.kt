package com.moneymate.lite.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.repository.LoanFileRepository
import com.moneymate.lite.data.repository.LoanRepository
import com.moneymate.lite.data.repository.PaymentRepository
import com.moneymate.lite.data.repository.PersonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreHelper @Inject constructor() {

    suspend fun restoreAll(
        loanFileRepository: LoanFileRepository,
        personRepository: PersonRepository,
        loanRepository: LoanRepository,
        paymentRepository: PaymentRepository
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            val db = FirebaseFirestore.getInstance()

            val uidsToTry = mutableListOf(userId)
            if (userId.startsWith("O", ignoreCase = true)) {
                val alt = "0" + userId.substring(1)
                uidsToTry.add(alt)
            } else if (userId.startsWith("0")) {
                val alt = "O" + userId.substring(1)
                uidsToTry.add(alt)
                val altLower = "o" + userId.substring(1)
                uidsToTry.add(altLower)
            }

            val rootCollections = listOf("files", "users")
            val subCollections = listOf("loan_files", "files")
            
            var restoredFilesCount = 0

            data class QueryConfig(val root: String, val docId: String, val sub: String)
            val queryConfigs = mutableListOf<QueryConfig>()

            // User-specific paths
            for (root in rootCollections) {
                for (targetUid in uidsToTry) {
                    for (sub in subCollections) {
                        queryConfigs.add(QueryConfig(root, targetUid, sub))
                    }
                }
            }

            // Legacy path (shared under boss_data)
            queryConfigs.add(QueryConfig("boss_data", "files", "loan_files"))

            for (config in queryConfigs) {
                try {
                    val filesSnapshot = db.collection(config.root).document(config.docId)
                        .collection(config.sub).get().await()

                    if (filesSnapshot.isEmpty) continue

                    for (fileDoc in filesSnapshot.documents) {
                        val fileId = fileDoc.id.toLongId()
                        val file = LoanFile(
                            id = fileId,
                            name = fileDoc.getString("name") ?: "",
                            createdAt = if (safeGetLong(fileDoc, "createdAt") > 0) safeGetLong(fileDoc, "createdAt") else System.currentTimeMillis(),
                            isDeleted = safeGetBoolean(fileDoc, "isDeleted")
                        )
                        loanFileRepository.insert(file)
                        restoredFilesCount++

                        val personsSnapshot = fileDoc.reference
                            .collection("persons").get().await()

                        for (personDoc in personsSnapshot.documents) {
                            val personId = personDoc.id.toLongId()
                            val personFileIdStr = personDoc.getString("fileId") ?: fileDoc.id
                            val person = Person(
                                id = personId,
                                fileId = personFileIdStr.toLongId(),
                                name = personDoc.getString("name") ?: "",
                                mobileNumber = personDoc.getString("mobileNumber"),
                                place = personDoc.getString("place"),
                                notes = personDoc.getString("notes"),
                                sortOrder = safeGetLong(personDoc, "sortOrder").toInt(),
                                createdAt = if (safeGetLong(personDoc, "createdAt") > 0) safeGetLong(personDoc, "createdAt") else System.currentTimeMillis(),
                                isDeleted = safeGetBoolean(personDoc, "isDeleted")
                            )
                            personRepository.insert(person)

                            val loansSnapshot = personDoc.reference
                                .collection("loans").get().await()

                            if (!loansSnapshot.isEmpty) {
                                // --- NEW SCHEMA ---
                                for (loanDoc in loansSnapshot.documents) {
                                    val loanId = loanDoc.id.toLongId()
                                    val loanPersonIdStr = loanDoc.getString("personId") ?: personDoc.id
                                    val loan = Loan(
                                        id = loanId,
                                        personId = loanPersonIdStr.toLongId(),
                                        totalAmount = safeGetDouble(loanDoc, "totalAmount"),
                                        dateGiven = safeGetLong(loanDoc, "dateGiven"),
                                        isCompleted = safeGetBoolean(loanDoc, "isCompleted"),
                                        completedAt = safeGetTimestampLong(loanDoc, "completedAt"),
                                        createdAt = if (safeGetLong(loanDoc, "createdAt") > 0) safeGetLong(loanDoc, "createdAt") else System.currentTimeMillis(),
                                        isDeleted = safeGetBoolean(loanDoc, "isDeleted")
                                    )
                                    loanRepository.insertLoan(loan)

                                    val paymentsSnapshot = loanDoc.reference
                                        .collection("payments").get().await()

                                    for (paymentDoc in paymentsSnapshot.documents) {
                                        val paymentId = paymentDoc.id.toLongId()
                                        val payment = Payment(
                                            id = paymentId,
                                            loanId = loanId,
                                            amount = safeGetDouble(paymentDoc, "amount"),
                                            date = safeGetLong(paymentDoc, "date"),
                                            createdAt = if (safeGetLong(paymentDoc, "createdAt") > 0) safeGetLong(paymentDoc, "createdAt") else System.currentTimeMillis(),
                                            isDeleted = safeGetBoolean(paymentDoc, "isDeleted")
                                        )
                                        paymentRepository.insert(payment)
                                    }
                                }
                            } else {
                                // --- LEGACY SCHEMA ---
                                val amountGiven = safeGetDouble(personDoc, "amountGiven")
                                val dateGiven = safeGetLong(personDoc, "dateGiven")
                                val isCompleted = safeGetBoolean(personDoc, "isCompleted")
                                val completedAt = safeGetTimestampLong(personDoc, "completedAt")
                                
                                val virtualLoanId = personId xor 0x5A5A5A5A5A5A5A5AL
                                val virtualLoan = Loan(
                                    id = virtualLoanId,
                                    personId = personId,
                                    totalAmount = amountGiven,
                                    dateGiven = if (dateGiven > 0) dateGiven else (safeGetLong(personDoc, "createdAt").takeIf { it > 0 } ?: System.currentTimeMillis()),
                                    isCompleted = isCompleted,
                                    completedAt = completedAt,
                                    createdAt = if (dateGiven > 0) dateGiven else (safeGetLong(personDoc, "createdAt").takeIf { it > 0 } ?: System.currentTimeMillis()),
                                    isDeleted = safeGetBoolean(personDoc, "isDeleted")
                                )
                                loanRepository.insertLoan(virtualLoan)

                                val paymentsSnapshot = personDoc.reference
                                    .collection("payments").get().await()

                                for (paymentDoc in paymentsSnapshot.documents) {
                                    val paymentId = paymentDoc.id.toLongId()
                                    val payment = Payment(
                                        id = paymentId,
                                        loanId = virtualLoanId,
                                        amount = safeGetDouble(paymentDoc, "amount"),
                                        date = safeGetLong(paymentDoc, "date"),
                                        createdAt = if (safeGetLong(paymentDoc, "createdAt") > 0) safeGetLong(paymentDoc, "createdAt") else System.currentTimeMillis(),
                                        isDeleted = safeGetBoolean(paymentDoc, "isDeleted")
                                    )
                                    paymentRepository.insert(payment)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RestoreHelper", "Failed querying path: ${config.root}/${config.docId}/${config.sub}: ${e.message}")
                }
            }

            if (restoredFilesCount == 0) {
                Result.failure(Exception("No backup files found in the cloud for this account. Please verify you are logged into the correct Google Account."))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String.toLongId(): Long {
        this.toLongOrNull()?.let { return it }
        try {
            val uuid = java.util.UUID.fromString(this)
            return uuid.mostSignificantBits
        } catch (e: Exception) {
            var hash = -3750763034362895579L
            for (i in 0 until this.length) {
                hash = hash xor this[i].code.toLong()
                hash *= 1099511628211L
            }
            return hash
        }
    }

    private fun safeGetLong(doc: DocumentSnapshot, field: String): Long {
        return when (val value = doc.get(field)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: value.toLongId()
            else -> 0L
        }
    }

    private fun safeGetDouble(doc: DocumentSnapshot, field: String): Double {
        return when (val value = doc.get(field)) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun safeGetBoolean(doc: DocumentSnapshot, field: String): Boolean {
        return when (val value = doc.get(field)) {
            is Boolean -> value
            is String -> value.toBoolean()
            is Number -> value.toInt() == 1
            else -> false
        }
    }

    private fun safeGetTimestampLong(doc: DocumentSnapshot, field: String): Long? {
        return when (val value = doc.get(field)) {
            is Number -> value.toLong()
            is com.google.firebase.Timestamp -> value.toDate().time
            is String -> value.toLongOrNull()
            else -> null
        }
    }
}
