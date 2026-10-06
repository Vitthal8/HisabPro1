package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.model.BankDetails
import com.hisabpro.app.data.model.BusinessProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _activeBusinessId = MutableStateFlow("")
    val activeBusinessId: StateFlow<String> = _activeBusinessId.asStateFlow()

    private val _activeBusiness = MutableStateFlow(BusinessProfile())
    val activeBusiness: StateFlow<BusinessProfile> = _activeBusiness.asStateFlow()

    val activeBusinessDatabaseId: String
        get() {
            val active = _activeBusiness.value
            if (active.id.isNotBlank()) return active.id
            val name = active.shopName.trim()
            if (name.isBlank()) return "biz_main_store"
            val sanitized = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
            return "biz_$sanitized"
        }

    val activeBusinessDatabaseIdFlow: StateFlow<String> = _activeBusiness
        .map { profile ->
            if (profile.id.isNotBlank()) {
                profile.id
            } else {
                val name = profile.shopName.trim()
                if (name.isBlank()) {
                    "biz_main_store"
                } else {
                    val sanitized = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    "biz_$sanitized"
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = "biz_main_store"
        )

    init {
        loadBusinesses()
    }

    companion object {
        private const val KEY_BUSINESS_LIST = "key_business_list_json"
        private const val KEY_ACTIVE_ID = "key_active_business_id"
        private const val KEY_DELETED_BUSINESS_IDS = "key_deleted_business_ids"
        private const val KEY_DELETED_TIMESTAMPS_JSON = "key_deleted_business_timestamps_json"

        @Volatile
        private var INSTANCE: BusinessManager? = null

        fun getInstance(context: Context): BusinessManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BusinessManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun isBusinessDeleted(businessIdOrName: String): Boolean {
        if (businessIdOrName.isBlank()) return false
        val deletedSet = prefs.getStringSet(KEY_DELETED_BUSINESS_IDS, emptySet()) ?: emptySet()
        val sanitized = sanitizeBizId(businessIdOrName)
        return deletedSet.contains(businessIdOrName) ||
               deletedSet.contains(sanitized) ||
               deletedSet.any { it.equals(businessIdOrName, ignoreCase = true) }
    }

    fun getBusinessDeletedTimestamp(businessIdOrName: String): Long? {
        if (businessIdOrName.isBlank()) return null
        if (!isBusinessDeleted(businessIdOrName)) return null
        val rawJson = prefs.getString(KEY_DELETED_TIMESTAMPS_JSON, null) ?: return System.currentTimeMillis()
        return try {
            val obj = JSONObject(rawJson)
            val sanitized = sanitizeBizId(businessIdOrName)
            when {
                obj.has(businessIdOrName) -> obj.getLong(businessIdOrName)
                obj.has(sanitized) -> obj.getLong(sanitized)
                else -> System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun isRogueOrDeleted(profile: BusinessProfile): Boolean {
        if (isBusinessDeleted(profile.id) || isBusinessDeleted(profile.shopName)) return true
        if (profile.shopName.contains("[Restored]", ignoreCase = true) || profile.id.contains("[Restored]")) return true
        if (profile.shopName.equals("Ganesh Traders", ignoreCase = true) || profile.id == "biz_ganesh_traders") return true
        return false
    }

    fun markBusinessDeleted(businessIdOrName: String, timestamp: Long = System.currentTimeMillis()) {
        if (businessIdOrName.isBlank()) return
        val currentSet = prefs.getStringSet(KEY_DELETED_BUSINESS_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(businessIdOrName)
        val sanitized = sanitizeBizId(businessIdOrName)
        if (sanitized != "default_business") {
            currentSet.add(sanitized)
        }

        val rawJson = prefs.getString(KEY_DELETED_TIMESTAMPS_JSON, null)
        val obj = try {
            if (!rawJson.isNullOrBlank()) JSONObject(rawJson) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
        obj.put(businessIdOrName, timestamp)
        if (sanitized != "default_business") {
            obj.put(sanitized, timestamp)
        }

        prefs.edit()
            .putStringSet(KEY_DELETED_BUSINESS_IDS, currentSet)
            .putString(KEY_DELETED_TIMESTAMPS_JSON, obj.toString())
            .apply()
    }

    private fun loadBusinesses() {
        val rawJson = prefs.getString(KEY_BUSINESS_LIST, null)
        val activeId = prefs.getString(KEY_ACTIVE_ID, "") ?: ""
        _activeBusinessId.value = activeId

        if (!rawJson.isNullOrBlank()) {
            try {
                val array = JSONArray(rawJson)
                val list = mutableListOf<BusinessProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val parsed = parseProfileFromJson(obj)
                    if (!isRogueOrDeleted(parsed)) {
                        list.add(parsed)
                    }
                }
                if (list.isNotEmpty()) {
                    _businesses.value = list
                    val matched = list.find { it.id == activeId || it.shopName == activeId || it.gstin == activeId } ?: list.first()
                    _activeBusiness.value = matched
                    _activeBusinessId.value = matched.id.ifBlank { matched.shopName }
                    SettingsRepository.getInstance(appContext).updateProfile(matched)
                    persistBusinessesToPrefs(list, matched.id.ifBlank { matched.shopName })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        scope.launch {
            reloadFromRoom()
            syncToDatabase(enqueueForSync = false)
        }
    }

    suspend fun reloadFromRoom() {
        try {
            // Clean up rogue legacy entities
            db.businessDao().deleteBusiness("biz_ganesh_traders")

            val dbEntities = db.businessDao().getAllBusinessesSync()
            if (dbEntities.isEmpty()) return

            val currentList = _businesses.value.filterNot { isRogueOrDeleted(it) }.toMutableList()
            val map = currentList.associateBy { it.id.ifBlank { sanitizeBizId(it.shopName) } }.toMutableMap()

            for (ent in dbEntities) {
                if (ent.id == "biz_ganesh_traders" || 
                    ent.name.contains("Ganesh Traders", ignoreCase = true) || 
                    ent.name.contains("[Restored]", ignoreCase = true) ||
                    isBusinessDeleted(ent.id) || 
                    isBusinessDeleted(ent.name)) {
                    db.businessDao().deleteBusiness(ent.id)
                    markBusinessDeleted(ent.id)
                    markBusinessDeleted(ent.name)
                    continue
                }
                val existing = map[ent.id]
                val merged = BusinessProfile(
                    id = ent.id,
                    shopName = ent.name.ifBlank { existing?.shopName ?: "" },
                    ownerName = ent.ownerName.ifBlank { existing?.ownerName ?: "" },
                    phone = ent.phone.ifBlank { existing?.phone ?: "" },
                    email = ent.email.ifBlank { existing?.email ?: "" },
                    address = ent.address.ifBlank { existing?.address ?: "" },
                    isGstRegistered = ent.gstEnabled,
                    gstin = ent.gstin.ifBlank { existing?.gstin ?: "" },
                    pan = ent.pan.ifBlank { existing?.pan ?: "" },
                    upiId = ent.upiId.ifBlank { existing?.upiId ?: "" },
                    bankName = ent.bankName.ifBlank { existing?.bankName ?: "" },
                    accountNumber = ent.accountNumber.ifBlank { existing?.accountNumber ?: "" },
                    ifscCode = ent.ifscCode.ifBlank { existing?.ifscCode ?: "" },
                    termsAndConditions = ent.termsAndConditions.ifBlank { existing?.termsAndConditions ?: "" },
                    logoPath = ent.logoPath.ifBlank { existing?.logoPath ?: "" },
                    hasCompletedOnboarding = true
                )
                map[ent.id] = merged
            }

            val updatedList = map.values.toList()
            if (updatedList.isNotEmpty()) {
                _businesses.value = updatedList
                val currentActiveId = _activeBusinessId.value
                val matched = updatedList.find { it.id == currentActiveId || it.shopName == currentActiveId } ?: updatedList.first()
                _activeBusiness.value = matched
                SettingsRepository.getInstance(appContext).updateProfile(matched)
                persistBusinessesToPrefs(updatedList, matched.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sanitizeBizId(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return "default_business"
        val sanitized = trimmed.lowercase().replace(Regex("[^a-z0-9]"), "_")
        return "biz_$sanitized"
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
        val primaryBank = obj.optString("bankName", "")
        val primaryAcc = obj.optString("accountNumber", "")
        val primaryIfsc = obj.optString("ifscCode", "")
        if (loadedBankAccounts.isEmpty() && primaryBank.isNotBlank()) {
            loadedBankAccounts.add(BankDetails(primaryBank, primaryAcc, primaryIfsc))
        }

        return BusinessProfile(
            id = obj.optString("id", ""),
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
            termsAndConditions = obj.optString("termsAndConditions", "1. Goods once sold cannot be returned.\n2. Due in 15 days."),
            logoPath = obj.optString("logoPath", ""),
            isThermalPrinterMode = obj.optBoolean("isThermalPrinterMode", false),
            showUpiQrOnInvoice = obj.optBoolean("showUpiQrOnInvoice", true),
            appLanguage = obj.optString("appLanguage", "en"),
            isDarkMode = obj.optBoolean("isDarkMode", false),
            themeAccent = obj.optString("themeAccent", "Saffron"),
            hasCompletedOnboarding = obj.optBoolean("hasCompletedOnboarding", false) || obj.optString("shopName", "").isNotBlank()
        )
    }

    private fun saveBusinessesInternal(list: List<BusinessProfile>, activeId: String) {
        _businesses.value = list
        _activeBusinessId.value = activeId
        val active = list.find { it.shopName == activeId || it.gstin == activeId || it.id == activeId } ?: list.firstOrNull() ?: BusinessProfile()
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
                put("id", p.id)
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

    suspend fun syncToDatabase(enqueueForSync: Boolean = true) {
        try {
            val entities = _businesses.value.map { p ->
                val name = p.shopName.trim()
                val bizId = if (p.id.isNotBlank()) {
                    p.id
                } else if (name.isNotBlank()) {
                    val sanitized = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
                    "biz_$sanitized"
                } else {
                    error("No active business selected")
                }
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
                    bankName = p.bankName,
                    accountNumber = p.accountNumber,
                    ifscCode = p.ifscCode,
                    termsAndConditions = p.termsAndConditions
                )
            }
            db.businessDao().insertAllBusinesses(entities)

            if (enqueueForSync) {
                for (entity in entities) {
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
                    com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                        entityType = "business",
                        entityId = entity.id,
                        operation = "UPSERT",
                        payloadJson = payload.toString()
                    )
                }
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).triggerSync(isManual = false)
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
        val current = _businesses.value
        val updated = if (current.isEmpty()) {
            listOf(updatedProfile)
        } else {
            current.map {
                if (it.shopName == _activeBusiness.value.shopName || (it.id.isNotBlank() && it.id == _activeBusiness.value.id)) updatedProfile else it
            }
        }
        saveBusinessesInternal(updated, updatedProfile.shopName)
    }

    fun resetForNewUser(initialProfile: BusinessProfile? = null) {
        prefs.edit().clear().apply()
        if (initialProfile != null && initialProfile.shopName.isNotBlank()) {
            val list = listOf(initialProfile)
            _businesses.value = list
            _activeBusinessId.value = initialProfile.id.ifBlank { initialProfile.shopName }
            _activeBusiness.value = initialProfile
            persistBusinessesToPrefs(list, initialProfile.id.ifBlank { initialProfile.shopName })
        } else {
            _businesses.value = emptyList()
            _activeBusinessId.value = ""
            _activeBusiness.value = BusinessProfile(
                id = "",
                shopName = "",
                ownerName = "",
                email = "",
                phone = "",
                hasCompletedOnboarding = false
            )
        }
    }

    fun restoreBusinessesFromCloud(profiles: List<BusinessProfile>, replaceLocal: Boolean = false) {
        scope.launch {
            reloadFromRoomAndCloud(profiles, replaceLocal)
        }
    }

    suspend fun reloadFromRoomAndCloud(cloudProfiles: List<BusinessProfile> = emptyList(), replaceLocal: Boolean = false) {
        try {
            val validCloudProfiles = cloudProfiles.filterNot { isBusinessDeleted(it.id) || isBusinessDeleted(it.shopName) }
            if (validCloudProfiles.isNotEmpty()) {
                val entities = validCloudProfiles.map { p ->
                    val bizId = if (p.id.isNotBlank()) p.id else sanitizeBizId(p.shopName)
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
                        bankName = p.bankName,
                        accountNumber = p.accountNumber,
                        ifscCode = p.ifscCode,
                        termsAndConditions = p.termsAndConditions
                    )
                }
                db.businessDao().insertAllBusinesses(entities)
            }

            val dbEntities = db.businessDao().getAllBusinessesSync()
            val currentList = if (replaceLocal) mutableListOf() else _businesses.value.filterNot { isBusinessDeleted(it.id) || isBusinessDeleted(it.shopName) }.toMutableList()
            val map = currentList.associateBy { if (it.id.isNotBlank()) it.id else sanitizeBizId(it.shopName) }.toMutableMap()

            for (p in validCloudProfiles) {
                val bizId = if (p.id.isNotBlank()) p.id else sanitizeBizId(p.shopName)
                map[bizId] = p.copy(id = bizId)
            }

            if (!replaceLocal) {
                for (ent in dbEntities) {
                    if (isBusinessDeleted(ent.id) || isBusinessDeleted(ent.name)) continue
                    val existing = map[ent.id]
                    val merged = BusinessProfile(
                        id = ent.id,
                        shopName = ent.name.ifBlank { existing?.shopName ?: "" },
                        ownerName = ent.ownerName.ifBlank { existing?.ownerName ?: "" },
                        phone = ent.phone.ifBlank { existing?.phone ?: "" },
                        email = ent.email.ifBlank { existing?.email ?: "" },
                        address = ent.address.ifBlank { existing?.address ?: "" },
                        isGstRegistered = ent.gstEnabled,
                        gstin = ent.gstin.ifBlank { existing?.gstin ?: "" },
                        pan = ent.pan.ifBlank { existing?.pan ?: "" },
                        upiId = ent.upiId.ifBlank { existing?.upiId ?: "" },
                        bankName = ent.bankName.ifBlank { existing?.bankName ?: "" },
                        accountNumber = ent.accountNumber.ifBlank { existing?.accountNumber ?: "" },
                        ifscCode = ent.ifscCode.ifBlank { existing?.ifscCode ?: "" },
                        termsAndConditions = ent.termsAndConditions.ifBlank { existing?.termsAndConditions ?: "" },
                        logoPath = ent.logoPath.ifBlank { existing?.logoPath ?: "" },
                        hasCompletedOnboarding = true
                    )
                    map[ent.id] = merged
                }
            } else {
                for (ent in dbEntities) {
                    if (isBusinessDeleted(ent.id) || isBusinessDeleted(ent.name)) continue
                    if (map.containsKey(ent.id)) {
                        // Preserved from cloudProfiles
                    } else if (validCloudProfiles.isEmpty() && ent.name.isNotBlank()) {
                        map[ent.id] = BusinessProfile(
                            id = ent.id,
                            shopName = ent.name,
                            ownerName = ent.ownerName,
                            phone = ent.phone,
                            email = ent.email,
                            address = ent.address,
                            isGstRegistered = ent.gstEnabled,
                            gstin = ent.gstin,
                            pan = ent.pan,
                            upiId = ent.upiId,
                            bankName = ent.bankName,
                            accountNumber = ent.accountNumber,
                            ifscCode = ent.ifscCode,
                            termsAndConditions = ent.termsAndConditions,
                            logoPath = ent.logoPath,
                            hasCompletedOnboarding = true
                        )
                    }
                }
            }

            val updatedList = map.values.toList()
            if (updatedList.isNotEmpty()) {
                _businesses.value = updatedList
                val currentActiveId = _activeBusinessId.value
                val matched = updatedList.find { it.id == currentActiveId || it.shopName == currentActiveId } ?: updatedList.first()
                _activeBusiness.value = matched
                _activeBusinessId.value = matched.id.ifBlank { matched.shopName }
                SettingsRepository.getInstance(appContext).updateProfile(matched)
                persistBusinessesToPrefs(updatedList, matched.id.ifBlank { matched.shopName })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistBusinessesToPrefs(list: List<BusinessProfile>, activeId: String) {
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
                put("id", p.id)
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
    }

    suspend fun deleteBusinessCascade(businessIdOrShopName: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val list = _businesses.value
            if (list.size <= 1) {
                return@withContext Result.failure(Exception("Cannot delete company: At least one business profile must remain."))
            }

            val targetProfile = list.find { 
                it.id == businessIdOrShopName || 
                it.shopName.equals(businessIdOrShopName, ignoreCase = true) ||
                sanitizeBizId(it.shopName) == businessIdOrShopName
            } ?: return@withContext Result.failure(Exception("Company profile '$businessIdOrShopName' not found."))

            val targetDbId = targetProfile.id.ifBlank { sanitizeBizId(targetProfile.shopName) }
            val now = System.currentTimeMillis()

            // 1. Mark as permanently deleted in local persistent tombstone set with LWW timestamp
            markBusinessDeleted(targetDbId, now)
            if (targetProfile.id.isNotBlank()) markBusinessDeleted(targetProfile.id, now)
            markBusinessDeleted(targetProfile.shopName, now)
            val nameSanitized = sanitizeBizId(targetProfile.shopName)
            if (nameSanitized != "default_business") {
                markBusinessDeleted(nameSanitized, now)
            }

            // 2. Atomically delete all dependent database records and insert SyncOutbox DELETE event
            db.businessDao().deleteBusinessCascadeLocally(
                businessId = targetDbId,
                outboxDao = db.syncOutboxDao()
            )

            // 3. Enqueue DELETE in CloudSyncManager syncQueue so executePush will upload deletion to Supabase
            val delPayload = JSONObject().apply {
                put("id", targetDbId)
                put("user_id", com.hisabpro.app.data.sync.SupabaseAuthManager.getInstance(appContext).getCurrentSession()?.userId ?: "")
                put("deleted_at", now)
                put("updated_at", now)
            }
            com.hisabpro.app.data.sync.CloudSyncManager.getInstance(appContext).enqueueChange(
                entityType = "business",
                entityId = targetDbId,
                operation = "DELETE",
                payloadJson = delPayload.toString()
            )

            // 4. Remove company from active in-memory list and switch active business
            val updatedList = list.filterNot { 
                it.id == targetProfile.id || 
                it.shopName.equals(targetProfile.shopName, ignoreCase = true) ||
                it.id == targetDbId
            }
            val newActive = updatedList.first()

            saveBusinessesInternal(updatedList, newActive.shopName)

            // 5. Refresh active in-memory repositories
            InvoiceRepository.getInstance(appContext).reloadFromDatabase()
            PartyRepository.getInstance(appContext).reloadFromDatabase()
            ItemRepository.getInstance(appContext).reloadFromDatabase()
            TransactionRepository.getInstance(appContext).reloadFromDatabase()

            // 6. Trigger sync worker to upload deletion to Supabase
            com.hisabpro.app.data.sync.CloudSyncManager.getInstance(appContext).triggerSync(isManual = false)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun deleteBusiness(shopName: String): Boolean {
        if (_businesses.value.size <= 1) return false // Cannot delete the only business
        val updated = _businesses.value.filterNot { it.shopName == shopName }
        val newActive = updated.first().shopName
        saveBusinessesInternal(updated, newActive)
        return true
    }
}
