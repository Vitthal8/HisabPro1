package com.hisabpro.app.domain.subscription

/**
 * Single Source of Truth for Google Play Billing 6+ Product Identifiers (SKUs)
 * and Subscription Product Constants in HisabPro.
 */
object BillingConstants {

    // Google Play Billing In-App Subscription Product IDs
    const val SKU_PRO_MONTHLY = "hisabpro_pro_monthly"
    const val SKU_PRO_YEARLY = "hisabpro_pro_yearly"
    const val SKU_PREMIUM_MONTHLY = "hisabpro_premium_monthly"
    const val SKU_PREMIUM_YEARLY = "hisabpro_premium_yearly"

    val ALL_SUBSCRIPTION_SKUS = listOf(
        SKU_PRO_MONTHLY,
        SKU_PRO_YEARLY,
        SKU_PREMIUM_MONTHLY,
        SKU_PREMIUM_YEARLY
    )
}
