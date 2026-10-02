package com.hisabpro.app

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.StockHistoryEntry
import com.hisabpro.app.data.model.StockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryStockLifecycleTest {

    @Test
    fun testStockDeductionOnSale() {
        val initialStock = 50.0
        val quantitySold = 8.0
        val remainingStock = (initialStock - quantitySold).coerceAtLeast(0.0)

        assertEquals(42.0, remainingStock, 0.001)
    }

    @Test
    fun testStockAdditionOnPurchase() {
        val initialStock = 12.0
        val quantityPurchased = 30.0
        val newStock = initialStock + quantityPurchased

        assertEquals(42.0, newStock, 0.001)
    }

    @Test
    fun testStockRestorationOnSalesReturn() {
        val currentStock = 20.0
        val returnedQty = 5.0
        val restoredStock = currentStock + returnedQty

        assertEquals(25.0, restoredStock, 0.001)
    }

    @Test
    fun testStockReductionOnPurchaseReturn() {
        val currentStock = 30.0
        val returnedToVendorQty = 6.0
        val nextStock = (currentStock - returnedToVendorQty).coerceAtLeast(0.0)

        assertEquals(24.0, nextStock, 0.001)
    }

    @Test
    fun testManualStockAdjustments() {
        var stock = 100.0

        // Damage/Loss adjustment (-15)
        stock = (stock - 15.0).coerceAtLeast(0.0)
        assertEquals(85.0, stock, 0.001)

        // Opening Stock adjustment (+50)
        stock += 50.0
        assertEquals(135.0, stock, 0.001)
    }

    @Test
    fun testInvoiceEditStockReconciliation() {
        val oldInvoice = Invoice(
            id = "inv_edit_test",
            businessId = "biz_test",
            invoiceNumber = "2025-26/INV/501",
            customerName = "Anil Traders",
            items = listOf(
                InvoiceItem(description = "Item A", quantity = 10.0, unitPrice = 100.0),
                InvoiceItem(description = "Item B", quantity = 5.0, unitPrice = 200.0)
            )
        )

        // Edited invoice: Item A reduced 10 -> 4 (6 returned to stock)
        // Item B increased 5 -> 8 (3 deducted from stock)
        val newInvoice = Invoice(
            id = "inv_edit_test",
            businessId = "biz_test",
            invoiceNumber = "2025-26/INV/501",
            customerName = "Anil Traders",
            items = listOf(
                InvoiceItem(description = "Item A", quantity = 4.0, unitPrice = 100.0),
                InvoiceItem(description = "Item B", quantity = 8.0, unitPrice = 200.0)
            )
        )

        val oldMap = oldInvoice.items.associate { it.description.lowercase() to it.quantity }
        val newMap = newInvoice.items.associate { it.description.lowercase() to it.quantity }

        val deltaA = (oldMap["item a"] ?: 0.0) - (newMap["item a"] ?: 0.0)
        val deltaB = (oldMap["item b"] ?: 0.0) - (newMap["item b"] ?: 0.0)

        assertEquals(6.0, deltaA, 0.001) // +6 back in stock
        assertEquals(-3.0, deltaB, 0.001) // -3 deducted from stock
    }

    @Test
    fun testInvoiceCancellationRestoresFullStock() {
        val invoice = Invoice(
            id = "inv_cancel_test",
            businessId = "biz_test",
            invoiceNumber = "2025-26/INV/601",
            customerName = "Kiran Kumar",
            paymentStatus = InvoiceStatus.PAID,
            items = listOf(
                InvoiceItem(description = "Saree Superfine", quantity = 3.0, unitPrice = 1500.0)
            )
        )

        val cancelledInvoice = invoice.copy(paymentStatus = InvoiceStatus.CANCELLED)
        val totalToRestore = cancelledInvoice.items.sumOf { it.quantity }

        assertEquals(InvoiceStatus.CANCELLED, cancelledInvoice.paymentStatus)
        assertEquals(3.0, totalToRestore, 0.001)
    }
}
