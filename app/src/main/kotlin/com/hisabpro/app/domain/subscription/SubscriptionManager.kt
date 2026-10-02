package com.hisabpro.app.domain.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Subscription tiers for HisabPro as specified in monetization guidelines:
 *
 * FREE:
 * - Unlimited parties
 * - Unlimited items
 * - Maximum 50 invoices/month
 * - One business
 * - Local backup
 * - AdMob banner
 *
 * PRO:
 * - ₹99/month, ₹799/year
 * - Unlimited invoices
 * - Custom logo PDF
 * - WhatsApp sharing
 * - Excel export
 * - Ad-free
 *
 * PREMIUM:
 * - ₹199/month, ₹1499/year
 * - Everything in Pro
 * - Up to 5 businesses
 * - Supabase cloud sync
 * - Cloud backup
 * - GSTR-1 export
 * - Priority support
 */
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

data class PlanFeature(
    val title: String,
    val isIncludedInFree: Boolean,
    val isIncludedInPro: Boolean,
    val isIncludedInPremium: Boolean
)

sealed class EntitlementCheck {
    object Granted : EntitlementCheck()
    data class LimitExceeded(val message: String, val currentCount: Int, val maxAllowed: Int) : EntitlementCheck()

    val isGranted: Boolean get() = this is Granted
}

sealed class BillingState {
    object Idle : BillingState()
    object Loading : BillingState()
    data class Success(val plan: SubscriptionPlan, val message: String) : BillingState()
    data class Error(val message: String) : BillingState()
}

/**
 * Centralized Entitlement & Monetization Layer for HisabPro.
 * Ready for Google Play Billing 6+ / In-App Subscriptions.
 */
object SubscriptionManager {

    // Google Play Billing In-App Product / Subscription IDs
    const val SKU_PRO_MONTHLY = "hisabpro_pro_monthly"
    const val SKU_PRO_YEARLY = "hisabpro_pro_yearly"
    const val SKU_PREMIUM_MONTHLY = "hisabpro_premium_monthly"
    const val SKU_PREMIUM_YEARLY = "hisabpro_premium_yearly"

    private val _activePlanFlow = MutableStateFlow(SubscriptionPlan.FREE)
    val activePlanFlow: StateFlow<SubscriptionPlan> = _activePlanFlow.asStateFlow()

    private val _billingStateFlow = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingStateFlow: StateFlow<BillingState> = _billingStateFlow.asStateFlow()

    fun getActivePlan(): SubscriptionPlan = _activePlanFlow.value

    fun setActivePlan(plan: SubscriptionPlan) {
        _activePlanFlow.value = plan
    }

    val isAdFree: Boolean
        get() = _activePlanFlow.value != SubscriptionPlan.FREE

    val isCustomLogoEnabled: Boolean
        get() = _activePlanFlow.value != SubscriptionPlan.FREE

    val isWhatsAppDirectShareEnabled: Boolean
        get() = _activePlanFlow.value != SubscriptionPlan.FREE

    val isExcelExportEnabled: Boolean
        get() = _activePlanFlow.value != SubscriptionPlan.FREE

    fun canExportGstr1(): Boolean {
        return _activePlanFlow.value == SubscriptionPlan.PRO || _activePlanFlow.value == SubscriptionPlan.PREMIUM
    }

    fun canUseCloudSync(): Boolean {
        return _activePlanFlow.value == SubscriptionPlan.PREMIUM
    }

    fun getMaxBusinesses(): Int {
        return _activePlanFlow.value.maxBusinesses
    }

    /**
     * Checks if creating an invoice is permitted under current tier.
     */
    fun checkInvoiceCreationAllowed(currentMonthInvoiceCount: Int): EntitlementCheck {
        val currentPlan = _activePlanFlow.value
        if (currentPlan == SubscriptionPlan.FREE && currentMonthInvoiceCount >= currentPlan.invoiceLimitPerMonth) {
            return EntitlementCheck.LimitExceeded(
                message = "You have reached the free limit of ${currentPlan.invoiceLimitPerMonth} bills this month. Upgrade to Pro for unlimited billing.",
                currentCount = currentMonthInvoiceCount,
                maxAllowed = currentPlan.invoiceLimitPerMonth
            )
        }
        return EntitlementCheck.Granted
    }

    /**
     * Checks if adding a new business profile is allowed under current tier.
     */
    fun checkBusinessCreationAllowed(currentBusinessCount: Int): EntitlementCheck {
        val maxAllowed = getMaxBusinesses()
        if (currentBusinessCount >= maxAllowed) {
            val upgradeTarget = if (_activePlanFlow.value == SubscriptionPlan.FREE) "Pro / Premium" else "Premium"
            return EntitlementCheck.LimitExceeded(
                message = "You have reached the maximum limit of $maxAllowed business profiles. Upgrade to $upgradeTarget to manage up to 5 shops.",
                currentCount = currentBusinessCount,
                maxAllowed = maxAllowed
            )
        }
        return EntitlementCheck.Granted
    }

    /**
     * Plan comparison matrix for UI subscription paywall.
     */
    fun getComparisonFeatures(): List<PlanFeature> = listOf(
        PlanFeature("Unlimited Customers & Items", isIncludedInFree = true, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("Offline-First Accounting", isIncludedInFree = true, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("Monthly Invoice Limit", isIncludedInFree = false, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("Ad-Free Experience", isIncludedInFree = false, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("Custom Business Logo on Bills", isIncludedInFree = false, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("WhatsApp Direct Bill Sharing", isIncludedInFree = false, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("Excel / CSV Export", isIncludedInFree = false, isIncludedInPro = true, isIncludedInPremium = true),
        PlanFeature("Multiple Businesses (Up to 5)", isIncludedInFree = false, isIncludedInPro = false, isIncludedInPremium = true),
        PlanFeature("Supabase Cloud Sync & Backup", isIncludedInFree = false, isIncludedInPro = false, isIncludedInPremium = true),
        PlanFeature("GSTR-1 & GSTR-3B Tax Filing Reports", isIncludedInFree = false, isIncludedInPro = false, isIncludedInPremium = true),
        PlanFeature("Priority Support", isIncludedInFree = false, isIncludedInPro = false, isIncludedInPremium = true)
    )

    /**
     * Simulates purchase activation (used in debug or when purchase completes).
     */
    fun activatePlan(plan: SubscriptionPlan) {
        _activePlanFlow.value = plan
        _billingStateFlow.value = BillingState.Success(
            plan = plan,
            message = "Congratulations! You have upgraded to ${plan.title}."
        )
    }

    fun resetBillingState() {
        _billingStateFlow.value = BillingState.Idle
    }
}
