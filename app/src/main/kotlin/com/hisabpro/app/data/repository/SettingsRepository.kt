package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.model.BankDetails
import com.hisabpro.app.data.model.BusinessProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("hisab_pro_settings_v1", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILE = "business_profile_key"
        private val _sharedProfile = MutableStateFlow(BusinessProfile())
        val sharedProfile: StateFlow<BusinessProfile> = _sharedProfile.asStateFlow()
        private var isInitialized = false

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val profile: StateFlow<BusinessProfile> = _sharedProfile.asStateFlow()

    init {
        synchronized(SettingsRepository::class.java) {
            if (!isInitialized) {
                loadProfile()
                isInitialized = true
            }
        }
    }

    private fun loadProfile() {
        val json = prefs.getString(KEY_PROFILE, null)
        if (json.isNullOrBlank()) {
            val uninitialized = BusinessProfile(
                id = "",
                shopName = "",
                ownerName = "",
                phone = "",
                email = "",
                hasCompletedOnboarding = false
            )
            _sharedProfile.value = uninitialized
        } else {
            try {
                val obj = JSONObject(json)
                val bankAccountsArr = obj.optJSONArray("bankAccounts")
                val loadedBankAccounts = mutableListOf<BankDetails>()
                if (bankAccountsArr != null) {
                    for (i in 0 until bankAccountsArr.length()) {
                        val bObj = bankAccountsArr.optJSONObject(i)
                        if (bObj != null) {
                            loadedBankAccounts.add(
                                BankDetails(
                                    bankName = bObj.optString("bankName", ""),
                                    accountNumber = bObj.optString("accountNumber", ""),
                                    ifscCode = bObj.optString("ifscCode", "")
                                )
                            )
                        }
                    }
                }
                val primaryBank = obj.optString("bankName", "")
                val primaryAcc = obj.optString("accountNumber", "")
                val primaryIfsc = obj.optString("ifscCode", "")
                if (loadedBankAccounts.isEmpty() && primaryBank.isNotBlank()) {
                    loadedBankAccounts.add(BankDetails(primaryBank, primaryAcc, primaryIfsc))
                }

                val loaded = BusinessProfile(
                    shopName = obj.optString("shopName", ""),
                    ownerName = obj.optString("ownerName", ""),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    isGstRegistered = obj.optBoolean("isGstRegistered", false),
                    gstin = obj.optString("gstin", ""),
                    pan = obj.optString("pan", ""),
                    isCompositionScheme = obj.optBoolean("isCompositionScheme", false),
                    compositionType = obj.optString("compositionType", "TRADER"),
                    address = obj.optString("address", ""),
                    city = obj.optString("city", "Pune"),
                    state = obj.optString("state", "Maharashtra"),
                    stateCode = obj.optString("stateCode", "27"),
                    pincode = obj.optString("pincode", ""),
                    upiId = obj.optString("upiId", ""),
                    bankName = primaryBank,
                    accountNumber = primaryAcc,
                    ifscCode = primaryIfsc,
                    bankAccounts = loadedBankAccounts,
                    invoicePrefix = obj.optString("invoicePrefix", "INV"),
                    purchasePrefix = obj.optString("purchasePrefix", "PUR"),
                    termsAndConditions = obj.optString(
                        "termsAndConditions",
                        "1. Goods once sold cannot be returned without original invoice.\n2. Payment terms: Due within 15 days of invoice date."
                    ),
                    logoPath = obj.optString("logoPath", ""),
                    isThermalPrinterMode = obj.optBoolean("isThermalPrinterMode", false),
                    showUpiQrOnInvoice = obj.optBoolean("showUpiQrOnInvoice", true),
                    appLanguage = obj.optString("appLanguage", "en"),
                    isDarkMode = obj.optBoolean("isDarkMode", false),
                    themeAccent = obj.optString("themeAccent", "Saffron"),
                    hasCompletedOnboarding = obj.optBoolean("hasCompletedOnboarding", false)
                )
                _sharedProfile.value = loaded
            } catch (e: Exception) {
                e.printStackTrace()
                _sharedProfile.value = BusinessProfile()
            }
        }
    }

    fun resetProfile(newProfile: BusinessProfile? = null) {
        prefs.edit().remove(KEY_PROFILE).apply()
        val target = newProfile ?: BusinessProfile(
            id = "",
            shopName = "",
            ownerName = "",
            phone = "",
            email = "",
            hasCompletedOnboarding = false
        )
        if (newProfile != null && newProfile.shopName.isNotBlank()) {
            saveProfile(newProfile)
        } else {
            _sharedProfile.value = target
        }
    }

    fun reloadProfile() {
        loadProfile()
    }

    fun saveProfile(profile: BusinessProfile) {
        val firstBank = profile.effectiveBankAccounts.firstOrNull() ?: BankDetails()
        val bankArr = JSONArray()
        for (b in profile.effectiveBankAccounts) {
            bankArr.put(
                JSONObject().apply {
                    put("bankName", b.bankName)
                    put("accountNumber", b.accountNumber)
                    put("ifscCode", b.ifscCode)
                }
            )
        }

        val obj = JSONObject().apply {
            put("shopName", profile.shopName)
            put("ownerName", profile.ownerName)
            put("phone", profile.phone)
            put("email", profile.email)
            put("isGstRegistered", profile.isGstRegistered)
            put("gstin", profile.gstin)
            put("pan", profile.pan)
            put("isCompositionScheme", profile.isCompositionScheme)
            put("compositionType", profile.compositionType)
            put("address", profile.address)
            put("city", profile.city)
            put("state", profile.state)
            put("stateCode", profile.stateCode)
            put("pincode", profile.pincode)
            put("upiId", profile.upiId)
            put("bankName", firstBank.bankName)
            put("accountNumber", firstBank.accountNumber)
            put("ifscCode", firstBank.ifscCode)
            put("bankAccounts", bankArr)
            put("invoicePrefix", profile.invoicePrefix)
            put("purchasePrefix", profile.purchasePrefix)
            put("termsAndConditions", profile.termsAndConditions)
            put("logoPath", profile.logoPath)
            put("isThermalPrinterMode", profile.isThermalPrinterMode)
            put("showUpiQrOnInvoice", profile.showUpiQrOnInvoice)
            put("appLanguage", profile.appLanguage)
            put("isDarkMode", profile.isDarkMode)
            put("themeAccent", profile.themeAccent)
            put("hasCompletedOnboarding", profile.hasCompletedOnboarding)
        }
        prefs.edit().putString(KEY_PROFILE, obj.toString()).apply()
        _sharedProfile.value = profile
    }

    fun updateProfile(profile: BusinessProfile) {
        saveProfile(profile)
    }
}

