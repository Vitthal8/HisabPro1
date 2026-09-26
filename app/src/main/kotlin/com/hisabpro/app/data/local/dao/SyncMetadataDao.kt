package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabpro.app.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE table_name = :tableName")
    suspend fun getMetadata(tableName: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    fun getAllMetadataFlow(): Flow<List<SyncMetadataEntity>>

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllMetadata(): List<SyncMetadataEntity>

    @Query("UPDATE sync_metadata SET last_pulled_at = :timestamp WHERE table_name = :tableName")
    suspend fun updateLastPulled(tableName: String, timestamp: Long)

    @Query("UPDATE sync_metadata SET last_pushed_at = :timestamp WHERE table_name = :tableName")
    suspend fun updateLastPushed(tableName: String, timestamp: Long)

    @Query("UPDATE sync_metadata SET last_sync_status = :status, error_message = :error WHERE table_name = :tableName")
    suspend fun updateStatus(tableName: String, status: String, error: String?)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll()
}
