package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.model.BankDetails
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Multi-Business & Company profile manager for HisabPro.
 * Enables seamless switching between multiple shops/firms (e.g., Shop 1, Shop 2).
 */
class BusinessManager private constructor(private val context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("hisab_pro_multi_business_v1", Context.MODE_PRIVATE)

    private val _businesses = MutableStateFlow<List<BusinessProfile>>(emptyList())
    val businesses: StateFlow<List<BusinessProfile>> = _businesses.asStateFlow()

    private val _activeBusinessId = MutableStateFlow("default_business")
    val activeBusinessId: StateFlow<String> = _activeBusinessId.asStateFlow()

    private val _activeBusiness = MutableStateFlow(BusinessProfile())
    val activeBusiness: StateFlow<BusinessProfile> = _activeBusiness.asStateFlow()

    val activeBusinessDatabaseId: String
        get() {
            val active = _activeBusiness.value
            val name = active.shopName.trim()
            if (name.isBlank() || name.equals("HisabPro Enterprises", ignoreCase = true)) {
                // Never allow writes to land in default_business once a real custom business exists!
                val realBiz = _businesses.value.firstOrNull { !it.shopName.equals("HisabPro Enterprises", ignoreCase = true) }
                if (realBiz != null) {
                    return getBusinessDatabaseId(realBiz)
                }
                return "default_business"
            }
            val sanitized = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
            return "biz_$sanitized"
        }

    init {
        loadBusinesses()
    }

    companion object {
        private const val KEY_BUSINESS_LIST = "key_business_list_json"
        private const val KEY_ACTIVE_ID = "key_active_business_id"

        @Volatile
        private var INSTANCE: BusinessManager? = null

        fun getInstance(context: Context): BusinessManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BusinessManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadBusinesses() {
        val rawJson = prefs.getString(KEY_BUSINESS_LIST, null)
        val activeId = prefs.getString(KEY_ACTIVE_ID, "default_business") ?: "default_business"
        _activeBusinessId.value = activeId

        if (rawJson.isNullOrBlank()) {
            val defaultPrimary = SettingsRepository.getInstance(appContext).profile.value.copy(
                shopName = if (SettingsRepository.getInstance(appContext).profile.value.shopName.isNotBlank())
                    SettingsRepository.getInstance(appContext).profile.value.shopName else "HisabPro Enterprises"
            )
            val initialList = listOf(defaultPrimary)
            saveBusinessesInternal(initialList, activeId)
        } else {
            try {
                val array = JSONArray(rawJson)
                val list = mutableListOf<BusinessProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseProfileFromJson(obj))
                }
                if (list.isEmpty()) {
                    list.add(BusinessProfile())
                }
                _businesses.value = list
                val matched = list.find { it.shopName == activeId || it.gstin == activeId } ?: list.first()
                _activeBusiness.value = matched
                SettingsRepository.getInstance(appContext).updateProfile(matched)
            } catch (e: Exception) {
                e.printStackTrace()
                val fallback = listOf(BusinessProfile())
                _businesses.value = fallback
                _activeBusiness.value = fallback.first()
            }
        }

        scope.launch {
            syncToDatabase()
        }
    }

    private fun parseProfileFromJson(obj: JSONObject): BusinessProfile {
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
        val primaryBank = obj.optString("bankName", "State Bank of India")
        val primaryAcc = obj.optString("accountNumber", "987654321012")
        val primaryIfsc = obj.optString("ifscCode", "SBIN0001234")
        if (loadedBankAccounts.isEmpty()) {
            loadedBankAccounts.add(BankDetails(primaryBank, primaryAcc, primaryIfsc))
        }

        return BusinessProfile(
            id = obj.optString("id", ""),
            shopName = obj.optString("shopName", "HisabPro Enterprises"),
            ownerName = obj.optString("ownerName", "Vittal Mali"),
            phone = obj.optString("phone", "+91 98765 43210"),
            email = obj.optString("email", "hisabpro@business.in"),
            isGstRegistered = obj.optBoolean("isGstRegistered", false),
            gstin = obj.optString("gstin", ""),
            pan = obj.optString("pan", ""),
            isCompositionScheme = obj.optBoolean("isCompositionScheme", false),
            compositionType = obj.optString("compositionType", "TRADER"),
            address = obj.optString("address", "Shop No. 12, Market Yard Main Road"),
            city = obj.optString("city", "Pune"),
            state = obj.optString("state", "Maharashtra"),
            stateCode = obj.optString("stateCode", "27"),
            pincode = obj.optString("pincode", "411037"),
            upiId = obj.optString("upiId", "vittal@okhdfcbank"),
            bankName = primaryBank,
            accountNumber = primaryAcc,
            ifscCode = primaryIfsc,
            bankAccounts = loadedBankAccounts,
            invoicePrefix = obj.optString("invoicePrefix", "INV"),
            purchasePrefix = obj.optString("purchasePrefix", "PUR"),
            termsAndConditions = obj.optString("termsAndConditions", "1. Goods once sold cannot be returned.\n2. Due in 15 days."),
            logoPath = obj.optString("logoPath", ""),
            isThermalPrinterMode = obj.optBoolean("isThermalPrinterMode", false),
            showUpiQrOnInvoice = obj.optBoolean("showUpiQrOnInvoice", true),
            appLanguage = obj.optString("appLanguage", "en"),
            isDarkMode = obj.optBoolean("isDarkMode", false),
            themeAccent = obj.optString("themeAccent", "Saffron"),
            hasCompletedOnboarding = true
        )
    }

    private fun saveBusinessesInternal(list: List<BusinessProfile>, activeId: String) {
        _businesses.value = list
        _activeBusinessId.value = activeId
        val active = list.find { it.shopName == activeId || it.gstin == activeId } ?: list.firstOrNull() ?: BusinessProfile()
        _activeBusiness.value = active
        SettingsRepository.getInstance(appContext).updateProfile(active)

        val array = JSONArray()
        for (p in list) {
            val firstBank = p.effectiveBankAccounts.firstOrNull() ?: BankDetails()
            val bankArr = JSONArray()
            for (b in p.effectiveBankAccounts) {
                bankArr.put(
                    JSONObject().apply {
                        put("bankName", b.bankName)
                        put("accountNumber", b.accountNumber)
                        put("ifscCode", b.ifscCode)
                    }
                )
            }

            val obj = JSONObject().apply {
                put("id", p.id.ifBlank { getBusinessDatabaseId(p) })
                put("shopName", p.shopName)
                put("ownerName", p.ownerName)
                put("phone", p.phone)
                put("email", p.email)
                put("isGstRegistered", p.isGstRegistered)
                put("gstin", p.gstin)
                put("pan", p.pan)
                put("isCompositionScheme", p.isCompositionScheme)
                put("compositionType", p.compositionType)
                put("address", p.address)
                put("city", p.city)
                put("state", p.state)
                put("stateCode", p.stateCode)
                put("pincode", p.pincode)
                put("upiId", p.upiId)
                put("bankName", firstBank.bankName)
                put("accountNumber", firstBank.accountNumber)
                put("ifscCode", firstBank.ifscCode)
                put("bankAccounts", bankArr)
                put("invoicePrefix", p.invoicePrefix)
                put("purchasePrefix", p.purchasePrefix)
                put("termsAndConditions", p.termsAndConditions)
                put("logoPath", p.logoPath)
                put("isThermalPrinterMode", p.isThermalPrinterMode)
                put("showUpiQrOnInvoice", p.showUpiQrOnInvoice)
                put("appLanguage", p.appLanguage)
                put("isDarkMode", p.isDarkMode)
                put("themeAccent", p.themeAccent)
            }
            array.put(obj)
        }

        prefs.edit()
            .putString(KEY_BUSINESS_LIST, array.toString())
            .putString(KEY_ACTIVE_ID, activeId)
            .apply()

        scope.launch {
            syncToDatabase()
        }
    }

    fun getBusinessDatabaseId(profile: BusinessProfile): String {
        if (profile.id.isNotBlank() && profile.id != "default_business") return profile.id
        val name = profile.shopName.trim()
        if (name.isBlank() || name.equals("HisabPro Enterprises", ignoreCase = true)) return "default_business"
        val sanitized = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        return "biz_$sanitized"
    }

    suspend fun syncToDatabase() {
        try {
            val entities = _businesses.value.map { p ->
                val bizId = getBusinessDatabaseId(p)
                val firstBank = p.effectiveBankAccounts.firstOrNull() ?: BankDetails()
                BusinessEntity(
                    id = bizId,
                    name = p.shopName,
                    ownerName = p.ownerName,
                    address = p.address,
                    phone = p.phone,
                    email = p.email,
                    gstin = p.gstin,
                    pan = p.pan,
                    logoPath = p.logoPath,
                    gstEnabled = p.isGstRegistered,
                    financialYearStart = "01-04",
                    upiId = p.upiId,
                    bankName = firstBank.bankName,
                    accountNumber = firstBank.accountNumber,
                    ifscCode = firstBank.ifscCode,
                    termsAndConditions = p.termsAndConditions,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            db.businessDao().insertAllBusinesses(entities)

            for (entity in entities) {
                // Never push unconfigured demo placeholder if real custom businesses exist
                val isDemoPlaceholder = entity.id == "default_business" && entity.name.equals("HisabPro Enterprises", ignoreCase = true)
                val hasRealBusinesses = entities.any { it.id != "default_business" }
                if (isDemoPlaceholder && hasRealBusinesses) {
                    continue
                }

                val payload = JSONObject().apply {
                    put("id", entity.id)
                    put("name", entity.name)
                    put("owner_name", entity.ownerName)
                    put("address", entity.address)
                    put("phone", entity.phone)
                    put("email", entity.email)
                    put("gstin", entity.gstin)
                    put("pan", entity.pan)
                    put("logo_path", entity.logoPath)
                    put("gst_enabled", entity.gstEnabled)
                    put("financial_year_start", entity.financialYearStart)
                    put("upi_id", entity.upiId)
                    put("bank_name", entity.bankName)
                    put("account_number", entity.accountNumber)
                    put("ifsc_code", entity.ifscCode)
                    put("terms_and_conditions", entity.termsAndConditions)
                    put("created_at", entity.createdAt)
                    put("updated_at", entity.updatedAt)
                }
                CloudSyncManager.getInstance(appContext).enqueueChange(
                    entityType = "business",
                    entityId = entity.id,
                    operation = "UPSERT",
                    payloadJson = payload.toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchBusiness(business: BusinessProfile) {
        saveBusinessesInternal(_businesses.value, business.shopName)
    }

    fun addBusiness(newProfile: BusinessProfile) {
        val current = _businesses.value.toMutableList()
        // Prevent duplicate names
        val filtered = current.filterNot { it.shopName.equals(newProfile.shopName.trim(), ignoreCase = true) }
        val updated = filtered + newProfile
        saveBusinessesInternal(updated, newProfile.shopName)
    }

    fun updateActiveBusiness(updatedProfile: BusinessProfile) {
        val current = _businesses.value.map {
            if (it.shopName == _activeBusiness.value.shopName) updatedProfile else it
        }
        saveBusinessesInternal(current, updatedProfile.shopName)
    }

    fun restoreBusinessesFromCloud(profiles: List<BusinessProfile>) {
        if (profiles.isNotEmpty()) {
            val hasCustom = profiles.any { !it.shopName.equals("HisabPro Enterprises", ignoreCase = true) }
            val resolvedProfiles = if (hasCustom) {
                profiles.filterNot { it.shopName.equals("HisabPro Enterprises", ignoreCase = true) }
            } else {
                profiles
            }
            val primary = resolvedProfiles.first()
            val primaryBizId = getBusinessDatabaseId(primary)
            saveBusinessesInternal(resolvedProfiles, primary.shopName)

            if (hasCustom) {
                scope.launch {
                    try {
                        db.runInTransaction {
                            db.openHelper.writableDatabase.execSQL(
                                "UPDATE OR IGNORE invoices SET business_id = ? WHERE business_id = 'default_business'",
                                arrayOf(primaryBizId)
                            )
                            db.openHelper.writableDatabase.execSQL(
                                "UPDATE OR IGNORE parties SET business_id = ? WHERE business_id = 'default_business'",
                                arrayOf(primaryBizId)
                            )
                            db.openHelper.writableDatabase.execSQL(
                                "UPDATE OR IGNORE items SET business_id = ? WHERE business_id = 'default_business'",
                                arrayOf(primaryBizId)
                            )
                            db.openHelper.writableDatabase.execSQL(
                                "UPDATE OR IGNORE khata_entries SET business_id = ? WHERE business_id = 'default_business'",
                                arrayOf(primaryBizId)
                            )
                            db.openHelper.writableDatabase.execSQL(
                                "UPDATE OR IGNORE payments SET business_id = ? WHERE business_id = 'default_business'",
                                arrayOf(primaryBizId)
                            )
                            db.openHelper.writableDatabase.execSQL(
                                "UPDATE OR IGNORE expenses SET business_id = ? WHERE business_id = 'default_business'",
                                arrayOf(primaryBizId)
                            )
                            db.openHelper.writableDatabase.execSQL(
                                "DELETE FROM businesses WHERE id = 'default_business'"
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun deleteBusiness(shopName: String): Boolean {
        if (_businesses.value.size <= 1) return false // Cannot delete the only business
        val toDelete = _businesses.value.find { it.shopName == shopName }
        val updated = _businesses.value.filterNot { it.shopName == shopName }
        val newActive = updated.first().shopName
        saveBusinessesInternal(updated, newActive)

        if (toDelete != null) {
            val deletedId = getBusinessDatabaseId(toDelete)
            scope.launch {
                try {
                    db.businessDao().deleteBusiness(deletedId)
                    CloudSyncManager.getInstance(appContext).enqueueChange(
                        entityType = "business",
                        entityId = deletedId,
                        operation = "DELETE",
                        payloadJson = "{}"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }
}
