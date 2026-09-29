package com.hisabpro.app

import com.hisabpro.app.ads.AdConfig
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for AdMob configuration and monetization policy rules.
 */
class AdMobIntegrationTest {

    @Test
    fun testAdMobOfficialGoogleTestIdentifiersConfigured() {
        // Must use Google's official public test IDs during development
        assertEquals(
            "ca-app-pub-3940256099942544~3347511713",
            AdConfig.APP_ID
        )
        assertEquals(
            "ca-app-pub-3940256099942544/9214589741",
            AdConfig.BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun testAdDisplayGovernedByActivePlan() {
        // Free tier displays ads
        SubscriptionManager.setActivePlan(SubscriptionPlan.FREE)
        assertTrue(AdConfig.shouldShowAds())

        // Pro tier is ad-free
        SubscriptionManager.setActivePlan(SubscriptionPlan.PRO)
        assertFalse(AdConfig.shouldShowAds())

        // Premium tier is ad-free
        SubscriptionManager.setActivePlan(SubscriptionPlan.PREMIUM)
        assertFalse(AdConfig.shouldShowAds())

        // Reset to Free for normal app behavior
        SubscriptionManager.setActivePlan(SubscriptionPlan.FREE)
    }

    @Test
    fun testSubscriptionTierIntegrity() {
        assertEquals("Free Starter", SubscriptionPlan.FREE.title)
        assertEquals(0, SubscriptionPlan.FREE.monthlyPrice)
        assertEquals(50, SubscriptionPlan.FREE.invoiceLimitPerMonth)

        assertEquals("Pro Business", SubscriptionPlan.PRO.title)
        assertEquals(99, SubscriptionPlan.PRO.monthlyPrice)
        assertEquals(799, SubscriptionPlan.PRO.yearlyPrice)

        assertEquals("Premium Enterprise", SubscriptionPlan.PREMIUM.title)
        assertEquals(199, SubscriptionPlan.PREMIUM.monthlyPrice)
        assertEquals(1499, SubscriptionPlan.PREMIUM.yearlyPrice)
        assertEquals(5, SubscriptionPlan.PREMIUM.maxBusinesses)
    }
}
