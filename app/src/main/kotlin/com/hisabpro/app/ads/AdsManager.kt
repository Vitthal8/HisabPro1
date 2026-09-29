package com.hisabpro.app.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

object AdsManager {
    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (!isInitialized.get() && AdConfig.shouldShowAds()) {
            try {
                MobileAds.initialize(context) { _ ->
                    isInitialized.set(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
