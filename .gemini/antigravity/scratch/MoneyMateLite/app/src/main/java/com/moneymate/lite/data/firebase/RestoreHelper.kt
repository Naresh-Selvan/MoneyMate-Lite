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

            val filesSnapshot = db.collection("files").document(userId)
                .collection("loan_files").get().await()

            for (fileDoc in filesSnapshot.documents) {
                val file = fileDoc.toObject(LoanFile::class.java) ?: continue
                loanFileRepository.insert(file)

                val personsSnapshot = fileDoc.reference
                    .collection("persons").get().await()

                for (personDoc in personsSnapshot.documents) {
                    val person = personDoc.toObject(Person::class.java) ?: continue
                    personRepository.insert(person)

                    val loansSnapshot = personDoc.reference
                        .collection("loans").get().await()

                    for (loanDoc in loansSnapshot.documents) {
                        val loan = loanDoc.toObject(Loan::class.java) ?: continue
                        loanRepository.insertLoan(loan)

                        val paymentsSnapshot = loanDoc.reference
                            .collection("payments").get().await()

                        for (paymentDoc in paymentsSnapshot.documents) {
                            val payment = paymentDoc.toObject(Payment::class.java) ?: continue
                            paymentRepository.insert(payment)
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
