package com.hisabpro.app.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Production-ready AdMob Banner Composable for HisabPro.
 * Features:
 * 1. Checks UMP consent via [ConsentManager.canRequestAds].
 * 2. Checks active subscription plan / ad policy via [AdConfig.shouldShowAds].
 * 3. Hides layout entirely when ad is loading or fails to load, preventing awkward whitespace.
 * 4. Proper Compose lifecycle handling: calls [AdView.resume], [AdView.pause], and [AdView.destroy].
 * 5. Defensive try/catch to ensure advertising SDK failures never crash accounting features.
 */
@Composable
fun HisabProBannerAd(
    modifier: Modifier = Modifier
) {
    if (!AdConfig.shouldShowAds()) return

    val context = LocalContext.current
    val consentManager = remember { ConsentManager.getInstance(context) }
    if (!consentManager.canRequestAds) {
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    var currentAdView by remember { mutableStateOf<AdView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Activity / Screen Lifecycle synchronization (pause/resume/destroy)
    DisposableEffect(lifecycleOwner, currentAdView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        currentAdView?.resume()
                    } catch (e: Exception) {
                        Log.w("HisabProBannerAd", "Error resuming AdView: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        currentAdView?.pause()
                    } catch (e: Exception) {
                        Log.w("HisabProBannerAd", "Error pausing AdView: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        currentAdView?.destroy()
                    } catch (e: Exception) {
                        Log.w("HisabProBannerAd", "Error destroying AdView: ${e.message}")
                    }
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Only render container spacing when the ad has successfully loaded to avoid awkward blank gaps
    Box(
        modifier = if (isAdLoaded) {
            modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("admob_banner_container")
        } else {
            Modifier
        },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                try {
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdConfig.BANNER_AD_UNIT_ID
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                isAdLoaded = true
                                Log.d("HisabProBannerAd", "Banner ad loaded successfully")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                super.onAdFailedToLoad(error)
                                isAdLoaded = false
                                Log.w(
                                    "HisabProBannerAd",
                                    "Banner ad failed to load: code=${error.code}, message=${error.message}, domain=${error.domain}"
                                )
                            }
                        }
                        currentAdView = this
                        loadAd(AdRequest.Builder().build())
                    }
                } catch (e: Exception) {
                    Log.e("HisabProBannerAd", "Exception initializing AdView: ${e.message}", e)
                    isAdLoaded = false
                    android.view.View(ctx) // Safe invisible fallback
                }
            },
            onRelease = { adView ->
                try {
                    if (adView is AdView) {
                        adView.destroy()
                    }
                } catch (e: Exception) {
                    Log.w("HisabProBannerAd", "Error in onRelease: ${e.message}")
                }
                currentAdView = null
            },
            modifier = if (isAdLoaded) Modifier.fillMaxWidth() else Modifier
        )
    }
}
