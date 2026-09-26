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

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED') ORDER BY id ASC LIMIT :limit")
    suspend fun getPendingItems(limit: Int = 200): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'IN_PROGRESS' OR status = 'FAILED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'IN_PROGRESS' OR status = 'FAILED'")
    suspend fun getPendingCountSync(): Int

    @Query("UPDATE sync_queue SET status = :status, last_error = :error, retry_count = :retryCount, next_retry_at = :nextRetryAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?, retryCount: Int, nextRetryAt: Long)

    @Query("UPDATE sync_queue SET status = 'IN_PROGRESS' WHERE id IN (:ids)")
    suspend fun markInProgress(ids: List<Long>)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedItems()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
