package com.hisabpro.app.ads

import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan

/**
 * AdMob Configuration for HisabPro.
 *
 * CRITICAL POLICY NOTICE:
 * Development builds MUST use Google's official test IDs below.
 * Never use live production ad unit IDs during development, and never click live ads.
 * Violating this can lead to an immediate Google AdMob account suspension for invalid traffic.
 *
 * Official Google Sample IDs:
 * - Sample App ID: ca-app-pub-3940256099942544~3347511713
 * - Sample Banner Ad Unit: ca-app-pub-3940256099942544/9214589741
 */
object AdConfig {
    // Official Google AdMob test IDs for Android development
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    // Currently active IDs (Point to official Google Test IDs during development)
    const val APP_ID = TEST_APP_ID
    const val BANNER_AD_UNIT_ID = TEST_BANNER_AD_UNIT_ID

    /**
     * Governs whether ads should be displayed.
     * In the Free version, ads are enabled to support monetization.
     * Future Pro/Premium tiers will disable ads for a clean ad-free experience.
     */
    fun shouldShowAds(): Boolean {
        return SubscriptionManager.getActivePlan() == SubscriptionPlan.FREE
    }
}
