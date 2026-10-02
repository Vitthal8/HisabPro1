package com.hisabpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.SyncOutbox
import com.hisabpro.app.data.sync.outbox.InvoiceItemPayload
import com.hisabpro.app.data.sync.outbox.InvoicePayload
import com.hisabpro.app.data.sync.outbox.OutboxPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.util.UUID

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    fun getAllInvoices(businessId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    suspend fun getAllInvoicesSync(businessId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    fun getInvoicesByDateRange(businessId: String, startDate: Long, endDate: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE business_id = :businessId AND party_id = :partyId AND (deleted_at IS NULL OR deleted_at = 0) ORDER BY date DESC")
    fun getInvoicesForParty(businessId: String, partyId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdSync(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id AND business_id = :businessId LIMIT 1")
    suspend fun getInvoiceByIdAndBusinessSync(id: String, businessId: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    fun getItemsForInvoice(invoiceId: String): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun getItemsForInvoiceSync(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE business_id = :businessId AND date BETWEEN :startDate AND :endDate")
    fun getTotalSalesByDateRange(businessId: String, startDate: Long, endDate: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Query("DELETE FROM invoices WHERE id = :id AND business_id = :businessId")
    suspend fun deleteInvoice(id: String, businessId: String): Int

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceLegacy(id: String)

    @Transaction
    suspend fun insertInvoiceWithItems(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        insertInvoice(invoice)
        deleteItemsForInvoice(invoice.id)
        insertInvoiceItems(items)
    }

    @Transaction
    suspend fun deleteInvoiceWithItems(invoiceId: String, businessId: String) {
        deleteItemsForInvoice(invoiceId)
        deleteInvoice(invoiceId, businessId)
    }

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    suspend fun getAllInvoicesGlobalSync(): List<InvoiceEntity>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsGlobalSync(): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInvoices(invoices: List<InvoiceEntity>)

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAllInvoiceItems()

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()

    /**
     * Demonstrates a single atomic Room @Transaction where an Invoice, its InvoiceItems,
     * and their respective SyncOutbox events are written to the local database simultaneously.
     */
    @Transaction
    suspend fun insertInvoiceWithOutbox(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        outboxDao: SyncOutboxDao,
        json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
        eventIdGenerator: () -> String = { UUID.randomUUID().toString() }
    ) {
        // 1. Local state changes: Insert invoice and invoice items
        insertInvoice(invoice)
        deleteItemsForInvoice(invoice.id)
        insertInvoiceItems(items)

        // 2. Outbox event creation for Invoice
        val invoiceIdempotencyKey = "inv_${invoice.id}_${invoice.updatedAt}"
        val invoiceOutboxPayload = OutboxPayload(
            idempotencyKey = invoiceIdempotencyKey,
            entityId = invoice.id,
            operation = if (invoice.createdAt == invoice.updatedAt) "INSERT" else "UPDATE",
            data = invoice.toPayload()
        )
        val invoiceJsonPayload = json.encodeToString(
            OutboxPayload.serializer(InvoicePayload.serializer()),
            invoiceOutboxPayload
        )
        val invoiceEvent = SyncOutbox(
            eventId = eventIdGenerator(),
            entityId = invoice.id,
            tableName = "invoices",
            operationType = if (invoice.createdAt == invoice.updatedAt) "INSERT" else "UPDATE",
            payload = invoiceJsonPayload,
            createdAt = System.currentTimeMillis()
        )

        // 3. Outbox event creation for Invoice Items
        val itemEvents = items.map { item ->
            val itemIdempotencyKey = "item_${item.id}_${invoice.updatedAt}"
            val itemOutboxPayload = OutboxPayload(
                idempotencyKey = itemIdempotencyKey,
                entityId = item.id,
                operation = "INSERT",
                data = item.toPayload()
            )
            val itemJsonPayload = json.encodeToString(
                OutboxPayload.serializer(InvoiceItemPayload.serializer()),
                itemOutboxPayload
            )
            SyncOutbox(
                eventId = eventIdGenerator(),
                entityId = item.id,
                tableName = "invoice_items",
                operationType = "INSERT",
                payload = itemJsonPayload,
                createdAt = System.currentTimeMillis()
            )
        }

        // 4. Atomically persist all outbox events in the same database transaction
        outboxDao.insertAll(listOf(invoiceEvent) + itemEvents)
    }

    /**
     * Demonstrates atomic deletion where an Invoice soft-deletion and its SyncOutbox DELETE event
     * are committed in a single Room @Transaction.
     */
    @Transaction
    suspend fun deleteInvoiceWithOutbox(
        invoiceId: String,
        businessId: String,
        outboxDao: SyncOutboxDao,
        json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
        eventIdGenerator: () -> String = { UUID.randomUUID().toString() }
    ) {
        val now = System.currentTimeMillis()
        val invoice = getInvoiceByIdAndBusinessSync(invoiceId, businessId)
        if (invoice != null) {
            val softDeletedInvoice = invoice.copy(deletedAt = now, updatedAt = now)
            updateInvoice(softDeletedInvoice)

            val invoiceOutboxPayload = OutboxPayload(
                idempotencyKey = "del_inv_${invoiceId}_$now",
                entityId = invoiceId,
                operation = "DELETE",
                data = softDeletedInvoice.toPayload()
            )
            val invoiceJsonPayload = json.encodeToString(
                OutboxPayload.serializer(InvoicePayload.serializer()),
                invoiceOutboxPayload
            )
            val invoiceEvent = SyncOutbox(
                eventId = eventIdGenerator(),
                entityId = invoiceId,
                tableName = "invoices",
                operationType = "DELETE",
                payload = invoiceJsonPayload,
                createdAt = now
            )
            outboxDao.insert(invoiceEvent)
        }
    }
}

/**
 * Extension function converting InvoiceEntity to serializable DTO.
 */
fun InvoiceEntity.toPayload(): InvoicePayload = InvoicePayload(
    id = id,
    userId = "",
    businessId = businessId,
    invoiceNo = invoiceNo,
    date = date,
    partyId = partyId,
    customerName = customerName,
    customerPhone = customerPhone,
    customerAddress = customerAddress,
    customerGstin = customerGstin,
    type = type,
    gstMode = gstMode,
    subtotal = subtotal,
    discount = discount,
    taxableAmount = taxableAmount,
    cgst = cgst,
    sgst = sgst,
    igst = igst,
    total = total,
    paidAmount = paidAmount,
    paymentStatus = paymentStatus,
    paymentMode = paymentMode,
    notes = notes,
    isGst = isGst,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/**
 * Extension function converting InvoiceItemEntity to serializable DTO.
 */
fun InvoiceItemEntity.toPayload(): InvoiceItemPayload = InvoiceItemPayload(
    id = id,
    userId = "",
    invoiceId = invoiceId,
    itemId = itemId,
    itemName = itemName,
    hsnCode = hsnCode,
    qty = qty,
    unit = unit,
    rate = rate,
    discount = discount,
    cgstRate = cgstRate,
    sgstRate = sgstRate,
    igstRate = igstRate,
    amount = amount,
    deletedAt = deletedAt
)
