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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadHelper @Inject constructor() {

    suspend fun uploadAll(
        loanFileRepository: LoanFileRepository,
        personRepository: PersonRepository,
        loanRepository: LoanRepository,
        paymentRepository: PaymentRepository
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            val db = FirebaseFirestore.getInstance()

            val files = loanFileRepository.getAllFiles().first()
            for (file in files) {
                if (file.isDeleted) continue
                val fileRef = db.collection("files").document(userId)
                    .collection("loan_files").document(file.id.toString())
                fileRef.set(file)

                val persons = personRepository.getPersonsByFile(file.id).first()
                for (person in persons) {
                    if (person.isDeleted) continue
                    val personRef = fileRef.collection("persons")
                        .document(person.id.toString())
                    personRef.set(person)

                    val loans = loanRepository.getLoansByPerson(person.id).first()
                    for (loan in loans) {
                        if (loan.isDeleted) continue
                        val loanRef = personRef.collection("loans")
                            .document(loan.id.toString())
                        loanRef.set(loan)

                        val payments = paymentRepository.getPaymentsByLoan(loan.id).first()
                        for (payment in payments) {
                            if (payment.isDeleted) continue
                            loanRef.collection("payments")
                                .document(payment.id.toString())
                                .set(payment)
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
