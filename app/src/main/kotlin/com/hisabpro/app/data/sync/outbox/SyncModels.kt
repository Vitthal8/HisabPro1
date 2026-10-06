package com.hisabpro.app.data.sync.outbox

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Generic container for outbox entity snapshot payloads.
 * Strictly guarantees every payload snapshot contains an idempotency_key for safe network retries.
 */
@Serializable
data class OutboxPayload<T>(
    @SerialName("idempotency_key")
    val idempotencyKey: String,

    @SerialName("entity_id")
    val entityId: String,

    @SerialName("operation")
    val operation: String, // "INSERT", "UPDATE", "DELETE"

    @SerialName("data")
    val data: T
)

/**
 * Serializable DTO mapping for Invoice records to PostgreSQL schema.
 */
@Serializable
data class InvoicePayload(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("business_id")
    val businessId: String,

    @SerialName("invoice_no")
    val invoiceNo: String,

    @SerialName("date")
    val date: Long,

    @SerialName("party_id")
    val partyId: String? = null,

    @SerialName("customer_name")
    val customerName: String = "",

    @SerialName("customer_phone")
    val customerPhone: String = "",

    @SerialName("customer_address")
    val customerAddress: String = "",

    @SerialName("customer_gstin")
    val customerGstin: String = "",

    @SerialName("type")
    val type: String = "NON_GST_BILL",

    @SerialName("gst_mode")
    val gstMode: String = "EXEMPT",

    @SerialName("subtotal")
    val subtotal: Long = 0L,

    @SerialName("discount")
    val discount: Long = 0L,

    @SerialName("taxable_amount")
    val taxableAmount: Long = 0L,

    @SerialName("cgst")
    val cgst: Long = 0L,

    @SerialName("sgst")
    val sgst: Long = 0L,

    @SerialName("igst")
    val igst: Long = 0L,

    @SerialName("total")
    val total: Long = 0L,

    @SerialName("paid_amount")
    val paidAmount: Long = 0L,

    @SerialName("payment_status")
    val paymentStatus: String = "PAID",

    @SerialName("payment_mode")
    val paymentMode: String = "Cash",

    @SerialName("notes")
    val notes: String = "",

    @SerialName("is_gst")
    val isGst: Boolean = false,

    @SerialName("created_at")
    val createdAt: Long = 0L,

    @SerialName("updated_at")
    val updatedAt: Long = 0L,

    @SerialName("deleted_at")
    val deletedAt: Long? = null
)

/**
 * Serializable DTO mapping for InvoiceItem records to PostgreSQL schema.
 */
@Serializable
data class InvoiceItemPayload(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("invoice_id")
    val invoiceId: String,

    @SerialName("business_id")
    val businessId: String = "",

    @SerialName("item_id")
    val itemId: String? = null,

    @SerialName("item_name")
    val itemName: String,

    @SerialName("hsn_code")
    val hsnCode: String = "",

    @SerialName("qty")
    val qty: Double = 1.0,

    @SerialName("unit")
    val unit: String = "Pcs",

    @SerialName("rate")
    val rate: Long = 0L,

    @SerialName("discount")
    val discount: Long = 0L,

    @SerialName("cgst_rate")
    val cgstRate: Double = 0.0,

    @SerialName("sgst_rate")
    val sgstRate: Double = 0.0,

    @SerialName("igst_rate")
    val igstRate: Double = 0.0,

    @SerialName("amount")
    val amount: Long = 0L,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @SerialName("deleted_at")
    val deletedAt: Long? = null,

    @SerialName("synced_at")
    val syncedAt: Long? = null
)

/**
 * Batched HTTP request structure sent by SyncWorker to backend.
 */
@Serializable
data class BatchSyncRequest(
    @SerialName("events")
    val events: List<OutboxEventDto>
)

/**
 * Individual outbox event item inside the HTTP POST sync request.
 */
@Serializable
data class OutboxEventDto(
    @SerialName("event_id")
    val eventId: String,

    @SerialName("idempotency_key")
    val idempotencyKey: String,

    @SerialName("table_name")
    val tableName: String,

    @SerialName("operation_type")
    val operationType: String,

    @SerialName("entity_id")
    val entityId: String,

    @SerialName("payload")
    val payload: JsonElement,

    @SerialName("created_at")
    val createdAt: Long
)

/**
 * HTTP POST batch response from backend sync endpoint.
 */
@Serializable
data class BatchSyncResponse(
    @SerialName("success")
    val success: Boolean,

    @SerialName("processed_event_ids")
    val processedEventIds: List<String> = emptyList(),

    @SerialName("error")
    val error: String? = null
)
