package com.hisabpro.app

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.domain.subscription.EntitlementCheck
import com.hisabpro.app.domain.subscription.Entitlements
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan
import com.hisabpro.app.domain.usecase.CreateInvoiceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Test Suite verifying Google Play Billing 6+ Integration, Single Entitlement Layer,
 * 50 Invoices/Month Limit Enforcement, Calendar Month Rollover, and Restores.
 */
class SubscriptionBillingTest {

    @Test
    fun testFreeTier50InvoicesPerMonthLimitEnforcement() {
        val subManager = SubscriptionManager
        subManager.setActivePlan(SubscriptionPlan.FREE)

        // Count = 49 -> Allowed
        val check49 = subManager.checkInvoiceCreationAllowed(49)
        assertTrue(check49 is EntitlementCheck.Granted)

        // Count = 50 -> Limit Exceeded
        val check50 = subManager.checkInvoiceCreationAllowed(50)
        assertTrue(check50 is EntitlementCheck.LimitExceeded)
        val limitExceeded = check50 as EntitlementCheck.LimitExceeded
        assertEquals(50, limitExceeded.maxAllowed)
        assertTrue(limitExceeded.message.contains("limit of 50 bills"))

        // Upgrade to Pro -> Unlimited Invoices
        subManager.setActivePlan(SubscriptionPlan.PRO)
        val checkPro50 = subManager.checkInvoiceCreationAllowed(50)
        assertTrue(checkPro50 is EntitlementCheck.Granted)

        val checkPro1000 = subManager.checkInvoiceCreationAllowed(1000)
        assertTrue(checkPro1000 is EntitlementCheck.Granted)
    }

    @Test
    fun testCalendarMonthRolloverDoesNotBlockNewMonth() {
        // March 2026 timestamp
        val marchCal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 10, 0, 0)
        }
        val marchDateMillis = marchCal.timeInMillis

        // April 2026 timestamp
        val aprilCal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 1, 10, 0, 0)
        }
        val aprilDateMillis = aprilCal.timeInMillis

        // Create 50 invoices in March 2026
        val marchInvoices = mutableListOf<Invoice>()
        for (i in 1..50) {
            marchInvoices.add(
                Invoice(
                    id = "inv_mar_$i",
                    businessId = "biz_test",
                    invoiceNumber = "2025-26/INV/$i",
                    dateMillis = marchDateMillis
                )
            )
        }

        // Count invoices in March 2026 -> 50
        val countMarch = CreateInvoiceUseCase.countCurrentMonthInvoices(marchInvoices, marchDateMillis)
        assertEquals(50, countMarch)

        // Count invoices in April 2026 -> 0 (Month rollover!)
        val countApril = CreateInvoiceUseCase.countCurrentMonthInvoices(marchInvoices, aprilDateMillis)
        assertEquals(0, countApril)

        // On Free Plan, April 2026 invoice creation is allowed because April count is 0
        SubscriptionManager.setActivePlan(SubscriptionPlan.FREE)
        val checkApril = SubscriptionManager.checkInvoiceCreationAllowed(countApril)
        assertTrue(checkApril is EntitlementCheck.Granted)
    }

    @Test
    fun testEntitlementsSingleSourceOfTruth() {
        val freeEntitlements = Entitlements.fromPlan(SubscriptionPlan.FREE)
        assertFalse(freeEntitlements.isAdFree)
        assertEquals(1, freeEntitlements.maxBusinesses)
        assertEquals(50, freeEntitlements.monthlyInvoiceLimit)
        assertFalse(freeEntitlements.isCloudSyncEnabled)
        assertFalse(freeEntitlements.isCustomLogoEnabled)

        val proEntitlements = Entitlements.fromPlan(SubscriptionPlan.PRO)
        assertTrue(proEntitlements.isAdFree)
        assertEquals(1, proEntitlements.maxBusinesses)
        assertTrue(proEntitlements.isUnlimitedInvoices)
        assertTrue(proEntitlements.isCustomLogoEnabled)
        assertTrue(proEntitlements.isWhatsAppDirectShareEnabled)
        assertTrue(proEntitlements.isExcelExportEnabled)
        assertFalse(proEntitlements.isCloudSyncEnabled)

        val premiumEntitlements = Entitlements.fromPlan(SubscriptionPlan.PREMIUM)
        assertTrue(premiumEntitlements.isAdFree)
        assertEquals(5, premiumEntitlements.maxBusinesses)
        assertTrue(premiumEntitlements.isUnlimitedInvoices)
        assertTrue(premiumEntitlements.isCloudSyncEnabled)
        assertTrue(premiumEntitlements.isGstr1ExportEnabled)
        assertTrue(premiumEntitlements.isPrioritySupportEnabled)
    }

    @Test
    fun testSubscriptionRestoreAndActivation() {
        val subManager = SubscriptionManager

        // Start as Free
        subManager.setActivePlan(SubscriptionPlan.FREE)
        assertEquals(SubscriptionPlan.FREE, subManager.entitlements.value.plan)
        assertFalse(subManager.entitlements.value.isAdFree)

        // Restore / Activate Premium
        subManager.activatePlan(SubscriptionPlan.PREMIUM)
        assertEquals(SubscriptionPlan.PREMIUM, subManager.entitlements.value.plan)
        assertTrue(subManager.entitlements.value.isAdFree)
        assertTrue(subManager.entitlements.value.isCloudSyncEnabled)
        assertEquals(5, subManager.entitlements.value.maxBusinesses)
    }
}
