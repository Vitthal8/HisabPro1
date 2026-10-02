package com.hisabpro.app

import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan
import com.hisabpro.app.util.ThermalSlipGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosAndSubscriptionTest {

    @Test
    fun testEscPosBinaryCommandsGeneration() {
        val invoice = Invoice(
            id = "test_inv_001",
            businessId = "biz_shree_ganesh",
            invoiceNumber = "2025-26/INV/001",
            type = InvoiceType.NON_GST_BILL,
            customerName = "Ramesh Kirana",
            customerPhone = "9876543210",
            items = listOf(
                InvoiceItem(
                    id = "1",
                    description = "Basmati Rice 5kg",
                    quantity = 2.0,
                    unit = "Bag",
                    unitPrice = 450.0,
                    gstRate = 0.0
                )
            ),
            paidAmount = 900.0,
            paymentMode = "Cash"
        )

        val profile = BusinessProfile(
            shopName = "Shree Ganesh Traders",
            phone = "9822012345",
            address = "MG Road, Pune",
            city = "Pune",
            state = "Maharashtra",
            stateCode = "27",
            upiId = "shreeganesh@upi",
            isGstRegistered = false
        )

        val escPosBytes = ThermalSlipGenerator.generateEscPosBytes(
            invoice = invoice,
            profile = profile,
            widthChars = 32,
            kickDrawer = true,
            cutPaper = true
        )

        assertNotNull(escPosBytes)
        assertTrue(escPosBytes.isNotEmpty())

        // Check ESC @ init byte: 0x1B, 0x40
        assertEquals(0x1B.toByte(), escPosBytes[0])
        assertEquals(0x40.toByte(), escPosBytes[1])

        // Verify content contains shop name and invoice number in ISO-8859-1 string representation
        val byteString = String(escPosBytes, Charsets.ISO_8859_1)
        assertTrue(byteString.contains("SHREE GANESH"))
        assertTrue(byteString.contains("2025-26/INV/001"))
        assertTrue(byteString.contains("Basmati Rice"))
    }

    @Test
    fun testSubscriptionEntitlements() {
        SubscriptionManager.setActivePlan(SubscriptionPlan.FREE)
        assertEquals(SubscriptionPlan.FREE, SubscriptionManager.getActivePlan())
        assertFalse(SubscriptionManager.isAdFree)
        assertFalse(SubscriptionManager.canUseCloudSync())
        assertFalse(SubscriptionManager.canExportGstr1())
        assertEquals(1, SubscriptionManager.getMaxBusinesses())

        // 50 bills limit in Free
        val allowedAt49 = SubscriptionManager.checkInvoiceCreationAllowed(49)
        assertTrue(allowedAt49.isGranted)
        val blockedAt50 = SubscriptionManager.checkInvoiceCreationAllowed(50)
        assertFalse(blockedAt50.isGranted)

        // Upgrade to Pro
        SubscriptionManager.setActivePlan(SubscriptionPlan.PRO)
        assertTrue(SubscriptionManager.isAdFree)
        assertTrue(SubscriptionManager.canExportGstr1())
        assertFalse(SubscriptionManager.canUseCloudSync())
        assertTrue(SubscriptionManager.checkInvoiceCreationAllowed(100).isGranted)

        // Upgrade to Premium
        SubscriptionManager.setActivePlan(SubscriptionPlan.PREMIUM)
        assertTrue(SubscriptionManager.isAdFree)
        assertTrue(SubscriptionManager.canExportGstr1())
        assertTrue(SubscriptionManager.canUseCloudSync())
        assertEquals(5, SubscriptionManager.getMaxBusinesses())
        assertTrue(SubscriptionManager.checkBusinessCreationAllowed(4).isGranted)
        assertFalse(SubscriptionManager.checkBusinessCreationAllowed(5).isGranted)

        // Reset to Free
        SubscriptionManager.setActivePlan(SubscriptionPlan.FREE)
    }
}
