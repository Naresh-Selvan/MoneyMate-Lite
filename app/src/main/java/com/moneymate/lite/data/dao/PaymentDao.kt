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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    // Official total paid excludes unconfirmed offline payments
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = :loanId AND isDeleted = 0 AND syncStatus != 'PENDING_SYNC'")
    suspend fun getTotalPaidForLoan(loanId: Long): Double

    // Total paid including pending sync payments
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = :loanId AND isDeleted = 0")
    suspend fun getTotalPaidForLoanWithPending(loanId: Long): Double

    // Pending sync amount only
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = :loanId AND isDeleted = 0 AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingPaidForLoan(loanId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(payments.amount), 0.0) 
        FROM payments 
        INNER JOIN loans ON payments.loanId = loans.id
        INNER JOIN persons ON loans.personId = persons.id
        INNER JOIN loan_files ON persons.fileId = loan_files.id
        WHERE payments.date BETWEEN :todayStart AND :todayEnd 
          AND payments.isDeleted = 0 
          AND payments.syncStatus != 'PENDING_SYNC'
          AND loans.isDeleted = 0
          AND persons.isDeleted = 0 
          AND loan_files.isDeleted = 0
    """)
    suspend fun getTodayCollectionTotal(todayStart: Long, todayEnd: Long): Double

    @Query("""
        SELECT COALESCE(SUM(loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0 AND syncStatus != 'PENDING_SYNC')), 0.0) 
        FROM loans 
        INNER JOIN persons ON loans.personId = persons.id
        INNER JOIN loan_files ON persons.fileId = loan_files.id
        WHERE loans.isCompleted = 0 AND loans.isDeleted = 0
          AND persons.isDeleted = 0 AND loan_files.isDeleted = 0
    """)
    suspend fun getTotalOutstandingBalance(): Double

    @Query("UPDATE payments SET isDeleted = 1, lastModified = :modified WHERE id = :id")
    suspend fun softDelete(id: Long, modified: Long = System.currentTimeMillis())

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) 
        FROM payments 
        WHERE recordedByWorkerId = :workerUid 
          AND date >= :startOfDay 
          AND isDeleted = 0
    """)
    fun getTodayCollectionsByWorker(workerUid: String, startOfDay: Long): Flow<Double>

    @Query("""
        SELECT COUNT(*) 
        FROM payments 
        WHERE recordedByWorkerId = :workerUid 
          AND date >= :startOfDay 
          AND isDeleted = 0
    """)
    fun getTodayPaymentCountByWorker(workerUid: String, startOfDay: Long): Flow<Int>

    @Query("UPDATE payments SET isDeleted = 1, lastModified = :modified WHERE sharedId = :sharedId")
    suspend fun softDeleteBySharedId(sharedId: String, modified: Long = System.currentTimeMillis())

    @Query("UPDATE payments SET isDeleted = 1, lastModified = :modified WHERE loanId = :loanId")
    suspend fun softDeletePaymentsForLoan(loanId: Long, modified: Long = System.currentTimeMillis())

    @Query("UPDATE payments SET isDeleted = 0, lastModified = :modified WHERE id = :id")
    suspend fun restorePayment(id: Long, modified: Long = System.currentTimeMillis())

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

    @Query("SELECT * FROM payments WHERE sharedId = :sharedId LIMIT 1")
    suspend fun getPaymentBySharedId(sharedId: String): Payment?

    @Query("SELECT * FROM payments WHERE sharedId = :sharedId LIMIT 1")
    fun getPaymentBySharedIdFlow(sharedId: String): Flow<Payment?>

    @Query("SELECT * FROM payments WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPaymentsPendingSync(): List<Payment>

    @Query("UPDATE payments SET syncStatus = :status, syncError = :error, lastModified = :modified WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, error: String?, modified: Long = System.currentTimeMillis())

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
          AND persons.isDeleted = 0
          AND payments.date BETWEEN :startOfDay AND :endOfDay
    """)
    fun getPaymentsReceivedOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>>

    @Query("SELECT * FROM payments WHERE isDeleted = 0")
    suspend fun getAllPaymentsList(): List<Payment>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsIncludingDeleted(): List<Payment>

    @Query("""
        UPDATE payments 
        SET loanId = (SELECT id FROM loans WHERE loans.sharedId = payments.loanSharedId LIMIT 1) 
        WHERE loanSharedId IS NOT NULL 
          AND loanSharedId != '' 
          AND (SELECT id FROM loans WHERE loans.sharedId = payments.loanSharedId LIMIT 1) IS NOT NULL
    """)
    suspend fun relinkPaymentsToLocalLoans(): Int

    @Query("""
        UPDATE payments 
        SET syncStatus = 'PENDING_SYNC', 
            businessId = :businessId,
            loanSharedId = COALESCE((SELECT sharedId FROM loans WHERE loans.id = payments.loanId LIMIT 1), loanSharedId),
            lastModified = :now
        WHERE (businessId = '' OR businessId IS NULL OR syncStatus != 'SYNCED')
          AND isDeleted = 0
    """)
    suspend fun markUnsyncedPaymentsForSync(businessId: String, now: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM payments")
    suspend fun clearAll()
}

data class DeletedPaymentWithPerson(
    val id: Long,
    val amount: Double,
    val date: Long,
    val personName: String,
    val personId: Long,
    val loanId: Long
)
