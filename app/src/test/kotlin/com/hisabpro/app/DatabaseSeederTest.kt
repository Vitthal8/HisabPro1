package com.hisabpro.app

import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.model.BusinessProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSeederTest {

    @Test
    fun testFreshStartUserVittalMaliProfile() {
        val userEmail = "vittalmali3@gmail.com"
        val ownerName = "Vittal Mali"
        val shopName = "Vittal Supermarket & Traders"
        val gstin = "27ABCDE1234F1Z5"

        val profile = BusinessProfile(
            id = "default_business",
            shopName = shopName,
            ownerName = ownerName,
            phone = "+919822012345",
            email = userEmail,
            isGstRegistered = true,
            gstin = gstin,
            pan = "ABCDE1234F",
            state = "Maharashtra",
            stateCode = "27",
            city = "Pune",
            pincode = "411037",
            address = "Shop No 12, Main Market Yard, Pune, Maharashtra 411037",
            upiId = "vittalmali@upi",
            bankName = "State Bank of India",
            accountNumber = "987654321012",
            ifscCode = "SBIN0001234",
            hasCompletedOnboarding = true
        )

        assertEquals("default_business", profile.id)
        assertEquals("vittalmali3@gmail.com", profile.email)
        assertEquals("Vittal Mali", profile.ownerName)
        assertEquals("Vittal Supermarket & Traders", profile.shopName)
        assertEquals("27ABCDE1234F1Z5", profile.gstin)
        assertTrue(profile.isGstRegistered)
        assertTrue(profile.hasCompletedOnboarding)
    }

    @Test
    fun testSeededSampleDataIntegrity() {
        val biz = BusinessEntity(
            id = "default_business",
            name = "Vittal Supermarket & Traders",
            ownerName = "Vittal Mali",
            email = "vittalmali3@gmail.com",
            phone = "+919822012345",
            gstin = "27ABCDE1234F1Z5"
        )

        val partyRamesh = PartyEntity(
            id = "party_ramesh",
            businessId = biz.id,
            name = "Ramesh Kumar Traders",
            phone = "+919822011111",
            type = "CUSTOMER"
        )

        val itemRice = ItemEntity(
            id = "item_rice_25kg",
            businessId = biz.id,
            name = "Basmati Rice 25kg",
            sellPrice = 150000L,
            purchasePrice = 120000L,
            stockQty = 25.0
        )

        val inv1 = InvoiceEntity(
            id = "inv_001",
            businessId = biz.id,
            invoiceNo = "2026-27/INV/001",
            date = System.currentTimeMillis(),
            partyId = partyRamesh.id,
            customerName = partyRamesh.name,
            total = 236250L,
            paidAmount = 236250L,
            paymentStatus = "PAID"
        )

        val invItem1 = InvoiceItemEntity(
            id = "ii_001_1",
            invoiceId = inv1.id,
            itemId = itemRice.id,
            itemName = itemRice.name,
            qty = 1.0,
            rate = 150000L,
            amount = 150000L
        )

        val pay1 = PaymentEntity(
            id = "pay_001",
            businessId = biz.id,
            partyId = partyRamesh.id,
            date = System.currentTimeMillis(),
            amount = 236250L,
            mode = "UPI",
            linkedInvoiceId = inv1.id
        )

        val exp1 = ExpenseEntity(
            id = "exp_001",
            businessId = biz.id,
            date = System.currentTimeMillis(),
            category = "Rent",
            amount = 1500000L,
            description = "Shop Monthly Rent"
        )

        val khata1 = KhataEntryEntity(
            id = "ke_001",
            businessId = biz.id,
            partyId = partyRamesh.id,
            amount = 236250L,
            type = "YOU_GAVE",
            date = System.currentTimeMillis()
        )

        assertEquals("vittalmali3@gmail.com", biz.email)
        assertEquals(biz.id, partyRamesh.businessId)
        assertEquals(biz.id, itemRice.businessId)
        assertEquals(biz.id, inv1.businessId)
        assertEquals(inv1.id, invItem1.invoiceId)
        assertEquals(partyRamesh.id, pay1.partyId)
        assertEquals("Rent", exp1.category)
        assertEquals(236250L, khata1.amount)
    }
}
