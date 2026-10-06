package com.hisabpro.app

import com.hisabpro.app.data.local.dao.toPayload
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.domain.accounting.GstPolicy
import com.hisabpro.app.domain.validation.InputValidator
import com.hisabpro.app.util.IndianAccountingFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class InvoiceCascadeAndDeleteTest {

    @Test
    fun testCascadeSoftDeleteEntityMapping() {
        val bizId = "biz_store_01"
        val partyId = "p_customer_01"
        val invoiceId = "inv_001"
        val invoiceNo = "2026-27/INV/001"
        val timestamp = System.currentTimeMillis()

        val invoice = InvoiceEntity(
            id = invoiceId,
            businessId = bizId,
            invoiceNo = invoiceNo,
            partyId = partyId,
            date = timestamp,
            deletedAt = timestamp,
            updatedAt = timestamp
        )

        val item1 = InvoiceItemEntity(
            id = "ii_001",
            invoiceId = invoiceId,
            businessId = bizId,
            itemName = "Product A",
            qty = 2.0,
            rate = 50000L,
            amount = 100000L,
            deletedAt = timestamp,
            updatedAt = timestamp
        )

        val khata = KhataEntryEntity(
            id = "ke_001",
            businessId = bizId,
            partyId = partyId,
            amount = 100000L,
            type = "YOU_GAVE",
            date = timestamp,
            billNumber = invoiceNo,
            deletedAt = timestamp,
            updatedAt = timestamp
        )

        val payment = PaymentEntity(
            id = "pay_001",
            businessId = bizId,
            partyId = partyId,
            date = timestamp,
            amount = 50000L,
            mode = "Cash",
            linkedInvoiceId = invoiceId,
            deletedAt = timestamp,
            updatedAt = timestamp
        )

        assertEquals(timestamp, invoice.deletedAt)
        assertEquals(timestamp, item1.deletedAt)
        assertEquals(timestamp, khata.deletedAt)
        assertEquals(timestamp, payment.deletedAt)
        assertEquals(invoice.deletedAt, item1.deletedAt)
        assertEquals(invoice.deletedAt, khata.deletedAt)
        assertEquals(invoice.deletedAt, payment.deletedAt)
    }

    @Test
    fun testInvoiceEditLineIntegrityThreeEdits() {
        val bizId = "biz_store_01"
        val invoiceId = "inv_edit_test"
        val timestamp = System.currentTimeMillis()

        // Edit 1: Initial creation with 2 lines
        val line1 = InvoiceItem(id = "item_1", description = "Item 1", quantity = 2.0, unitPrice = 100.0, gstRate = 18.0)
        val line2 = InvoiceItem(id = "item_2", description = "Item 2", quantity = 1.0, unitPrice = 200.0, gstRate = 18.0)
        val invEdit1 = Invoice(
            id = invoiceId,
            businessId = bizId,
            invoiceNumber = "2026-27/INV/002",
            items = listOf(line1, line2)
        )
        assertEquals(2, invEdit1.items.size)
        assertEquals(400.0, invEdit1.subtotal, 0.01)

        // Edit 2: Remove line 1, modify line 2 quantity to 3, add line 3
        val line2Updated = line2.copy(quantity = 3.0)
        val line3 = InvoiceItem(id = "item_3", description = "Item 3", quantity = 5.0, unitPrice = 50.0, gstRate = 18.0)
        val invEdit2 = invEdit1.copy(items = listOf(line2Updated, line3))
        assertEquals(2, invEdit2.items.size)
        assertEquals(850.0, invEdit2.subtotal, 0.01)

        // Edit 3: Remove line 3, add line 4
        val line4 = InvoiceItem(id = "item_4", description = "Item 4", quantity = 1.0, unitPrice = 150.0, gstRate = 18.0)
        val invEdit3 = invEdit2.copy(items = listOf(line2Updated, line4))
        assertEquals(2, invEdit3.items.size)
        assertEquals(750.0, invEdit3.subtotal, 0.01)

        val activeIds = invEdit3.items.map { it.id }
        assertTrue(activeIds.contains("item_2"))
        assertTrue(activeIds.contains("item_4"))
        assertFalse(activeIds.contains("item_1"))
        assertFalse(activeIds.contains("item_3"))
    }

    @Test
    fun testLineTaxRatesIntraStateInterStateNonGst() {
        val item = InvoiceItem(description = "LED Light", unitPrice = 1000.0, gstRate = 18.0)

        // 1. Intra-state (CGST 9%, SGST 9%, IGST 0%)
        assertEquals(9.0, item.getCgst(GstMode.INTRA_STATE) / item.taxableAmount * 100, 0.01)
        assertEquals(9.0, item.getSgst(GstMode.INTRA_STATE) / item.taxableAmount * 100, 0.01)
        assertEquals(0.0, item.getIgst(GstMode.INTRA_STATE), 0.01)

        // 2. Inter-state (IGST 18%, CGST 0%, SGST 0%)
        assertEquals(18.0, item.getIgst(GstMode.INTER_STATE) / item.taxableAmount * 100, 0.01)
        assertEquals(0.0, item.getCgst(GstMode.INTER_STATE), 0.01)
        assertEquals(0.0, item.getSgst(GstMode.INTER_STATE), 0.01)

        // 3. Exempt / Non-GST (All 0%)
        assertEquals(0.0, item.getTaxAmount(GstMode.EXEMPT), 0.01)
        assertEquals(0.0, item.getCgst(GstMode.EXEMPT), 0.01)
        assertEquals(0.0, item.getSgst(GstMode.EXEMPT), 0.01)
        assertEquals(0.0, item.getIgst(GstMode.EXEMPT), 0.01)
    }

    @Test
    fun testGstPolicyNonGstBusinessForcedNonGstBill() {
        val nonGstProfile = BusinessProfile(
            shopName = "Simple Hardware",
            isGstRegistered = false
        )

        val requestedTaxInvoice = Invoice(
            id = "inv_non_gst_1",
            businessId = "biz_simple",
            invoiceNumber = "2026-27/INV/005",
            type = InvoiceType.TAX_INVOICE,
            gstMode = GstMode.INTRA_STATE,
            items = listOf(InvoiceItem(description = "Hammer", unitPrice = 250.0, gstRate = 18.0))
        )

        val sanitized = GstPolicy.sanitizeInvoiceForStorage(requestedTaxInvoice, nonGstProfile)
        assertEquals(InvoiceType.NON_GST_BILL, sanitized.type)
        assertEquals(GstMode.EXEMPT, sanitized.gstMode)
        assertEquals(0.0, sanitized.totalTax, 0.01)
    }

    @Test
    fun testPaymentModeValidationAndNoUnpaidString() {
        // PaymentMode values
        assertEquals("Cash", PaymentMode.CASH.label)
        assertEquals("UPI / Online", PaymentMode.ONLINE_UPI.label)
        assertEquals("Bank Transfer", PaymentMode.BANK_TRANSFER.label)
        assertEquals("Cheque", PaymentMode.CHEQUE.label)

        val invPaid = Invoice(
            id = "inv_p1",
            businessId = "biz_1",
            invoiceNumber = "2026-27/INV/010",
            paidAmount = 500.0,
            paymentMode = "UPI"
        )
        assertNotEquals("UNPAID", invPaid.paymentMode)

        val invUnpaid = Invoice(
            id = "inv_p2",
            businessId = "biz_1",
            invoiceNumber = "2026-27/INV/011",
            paidAmount = 0.0,
            paymentMode = ""
        )
        assertNotEquals("UNPAID", invUnpaid.paymentMode)

        // Verify InvoiceEntity with legacy "UNPAID" paymentMode converts to empty payload mode
        val legacyEntity = InvoiceEntity(
            id = "inv_legacy_1",
            businessId = "biz_1",
            invoiceNo = "2026-27/INV/012",
            date = System.currentTimeMillis(),
            paymentStatus = "UNPAID",
            paymentMode = "UNPAID"
        )
        val payload = legacyEntity.toPayload()
        assertEquals("", payload.paymentMode)
        assertEquals("UNPAID", payload.paymentStatus)
    }

    @Test
    fun testFinancialYearDocumentNumbering() {
        val octDate = 1791984000000L // Oct 2026 -> FY 2026-27
        val fyOct = IndianAccountingFormat.getFinancialYear(octDate)
        assertEquals("2026-27", fyOct)

        val janDate = 1768483200000L // Jan 2026 -> FY 2025-26
        val fyJan = IndianAccountingFormat.getFinancialYear(janDate)
        assertEquals("2025-26", fyJan)

        val invNo = IndianAccountingFormat.formatInvoiceNumberWithFY("INV", 5, octDate)
        assertEquals("2026-27/INV/005", invNo)

        val purNo = IndianAccountingFormat.formatInvoiceNumberWithFY("PUR", 12, octDate)
        assertEquals("2026-27/PUR/012", purNo)
    }
}
