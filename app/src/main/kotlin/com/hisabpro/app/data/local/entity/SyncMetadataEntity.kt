package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "last_pulled_at")
    val lastPulledAt: Long = 0L,
    @ColumnInfo(name = "last_pushed_at")
    val lastPushedAt: Long = 0L,
    @ColumnInfo(name = "last_sync_status")
    val lastSyncStatus: String = "IDLE", // IDLE, SYNCING, SUCCESS, ERROR
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)
