package com.hisabpro.app.data.model

data class BankDetails(
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = ""
)

data class BusinessProfile(
    val id: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val isGstRegistered: Boolean = false,
    val gstin: String = "",
    val pan: String = "",
    val isCompositionScheme: Boolean = false,
    val compositionType: String = "TRADER", // "TRADER" (1%) or "SERVICE" (6%)
    val address: String = "",
    val city: String = "Pune",
    val state: String = "Maharashtra",
    val stateCode: String = "27",
    val pincode: String = "",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val bankAccounts: List<BankDetails> = emptyList(),
    val invoicePrefix: String = "INV",
    val purchasePrefix: String = "PUR",
    val termsAndConditions: String = "1. Goods once sold cannot be returned without original invoice.\n2. Payment terms: Due within 15 days of invoice date.\n3. Subject to local jurisdiction only.",
    val logoPath: String = "",
    val isThermalPrinterMode: Boolean = false,
    val showUpiQrOnInvoice: Boolean = true,
    val appLanguage: String = "en", // "en", "hi", "mr"
    val isDarkMode: Boolean = false,
    val themeAccent: String = "Saffron", // "Saffron", "Emerald", "Navy"
    val hasCompletedOnboarding: Boolean = false
) {
    val fullAddress: String
        get() = listOf(address, city, "$state - $pincode")
            .filter { it.isNotBlank() }
            .joinToString(", ")

    val effectiveBankAccounts: List<BankDetails>
        get() = if (bankAccounts.isNotEmpty()) bankAccounts else listOf(BankDetails(bankName, accountNumber, ifscCode))

    val effectiveGstRateComposition: Double
        get() = if (isCompositionScheme) {
            if (compositionType.equals("SERVICE", ignoreCase = true)) 6.0 else 1.0
        } else 0.0
}

