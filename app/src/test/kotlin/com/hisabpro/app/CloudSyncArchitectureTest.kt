package com.hisabpro.app

import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.sync.ConflictResolver
import com.hisabpro.app.data.sync.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncArchitectureTest {

    @Test
    fun testMasterDataConflictResolution_LWW() {
        val now = System.currentTimeMillis()
        val localParty = PartyEntity(
            id = "party_123",
            name = "Ramesh Kumar",
            phone = "9822011111",
            address = "Pune",
            updatedAt = now - 5000L
        )
        val remoteParty = PartyEntity(
            id = "party_123",
            name = "Ramesh Kumar Sharma",
            phone = "9822099999",
            address = "Shivajinagar Pune",
            updatedAt = now
        )

        val resolved = ConflictResolver.resolveParty(localParty, remoteParty)
        assertEquals("Ramesh Kumar Sharma", resolved.name)
        assertEquals("9822099999", resolved.phone)
    }

    @Test
    fun testTombstoneWinsOverOlderUpdate() {
        val now = System.currentTimeMillis()
        val localItem = ItemEntity(
            id = "item_100",
            name = "Atta 10kg",
            sellPrice = 45000L,
            updatedAt = now - 10000L,
            deletedAt = null
        )
        val remoteItem = ItemEntity(
            id = "item_100",
            name = "Atta 10kg",
            sellPrice = 45000L,
            updatedAt = now - 1000L,
            deletedAt = now - 1000L
        )

        val resolved = ConflictResolver.resolveItem(localItem, remoteItem)
        assertNotNull(resolved.deletedAt)
        assertEquals(now - 1000L, resolved.deletedAt)
    }

    @Test
    fun testInvoiceMonetaryIntegrityPreserved() {
        val now = System.currentTimeMillis()
        val localInvoice = InvoiceEntity(
            id = "inv_001",
            invoiceNo = "2026-27/INV/001",
            date = now,
            subtotal = 100000L, // ₹1,000 in paise
            cgst = 9000L,
            sgst = 9000L,
            total = 118000L,
            paidAmount = 118000L,
            updatedAt = now - 2000L
        )
        val remoteInvoice = InvoiceEntity(
            id = "inv_001",
            invoiceNo = "2026-27/INV/001",
            date = now,
            subtotal = 100000L,
            cgst = 9000L,
            sgst = 9000L,
            total = 118000L,
            paidAmount = 118000L,
            updatedAt = now
        )

        val resolved = ConflictResolver.resolveInvoice(localInvoice, remoteInvoice)
        assertEquals(118000L, resolved.total)
        assertEquals(118000L, resolved.paidAmount)
        assertEquals("2026-27/INV/001", resolved.invoiceNo)
    }

    @Test
    fun testPaymentPreservation() {
        val now = System.currentTimeMillis()
        val localPayment = PaymentEntity(
            id = "pay_001",
            amount = 50000L, // ₹500 in paise
            mode = "UPI",
            referenceNo = "UPI-REF-12345",
            date = now,
            updatedAt = now
        )
        val resolved = ConflictResolver.resolvePayment(null, localPayment)
        assertEquals(50000L, resolved.amount)
        assertEquals("UPI", resolved.mode)
        assertEquals("UPI-REF-12345", resolved.referenceNo)
    }

    @Test
    fun testUserSessionValidation() {
        val session = UserSession(
            userId = "user_uuid_12345",
            email = "shop@hisabpro.in",
            phone = "+919822012345",
            accessToken = "mock_jwt_token",
            expiresAt = System.currentTimeMillis() + 3600000L
        )
        assertEquals("user_uuid_12345", session.userId)
        assertEquals("shop@hisabpro.in", session.email)
        assertEquals("+919822012345", session.phone)
        assertTrue(session.expiresAt > System.currentTimeMillis())
    }
}
