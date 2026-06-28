package com.moneymate.lite.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
    ): Result<String> = withContext(Dispatchers.IO) {
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
            var restoredPersonsCount = 0
            var restoredLoansCount = 0
            var restoredPaymentsCount = 0

            val checkedPaths = mutableListOf<String>()
            val successfulPaths = mutableListOf<String>()
            val errors = mutableListOf<String>()

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
                val pathStr = "${config.root}/${config.docId.take(5)}.../${config.sub}"
                checkedPaths.add(pathStr)
                try {
                    val filesSnapshot = db.collection(config.root).document(config.docId)
                        .collection(config.sub).get().await()

                    if (filesSnapshot.isEmpty) {
                        android.util.Log.d("RestoreHelper", "Collection is empty for config: ${config.root}/${config.docId}/${config.sub}")
                        continue
                    }

                    successfulPaths.add("$pathStr (${filesSnapshot.documents.size} files)")
                    android.util.Log.d("RestoreHelper", "Found ${filesSnapshot.documents.size} files for config: ${config.root}/${config.docId}/${config.sub}")

                    for (fileDoc in filesSnapshot.documents) {
                        val fileId = fileDoc.id.toLongId()
                        val fileName = fileDoc.getString("name") ?: ""
                        android.util.Log.d("RestoreHelper", "Restoring file: docId=${fileDoc.id}, mappedId=$fileId, name=$fileName")
                        
                        try {
                            val file = LoanFile(
                                id = fileId,
                                name = fileName,
                                createdAt = if (safeGetLong(fileDoc, "createdAt") > 0) safeGetLong(fileDoc, "createdAt") else System.currentTimeMillis(),
                                isDeleted = false // Force false to ensure it is visible in the main list
                            )
                            loanFileRepository.insert(file)
                            restoredFilesCount++
                        } catch (e: Exception) {
                            errors.add("File $fileName save failed: ${e.message}")
                            continue
                        }

                        // Try "persons" collection, fallback to "customers"
                        var personsSnapshot = try {
                            var snap = fileDoc.reference.collection("persons").get().await()
                            if (snap.isEmpty) {
                                snap = fileDoc.reference.collection("customers").get().await()
                            }
                            snap
                        } catch (e: Exception) {
                            if (isPermissionDenied(e)) {
                                android.util.Log.w("RestoreHelper", "Permission denied querying customers under file $fileName")
                            } else {
                                errors.add("Query customers under file $fileName failed: ${e.message}")
                            }
                            null
                        }

                        if (personsSnapshot != null && !personsSnapshot.isEmpty) {
                            android.util.Log.d("RestoreHelper", "Found ${personsSnapshot.documents.size} customers for file: $fileName")

                            for (personDoc in personsSnapshot.documents) {
                                val personId = personDoc.id.toLongId()
                                val personFileIdStr = personDoc.getString("fileId") ?: fileDoc.id
                                val personName = personDoc.getString("name") ?: ""
                                val mobile = personDoc.getString("mobileNumber")
                                    ?: personDoc.getString("phone")
                                    ?: personDoc.getString("phoneNumber")
                                val place = personDoc.getString("place")
                                    ?: personDoc.getString("address")

                                android.util.Log.d("RestoreHelper", "Restoring customer: docId=${personDoc.id}, mappedId=$personId, name=$personName")

                                try {
                                    val person = Person(
                                        id = personId,
                                        fileId = personFileIdStr.toLongId(),
                                        name = personName,
                                        mobileNumber = mobile,
                                        place = place,
                                        notes = personDoc.getString("notes"),
                                        sortOrder = safeGetLong(personDoc, "sortOrder").toInt(),
                                        createdAt = if (safeGetLong(personDoc, "createdAt") > 0) safeGetLong(personDoc, "createdAt") else System.currentTimeMillis(),
                                        isDeleted = false // Force false to ensure visible
                                    )
                                    personRepository.insert(person)
                                    restoredPersonsCount++
                                } catch (e: Exception) {
                                    errors.add("Customer $personName save failed: ${e.message}")
                                    continue
                                }

                                val loansSnapshot = try {
                                    personDoc.reference.collection("loans").get().await()
                                } catch (e: Exception) {
                                    if (isPermissionDenied(e)) {
                                        android.util.Log.w("RestoreHelper", "Permission denied querying loans for customer $personName")
                                    } else {
                                        errors.add("Query loans for customer $personName failed: ${e.message}")
                                    }
                                    null
                                }

                                if (loansSnapshot != null && !loansSnapshot.isEmpty) {
                                    // --- NEW SCHEMA ---
                                    android.util.Log.d("RestoreHelper", "Restoring loan (New Schema) for customer: $personName")
                                    for (loanDoc in loansSnapshot.documents) {
                                        val loanId = loanDoc.id.toLongId()
                                        val loanPersonIdStr = loanDoc.getString("personId") ?: personDoc.id
                                        
                                        val totalAmount = safeGetDouble(loanDoc, "totalAmount").takeIf { it > 0 }
                                            ?: safeGetDouble(loanDoc, "amount")
                                            ?: safeGetDouble(loanDoc, "amountGiven")
                                            
                                        val dateGiven = safeGetLong(loanDoc, "dateGiven").takeIf { it > 0 }
                                            ?: safeGetLong(loanDoc, "date")
                                            ?: safeGetLong(loanDoc, "createdAt")
                                            
                                        try {
                                            val loan = Loan(
                                                id = loanId,
                                                personId = loanPersonIdStr.toLongId(),
                                                totalAmount = totalAmount,
                                                dateGiven = if (dateGiven > 0) dateGiven else System.currentTimeMillis(),
                                                isCompleted = safeGetBoolean(loanDoc, "isCompleted"),
                                                completedAt = safeGetTimestampLong(loanDoc, "completedAt"),
                                                createdAt = if (safeGetLong(loanDoc, "createdAt") > 0) safeGetLong(loanDoc, "createdAt") else System.currentTimeMillis(),
                                                isDeleted = false // Force active
                                            )
                                            loanRepository.insertLoan(loan)
                                            restoredLoansCount++
                                        } catch (e: Exception) {
                                            errors.add("Loan for customer $personName save failed: ${e.message}")
                                            continue
                                        }

                                        // Try "payments" collection, fallback to "transactions"
                                        var paymentsSnapshot = try {
                                            var snap = loanDoc.reference.collection("payments").get().await()
                                            if (snap.isEmpty) {
                                                snap = loanDoc.reference.collection("transactions").get().await()
                                            }
                                            snap
                                        } catch (e: Exception) {
                                            if (isPermissionDenied(e)) {
                                                android.util.Log.w("RestoreHelper", "Permission denied querying payments for loan $loanId")
                                            } else {
                                                errors.add("Query payments for loan $loanId failed: ${e.message}")
                                            }
                                            null
                                        }

                                        if (paymentsSnapshot != null) {
                                            for (paymentDoc in paymentsSnapshot.documents) {
                                                val paymentId = paymentDoc.id.toLongId()
                                                val pAmount = safeGetDouble(paymentDoc, "amount").takeIf { it > 0 }
                                                    ?: safeGetDouble(paymentDoc, "value")
                                                    ?: safeGetDouble(paymentDoc, "amountPaid")
                                                    
                                                val pDate = safeGetLong(paymentDoc, "date").takeIf { it > 0 }
                                                    ?: safeGetLong(paymentDoc, "timestamp")
                                                    ?: safeGetLong(paymentDoc, "createdAt")

                                                try {
                                                    val payment = Payment(
                                                        id = paymentId,
                                                        loanId = loanId,
                                                        amount = pAmount,
                                                        date = if (pDate > 0) pDate else System.currentTimeMillis(),
                                                        createdAt = if (safeGetLong(paymentDoc, "createdAt") > 0) safeGetLong(paymentDoc, "createdAt") else System.currentTimeMillis(),
                                                        isDeleted = false
                                                    )
                                                    paymentRepository.insert(payment)
                                                    restoredPaymentsCount++
                                                } catch (e: Exception) {
                                                    errors.add("Payment save failed: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // --- LEGACY SCHEMA ---
                                    android.util.Log.d("RestoreHelper", "Restoring loan (Legacy Schema) for customer: $personName")
                                    val amountGiven = safeGetDouble(personDoc, "amountGiven").takeIf { it > 0 }
                                        ?: safeGetDouble(personDoc, "totalAmount")
                                        ?: safeGetDouble(personDoc, "amount")
                                        
                                    val dateGiven = safeGetLong(personDoc, "dateGiven").takeIf { it > 0 }
                                        ?: safeGetLong(personDoc, "createdAt")
                                        
                                    val isCompleted = safeGetBoolean(personDoc, "isCompleted")
                                    val completedAt = safeGetTimestampLong(personDoc, "completedAt")
                                    
                                    val virtualLoanId = personId xor 0x5A5A5A5A5A5A5A5AL
                                    try {
                                        val virtualLoan = Loan(
                                            id = virtualLoanId,
                                            personId = personId,
                                            totalAmount = amountGiven,
                                            dateGiven = if (dateGiven > 0) dateGiven else System.currentTimeMillis(),
                                            isCompleted = isCompleted,
                                            completedAt = completedAt,
                                            createdAt = if (dateGiven > 0) dateGiven else System.currentTimeMillis(),
                                            isDeleted = false
                                        )
                                        loanRepository.insertLoan(virtualLoan)
                                        restoredLoansCount++
                                    } catch (e: Exception) {
                                        errors.add("Legacy loan for customer $personName save failed: ${e.message}")
                                        continue
                                    }

                                    // Try "payments" collection, fallback to "transactions"
                                    var paymentsSnapshot = try {
                                        var snap = personDoc.reference.collection("payments").get().await()
                                        if (snap.isEmpty) {
                                            snap = personDoc.reference.collection("transactions").get().await()
                                        }
                                        snap
                                    } catch (e: Exception) {
                                        if (isPermissionDenied(e)) {
                                            android.util.Log.w("RestoreHelper", "Permission denied querying legacy payments for customer $personName")
                                        } else {
                                            errors.add("Query legacy payments for customer $personName failed: ${e.message}")
                                        }
                                        null
                                    }

                                    if (paymentsSnapshot != null) {
                                        for (paymentDoc in paymentsSnapshot.documents) {
                                            val paymentId = paymentDoc.id.toLongId()
                                            val pAmount = safeGetDouble(paymentDoc, "amount").takeIf { it > 0 }
                                                ?: safeGetDouble(paymentDoc, "value")
                                                ?: safeGetDouble(paymentDoc, "amountPaid")
                                                
                                            val pDate = safeGetLong(paymentDoc, "date").takeIf { it > 0 }
                                                ?: safeGetLong(paymentDoc, "timestamp")
                                                ?: safeGetLong(paymentDoc, "createdAt")

                                            try {
                                                val payment = Payment(
                                                    id = paymentId,
                                                    loanId = virtualLoanId,
                                                    amount = pAmount,
                                                    date = if (pDate > 0) pDate else System.currentTimeMillis(),
                                                    createdAt = if (safeGetLong(paymentDoc, "createdAt") > 0) safeGetLong(paymentDoc, "createdAt") else System.currentTimeMillis(),
                                                    isDeleted = false
                                                )
                                                paymentRepository.insert(payment)
                                                restoredPaymentsCount++
                                            } catch (e: Exception) {
                                                errors.add("Legacy payment save failed: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isPermissionDenied(e)) {
                        android.util.Log.w("RestoreHelper", "Permission denied for config path ${config.root}/${config.docId}/${config.sub}")
                    } else {
                        errors.add("Query path failed for $pathStr: ${e.message}")
                        android.util.Log.e("RestoreHelper", "Error restoring config path ${config.root}/${config.docId}/${config.sub}", e)
                    }
                }
            }

            val errorSummary = if (errors.isEmpty()) "" else " Errors/warnings: ${errors.distinct().take(3).joinToString("; ")}"
            val pathSummary = if (successfulPaths.isEmpty()) {
                "Checked: ${checkedPaths.distinct().joinToString(", ")}"
            } else {
                "Paths: ${successfulPaths.distinct().joinToString(", ")}"
            }
            val summary = "Files: $restoredFilesCount, Cust: $restoredPersonsCount, Loans: $restoredLoansCount, Pay: $restoredPaymentsCount. $pathSummary.$errorSummary"

            if (restoredFilesCount == 0) {
                Result.failure(Exception("No backups found. $summary"))
            } else {
                Result.success(summary)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isPermissionDenied(e: Exception): Boolean {
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return true
        }
        return e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
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
