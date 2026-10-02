package com.hisabpro.app

import com.hisabpro.app.data.local.dao.toPayload
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.SyncOutbox
import com.hisabpro.app.data.sync.outbox.BatchSyncRequest
import com.hisabpro.app.data.sync.outbox.InvoiceItemPayload
import com.hisabpro.app.data.sync.outbox.InvoicePayload
import com.hisabpro.app.data.sync.outbox.OutboxEventDto
import com.hisabpro.app.data.sync.outbox.OutboxPayload
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TransactionalOutboxSyncTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testSyncOutboxPayloadIdempotencyKey() {
        val invoiceId = "inv_tx_101"
        val timestamp = 1760000000000L
        val idempotencyKey = "inv_${invoiceId}_$timestamp"

        val invoicePayload = OutboxPayload(
            idempotencyKey = idempotencyKey,
            entityId = invoiceId,
            operation = "INSERT",
            data = InvoicePayload(
                id = invoiceId,
                businessId = "biz_test",
                invoiceNo = "2026-27/INV/001",
                date = timestamp,
                total = 118000L
            )
        )

        val serializedJson = json.encodeToString(
            OutboxPayload.serializer(InvoicePayload.serializer()),
            invoicePayload
        )

        assertTrue(serializedJson.contains("\"idempotency_key\":\"$idempotencyKey\""))
        assertTrue(serializedJson.contains("\"entity_id\":\"$invoiceId\""))
        assertTrue(serializedJson.contains("\"operation\":\"INSERT\""))

        val parsedElement = json.parseToJsonElement(serializedJson)
        val extractedIdempotencyKey = parsedElement.jsonObject["idempotency_key"]?.jsonPrimitive?.content
        assertEquals(idempotencyKey, extractedIdempotencyKey)
    }

    @Test
    fun testInvoiceAndItemPayloadConversion() {
        val invoiceEntity = InvoiceEntity(
            id = "inv_007",
            businessId = "biz_main",
            invoiceNo = "2026-27/INV/007",
            date = System.currentTimeMillis(),
            customerName = "Ramesh Traders",
            total = 250000L,
            paidAmount = 250000L
        )

        val itemEntity = InvoiceItemEntity(
            id = "item_007_1",
            invoiceId = "inv_007",
            itemName = "Basmati Rice 25kg",
            qty = 2.0,
            rate = 125000L,
            amount = 250000L
        )

        val invDto = invoiceEntity.toPayload()
        val itemDto = itemEntity.toPayload()

        assertEquals("inv_007", invDto.id)
        assertEquals("biz_main", invDto.businessId)
        assertEquals("Ramesh Traders", invDto.customerName)
        assertEquals(250000L, invDto.total)

        assertEquals("item_007_1", itemDto.id)
        assertEquals("inv_007", itemDto.invoiceId)
        assertEquals("Basmati Rice 25kg", itemDto.itemName)
        assertEquals(250000L, itemDto.amount)
    }

    @Test
    fun testBatchSyncRequestSerialization() {
        val eventId1 = UUID.randomUUID().toString()
        val eventId2 = UUID.randomUUID().toString()

        val event1 = OutboxEventDto(
            eventId = eventId1,
            idempotencyKey = "idem_inv_100",
            tableName = "invoices",
            operationType = "INSERT",
            entityId = "inv_100",
            payload = json.parseToJsonElement("""{"idempotency_key":"idem_inv_100","data":{"id":"inv_100"}}"""),
            createdAt = System.currentTimeMillis() - 1000L
        )

        val event2 = OutboxEventDto(
            eventId = eventId2,
            idempotencyKey = "idem_item_101",
            tableName = "invoice_items",
            operationType = "INSERT",
            entityId = "item_101",
            payload = json.parseToJsonElement("""{"idempotency_key":"idem_item_101","data":{"id":"item_101"}}"""),
            createdAt = System.currentTimeMillis()
        )

        val batchRequest = BatchSyncRequest(events = listOf(event1, event2))
        val serializedJson = json.encodeToString(BatchSyncRequest.serializer(), batchRequest)

        assertTrue(serializedJson.contains(eventId1))
        assertTrue(serializedJson.contains(eventId2))
        assertTrue(serializedJson.contains("idem_inv_100"))
        assertTrue(serializedJson.contains("idem_item_101"))

        val deserialized = json.decodeFromString(BatchSyncRequest.serializer(), serializedJson)
        assertEquals(2, deserialized.events.size)
        assertEquals("invoices", deserialized.events[0].tableName)
        assertEquals("invoice_items", deserialized.events[1].tableName)
    }

    @Test
    fun testOutboxChronologicalOrdering() {
        val now = System.currentTimeMillis()
        val eventList = listOf(
            SyncOutbox("e3", "inv_3", "invoices", "INSERT", "{}", now + 2000L),
            SyncOutbox("e1", "inv_1", "invoices", "INSERT", "{}", now),
            SyncOutbox("e2", "inv_2", "invoices", "INSERT", "{}", now + 1000L)
        )

        val sorted = eventList.sortedBy { it.createdAt }

        assertEquals("e1", sorted[0].eventId)
        assertEquals("e2", sorted[1].eventId)
        assertEquals("e3", sorted[2].eventId)
    }

    @Test
    fun testSimulatedAtomicInvoiceAndOutboxWrites() {
        val outbox = mutableListOf<SyncOutbox>()
        val invoices = mutableListOf<InvoiceEntity>()
        val invoiceItems = mutableListOf<InvoiceItemEntity>()

        val invoice = InvoiceEntity(
            id = "inv_atomic_01",
            businessId = "biz_test",
            invoiceNo = "2026-27/INV/001",
            date = System.currentTimeMillis(),
            total = 100000L
        )

        val item1 = InvoiceItemEntity(
            id = "item_a1",
            invoiceId = invoice.id,
            itemName = "Widget A",
            qty = 1.0,
            rate = 50000L,
            amount = 50000L
        )

        val item2 = InvoiceItemEntity(
            id = "item_a2",
            invoiceId = invoice.id,
            itemName = "Widget B",
            qty = 1.0,
            rate = 50000L,
            amount = 50000L
        )

        // Simulated @Transaction execution
        invoices.add(invoice)
        invoiceItems.add(item1)
        invoiceItems.add(item2)

        val invPayload = OutboxPayload(
            idempotencyKey = "inv_${invoice.id}_${invoice.updatedAt}",
            entityId = invoice.id,
            operation = "INSERT",
            data = invoice.toPayload()
        )
        val invEvent = SyncOutbox(
            eventId = "evt_inv_01",
            entityId = invoice.id,
            tableName = "invoices",
            operationType = "INSERT",
            payload = json.encodeToString(OutboxPayload.serializer(InvoicePayload.serializer()), invPayload)
        )

        val itemEvents = listOf(item1, item2).mapIndexed { idx, item ->
            val payload = OutboxPayload(
                idempotencyKey = "item_${item.id}_${invoice.updatedAt}",
                entityId = item.id,
                operation = "INSERT",
                data = item.toPayload()
            )
            SyncOutbox(
                eventId = "evt_item_$idx",
                entityId = item.id,
                tableName = "invoice_items",
                operationType = "INSERT",
                payload = json.encodeToString(OutboxPayload.serializer(InvoiceItemPayload.serializer()), payload)
            )
        }

        outbox.add(invEvent)
        outbox.addAll(itemEvents)

        assertEquals(1, invoices.size)
        assertEquals(2, invoiceItems.size)
        assertEquals(3, outbox.size)

        assertEquals("invoices", outbox[0].tableName)
        assertEquals("invoice_items", outbox[1].tableName)
        assertEquals("invoice_items", outbox[2].tableName)
    }

    @Test
    fun testOutboxPurgeOnlyOnHttpSuccess() {
        val outbox = mutableListOf(
            SyncOutbox("e1", "inv_1", "invoices", "INSERT", "{}", System.currentTimeMillis()),
            SyncOutbox("e2", "inv_2", "invoices", "INSERT", "{}", System.currentTimeMillis())
        )

        fun processBatchSync(httpStatusCode: Int) {
            if (httpStatusCode in 200..299) {
                outbox.clear()
            }
        }

        // 1. First attempt fails with 500 internal server error
        processBatchSync(500)
        assertEquals(2, outbox.size) // Retained for exponential retry!

        // 2. Second attempt succeeds with 200 OK
        processBatchSync(200)
        assertEquals(0, outbox.size) // Successfully purged!
    }

    @Test
    fun testSingleFlightSyncMutexLock() = runBlocking {
        val mutex = Mutex()
        var activeRuns = 0
        var maxConcurrent = 0

        suspend fun runSyncJob() {
            if (mutex.isLocked) return
            mutex.withLock {
                activeRuns++
                if (activeRuns > maxConcurrent) maxConcurrent = activeRuns
                kotlinx.coroutines.delay(20)
                activeRuns--
            }
        }

        val j1 = launch { runSyncJob() }
        val j2 = launch { runSyncJob() }
        joinAll(j1, j2)

        assertEquals(1, maxConcurrent)
    }
}
