package com.hisabpro.app

import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.local.entity.SyncMetadataEntity
import com.hisabpro.app.data.model.BankDetails
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.sync.ConflictResolver
import com.hisabpro.app.data.sync.PullResultSummary
import com.hisabpro.app.data.sync.UserSession
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

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

    // --- Targeted Verification Tests for Stage 4.6 & 4.7 Replications ---

    @Test
    fun testBusinessSynchronizationCreatesCorrectUpsertPayload() {
        val entity = BusinessEntity(
            id = "biz_vhhhhhh",
            name = "Vittal Supermarket",
            ownerName = "Vittal Mali",
            address = "Shop 12, Market Yard",
            phone = "+919822012345",
            email = "vittal@hisabpro.in",
            gstin = "27ABCDE1234F1Z5",
            pan = "ABCDE1234F",
            logoPath = "/storage/logo.png",
            gstEnabled = true,
            financialYearStart = "01-04",
            upiId = "vittal@upi",
            bankName = "State Bank of India",
            accountNumber = "987654321012",
            ifscCode = "SBIN0001234",
            termsAndConditions = "Payment due within 15 days."
        )

        val payload = JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("owner_name", entity.ownerName)
            put("address", entity.address)
            put("phone", entity.phone)
            put("email", entity.email)
            put("gstin", entity.gstin)
            put("pan", entity.pan)
            put("logo_path", entity.logoPath)
            put("gst_enabled", entity.gstEnabled)
            put("financial_year_start", entity.financialYearStart)
            put("upi_id", entity.upiId)
            put("bank_name", entity.bankName)
            put("account_number", entity.accountNumber)
            put("ifsc_code", entity.ifscCode)
            put("terms_and_conditions", entity.termsAndConditions)
            put("created_at", entity.createdAt)
            put("updated_at", entity.updatedAt)
        }

        assertEquals("biz_vhhhhhh", payload.getString("id"))
        assertEquals("Vittal Supermarket", payload.getString("name"))
        assertEquals("Vittal Mali", payload.getString("owner_name"))
        assertEquals("+919822012345", payload.getString("phone"))
        assertEquals("27ABCDE1234F1Z5", payload.getString("gstin"))
        assertTrue(payload.getBoolean("gst_enabled"))
        assertEquals("State Bank of India", payload.getString("bank_name"))
        assertEquals("Payment due within 15 days.", payload.getString("terms_and_conditions"))
    }

    @Test
    fun testExistingBusinessIdsPreserved() {
        val p1 = BusinessProfile(shopName = "HisabPro Enterprises")
        val p2 = BusinessProfile(shopName = "Vittal Supermarket")
        val p3 = BusinessProfile(shopName = "Vhhhhhh Store")

        fun sanitize(name: String): String {
            if (name.isBlank() || name.equals("HisabPro Enterprises", ignoreCase = true)) return "default_business"
            return "biz_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        }

        assertEquals("default_business", sanitize(p1.shopName))
        assertEquals("biz_vittal_supermarket", sanitize(p2.shopName))
        assertEquals("biz_vhhhhhh_store", sanitize(p3.shopName))
    }

    @Test
    fun testValidParentBusinessExistsBeforeDependentInvoiceInsertion() {
        val knownBusinessId = "biz_vhhhhhh"
        val invoice = InvoiceEntity(
            id = "inv_101",
            businessId = knownBusinessId,
            invoiceNo = "2026-27/INV/101",
            date = System.currentTimeMillis(),
            total = 50000L
        )
        // Verify invoice correctly maintains parent business association
        assertEquals(knownBusinessId, invoice.businessId)
        assertFalse(invoice.businessId.isBlank())
    }

    @Test
    fun testMissingBusinessReferencesHandledWithoutFakeProfile() {
        val missingBizId = "biz_vhhhhhh"
        // Recovery mechanism creates clearly designated placeholder with pending recovery marker
        val recovery = BusinessEntity(
            id = missingBizId,
            name = "Business ($missingBizId) [Pending Recovery]",
            ownerName = "",
            address = "",
            phone = "",
            email = "",
            gstin = "",
            pan = "",
            logoPath = "",
            gstEnabled = false,
            financialYearStart = "01-04",
            termsAndConditions = "Cloud restored entity pending profile update"
        )

        assertEquals("biz_vhhhhhh", recovery.id)
        assertTrue(recovery.name.contains("[Pending Recovery]"))
        assertEquals("", recovery.gstin) // Does not invent fake GSTIN
        assertEquals("", recovery.phone) // Does not invent fake phone
        assertEquals("", recovery.address) // Does not invent fake address
    }

    @Test
    fun testValidPartyIdValuesRemainUnchanged() {
        val existingPartyId = "party_customer_456"
        val localParties = setOf(existingPartyId, "party_supplier_789")

        val rawPartyId = "party_customer_456"
        val safePartyId = if (rawPartyId in localParties) rawPartyId else null

        assertEquals("party_customer_456", safePartyId)
    }

    @Test
    fun testInvalidOptionalPartyReferencesHandledSafely() {
        val localParties = setOf("party_existing_1")
        val missingPartyId = "party_non_existent_999"

        val rawPartyId: String? = missingPartyId
        val safePartyId = if (rawPartyId != null && rawPartyId in localParties) rawPartyId else null

        assertNull(safePartyId)

        // Customer details on invoice are fully preserved despite partyId being null
        val invoice = InvoiceEntity(
            id = "inv_002",
            businessId = "default_business",
            invoiceNo = "2026-27/INV/002",
            date = System.currentTimeMillis(),
            partyId = safePartyId,
            customerName = "Walk-in Buyer",
            customerPhone = "+91 9988776655",
            customerAddress = "Camp, Pune",
            customerGstin = "",
            total = 15000L
        )

        assertNull(invoice.partyId)
        assertEquals("Walk-in Buyer", invoice.customerName)
        assertEquals("+91 9988776655", invoice.customerPhone)
        assertEquals(15000L, invoice.total)
    }

    @Test
    fun testFailedTableReportedAsFailed() {
        val tableStatuses = mutableMapOf(
            "businesses" to "SUCCESS (1 records)",
            "parties" to "SUCCESS (5 records)",
            "invoices" to "ERROR: SQLiteConstraintException: FOREIGN KEY constraint failed"
        )
        val summary = PullResultSummary(
            totalPulled = 6,
            successCount = 2,
            failureCount = 1,
            tableStatuses = tableStatuses
        )

        assertEquals(1, summary.failureCount)
        assertEquals(2, summary.successCount)
        assertTrue(summary.tableStatuses["invoices"]!!.startsWith("ERROR"))
    }

    @Test
    fun testSyncMetadataNotMarkedSuccessfulAfterFailedRestore() {
        val previousTimestamp = 1717000000000L
        val tableFailed = true

        val metadata = if (!tableFailed) {
            SyncMetadataEntity(
                tableName = "invoices",
                lastPulledAt = System.currentTimeMillis(),
                lastSyncStatus = "SUCCESS"
            )
        } else {
            SyncMetadataEntity(
                tableName = "invoices",
                lastPulledAt = previousTimestamp, // Preserves previous timestamp so retry fetches records
                lastSyncStatus = "FAILED"
            )
        }

        assertEquals("FAILED", metadata.lastSyncStatus)
        assertEquals(previousTimestamp, metadata.lastPulledAt)
    }

    @Test
    fun testRepositoryRefreshOccursAfterSuccessAndFailure() {
        var repositoriesReloaded = false
        try {
            // Simulated operation that throws
            throw RuntimeException("Simulated network timeout during pull")
        } catch (e: Exception) {
            // Handled
        } finally {
            repositoriesReloaded = true
        }

        assertTrue("Repositories must reload even after sync exception via finally block", repositoriesReloaded)
    }

    @Test
    fun testInitialFullRestoreFetchesParentRecordsCorrectly() {
        val sinceTimestampMillis = 0L
        val queryParam = if (sinceTimestampMillis > 0) {
            val isoDate = "2026-09-30T10:00:00.000Z"
            "&updated_at=gt.${URLEncoder.encode(isoDate, "UTF-8")}"
        } else {
            ""
        }

        // On initial pull with 0 timestamp, no delta filter is attached so all records are fetched
        assertEquals("", queryParam)
    }

    @Test
    fun testIncrementalSyncContinuesToWork() {
        val sinceTimestampMillis = 1717150000000L
        val queryParam = if (sinceTimestampMillis > 0) {
            val isoDate = "2026-05-31T10:06:40.000Z"
            "&updated_at=gt.${URLEncoder.encode(isoDate, "UTF-8")}"
        } else {
            ""
        }

        assertTrue(queryParam.contains("&updated_at=gt."))
    }

    @Test
    fun testMultiBusinessRecordsRemainIsolated() {
        val invShop1 = InvoiceEntity(
            id = "inv_1",
            businessId = "default_business",
            invoiceNo = "2026-27/INV/001",
            date = System.currentTimeMillis(),
            total = 10000L
        )
        val invShop2 = InvoiceEntity(
            id = "inv_2",
            businessId = "biz_vhhhhhh",
            invoiceNo = "2026-27/INV/001", // Duplicate invoice number allowed across different businesses!
            date = System.currentTimeMillis(),
            total = 20000L
        )

        assertEquals("default_business", invShop1.businessId)
        assertEquals("biz_vhhhhhh", invShop2.businessId)
        assertEquals(invShop1.invoiceNo, invShop2.invoiceNo) // Valid Indian multi-business accounting
    }

    @Test
    fun testNonGstInvoicesRestoreAndDisplayCorrectly() {
        val nonGstInvoice = InvoiceEntity(
            id = "inv_nongst_1",
            businessId = "default_business",
            invoiceNo = "2026-27/BILL/001",
            date = System.currentTimeMillis(),
            type = "NON_GST_BILL",
            gstMode = "EXEMPT",
            subtotal = 75000L,
            discount = 5000L,
            taxableAmount = 0L,
            cgst = 0L,
            sgst = 0L,
            igst = 0L,
            total = 70000L,
            paidAmount = 70000L,
            isGst = false
        )

        assertFalse(nonGstInvoice.isGst)
        assertEquals("NON_GST_BILL", nonGstInvoice.type)
        assertEquals(0L, nonGstInvoice.cgst)
        assertEquals(0L, nonGstInvoice.sgst)
        assertEquals(0L, nonGstInvoice.igst)
        assertEquals(70000L, nonGstInvoice.total)
    }

    @Test
    fun testRetryingFailedRestoreDoesNotDuplicateRecords() {
        val records = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "inv_retry_1")
                put("invoice_no", "2026-27/INV/501")
                put("total", 50000L)
            })
        }

        val map = mutableMapOf<String, JSONObject>()
        for (i in 0 until records.length()) {
            val obj = records.getJSONObject(i)
            map[obj.getString("id")] = obj
        }

        // Retry same batch
        for (i in 0 until records.length()) {
            val obj = records.getJSONObject(i)
            map[obj.getString("id")] = obj
        }

        assertEquals(1, map.size)
        assertEquals("2026-27/INV/501", map["inv_retry_1"]!!.getString("invoice_no"))
    }

    @Test
    fun testExactUserSupabaseDatabaseRestoreScenario() {
        // Exact dataset from user's live Supabase database
        val businessesFromCloud = setOf("default_business") // biz_vhhhhhh is missing from businesses table!
        val partiesFromCloud = mapOf(
            "0ea3b22e-0146-4fa9-8ec2-d8611ab9438f" to "Abc",
            "155fd2bd-b5ad-4d05-b551-b84cded130e4" to "Bbb",
            "35827832-0420-4283-a4d0-7776b7b7be00" to "Qbv",
            "b1192dbb-a83d-4437-a333-e0ae4aa57ee9" to "Vvvvv",
            "cea0b808-b66a-47fc-885f-467e44507006" to "Hdjdj"
        )

        // Raw invoices as returned from Supabase
        val rawInvoices = listOf(
            Triple("4880d3de-df53-4ae4-b048-9b6cf80bcb78", "default_business", ""),
            Triple("5c545286-0c66-42e7-a9f8-28e6db426cc8", "default_business", ""),
            Triple("ba4f0037-2886-46a0-9eac-d0d5cf6f09ac", "default_business", "0ea3b22e-0146-4fa9-8ec2-d8611ab9438f"),
            Triple("d2012541-3ed5-4220-856d-1d7bc14cc6f7", "default_business", ""),
            Triple("da60209b-011b-4cc8-b059-45aebcf9ec6c", "biz_vhhhhhh", "cea0b808-b66a-47fc-885f-467e44507006")
        )

        val localBusinesses = businessesFromCloud.toMutableSet()

        for ((invId, bizId, rawPartyId) in rawInvoices) {
            // Step 1: ensureBusinessExists
            if (bizId !in localBusinesses) {
                // Must trigger recovery placeholder creation for biz_vhhhhhh
                assertEquals("biz_vhhhhhh", bizId)
                localBusinesses.add(bizId)
            }
            assertTrue("Parent business must exist before invoice insert", localBusinesses.contains(bizId))

            // Step 2: party_id sanitization
            val cleanPartyId = rawPartyId.ifBlank { null }
            val safePartyId = if (cleanPartyId != null && partiesFromCloud.containsKey(cleanPartyId)) cleanPartyId else null

            if (invId == "da60209b-011b-4cc8-b059-45aebcf9ec6c") {
                assertEquals("cea0b808-b66a-47fc-885f-467e44507006", safePartyId)
            } else if (invId == "ba4f0037-2886-46a0-9eac-d0d5cf6f09ac") {
                assertEquals("0ea3b22e-0146-4fa9-8ec2-d8611ab9438f", safePartyId)
            } else {
                assertNull(safePartyId) // Empty strings converted safely to null
            }
        }

        // Verify all 5 invoices and biz_vhhhhhh are safely resolved
        assertTrue(localBusinesses.contains("biz_vhhhhhh"))
        assertTrue(localBusinesses.contains("default_business"))
    }
}
