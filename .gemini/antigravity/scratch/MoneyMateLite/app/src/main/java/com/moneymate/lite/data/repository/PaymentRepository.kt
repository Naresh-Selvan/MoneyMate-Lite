package com.moneymate.lite.data.repository

import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.entity.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao
) {
    fun getPaymentsByLoan(loanId: Long): Flow<List<Payment>> = paymentDao.getPaymentsByLoan(loanId)

    suspend fun insert(payment: Payment): Long = withContext(Dispatchers.IO) {
        paymentDao.insert(payment)
    }

    suspend fun getTotalPaidForLoan(loanId: Long): Double = withContext(Dispatchers.IO) {
        paymentDao.getTotalPaidForLoan(loanId)
    }

    suspend fun getTodayCollectionTotal(todayStart: Long, todayEnd: Long): Double = withContext(Dispatchers.IO) {
        paymentDao.getTodayCollectionTotal(todayStart, todayEnd)
    }

    suspend fun getTotalOutstandingBalance(): Double = withContext(Dispatchers.IO) {
        paymentDao.getTotalOutstandingBalance()
    }
}
