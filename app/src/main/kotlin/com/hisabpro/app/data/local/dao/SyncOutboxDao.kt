package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.SyncOutbox
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SyncOutbox)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<SyncOutbox>)

    /**
     * Retrieves outbox events strictly in chronological order for batch synchronization.
     */
    @Query("SELECT * FROM sync_outbox ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingEventsChronological(limit: Int = 200): List<SyncOutbox>

    /**
     * Atomically deletes synced events from outbox once HTTP 200 OK is confirmed by server.
     */
    @Query("DELETE FROM sync_outbox WHERE event_id IN (:eventIds)")
    suspend fun deleteEventsByIds(eventIds: List<String>)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun getEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun getEventCountSync(): Int

    @Query("DELETE FROM sync_outbox WHERE table_name = :tableName AND entity_id = :entityId")
    suspend fun deleteByEntity(tableName: String, entityId: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearAll()
}
