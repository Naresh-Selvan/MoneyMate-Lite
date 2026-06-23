package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("UPDATE payments SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE payments SET isDeleted = 0 WHERE id = :id")
    suspend fun restorePayment(id: Long)

    @Query("""
        SELECT p.id, p.amount, p.date, pe.name AS personName, pe.id AS personId, p.loanId
        FROM payments p
        JOIN loans l ON p.loanId = l.id
        JOIN persons pe ON l.personId = pe.id
        WHERE pe.fileId = :fileId AND p.isDeleted = 1
        ORDER BY p.createdAt DESC
    """)
    fun getDeletedPaymentsByFile(fileId: Long): Flow<List<DeletedPaymentWithPerson>>

    @Update
    suspend fun update(payment: Payment)

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): Payment?

    @Query("""
        SELECT 
            payments.id AS id, 
            persons.id AS personId, 
            persons.name AS personName, 
            payments.amount AS amount, 
            payments.date AS date
        FROM payments
        INNER JOIN loans ON payments.loanId = loans.id
        INNER JOIN persons ON loans.personId = persons.id
        WHERE persons.fileId = :fileId 
          AND payments.isDeleted = 0 
          AND loans.isDeleted = 0
          AND payments.date BETWEEN :startOfDay AND :endOfDay
    """)
    fun getPaymentsReceivedOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>>
}

data class DeletedPaymentWithPerson(
    val id: Long,
    val amount: Double,
    val date: Long,
    val personName: String,
    val personId: Long,
    val loanId: Long
)
