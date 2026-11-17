package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneymate.lite.data.entity.ApprovalRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApprovalRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: ApprovalRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<ApprovalRequestEntity>)

    @Update
    suspend fun update(request: ApprovalRequestEntity)

    @Query("SELECT * FROM approval_requests WHERE businessId = :businessId AND status = 'PENDING' ORDER BY requestedAt DESC")
    fun getPendingRequestsFlow(businessId: String): Flow<List<ApprovalRequestEntity>>

    @Query("SELECT COUNT(*) FROM approval_requests WHERE businessId = :businessId AND status = 'PENDING'")
    fun getPendingRequestsCountFlow(businessId: String): Flow<Int>

    @Query("SELECT * FROM approval_requests WHERE businessId = :businessId AND workerId = :workerId ORDER BY requestedAt DESC")
    fun getRequestsByWorkerFlow(businessId: String, workerId: String): Flow<List<ApprovalRequestEntity>>

    @Query("SELECT * FROM approval_requests WHERE businessId = :businessId AND recordSharedId = :recordSharedId AND status = 'PENDING'")
    fun getPendingRequestsForRecordFlow(businessId: String, recordSharedId: String): Flow<List<ApprovalRequestEntity>>

    @Query("SELECT * FROM approval_requests WHERE businessId = :businessId AND recordSharedId = :recordSharedId AND status = 'PENDING'")
    suspend fun getPendingRequestsForRecord(businessId: String, recordSharedId: String): List<ApprovalRequestEntity>

    @Query("SELECT * FROM approval_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): ApprovalRequestEntity?

    @Query("SELECT * FROM approval_requests WHERE syncStatus = 'PENDING_SYNC' OR syncStatus = 'NOT_SUBMITTED'")
    suspend fun getUnsyncedRequests(): List<ApprovalRequestEntity>

    @Query("DELETE FROM approval_requests WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM approval_requests")
    suspend fun clearAll()
}
