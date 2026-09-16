package com.hisabpro.app.data.model

data class BusinessProfile(
    val shopName: String = "HisabPro Enterprises",
    val ownerName: String = "Vittal Mali",
    val phone: String = "+91 98765 43210",
    val email: String = "hisabpro@business.in",
    val gstin: String = "27AAAPH1234C1Z5",
    val address: String = "Shop No. 12, Market Yard Main Road",
    val city: String = "Pune",
    val state: String = "Maharashtra",
    val stateCode: String = "27",
    val pincode: String = "411037",
    val upiId: String = "vittal@okhdfcbank",
    val bankName: String = "State Bank of India",
    val accountNumber: String = "987654321012",
    val ifscCode: String = "SBIN0001234",
    val invoicePrefix: String = "INV",
    val purchasePrefix: String = "PUR",
    val termsAndConditions: String = "1. Goods once sold cannot be returned without original invoice.\n2. Payment terms: Due within 15 days of invoice date.\n3. Subject to local jurisdiction only.",
    val isThermalPrinterMode: Boolean = false,
    val showUpiQrOnInvoice: Boolean = true
) {
    val fullAddress: String
        get() = listOf(address, city, "$state - $pincode")
            .filter { it.isNotBlank() }
            .joinToString(", ")

    val isGstRegistered: Boolean
        get() = gstin.trim().length == 15
}
