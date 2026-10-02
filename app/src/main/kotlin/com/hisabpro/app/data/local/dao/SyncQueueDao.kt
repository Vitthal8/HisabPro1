package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueEntity>)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR (status = 'FAILED' AND next_retry_at <= :currentTime) ORDER BY id ASC LIMIT :limit")
    suspend fun getPendingItems(currentTime: Long = System.currentTimeMillis(), limit: Int = 200): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'IN_PROGRESS' OR status = 'FAILED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'IN_PROGRESS' OR status = 'FAILED'")
    suspend fun getPendingCountSync(): Int

    @Query("UPDATE sync_queue SET status = :status, last_error = :error, retry_count = :retryCount, next_retry_at = :nextRetryAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?, retryCount: Int, nextRetryAt: Long)

    @Query("UPDATE sync_queue SET status = 'IN_PROGRESS', next_retry_at = :currentTime WHERE id IN (:ids)")
    suspend fun markInProgress(ids: List<Long>, currentTime: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'IN_PROGRESS' AND (next_retry_at <= :cutoffTime OR created_at <= :cutoffTime)")
    suspend fun resetStaleInProgress(cutoffTime: Long): Int

    @Query("UPDATE sync_queue SET status = 'PENDING', next_retry_at = 0 WHERE status = 'FAILED' OR status = 'IN_PROGRESS'")
    suspend fun resetAllForManualSync(): Int

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedItems()

    @Query("DELETE FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId AND status = 'PENDING'")
    suspend fun deletePendingForEntity(entityType: String, entityId: String)

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
