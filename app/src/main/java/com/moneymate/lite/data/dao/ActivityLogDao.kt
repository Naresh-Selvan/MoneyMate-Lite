package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneymate.lite.data.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ActivityLogEntity>)

    @Query("SELECT * FROM activity_logs WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getActivityLogsFlow(businessId: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE businessId = :businessId AND (:actorId IS NULL OR actorId = :actorId) AND (:action IS NULL OR action = :action) AND (:fileSharedId IS NULL OR fileSharedId = :fileSharedId) ORDER BY timestamp DESC")
    fun getFilteredActivityLogsFlow(
        businessId: String,
        actorId: String?,
        action: String?,
        fileSharedId: String?
    ): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getUnsyncedLogs(): List<ActivityLogEntity>

    @Query("DELETE FROM activity_logs")
    suspend fun clearAll()
}
