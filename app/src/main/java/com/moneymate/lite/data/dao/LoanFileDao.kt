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

    @Query("SELECT * FROM loan_files WHERE isDeleted = 0 AND sharedId IN (:allowedSharedIds) ORDER BY sortOrder")
    fun getFilesForWorker(allowedSharedIds: List<String>): Flow<List<LoanFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: LoanFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<LoanFile>)

    @Update
    suspend fun update(file: LoanFile)

    @Query("UPDATE loan_files SET isDeleted = 1, lastModified = :modified WHERE id = :id")
    suspend fun softDelete(id: Long, modified: Long = System.currentTimeMillis())

    @Query("UPDATE loan_files SET isDeleted = 1, lastModified = :modified WHERE sharedId = :sharedId")
    suspend fun softDeleteBySharedId(sharedId: String, modified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM loan_files WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedFiles(): Flow<List<LoanFile>>

    @Query("UPDATE loan_files SET isDeleted = 0, lastModified = :modified WHERE id = :id")
    suspend fun restoreFile(id: Long, modified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM loan_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): LoanFile?

    @Query("SELECT * FROM loan_files WHERE id = :id LIMIT 1")
    fun getFileByIdFlow(id: Long): Flow<LoanFile?>

    @Query("SELECT * FROM loan_files WHERE sharedId = :sharedId LIMIT 1")
    suspend fun getFileBySharedId(sharedId: String): LoanFile?

    @Query("SELECT * FROM loan_files WHERE sharedId = :sharedId LIMIT 1")
    fun getFileBySharedIdFlow(sharedId: String): Flow<LoanFile?>

    @Query("SELECT * FROM loan_files WHERE isDeleted = 0")
    suspend fun getAllFilesList(): List<LoanFile>

    @Query("SELECT * FROM loan_files")
    suspend fun getAllFilesIncludingDeleted(): List<LoanFile>

    @Query("DELETE FROM loan_files")
    suspend fun clearAll()
}
