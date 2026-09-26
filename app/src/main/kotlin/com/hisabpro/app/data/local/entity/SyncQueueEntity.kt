package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["created_at"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: String,         // "business", "party", "item", "invoice", "payment", "expense", etc.
    @ColumnInfo(name = "entity_id")
    val entityId: String,           // UUID or identifier
    val operation: String,          // "UPSERT", "DELETE"
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,        // Serialized JSON payload snapshot
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, IN_PROGRESS, SYNCED, FAILED
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "next_retry_at")
    val nextRetryAt: Long = 0L
)
