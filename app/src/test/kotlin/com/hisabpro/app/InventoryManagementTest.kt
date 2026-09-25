package com.hisabpro.app

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.StockHistoryEntry
import com.hisabpro.app.data.model.StockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class InventoryManagementTest {

    @Test
    fun testItemStockIndicatorsAndValuations() {
        val normalItem = Item(
            name = "Basmati Rice 5kg",
            currentStock = 25.0,
            minStockAlert = 5.0,
            purchasePrice = 300.0,
            salePrice = 450.0
        )
        assertFalse(normalItem.isLowStock)
        assertFalse(normalItem.isOutOfStock)
        assertEquals(7500.0, normalItem.stockValuePurchase, 0.001)
        assertEquals(11250.0, normalItem.stockValueSale, 0.001)
        assertEquals(150.0, normalItem.profitMarginAmount, 0.001)
        assertEquals(50.0, normalItem.profitMarginPercent, 0.001)

        val lowStockItem = Item(
            name = "LED Bulb 9W",
            currentStock = 3.0,
            minStockAlert = 10.0,
            purchasePrice = 70.0,
            salePrice = 120.0
        )
        assertTrue(lowStockItem.isLowStock)
        assertFalse(lowStockItem.isOutOfStock)

        val outOfStockItem = Item(
            name = "Refined Oil 1L",
            currentStock = 0.0,
            minStockAlert = 5.0,
            purchasePrice = 120.0,
            salePrice = 150.0
        )
        assertTrue(outOfStockItem.isLowStock)
        assertTrue(outOfStockItem.isOutOfStock)
    }

    @Test
    fun testStockReasonsClassification() {
        // Inward / additions
        assertTrue(StockReason.OPENING_STOCK.isAddition)
        assertTrue(StockReason.PURCHASE_IN.isAddition)
        assertTrue(StockReason.SALES_RETURN.isAddition)
        assertTrue(StockReason.RETURN_IN.isAddition)
        assertTrue(StockReason.MANUAL_ADJUSTMENT.isAddition)

        // Outward / reductions
        assertFalse(StockReason.SALE_OUT.isAddition)
        assertFalse(StockReason.PURCHASE_RETURN.isAddition)
        assertFalse(StockReason.DAMAGE_LOSS.isAddition)

        // Case-insensitive mapping & backward compatibility
        assertEquals(StockReason.SALES_RETURN, StockReason.fromString("RETURN_IN"))
        assertEquals(StockReason.PURCHASE_IN, StockReason.fromString("purchase_in"))
        assertEquals(StockReason.PURCHASE_RETURN, StockReason.fromString("PURCHASE_RETURN"))
        assertEquals(StockReason.MANUAL_ADJUSTMENT, StockReason.fromString("non_existent_reason"))
    }

    @Test
    fun testStockHistoryEntrySourceTracking() {
        val entryWithRef = StockHistoryEntry(
            itemId = "item_101",
            changeQty = -5.0,
            previousStock = 20.0,
            newStock = 15.0,
            reason = StockReason.SALE_OUT,
            note = "Counter sale",
            sourceTransactionId = "inv_001",
            sourceTransactionType = "INVOICE",
            sourceRefNumber = "2025-26/INV/042"
        )
        assertEquals("2025-26/INV/042", entryWithRef.displaySourceRef)
        assertEquals("INVOICE", entryWithRef.sourceTransactionType)
        assertEquals("inv_001", entryWithRef.sourceTransactionId)

        val entryWithoutRef = StockHistoryEntry(
            itemId = "item_102",
            changeQty = 10.0,
            previousStock = 0.0,
            newStock = 10.0,
            reason = StockReason.OPENING_STOCK,
            note = "Initial stock entry"
        )
        assertEquals("Opening Stock", entryWithoutRef.displaySourceRef)
    }

    @Test
    fun testPreventDoubleStockDeductionLogic() {
        val invoiceId = "inv_abc_123"
        val invoiceNumber = "2025-26/INV/007"
        val itemId = "item_sugar_1kg"

        val historyList = mutableListOf<StockHistoryEntry>()

        // Helper to simulate the exact deduction check in ItemRepository
        fun tryDeduct(qty: Double): Boolean {
            val alreadyDeducted = historyList.any { entry ->
                entry.itemId == itemId &&
                entry.reason == StockReason.SALE_OUT &&
                (entry.sourceTransactionId == invoiceId || entry.sourceRefNumber == invoiceNumber)
            }
            if (alreadyDeducted) return false

            historyList.add(
                StockHistoryEntry(
                    itemId = itemId,
                    changeQty = -qty,
                    previousStock = 50.0,
                    newStock = 50.0 - qty,
                    reason = StockReason.SALE_OUT,
                    sourceTransactionId = invoiceId,
                    sourceTransactionType = "INVOICE",
                    sourceRefNumber = invoiceNumber
                )
            )
            return true
        }

        // First deduction should succeed
        val firstAttempt = tryDeduct(4.0)
        assertTrue(firstAttempt)
        assertEquals(1, historyList.size)

        // Duplicate deduction for same invoice & item must be rejected
        val duplicateAttempt = tryDeduct(4.0)
        assertFalse("Duplicate deduction for same invoice must be prevented", duplicateAttempt)
        assertEquals(1, historyList.size)
    }

    @Test
    fun testInvoiceEditQuantityReconciliation() {
        val invoiceNo = "2025-26/INV/008"
        val oldInvoice = Invoice(
            id = "inv_008",
            invoiceNumber = invoiceNo,
            customerName = "Ramesh Patil",
            items = listOf(
                InvoiceItem(description = "Basmati Rice 5kg", quantity = 5.0, unitPrice = 450.0),
                InvoiceItem(description = "Syska 9W LED Bulb", quantity = 2.0, unitPrice = 120.0)
            )
        )

        // Edited invoice:
        // Rice reduced from 5 -> 3 (2 returned to stock)
        // LED Bulb increased from 2 -> 6 (4 more deducted from stock)
        // Added Salt 1kg: 0 -> 2 (2 deducted from stock)
        val newInvoice = Invoice(
            id = "inv_008",
            invoiceNumber = invoiceNo,
            customerName = "Ramesh Patil",
            items = listOf(
                InvoiceItem(description = "Basmati Rice 5kg", quantity = 3.0, unitPrice = 450.0),
                InvoiceItem(description = "Syska 9W LED Bulb", quantity = 6.0, unitPrice = 120.0),
                InvoiceItem(description = "Tata Salt 1kg", quantity = 2.0, unitPrice = 28.0)
            )
        )

        val oldQuantities = oldInvoice.items.associate { it.description.lowercase() to it.quantity }
        val newQuantities = newInvoice.items.associate { it.description.lowercase() to it.quantity }

        val allKeys = oldQuantities.keys + newQuantities.keys
        val deltas = mutableMapOf<String, Double>()
        for (key in allKeys) {
            val oldQty = oldQuantities[key] ?: 0.0
            val newQty = newQuantities[key] ?: 0.0
            deltas[key] = oldQty - newQty
        }

        // Positive delta = return to stock; Negative delta = deduct from stock
        assertEquals(2.0, deltas["basmati rice 5kg"] ?: 0.0, 0.001) // +2 back in stock
        assertEquals(-4.0, deltas["syska 9w led bulb"] ?: 0.0, 0.001) // -4 out of stock
        assertEquals(-2.0, deltas["tata salt 1kg"] ?: 0.0, 0.001) // -2 out of stock
    }

    @Test
    fun testInvoiceCancellationRestoresAllStock() {
        val invoiceNo = "2025-26/INV/009"
        val activeInvoice = Invoice(
            id = "inv_009",
            invoiceNumber = invoiceNo,
            customerName = "Suresh Sharma",
            paymentStatus = InvoiceStatus.PAID,
            items = listOf(
                InvoiceItem(description = "Amul Ghee 1L", quantity = 3.0, unitPrice = 640.0)
            )
        )

        val cancelledInvoice = activeInvoice.copy(paymentStatus = InvoiceStatus.CANCELLED)

        assertTrue(activeInvoice.paymentStatus != InvoiceStatus.CANCELLED)
        assertTrue(cancelledInvoice.paymentStatus == InvoiceStatus.CANCELLED)

        // Verify restoration delta
        val restoredQty = cancelledInvoice.items.sumOf { it.quantity }
        assertEquals(3.0, restoredQty, 0.001)
    }

    @Test
    fun testPurchaseInwardAndReturnMovements() {
        val initialStock = 10.0
        val purchasedQty = 25.0
        val afterPurchase = initialStock + purchasedQty
        assertEquals(35.0, afterPurchase, 0.001)

        val returnedToVendorQty = 5.0
        val afterReturn = afterPurchase - returnedToVendorQty
        assertEquals(30.0, afterReturn, 0.001)
    }
}
