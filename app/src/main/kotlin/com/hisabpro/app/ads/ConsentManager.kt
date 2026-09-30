package com.hisabpro.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Manages Google User Messaging Platform (UMP) consent state for HisabPro.
 * Handles consent info refresh, consent form rendering, privacy choices, and error containment.
 */
class ConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    /**
     * Determines whether Google Mobile Ads can be requested under current consent status.
     * Guaranteed never to throw.
     */
    val canRequestAds: Boolean
        get() = try {
            consentInformation.canRequestAds()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking canRequestAds: ${e.message}")
            false
        }

    /**
     * Indicates whether the user is in a jurisdiction requiring privacy options (e.g., EEA/GDPR).
     */
    val isPrivacyOptionsRequired: Boolean
        get() = try {
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking privacyOptionsRequirementStatus: ${e.message}")
            false
        }

    /**
     * Refreshes consent information and presents the UMP consent form if required.
     * All exceptions are caught and forwarded to the listener to prevent application crashes.
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        try {
            val params = ConsentRequestParameters.Builder()
                .build()

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        activity
                    ) { formError ->
                        onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                    }
                },
                { requestConsentError ->
                    Log.w(TAG, "Consent info update failed: ${requestConsentError.message}")
                    onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during gatherConsent: ${e.message}", e)
            onConsentGatheringCompleteListener.consentGatheringComplete(null)
        }
    }

    /**
     * Shows the privacy options form so users can review or change their consent settings.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onConsentGatheringCompleteListener.consentGatheringComplete(
                FormError(1, "Activity is finishing or destroyed.")
            )
            return
        }
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(
                activity
            ) { formError ->
                if (formError != null) {
                    Log.w(TAG, "Privacy options form error: ${formError.message}")
                }
                onConsentGatheringCompleteListener.consentGatheringComplete(formError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception showing privacy options form: ${e.message}", e)
            onConsentGatheringCompleteListener.consentGatheringComplete(
                FormError(2, e.message ?: "Privacy options form unavailable")
            )
        }
    }

    companion object {
        private const val TAG = "HisabProConsent"

        @Volatile
        private var instance: ConsentManager? = null

        fun getInstance(context: Context): ConsentManager =
            instance ?: synchronized(this) {
                instance ?: ConsentManager(context.applicationContext).also { instance = it }
            }
    }
}

