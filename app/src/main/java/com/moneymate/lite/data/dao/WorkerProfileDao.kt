package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneymate.lite.data.entity.WorkerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worker: WorkerProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workers: List<WorkerProfileEntity>)

    @Update
    suspend fun update(worker: WorkerProfileEntity)

    @Query("SELECT * FROM worker_profiles WHERE businessId = :businessId ORDER BY displayName ASC")
    fun getAllWorkersFlow(businessId: String): Flow<List<WorkerProfileEntity>>

    @Query("SELECT * FROM worker_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getWorkerById(uid: String): WorkerProfileEntity?

    @Query("SELECT * FROM worker_profiles WHERE username = :username LIMIT 1")
    suspend fun getWorkerByUsername(username: String): WorkerProfileEntity?

    @Query("DELETE FROM worker_profiles WHERE uid = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM worker_profiles")
    suspend fun clearAll()
}
