package com.hisabpro.app.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Google Mobile Ads SDK initialization lifecycle for HisabPro.
 * Ensures initialization occurs at most once and only when consent allows.
 */
object AdsManager {
    private const val TAG = "HisabProAds"
    private val isInitialized = AtomicBoolean(false)
    private val isInitializing = AtomicBoolean(false)

    fun isInitialized(): Boolean = isInitialized.get()

    /**
     * Initializes MobileAds SDK if permitted by current consent state and monetization policy.
     * Safe to invoke multiple times; subsequent calls are no-ops.
     */
    fun initialize(context: Context) {
        if (isInitialized.get() || isInitializing.get()) {
            return
        }

        if (!AdConfig.shouldShowAds()) {
            Log.d(TAG, "Ads disabled by configuration or active plan")
            return
        }

        val consentManager = ConsentManager.getInstance(context)
        if (!consentManager.canRequestAds) {
            Log.d(TAG, "Cannot initialize MobileAds: Consent not yet granted or canRequestAds is false")
            return
        }

        if (isInitializing.compareAndSet(false, true)) {
            try {
                Log.d(TAG, "Initializing Google Mobile Ads SDK...")
                MobileAds.initialize(context.applicationContext) { status ->
                    isInitialized.set(true)
                    isInitializing.set(false)
                    Log.d(TAG, "MobileAds initialization complete. Adapter status: ${status.adapterStatusMap.keys}")
                }
            } catch (e: Exception) {
                isInitializing.set(false)
                Log.e(TAG, "Failed to initialize MobileAds: ${e.message}", e)
            }
        }
    }
}

