package com.moneymate.lite.data.repository

import androidx.room.Transaction
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanWithBalance
import com.moneymate.lite.data.dao.LoanWithBalanceAndFile
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao
) {
    suspend fun insertLoan(loan: Loan): Long = withContext(Dispatchers.IO) {
        loanDao.insert(loan)
    }

    suspend fun createLoan(personId: Long, totalAmount: Double, dateGiven: Long): Long = withContext(Dispatchers.IO) {
        val activeLoan = loanDao.getActiveLoanByPerson(personId)
        if (activeLoan != null) {
            throw IllegalStateException("Person already has an active loan")
        }
        loanDao.insert(
            Loan(
                personId = personId,
                totalAmount = totalAmount,
                dateGiven = dateGiven
            )
        )
    }

    @Transaction
    suspend fun recordPayment(loanId: Long, amount: Double, date: Long) = withContext(Dispatchers.IO) {
        paymentDao.insert(
            Payment(
                loanId = loanId,
                amount = amount,
                date = date
            )
        )
        val loan = loanDao.getLoanById(loanId)
        if (loan != null) {
            val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
            if (totalPaid >= loan.totalAmount) {
                loanDao.markCompleted(loanId, System.currentTimeMillis())
            }
        }
    }

    fun getLoansByPerson(personId: Long): Flow<List<Loan>> = loanDao.getLoansByPerson(personId)

    suspend fun getActiveLoanByPerson(personId: Long): Loan? = withContext(Dispatchers.IO) {
        loanDao.getActiveLoanByPerson(personId)
    }

    fun getActiveLoanByPersonFlow(personId: Long): Flow<Loan?> = loanDao.getActiveLoanByPersonFlow(personId)

    fun getAllActiveLoansInFile(fileId: Long): Flow<List<LoanWithBalance>> = loanDao.getAllActiveLoansInFile(fileId)

    fun getAllActiveLoansAcrossFiles(): Flow<List<LoanWithBalanceAndFile>> = loanDao.getAllActiveLoansAcrossFiles()

    fun getPaymentsByLoan(loanId: Long): Flow<List<Payment>> = paymentDao.getPaymentsByLoan(loanId)

    suspend fun getTodayCollectionTotal(todayStart: Long, todayEnd: Long): Double = withContext(Dispatchers.IO) {
        paymentDao.getTodayCollectionTotal(todayStart, todayEnd)
    }

    suspend fun getTotalOutstandingBalance(): Double = withContext(Dispatchers.IO) {
        paymentDao.getTotalOutstandingBalance()
    }

    suspend fun softDeletePayment(id: Long) = withContext(Dispatchers.IO) {
        paymentDao.softDelete(id)
    }

    suspend fun restorePayment(id: Long) = withContext(Dispatchers.IO) {
        paymentDao.restorePayment(id)
    }

    fun getDeletedPaymentsByFile(fileId: Long): Flow<List<com.moneymate.lite.data.dao.DeletedPaymentWithPerson>> =
        paymentDao.getDeletedPaymentsByFile(fileId)
}
