package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an item in the Transactional Outbox pattern.
 * Captures database mutation events locally in the same transaction as state changes.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["table_name", "entity_id"])
    ]
)
data class SyncOutbox(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "table_name")
    val tableName: String,

    @ColumnInfo(name = "operation_type")
    val operationType: String, // "INSERT", "UPDATE", "DELETE"

    @ColumnInfo(name = "payload")
    val payload: String, // JSON string containing payload snapshot with idempotency_key

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
