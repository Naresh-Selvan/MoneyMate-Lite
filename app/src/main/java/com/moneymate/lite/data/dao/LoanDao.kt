package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneymate.lite.data.entity.Loan
import kotlinx.coroutines.flow.Flow

data class LoanWithBalance(
    val loanId: Long,
    val personId: Long,
    val personName: String,
    val totalAmount: Double,
    val totalPaid: Double,
    val balance: Double,
    val dateGiven: Long
)

data class LoanWithBalanceAndFile(
    val loanId: Long,
    val personId: Long,
    val personName: String,
    val totalAmount: Double,
    val totalPaid: Double,
    val balance: Double,
    val dateGiven: Long,
    val fileName: String
)

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE personId = :personId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getLoansByPerson(personId: Long): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE personId = :personId AND isCompleted = 0 AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveLoanByPerson(personId: Long): Loan?

    @Query("SELECT * FROM loans WHERE personId = :personId AND isCompleted = 0 AND isDeleted = 0 LIMIT 1")
    fun getActiveLoanByPersonFlow(personId: Long): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Long): Loan?

    @Query("SELECT * FROM loans WHERE sharedId = :sharedId LIMIT 1")
    suspend fun getLoanBySharedId(sharedId: String): Loan?

    @Query("SELECT * FROM loans WHERE sharedId = :sharedId LIMIT 1")
    fun getLoanBySharedIdFlow(sharedId: String): Flow<Loan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<Loan>)

    @Update
    suspend fun update(loan: Loan)

    @Query("UPDATE loans SET isCompleted = 1, completedAt = :completedAt, lastModified = :modified WHERE id = :loanId")
    suspend fun markCompleted(loanId: Long, completedAt: Long, modified: Long = System.currentTimeMillis())

    @Query("UPDATE loans SET isCompleted = 1, completedAt = :completedAt, lastModified = :modified WHERE sharedId = :sharedId")
    suspend fun markCompletedBySharedId(sharedId: String, completedAt: Long, modified: Long = System.currentTimeMillis())

    @Query("""
        SELECT 
            loans.id AS loanId, 
            loans.personId AS personId, 
            persons.name AS personName, 
            loans.totalAmount AS totalAmount, 
            (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0) AS totalPaid,
            (loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0)) AS balance,
            loans.dateGiven AS dateGiven
        FROM loans
        INNER JOIN persons ON loans.personId = persons.id
        WHERE persons.fileId = :fileId AND loans.isCompleted = 0 AND loans.isDeleted = 0 AND persons.isDeleted = 0
    """)
    fun getAllActiveLoansInFile(fileId: Long): Flow<List<LoanWithBalance>>

    @Query("""
        SELECT 
            loans.id AS loanId, 
            loans.personId AS personId, 
            persons.name AS personName, 
            loans.totalAmount AS totalAmount, 
            (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0) AS totalPaid,
            (loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0)) AS balance,
            loans.dateGiven AS dateGiven,
            loan_files.name AS fileName
        FROM loans
        INNER JOIN persons ON loans.personId = persons.id
        INNER JOIN loan_files ON persons.fileId = loan_files.id
        WHERE loans.isCompleted = 0 AND loans.isDeleted = 0
            AND persons.isDeleted = 0 AND loan_files.isDeleted = 0
        ORDER BY balance DESC
    """)
    fun getAllActiveLoansAcrossFiles(): Flow<List<LoanWithBalanceAndFile>>

    @Query("""
        SELECT 
            loans.id AS id, 
            loans.personId AS personId, 
            persons.name AS personName, 
            loans.totalAmount AS amount, 
            loans.dateGiven AS date
        FROM loans
        INNER JOIN persons ON loans.personId = persons.id
        WHERE persons.fileId = :fileId 
          AND loans.isDeleted = 0 
          AND persons.isDeleted = 0
          AND loans.dateGiven BETWEEN :startOfDay AND :endOfDay
    """)
    fun getLoansGivenOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>>

    @Query("""
        SELECT 
            persons.id AS personId,
            (
                SELECT COALESCE(SUM(loans.totalAmount), 0.0) 
                FROM loans 
                WHERE loans.personId = persons.id 
                  AND loans.isDeleted = 0 
                  AND loans.dateGiven <= :upToDate
            ) - (
                SELECT COALESCE(SUM(payments.amount), 0.0) 
                FROM payments 
                INNER JOIN loans ON payments.loanId = loans.id
                WHERE loans.personId = persons.id 
                  AND payments.isDeleted = 0 
                  AND loans.isDeleted = 0
                  AND payments.date <= :upToDate
            ) AS balance
        FROM persons
        WHERE persons.fileId = :fileId AND persons.isDeleted = 0
    """)
    fun getBalancesUpToDate(fileId: Long, upToDate: Long): Flow<List<PersonBalanceUpToDate>>

    @Query("UPDATE loans SET isCompleted = 0, completedAt = null, lastModified = :modified WHERE id = :loanId")
    suspend fun markIncomplete(loanId: Long, modified: Long = System.currentTimeMillis())

    @Query("UPDATE loans SET isDeleted = 1, lastModified = :modified WHERE id = :id")
    suspend fun softDeleteLoan(id: Long, modified: Long = System.currentTimeMillis())

    @Query("UPDATE loans SET isDeleted = 1, lastModified = :modified WHERE sharedId = :sharedId")
    suspend fun softDeleteLoanBySharedId(sharedId: String, modified: Long = System.currentTimeMillis())

    @Query("UPDATE loans SET isDeleted = 1, lastModified = :modified WHERE personId = :personId")
    suspend fun softDeleteLoansForPerson(personId: Long, modified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM loans WHERE isDeleted = 0")
    suspend fun getAllLoansList(): List<Loan>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansIncludingDeleted(): List<Loan>

    @Query("SELECT * FROM loans WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getLoansPendingSync(): List<Loan>

    @Query("UPDATE loans SET syncStatus = :status, lastModified = :modified WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, modified: Long = System.currentTimeMillis())

    @Query("""
        UPDATE loans 
        SET personId = (SELECT id FROM persons WHERE persons.sharedId = loans.personSharedId LIMIT 1) 
        WHERE personSharedId IS NOT NULL 
          AND personSharedId != '' 
          AND (SELECT id FROM persons WHERE persons.sharedId = loans.personSharedId LIMIT 1) IS NOT NULL
    """)
    suspend fun relinkLoansToLocalPersons(): Int

    @Query("""
        UPDATE loans 
        SET syncStatus = 'PENDING_SYNC', 
            businessId = :businessId,
            personSharedId = COALESCE((SELECT sharedId FROM persons WHERE persons.id = loans.personId LIMIT 1), personSharedId),
            lastModified = :now
        WHERE (businessId = '' OR businessId IS NULL OR syncStatus != 'SYNCED')
          AND isDeleted = 0
    """)
    suspend fun markUnsyncedLoansForSync(businessId: String, now: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM loans")
    suspend fun clearAll()
}

data class DateTransactionEntity(
    val id: Long,
    val personId: Long,
    val personName: String,
    val amount: Double,
    val date: Long
)

data class PersonBalanceUpToDate(
    val personId: Long,
    val balance: Double
)
