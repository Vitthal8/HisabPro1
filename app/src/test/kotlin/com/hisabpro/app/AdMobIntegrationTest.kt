package com.hisabpro.app

import com.hisabpro.app.ads.AdConfig
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for AdMob configuration, build-type isolation, and monetization policy rules.
 */
class AdMobIntegrationTest {

    @Test
    fun testAdMobOfficialGoogleTestIdentifiersConfigured() {
        // Must use Google's official public test IDs during development / debug builds
        assertEquals(
            "ca-app-pub-3940256099942544~3347511713",
            AdConfig.TEST_APP_ID
        )
        assertEquals(
            "ca-app-pub-3940256099942544/9214589741",
            AdConfig.TEST_BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun testAdMobProductionIdentifiersConfigured() {
        // Verified production IDs supplied by user
        assertEquals(
            "ca-app-pub-2267867402830888~9218869645",
            AdConfig.PROD_APP_ID
        )
        assertEquals(
            "ca-app-pub-2267867402830888/9391008468",
            AdConfig.PROD_BANNER_AD_UNIT_ID
        )
    }

    @Test
    fun testDebugBuildUsesTestIdsAndCannotRequestProductionAds() {
        // In debug test environment, active IDs must strictly match Google test IDs
        assertEquals(AdConfig.TEST_APP_ID, AdConfig.APP_ID)
        assertEquals(AdConfig.TEST_BANNER_AD_UNIT_ID, AdConfig.BANNER_AD_UNIT_ID)

        // Must NOT match production IDs in debug builds
        assertNotEquals(AdConfig.PROD_APP_ID, AdConfig.APP_ID)
        assertNotEquals(AdConfig.PROD_BANNER_AD_UNIT_ID, AdConfig.BANNER_AD_UNIT_ID)

        // Production ads flag must be false
        assertFalse(AdConfig.IS_PRODUCTION_ADS)
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
