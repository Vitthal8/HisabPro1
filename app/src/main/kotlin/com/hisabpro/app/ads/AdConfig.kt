package com.hisabpro.app.ads

object AdConfig {
    // TEST ONLY - replace before production
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    fun shouldShowAds(): Boolean {
        // Can be tied to SubscriptionManager in future (FREE vs PRO/PREMIUM)
        return true
    }
}
