package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.model.BusinessProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_pro_settings_v1", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(BusinessProfile())
    val profile: StateFlow<BusinessProfile> = _profile.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val json = prefs.getString(KEY_PROFILE, null)
        if (json.isNullOrBlank()) {
            val defaultProfile = BusinessProfile()
            saveProfile(defaultProfile)
        } else {
            try {
                val obj = JSONObject(json)
                val loaded = BusinessProfile(
                    shopName = obj.optString("shopName", "HisabPro Enterprises"),
                    ownerName = obj.optString("ownerName", "Vittal Mali"),
                    phone = obj.optString("phone", "+91 98765 43210"),
                    email = obj.optString("email", "hisabpro@business.in"),
                    gstin = obj.optString("gstin", "27AAAPH1234C1Z5"),
                    address = obj.optString("address", "Shop No. 12, Market Yard Main Road"),
                    city = obj.optString("city", "Pune"),
                    state = obj.optString("state", "Maharashtra"),
                    stateCode = obj.optString("stateCode", "27"),
                    pincode = obj.optString("pincode", "411037"),
                    upiId = obj.optString("upiId", "vittal@okhdfcbank"),
                    bankName = obj.optString("bankName", "State Bank of India"),
                    accountNumber = obj.optString("accountNumber", "987654321012"),
                    ifscCode = obj.optString("ifscCode", "SBIN0001234"),
                    invoicePrefix = obj.optString("invoicePrefix", "INV"),
                    purchasePrefix = obj.optString("purchasePrefix", "PUR"),
                    termsAndConditions = obj.optString(
                        "termsAndConditions",
                        "1. Goods once sold cannot be returned without original invoice.\n2. Payment terms: Due within 15 days of invoice date."
                    ),
                    isThermalPrinterMode = obj.optBoolean("isThermalPrinterMode", false),
                    showUpiQrOnInvoice = obj.optBoolean("showUpiQrOnInvoice", true)
                )
                _profile.value = loaded
            } catch (e: Exception) {
                e.printStackTrace()
                _profile.value = BusinessProfile()
            }
        }
    }

    fun saveProfile(profile: BusinessProfile) {
        val obj = JSONObject().apply {
            put("shopName", profile.shopName)
            put("ownerName", profile.ownerName)
            put("phone", profile.phone)
            put("email", profile.email)
            put("gstin", profile.gstin)
            put("address", profile.address)
            put("city", profile.city)
            put("state", profile.state)
            put("stateCode", profile.stateCode)
            put("pincode", profile.pincode)
            put("upiId", profile.upiId)
            put("bankName", profile.bankName)
            put("accountNumber", profile.accountNumber)
            put("ifscCode", profile.ifscCode)
            put("invoicePrefix", profile.invoicePrefix)
            put("purchasePrefix", profile.purchasePrefix)
            put("termsAndConditions", profile.termsAndConditions)
            put("isThermalPrinterMode", profile.isThermalPrinterMode)
            put("showUpiQrOnInvoice", profile.showUpiQrOnInvoice)
        }
        prefs.edit().putString(KEY_PROFILE, obj.toString()).apply()
        _profile.value = profile
    }

    fun updateProfile(profile: BusinessProfile) {
        saveProfile(profile)
    }

    companion object {
        private const val KEY_PROFILE = "business_profile_key"
    }
}
