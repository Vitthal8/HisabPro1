package com.hisabpro.app.ads

import com.hisabpro.app.BuildConfig
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan

/**
 * AdMob Configuration for HisabPro.
 *
 * CRITICAL POLICY NOTICE:
 * Development/Debug builds strictly use Google's official sample IDs to comply with AdMob policy.
 * Release builds use the verified production App ID and Dashboard Banner Ad Unit ID.
 *
 * Official Google Test IDs:
 * - Test App ID: ca-app-pub-3940256099942544~3347511713
 * - Test Banner ID: ca-app-pub-3940256099942544/9214589741
 *
 * Configured Production IDs:
 * - Production App ID: ca-app-pub-2267867402830888~9218869645
 * - Production Banner ID: ca-app-pub-2267867402830888/9391008468
 */
object AdConfig {
    // Official Google sample test IDs
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    // Supplied Production IDs
    const val PROD_APP_ID = "ca-app-pub-2267867402830888~9218869645"
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-2267867402830888/9391008468"

    // Build-specific configuration injected via Gradle BuildConfig:
    // Debug -> TEST_APP_ID & TEST_BANNER_AD_UNIT_ID
    // Release -> PROD_APP_ID & PROD_BANNER_AD_UNIT_ID
    val APP_ID: String = BuildConfig.ADMOB_APP_ID
    val BANNER_AD_UNIT_ID: String = BuildConfig.BANNER_AD_UNIT_ID
    val IS_PRODUCTION_ADS: Boolean = BuildConfig.IS_PRODUCTION_ADS

    /**
     * Governs whether ads should be displayed.
     * In the Free version, ads are enabled to support monetization.
     * Future Pro/Premium tiers will disable ads for a clean ad-free experience.
     */
    fun shouldShowAds(): Boolean {
        return SubscriptionManager.getActivePlan() == SubscriptionPlan.FREE
    }
}
