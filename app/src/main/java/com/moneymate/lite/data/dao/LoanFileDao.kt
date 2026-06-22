package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneymate.lite.data.entity.LoanFile
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanFileDao {
    @Query("SELECT * FROM loan_files WHERE isDeleted = 0 ORDER BY sortOrder")
    fun getAllFiles(): Flow<List<LoanFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: LoanFile): Long

    @Update
    suspend fun update(file: LoanFile)

    @Query("UPDATE loan_files SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM loan_files WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedFiles(): Flow<List<LoanFile>>

    @Query("UPDATE loan_files SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreFile(id: Long)

    @Query("SELECT * FROM loan_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): LoanFile?

    @Query("SELECT * FROM loan_files WHERE id = :id LIMIT 1")
    fun getFileByIdFlow(id: Long): Flow<LoanFile?>
}
