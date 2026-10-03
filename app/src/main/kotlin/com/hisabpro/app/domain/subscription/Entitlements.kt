package com.hisabpro.app.domain.subscription

/**
 * Data Model representing Active Entitlements in HisabPro.
 * This is the SINGLE ENTITLEMENT LAYER for the app.
 * UI components and ViewModels check these feature flags and limit functions directly.
 */
data class Entitlements(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val isAdFree: Boolean = false,
    val maxBusinesses: Int = 1,
    val monthlyInvoiceLimit: Int = 50,
    val isUnlimitedInvoices: Boolean = false,
    val isCustomLogoEnabled: Boolean = false,
    val isWhatsAppDirectShareEnabled: Boolean = false,
    val isExcelExportEnabled: Boolean = false,
    val isCloudSyncEnabled: Boolean = false,
    val isGstr1ExportEnabled: Boolean = false,
    val isPrioritySupportEnabled: Boolean = false
) {
    /**
     * Checks if invoice creation is allowed for the given calendar month invoice count.
     */
    fun canCreateInvoice(currentMonthInvoiceCount: Int): Boolean {
        if (isUnlimitedInvoices) return true
        return currentMonthInvoiceCount < monthlyInvoiceLimit
    }

    /**
     * Checks if creating/adding a new business profile is allowed for current count.
     */
    fun canAddBusiness(currentBusinessCount: Int): Boolean {
        return currentBusinessCount < maxBusinesses
    }

    companion object {
        val FREE = Entitlements(
            plan = SubscriptionPlan.FREE,
            isAdFree = false,
            maxBusinesses = 1,
            monthlyInvoiceLimit = 50,
            isUnlimitedInvoices = false,
            isCustomLogoEnabled = false,
            isWhatsAppDirectShareEnabled = false,
            isExcelExportEnabled = false,
            isCloudSyncEnabled = false,
            isGstr1ExportEnabled = false,
            isPrioritySupportEnabled = false
        )

        val PRO = Entitlements(
            plan = SubscriptionPlan.PRO,
            isAdFree = true,
            maxBusinesses = 1,
            monthlyInvoiceLimit = Int.MAX_VALUE,
            isUnlimitedInvoices = true,
            isCustomLogoEnabled = true,
            isWhatsAppDirectShareEnabled = true,
            isExcelExportEnabled = true,
            isCloudSyncEnabled = false,
            isGstr1ExportEnabled = true,
            isPrioritySupportEnabled = false
        )

        val PREMIUM = Entitlements(
            plan = SubscriptionPlan.PREMIUM,
            isAdFree = true,
            maxBusinesses = 5,
            monthlyInvoiceLimit = Int.MAX_VALUE,
            isUnlimitedInvoices = true,
            isCustomLogoEnabled = true,
            isWhatsAppDirectShareEnabled = true,
            isExcelExportEnabled = true,
            isCloudSyncEnabled = true,
            isGstr1ExportEnabled = true,
            isPrioritySupportEnabled = true
        )

        fun fromPlan(plan: SubscriptionPlan): Entitlements = when (plan) {
            SubscriptionPlan.FREE -> FREE
            SubscriptionPlan.PRO -> PRO
            SubscriptionPlan.PREMIUM -> PREMIUM
        }
    }
}
