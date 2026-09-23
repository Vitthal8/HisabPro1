package com.hisabpro.app.domain.subscription

enum class SubscriptionPlan(
    val title: String,
    val monthlyPrice: Int,
    val yearlyPrice: Int,
    val invoiceLimitPerMonth: Int,
    val maxBusinesses: Int
) {
    FREE("Free Starter", 0, 0, 50, 1),
    PRO("Pro Business", 99, 799, Int.MAX_VALUE, 1),
    PREMIUM("Premium Enterprise", 199, 1499, Int.MAX_VALUE, 5)
}

sealed class EntitlementCheck {
    object Granted : EntitlementCheck()
    data class LimitExceeded(val message: String, val currentCount: Int, val maxAllowed: Int) : EntitlementCheck()

    val isGranted: Boolean get() = this is Granted
}

/**
 * Centralized Entitlement & Monetization Layer for HisabPro.
 * Controls tier limits (50 invoices/month on Free tier, GSTR-1 export, cloud sync).
 */
object SubscriptionManager {

    private var activePlan: SubscriptionPlan = SubscriptionPlan.FREE

    fun getActivePlan(): SubscriptionPlan = activePlan

    fun setActivePlan(plan: SubscriptionPlan) {
        activePlan = plan
    }

    /**
     * Checks if creating an invoice is permitted under current tier.
     */
    fun checkInvoiceCreationAllowed(currentMonthInvoiceCount: Int): EntitlementCheck {
        if (activePlan == SubscriptionPlan.FREE && currentMonthInvoiceCount >= activePlan.invoiceLimitPerMonth) {
            return EntitlementCheck.LimitExceeded(
                message = "You have reached the free limit of ${activePlan.invoiceLimitPerMonth} bills this month. Upgrade to Pro for unlimited billing.",
                currentCount = currentMonthInvoiceCount,
                maxAllowed = activePlan.invoiceLimitPerMonth
            )
        }
        return EntitlementCheck.Granted
    }

    /**
     * Checks if GSTR-1 export is accessible under current plan.
     */
    fun canExportGstr1(): Boolean {
        return activePlan == SubscriptionPlan.PRO || activePlan == SubscriptionPlan.PREMIUM
    }

    /**
     * Checks if Supabase Cloud Sync is enabled.
     */
    fun canUseCloudSync(): Boolean {
        return activePlan == SubscriptionPlan.PREMIUM
    }

    /**
     * Max businesses permitted.
     */
    fun getMaxBusinesses(): Int {
        return activePlan.maxBusinesses
    }
}
