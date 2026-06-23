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

            for (root in rootCollections) {
                for (targetUid in uidsToTry) {
                    for (sub in subCollections) {
                        try {
                            val filesSnapshot = db.collection(root).document(targetUid)
                                .collection(sub).get().await()

                            if (filesSnapshot.isEmpty) continue

                            for (fileDoc in filesSnapshot.documents) {
                                val file = LoanFile(
                                    id = safeGetLong(fileDoc, "id"),
                                    name = fileDoc.getString("name") ?: "",
                                    createdAt = safeGetLong(fileDoc, "createdAt"),
                                    isDeleted = safeGetBoolean(fileDoc, "isDeleted")
                                )
                                loanFileRepository.insert(file)
                                restoredFilesCount++

                                val personsSnapshot = fileDoc.reference
                                    .collection("persons").get().await()

                                for (personDoc in personsSnapshot.documents) {
                                    val person = Person(
                                        id = safeGetLong(personDoc, "id"),
                                        fileId = safeGetLong(personDoc, "fileId"),
                                        name = personDoc.getString("name") ?: "",
                                        mobileNumber = personDoc.getString("mobileNumber"),
                                        place = personDoc.getString("place"),
                                        notes = personDoc.getString("notes"),
                                        sortOrder = safeGetLong(personDoc, "sortOrder").toInt(),
                                        createdAt = safeGetLong(personDoc, "createdAt"),
                                        isDeleted = safeGetBoolean(personDoc, "isDeleted")
                                    )
                                    personRepository.insert(person)

                                    val loansSnapshot = personDoc.reference
                                        .collection("loans").get().await()

                                    for (loanDoc in loansSnapshot.documents) {
                                        val loan = Loan(
                                            id = safeGetLong(loanDoc, "id"),
                                            personId = safeGetLong(loanDoc, "personId"),
                                            totalAmount = safeGetDouble(loanDoc, "totalAmount"),
                                            dateGiven = safeGetLong(loanDoc, "dateGiven"),
                                            isCompleted = safeGetBoolean(loanDoc, "isCompleted"),
                                            completedAt = safeGetTimestampLong(loanDoc, "completedAt"),
                                            createdAt = safeGetLong(loanDoc, "createdAt"),
                                            isDeleted = safeGetBoolean(loanDoc, "isDeleted")
                                        )
                                        loanRepository.insertLoan(loan)

                                        val paymentsSnapshot = loanDoc.reference
                                            .collection("payments").get().await()

                                        for (paymentDoc in paymentsSnapshot.documents) {
                                            val payment = Payment(
                                                id = safeGetLong(paymentDoc, "id"),
                                                loanId = safeGetLong(paymentDoc, "loanId"),
                                                amount = safeGetDouble(paymentDoc, "amount"),
                                                date = safeGetLong(paymentDoc, "date"),
                                                createdAt = safeGetLong(paymentDoc, "createdAt"),
                                                isDeleted = safeGetBoolean(paymentDoc, "isDeleted")
                                            )
                                            paymentRepository.insert(payment)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("RestoreHelper", "Failed querying path: $root/$targetUid/$sub: ${e.message}")
                        }
                    }
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

    private fun safeGetLong(doc: DocumentSnapshot, field: String): Long {
        return when (val value = doc.get(field)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
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
