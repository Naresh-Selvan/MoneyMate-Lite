package com.moneymate.lite.data.repository

import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.entity.LoanFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanFileRepository @Inject constructor(
    private val loanFileDao: LoanFileDao
) {
    fun getAllFiles(): Flow<List<LoanFile>> = loanFileDao.getAllFiles()

    suspend fun insert(file: LoanFile): Long = withContext(Dispatchers.IO) {
        loanFileDao.insert(file)
    }

    suspend fun update(file: LoanFile) = withContext(Dispatchers.IO) {
        loanFileDao.update(file)
    }

    suspend fun softDelete(id: Long) = withContext(Dispatchers.IO) {
        loanFileDao.softDelete(id)
    }

    fun getDeletedFiles(): Flow<List<LoanFile>> = loanFileDao.getDeletedFiles()

    suspend fun restoreFile(id: Long) = withContext(Dispatchers.IO) {
        loanFileDao.restoreFile(id)
    }

    suspend fun getFileById(id: Long): LoanFile? = withContext(Dispatchers.IO) {
        loanFileDao.getFileById(id)
    }

    fun getFileByIdFlow(id: Long): Flow<LoanFile?> = loanFileDao.getFileByIdFlow(id)
}
