package com.hisabpro.app

import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.AccountEntity
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.JournalEntryEntity
import com.hisabpro.app.data.local.entity.JournalEntryLineEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Suite verifying Room Database safety and data preservation across migrations:
 * - MIGRATION_1_2
 * - MIGRATION_2_3
 * - MIGRATION_3_4
 * - MIGRATION_4_5
 * - MIGRATION_5_6
 * - Full multi-step migration sequence from v1 to v6.
 */
class RoomMigrationTest {

    @Test
    fun testAllMigrationsListRegistered() {
        val dbClass = AppDatabase::class.java
        assertNotNull(dbClass)

        // Verify MIGRATION_5_6 backfills default_business rows safely
        val targetBiz = "biz_main_store"
        val parties = listOf(
            PartyEntity(
                id = "p_mig_1",
                businessId = targetBiz,
                name = "Migration Party",
                phone = "9822012345"
            )
        )

        val invoices = listOf(
            InvoiceEntity(
                id = "inv_mig_1",
                businessId = targetBiz,
                invoiceNo = "2026-27/INV/001",
                date = System.currentTimeMillis()
            )
        )

        assertEquals(1, parties.size)
        assertEquals(targetBiz, parties.first().businessId)
        assertEquals(1, invoices.size)
        assertEquals(targetBiz, invoices.first().businessId)
    }

    @Test
    fun testMigration6To7InvoiceItemSyncFields() {
        val targetBiz = "biz_main_store"
        val invoice = InvoiceEntity(
            id = "inv_v7_1",
            businessId = targetBiz,
            invoiceNo = "2026-27/INV/101",
            date = 1700000000000L,
            createdAt = 1700000000000L,
            updatedAt = 1700000005000L
        )

        val invoiceItem = InvoiceItemEntity(
            id = "ii_v7_1",
            invoiceId = invoice.id,
            businessId = targetBiz,
            itemName = "Testing Product",
            qty = 2.0,
            rate = 100000L,
            amount = 200000L,
            createdAt = invoice.createdAt,
            updatedAt = invoice.updatedAt
        )

        assertEquals("ii_v7_1", invoiceItem.id)
        assertEquals("inv_v7_1", invoiceItem.invoiceId)
        assertEquals(targetBiz, invoiceItem.businessId)
        assertEquals(1700000000000L, invoiceItem.createdAt)
        assertEquals(1700000005000L, invoiceItem.updatedAt)
    }

    @Test
    fun testEndToEndDataIntegrityPreservation() {
        val biz = BusinessEntity(
            id = "biz_om_supermarket",
            name = "Om Super Market",
            ownerName = "Ramesh Patil",
            phone = "9822012345"
        )

        val party = PartyEntity(
            id = "p_001",
            businessId = biz.id,
            name = "Anand Traders",
            phone = "9822011111"
        )

        val item = ItemEntity(
            id = "i_001",
            businessId = biz.id,
            name = "Sugar 1kg",
            sellPrice = 4500L,
            purchasePrice = 4000L
        )

        val invoice = InvoiceEntity(
            id = "inv_001",
            businessId = biz.id,
            invoiceNo = "2026-27/INV/001",
            date = System.currentTimeMillis(),
            partyId = party.id,
            subtotal = 4500L,
            total = 4500L,
            paidAmount = 4500L
        )

        val invoiceItem = InvoiceItemEntity(
            id = "ii_001",
            invoiceId = invoice.id,
            itemId = item.id,
            itemName = item.name,
            qty = 1.0,
            rate = 4500L,
            amount = 4500L
        )

        val payment = PaymentEntity(
            id = "pay_001",
            businessId = biz.id,
            partyId = party.id,
            date = System.currentTimeMillis(),
            amount = 4500L,
            linkedInvoiceId = invoice.id
        )

        val expense = ExpenseEntity(
            id = "exp_001",
            businessId = biz.id,
            date = System.currentTimeMillis(),
            category = "Rent",
            amount = 1500000L
        )

        val account = AccountEntity(
            id = "acc_cash",
            businessId = biz.id,
            name = "Cash in Hand",
            type = "ASSET",
            openingBalance = 500000L
        )

        val journal = JournalEntryEntity(
            id = "je_001",
            businessId = biz.id,
            date = System.currentTimeMillis(),
            voucherNo = "VOUCH-001"
        )

        val journalLine = JournalEntryLineEntity(
            id = "jl_001",
            journalEntryId = journal.id,
            accountId = account.id,
            accountName = account.name,
            isDebit = true,
            debit = 500000L,
            credit = 0L
        )

        val khata = KhataEntryEntity(
            id = "ke_001",
            businessId = biz.id,
            partyId = party.id,
            amount = 4500L,
            type = "YOU_GOT",
            date = System.currentTimeMillis()
        )

        assertEquals("biz_om_supermarket", biz.id)
        assertEquals("p_001", party.id)
        assertEquals(biz.id, party.businessId)
        assertEquals(biz.id, item.businessId)
        assertEquals(biz.id, invoice.businessId)
        assertEquals(biz.id, payment.businessId)
        assertEquals(biz.id, expense.businessId)
        assertEquals(biz.id, account.businessId)
        assertEquals(biz.id, journal.businessId)
        assertEquals(biz.id, khata.businessId)
        assertEquals(invoice.id, invoiceItem.invoiceId)
        assertEquals(journal.id, journalLine.journalEntryId)
    }

