package com.hisabpro.app

import com.hisabpro.app.data.backup.BackupManager
import com.hisabpro.app.data.backup.BackupValidationResult
import com.hisabpro.app.util.toPaise
import com.hisabpro.app.util.toRupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseAndBackupTest {

    @Test
    fun testMonetaryPaiseConversions() {
        // Double ₹123.45 -> Long 12345 paise
        assertEquals(12345L, 123.45.toPaise())
        assertEquals(123.45, 12345L.toRupees(), 0.001)

        // Double ₹0.50 -> Long 50 paise
        assertEquals(50L, 0.50.toPaise())
        assertEquals(0.50, 50L.toRupees(), 0.001)

        // Double ₹10000.00 -> Long 1000000 paise
        assertEquals(1000000L, 10000.00.toPaise())
        assertEquals(10000.00, 1000000L.toRupees(), 0.001)
    }

    @Test
    fun testBusinessDataIsolationExplicitBusinessId() {
        val testBusinessId = "biz_test_store"

        // Verify entities carry explicit testBusinessId
        val partyEntity = com.hisabpro.app.data.local.entity.PartyEntity(
            id = "p_iso_1",
            businessId = testBusinessId,
            name = "Isolated Party",
            phone = "9822012345"
        )

        val invoiceEntity = com.hisabpro.app.data.local.entity.InvoiceEntity(
            id = "inv_iso_1",
            businessId = testBusinessId,
            invoiceNo = "2025-26/INV/999",
            date = System.currentTimeMillis()
        )

        assertEquals(testBusinessId, partyEntity.businessId)
        assertEquals(testBusinessId, invoiceEntity.businessId)
    }

    @Test
    fun testValidBackupFileValidation() {
        val json = """
        {
            "backup_version": 1,
            "app_name": "HisabPro",
            "app_version": "1.0.0",
            "created_at_millis": 1727188800000,
            "business_info": {
                "shop_name": "Om Super Market",
                "is_gst_registered": true
            },
            "counts": {
                "businesses": 1,
                "parties": 2,
                "items": 4,
                "invoices": 3,
                "invoice_items": 6,
                "payments": 1,
                "expenses": 1,
                "accounts": 1,
                "journal_entries": 0,
                "journal_lines": 0,
                "khata_entries": 2
            },
            "data": {
                "businesses": [],
                "parties": [],
                "items": [],
                "invoices": [],
                "invoice_items": [],
                "payments": [],
                "expenses": [],
                "accounts": [],
                "journal_entries": [],
                "journal_lines": [],
                "khata_entries": []
            }
        }
        """.trimIndent()

        val result = BackupManager.validateBackupJson(json, "om_market.hisabpro", json.length.toLong())
        assertTrue(result is BackupValidationResult.Valid)
        val valid = result as BackupValidationResult.Valid
        assertEquals("Om Super Market", valid.summary.businessName)
        assertEquals(true, valid.summary.isGst)
        assertEquals(3, valid.summary.counts.invoices)
    }

    @Test
    fun testCorruptedBackupJsonRejection() {
        val brokenJson = "{ backup_version: 1, app_name: 'HisabPro' " // Unclosed JSON

        val result = BackupManager.validateBackupJson(brokenJson, "broken.hisabpro", brokenJson.length.toLong())
        assertTrue(result is BackupValidationResult.Corrupted)
    }

    @Test
    fun testIncompatibleBackupVersionRejection() {
        val futureJson = """
        {
            "backup_version": 999,
            "app_name": "HisabPro",
            "data": {}
        }
        """.trimIndent()

        val result = BackupManager.validateBackupJson(futureJson, "future.hisabpro", futureJson.length.toLong())
        assertTrue(result is BackupValidationResult.IncompatibleVersion)
        val incomp = result as BackupValidationResult.IncompatibleVersion
        assertEquals(999, incomp.foundVersion)
    }

    @Test
    fun testNonHisabProJsonRejection() {
        val foreignJson = """
        {
            "backup_version": 1,
            "app_name": "ForeignApp",
            "data": {}
        }
        """.trimIndent()

        val result = BackupManager.validateBackupJson(foreignJson, "foreign.json", foreignJson.length.toLong())
        assertTrue(result is BackupValidationResult.InvalidFormat)
    }
}
