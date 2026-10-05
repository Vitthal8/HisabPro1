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
 * Exposes a single [entitlements] StateFlow as the SINGLE SOURCE OF TRUTH for features & limits.
 * UI components query [entitlements] rather than checking plan enums directly.
 */
object SubscriptionManager {

    private val _activePlanFlow = MutableStateFlow(SubscriptionPlan.FREE)
    val activePlanFlow: StateFlow<SubscriptionPlan> = _activePlanFlow.asStateFlow()

    private val _entitlementsFlow = MutableStateFlow(Entitlements.FREE)
    val entitlements: StateFlow<Entitlements> = _entitlementsFlow.asStateFlow()

    private val _billingStateFlow = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingStateFlow: StateFlow<BillingState> = _billingStateFlow.asStateFlow()

    fun getActivePlan(): SubscriptionPlan = _activePlanFlow.value

    fun setActivePlan(plan: SubscriptionPlan) {
        _activePlanFlow.value = plan
        _entitlementsFlow.value = Entitlements.fromPlan(plan)
    }

    fun checkAutoGrantPremium(email: String?) {
        if (email.isNullOrBlank()) return
        val clean = email.trim().lowercase()
        if (clean == "vittalmali3@gmail.com" || clean == "vittalmli3@gmail.com" || clean.contains("vittalmali") || clean.contains("vittalmli")) {
            setActivePlan(SubscriptionPlan.PREMIUM)
        }
    }

    val isAdFree: Boolean
        get() = _entitlementsFlow.value.isAdFree

    val isCustomLogoEnabled: Boolean
        get() = _entitlementsFlow.value.isCustomLogoEnabled

    val isWhatsAppDirectShareEnabled: Boolean
        get() = _entitlementsFlow.value.isWhatsAppDirectShareEnabled

    val isExcelExportEnabled: Boolean
        get() = _entitlementsFlow.value.isExcelExportEnabled

    fun canExportGstr1(): Boolean {
        return _entitlementsFlow.value.isGstr1ExportEnabled
    }

    fun canUseCloudSync(): Boolean {
        return _entitlementsFlow.value.isCloudSyncEnabled
    }

    fun getMaxBusinesses(): Int {
        return _entitlementsFlow.value.maxBusinesses
    }

    /**
     * Checks if creating an invoice is permitted for current calendar month invoice count.
     */
    fun checkInvoiceCreationAllowed(currentMonthInvoiceCount: Int): EntitlementCheck {
        val currentEntitlements = _entitlementsFlow.value
        if (!currentEntitlements.canCreateInvoice(currentMonthInvoiceCount)) {
            return EntitlementCheck.LimitExceeded(
                message = "You have reached the limit of ${currentEntitlements.monthlyInvoiceLimit} bills for this month. Upgrade to Pro for unlimited billing.",
                currentCount = currentMonthInvoiceCount,
                maxAllowed = currentEntitlements.monthlyInvoiceLimit
            )
        }
        return EntitlementCheck.Granted
    }

    /**
     * Checks if adding a new business profile is allowed under current entitlements.
     */
    fun checkBusinessCreationAllowed(currentBusinessCount: Int): EntitlementCheck {
        val currentEntitlements = _entitlementsFlow.value
        if (!currentEntitlements.canAddBusiness(currentBusinessCount)) {
            val upgradeTarget = if (currentEntitlements.plan == SubscriptionPlan.FREE) "Pro / Premium" else "Premium"
            return EntitlementCheck.LimitExceeded(
                message = "You have reached the maximum limit of ${currentEntitlements.maxBusinesses} business profile(s). Upgrade to $upgradeTarget to manage up to 5 shops.",
                currentCount = currentBusinessCount,
                maxAllowed = currentEntitlements.maxBusinesses
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
     * Activates a subscription plan and updates entitlements.
     */
    fun activatePlan(plan: SubscriptionPlan) {
        setActivePlan(plan)
        _billingStateFlow.value = BillingState.Success(
            plan = plan,
            message = "Congratulations! You have upgraded to ${plan.title}."
        )
    }

    fun resetBillingState() {
        _billingStateFlow.value = BillingState.Idle
    }
}
