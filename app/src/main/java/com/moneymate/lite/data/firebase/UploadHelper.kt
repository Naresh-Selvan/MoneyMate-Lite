package com.moneymate.lite.data.firebase

import com.google.firebase.auth.FirebaseAuth
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadHelper @Inject constructor() {

    suspend fun uploadAll(
        loanFileRepository: LoanFileRepository,
        personRepository: PersonRepository,
        loanRepository: LoanRepository,
        paymentRepository: PaymentRepository,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            val db = FirebaseFirestore.getInstance()

            val writeOps = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Any>>()

            val files = loanFileRepository.getAllFiles().first()
            for (file in files) {
                val fileRef = db.collection("files").document(userId)
                    .collection("loan_files").document(file.id.toString())
                writeOps.add(fileRef to file)

                val persons = personRepository.getPersonsByFile(file.id).first()
                for (person in persons) {
                    val personRef = fileRef.collection("persons")
                        .document(person.id.toString())
                    writeOps.add(personRef to person)

                    val loans = loanRepository.getLoansByPerson(person.id).first()
                    for (loan in loans) {
                        val loanRef = personRef.collection("loans")
                            .document(loan.id.toString())
                        writeOps.add(loanRef to loan)

                        val payments = paymentRepository.getPaymentsByLoan(loan.id).first()
                        for (payment in payments) {
                            val paymentRef = loanRef.collection("payments")
                                .document(payment.id.toString())
                            writeOps.add(paymentRef to payment)
                        }
                    }
                }
            }

            val totalOps = writeOps.size
            if (totalOps == 0) {
                return@withContext Result.success(Unit)
            }

            val batches = writeOps.chunked(450)
            var completedOps = 0

            onProgress(0, totalOps)

            for (batchOps in batches) {
                val batch = db.batch()
                for ((ref, data) in batchOps) {
                    batch.set(ref, data)
                }
                batch.commit().await()
                completedOps += batchOps.size
                onProgress(completedOps, totalOps)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