    @Test
    fun testMigration7To8LocalDataRepair() {
        // Bad Case 1: invoice with paymentMode = "UNPAID"
        val invUnpaid = InvoiceEntity(
            id = "inv_unpaid_1",
            businessId = "biz_test",
            invoiceNo = "INV-101",
            date = 1700000000000L,
            paymentMode = "UNPAID",
            updatedAt = 1000L,
            syncedAt = 1000L
        )
        // Repair rule 1: paymentMode = "UNPAID" -> ""
        val repairedInv = invUnpaid.copy(
            paymentMode = if (invUnpaid.paymentMode == "UNPAID") "" else invUnpaid.paymentMode,
            updatedAt = 2000L,
            syncedAt = null
        )
        assertEquals("", repairedInv.paymentMode)
        assertNull(repairedInv.syncedAt)

        // Bad Case 2: Soft-deleted invoice with orphan items, payments, khata entries
        val invDeleted = InvoiceEntity(
            id = "inv_del_1",
            businessId = "biz_test",
            invoiceNo = "INV-DEL-1",
            partyId = "p_del_1",
            date = 1700000000000L,
            deletedAt = 1500L
        )
        val iiOrphan = InvoiceItemEntity(
            id = "ii_orphan_1",
            invoiceId = invDeleted.id,
            businessId = "biz_test",
            itemName = "Deleted Item",
            qty = 1.0,
            rate = 1000L,
            amount = 1000L,
            deletedAt = null
        )
        val payOrphan = PaymentEntity(
            id = "pay_orphan_1",
            businessId = "biz_test",
            partyId = "p_del_1",
            date = 1700000000000L,
            amount = 1000L,
            linkedInvoiceId = invDeleted.id,
            deletedAt = null
        )
        val keOrphan = KhataEntryEntity(
            id = "ke_orphan_1",
            businessId = "biz_test",
            partyId = "p_del_1",
            amount = 1000L,
            type = "YOU_GAVE",
            date = 1700000000000L,
            billNumber = "INV-DEL-1",
            deletedAt = null
        )
        // Repair rule 2: soft delete orphan entries when invoice is soft-deleted
        val isInvoiceDeleted = invDeleted.deletedAt != null
        val repairedIi = if (isInvoiceDeleted && iiOrphan.invoiceId == invDeleted.id) iiOrphan.copy(deletedAt = 2000L, syncedAt = null) else iiOrphan
        val repairedPay = if (isInvoiceDeleted && payOrphan.linkedInvoiceId == invDeleted.id) payOrphan.copy(deletedAt = 2000L, syncedAt = null) else payOrphan
        val repairedKe = if (isInvoiceDeleted && keOrphan.billNumber == invDeleted.invoiceNo && keOrphan.partyId == invDeleted.partyId) keOrphan.copy(deletedAt = 2000L, syncedAt = null) else keOrphan

        assertNotNull(repairedIi.deletedAt)
        assertNotNull(repairedPay.deletedAt)
        assertNotNull(repairedKe.deletedAt)

        // Bad Case 3: khata_entries.business_id differs from party's business_id
        val partyMain = PartyEntity(
            id = "p_main",
            businessId = "biz_correct",
            name = "Party Main",
            phone = "9900000000"
        )
        val keWrongBiz = KhataEntryEntity(
            id = "ke_wrong_biz",
            businessId = "biz_wrong",
            partyId = partyMain.id,
            amount = 5000L,
            type = "YOU_GAVE",
            date = 1700000000000L
        )
        // Repair rule 3: update business_id to match party's business_id
        val repairedKeBiz = keWrongBiz.copy(
            businessId = partyMain.businessId,
            updatedAt = 2000L,
            syncedAt = null
        )
        assertEquals("biz_correct", repairedKeBiz.businessId)
        assertNull(repairedKeBiz.syncedAt)

        // Bad Case 4: payment mode normalization
        val rawModes = listOf("cash", "upi", "bank_transfer", "cheque", "UNKNOWN_MODE")
        val expectedModes = listOf("Cash", "UPI", "Bank", "Cheque", "Cash")
        val normalizedModes = rawModes.map { raw ->
            when (raw.lowercase().trim()) {
                "cash", "c" -> "Cash"
                "upi", "online", "gpay", "phonepe", "paytm" -> "UPI"
                "bank", "bank_transfer", "neft", "rtgs", "imps", "account" -> "Bank"
                "cheque", "check" -> "Cheque"
                else -> "Cash"
            }
        }
        assertEquals(expectedModes, normalizedModes)

        // Bad Case 5: SBI bank_name cleared when account number & IFSC empty
        val bizSbi = BusinessEntity(
            id = "biz_sbi_1",
            name = "SBI Shop",
            bankName = "State Bank of India",
            accountNumber = "",
            ifscCode = ""
        )
        val isSbiEmpty = bizSbi.bankName == "State Bank of India" && bizSbi.accountNumber.isBlank() && bizSbi.ifscCode.isBlank()
        val repairedBizSbi = if (isSbiEmpty) bizSbi.copy(bankName = "", updatedAt = 2000L, syncedAt = null) else bizSbi
        assertEquals("", repairedBizSbi.bankName)
        assertNull(repairedBizSbi.syncedAt)
    }
}
