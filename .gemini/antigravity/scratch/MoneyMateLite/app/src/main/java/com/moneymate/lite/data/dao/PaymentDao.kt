package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneymate.lite.data.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE loanId = :loanId AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsByLoan(loanId: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment): Long

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = :loanId AND isDeleted = 0")
    suspend fun getTotalPaidForLoan(loanId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE date BETWEEN :todayStart AND :todayEnd AND isDeleted = 0")
    suspend fun getTodayCollectionTotal(todayStart: Long, todayEnd: Long): Double

    @Query("""
        SELECT COALESCE(SUM(loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0)), 0.0) 
        FROM loans 
        WHERE loans.isCompleted = 0 AND loans.isDeleted = 0
    """)
    suspend fun getTotalOutstandingBalance(): Double
}
