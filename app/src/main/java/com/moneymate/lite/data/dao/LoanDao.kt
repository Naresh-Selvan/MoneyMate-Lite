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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Query("UPDATE loans SET isCompleted = 1, completedAt = :completedAt WHERE id = :loanId")
    suspend fun markCompleted(loanId: Long, completedAt: Long)

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

    @Query("UPDATE loans SET isCompleted = 0, completedAt = null WHERE id = :loanId")
    suspend fun markIncomplete(loanId: Long)

    @Query("UPDATE loans SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteLoan(id: Long)
}

data class DateTransactionEntity(
    val id: Long,
    val personId: Long,
    val personName: String,
    val amount: Double,
    val date: Long
)
