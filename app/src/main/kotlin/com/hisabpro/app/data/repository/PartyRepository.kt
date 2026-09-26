package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyTag
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.util.toPaise
import com.hisabpro.app.util.toRupees
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PartyRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val activeBizId: String
        get() = BusinessManager.getInstance(context).activeBusinessDatabaseId
    private val scope = CoroutineScope(Dispatchers.IO)

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("hisab_pro_parties_$activeBizId", Context.MODE_PRIVATE)

    private val _parties = MutableStateFlow<List<Party>>(emptyList())
    val parties: StateFlow<List<Party>> = _parties.asStateFlow()

    private val _entries = MutableStateFlow<List<KhataEntry>>(emptyList())
    val entries: StateFlow<List<KhataEntry>> = _entries.asStateFlow()

    init {
        scope.launch {
            BusinessManager.getInstance(context).activeBusinessId.collect {
                reloadFromDatabase()
            }
        }
    }

    suspend fun reloadFromDatabase() {
        try {
            val dbParties = db.partyDao().getAllPartiesSync(activeBizId)
            val partiesList = dbParties.map { p ->
                val type = try { PartyType.valueOf(p.type) } catch (e: Exception) { PartyType.CUSTOMER }
                val tag = try { PartyTag.valueOf(p.tag) } catch (e: Exception) { PartyTag.REGULAR }
                Party(
                    id = p.id,
                    name = p.name,
                    phone = p.phone,
                    address = p.address,
                    gstin = p.gstin,
                    type = type,
                    tag = tag,
                    createdAt = p.createdAt
                )
            }
            _parties.value = partiesList
            savePartiesInternal(partiesList)

            val dbEntries = db.khataDao().getAllEntriesSync(activeBizId)
            val entriesList = dbEntries.map { e ->
                val type = try { KhataEntryType.valueOf(e.type) } catch (e: Exception) { KhataEntryType.YOU_GAVE }
                KhataEntry(
                    id = e.id,
                    partyId = e.partyId,
                    amount = e.amount.toRupees(),
                    type = type,
                    dateMillis = e.date,
                    billNumber = e.billNumber,
                    note = e.note
                )
            }
            _entries.value = entriesList
            saveEntriesInternal(entriesList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePartiesInternal(list: List<Party>) {
        val array = JSONArray()
        for (p in list) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("phone", p.phone)
                put("address", p.address)
                put("gstin", p.gstin)
                put("type", p.type.name)
                put("tag", p.tag.name)
                put("createdAt", p.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PARTIES, array.toString()).apply()
        _parties.value = list

        scope.launch {
            try {
                ensureActiveBusiness()
                val entities = list.map { p ->
                    PartyEntity(
                        id = p.id,
                        businessId = activeBizId,
                        name = p.name,
                        phone = p.phone,
                        address = p.address,
                        gstin = p.gstin,
                        type = p.type.name,
                        tag = p.tag.name,
                        createdAt = p.createdAt
                    )
                }
                db.partyDao().insertAllParties(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveEntriesInternal(list: List<KhataEntry>) {
        val array = JSONArray()
        for (e in list) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("partyId", e.partyId)
                put("amount", e.amount)
                put("type", e.type.name)
                put("dateMillis", e.dateMillis)
                put("billNumber", e.billNumber)
                put("note", e.note)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
        _entries.value = list

        scope.launch {
            try {
                ensureActiveBusiness()
                val entities = list.map { e ->
                    KhataEntryEntity(
                        id = e.id,
                        businessId = activeBizId,
                        partyId = e.partyId,
                        amount = e.amount.toPaise(),
                        type = e.type.name,
                        date = e.dateMillis,
                        billNumber = e.billNumber,
                        note = e.note
                    )
                }
                db.khataDao().insertAllEntries(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncToDatabase() {
        try {
            ensureActiveBusiness()
            val partyEntities = _parties.value.map { p ->
                PartyEntity(
                    id = p.id,
                    businessId = activeBizId,
                    name = p.name,
                    phone = p.phone,
                    address = p.address,
                    gstin = p.gstin,
                    type = p.type.name,
                    tag = p.tag.name,
                    createdAt = p.createdAt
                )
            }
            if (partyEntities.isNotEmpty()) {
                db.partyDao().insertAllParties(partyEntities)
            }
            val entryEntities = _entries.value.map { e ->
                KhataEntryEntity(
                    id = e.id,
                    partyId = e.partyId,
                    amount = e.amount.toPaise(),
                    type = e.type.name,
                    date = e.dateMillis,
                    billNumber = e.billNumber,
                    note = e.note
                )
            }
            if (entryEntities.isNotEmpty()) {
                db.khataDao().insertAllEntries(entryEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun ensureActiveBusiness() {
        try {
            val bizId = activeBizId
            val profile = BusinessManager.getInstance(context).activeBusiness.value
            val existing = db.businessDao().getBusinessSync(bizId)
            if (existing == null) {
                db.businessDao().insertOrUpdate(
                    BusinessEntity(
                        id = bizId,
                        name = profile.shopName.ifBlank { "HisabPro Business" },
                        phone = profile.phone,
                        address = profile.address,
                        gstin = profile.gstin,
                        gstEnabled = profile.isGstRegistered
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addParty(
        name: String,
        phone: String,
        address: String,
        gstin: String,
        type: PartyType,
        tag: PartyTag
    ): Party {
        val newParty = Party(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            gstin = gstin.trim().uppercase(),
            type = type,
            tag = tag,
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(newParty) + _parties.value
        savePartiesInternal(updated)
        
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("id", newParty.id)
                    put("business_id", activeBizId)
                    put("name", newParty.name)
                    put("phone", newParty.phone)
                    put("email", "")
                    put("address", newParty.address)
                    put("gstin", newParty.gstin)
                    put("type", newParty.type.name)
                    put("tag", newParty.tag.name)
                    put("opening_balance", 0L)
                    put("created_at", newParty.createdAt)
                    put("updated_at", newParty.createdAt)
                }
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "party",
                    entityId = newParty.id,
                    operation = "UPSERT",
                    payloadJson = payload.toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return newParty
    }

    fun updateParty(party: Party) {
        val updated = _parties.value.map {
            if (it.id == party.id) party else it
        }
        savePartiesInternal(updated)
        
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("id", party.id)
                    put("business_id", activeBizId)
                    put("name", party.name)
                    put("phone", party.phone)
                    put("email", "")
                    put("address", party.address)
                    put("gstin", party.gstin)
                    put("type", party.type.name)
                    put("tag", party.tag.name)
                    put("opening_balance", 0L)
                    put("created_at", party.createdAt)
                    put("updated_at", System.currentTimeMillis())
                }
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "party",
                    entityId = party.id,
                    operation = "UPSERT",
                    payloadJson = payload.toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteParty(partyId: String) {
        val updatedParties = _parties.value.filterNot { it.id == partyId }
        val updatedEntries = _entries.value.filterNot { it.partyId == partyId }
        savePartiesInternal(updatedParties)
        saveEntriesInternal(updatedEntries)
        scope.launch {
            try {
                db.partyDao().deleteParty(partyId)
                db.khataDao().deleteEntriesForParty(partyId)
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "party",
                    entityId = partyId,
                    operation = "DELETE",
                    payloadJson = "{}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addKhataEntry(
        partyId: String,
        amount: Double,
        type: KhataEntryType,
        dateMillis: Long,
        billNumber: String,
        note: String
    ): KhataEntry {
        val newEntry = KhataEntry(
            id = UUID.randomUUID().toString(),
            partyId = partyId,
            amount = amount,
            type = type,
            dateMillis = dateMillis,
            billNumber = billNumber.trim(),
            note = note.trim()
        )
        val updated = listOf(newEntry) + _entries.value
        saveEntriesInternal(updated)
        
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("id", newEntry.id)
                    put("party_id", newEntry.partyId)
                    put("amount", newEntry.amount.toPaise())
                    put("type", newEntry.type.name)
                    put("date", newEntry.dateMillis)
                    put("bill_number", newEntry.billNumber)
                    put("note", newEntry.note)
                    put("created_at", newEntry.dateMillis)
                    put("updated_at", newEntry.dateMillis)
                }
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "khata_entry",
                    entityId = newEntry.id,
                    operation = "UPSERT",
                    payloadJson = payload.toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return newEntry
    }

    fun deleteKhataEntry(entryId: String) {
        val updated = _entries.value.filterNot { it.id == entryId }
        saveEntriesInternal(updated)
        scope.launch {
            try {
                db.khataDao().deleteEntry(entryId)
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "khata_entry",
                    entityId = entryId,
                    operation = "DELETE",
                    payloadJson = "{}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getEntriesForParty(partyId: String): List<KhataEntry> {
        return _entries.value
            .filter { it.partyId == partyId }
            .sortedByDescending { it.dateMillis }
    }

    fun resetToDemo() {
        val (initialParties, initialEntries) = createInitialData()
        savePartiesInternal(initialParties)
        saveEntriesInternal(initialEntries)
    }

    private fun createInitialData(): Pair<List<Party>, List<KhataEntry>> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        val p1 = Party(
            id = "p_sharma_kirana",
            name = "Ramesh Sharma (Kirana Store)",
            phone = "+91 98765 43210",
            address = "Shop 12, Main Market, Mumbai",
            gstin = "27AAAAA1234A1Z5",
            type = PartyType.CUSTOMER,
            tag = PartyTag.REGULAR,
            createdAt = now - (day * 30)
        )
        val p2 = Party(
            id = "p_gupta_hardware",
            name = "Gupta Hardware & Tools",
            phone = "+91 98123 45678",
            address = "Plot 45, Industrial Area, Pune",
            gstin = "27BBBBB5678B2Z6",
            type = PartyType.SUPPLIER,
            tag = PartyTag.REGULAR,
            createdAt = now - (day * 45)
        )
        val p3 = Party(
            id = "p_anjali_verma",
            name = "Anjali Verma",
            phone = "+91 97654 32109",
            address = "B-204, Green Heights, Andheri",
            gstin = "",
            type = PartyType.CUSTOMER,
            tag = PartyTag.OCCASIONAL,
            createdAt = now - (day * 15)
        )
        val p4 = Party(
            id = "p_apex_mart",
            name = "Apex Wholesale Supplies",
            phone = "+91 98220 11223",
            address = "GIDC Estate, Surat",
            gstin = "24CCCCC9999C1Z1",
            type = PartyType.SUPPLIER,
            tag = PartyTag.REGULAR,
            createdAt = now - (day * 60)
        )
        val p5 = Party(
            id = "p_vikram_patel",
            name = "Vikram Patel",
            phone = "+91 99887 76655",
            address = "Station Road, Ahmedabad",
            gstin = "",
            type = PartyType.CUSTOMER,
            tag = PartyTag.BLOCKED,
            createdAt = now - (day * 90)
        )

        val parties = listOf(p1, p2, p3, p4, p5)

        val entries = listOf(
            // Ramesh Sharma: customer took goods for 12,500, paid 5,000 -> Net: 7,500 receivable
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p1.id,
                amount = 12500.0,
                type = KhataEntryType.YOU_GAVE,
                dateMillis = now - (day * 4),
                billNumber = "INV-2024-089",
                note = "Wholesale grocery supplies on 15-day credit"
            ),
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p1.id,
                amount = 5000.0,
                type = KhataEntryType.YOU_GOT,
                dateMillis = now - (day * 1),
                billNumber = "REC-4410",
                note = "UPI partial payment received"
            ),
            // Gupta Hardware: supplier provided goods 24,000, we paid 14,000 -> Net: 10,000 payable
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p2.id,
                amount = 24000.0,
                type = KhataEntryType.YOU_GOT,
                dateMillis = now - (day * 10),
                billNumber = "BILL-8921",
                note = "Raw materials and power tools shipment"
            ),
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p2.id,
                amount = 14000.0,
                type = KhataEntryType.YOU_GAVE,
                dateMillis = now - (day * 3),
                billNumber = "NEFT-7812",
                note = "Bank transfer payment to vendor"
            ),
            // Anjali Verma: customer took goods 3,200, paid 3,200 -> Settled
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p3.id,
                amount = 3200.0,
                type = KhataEntryType.YOU_GAVE,
                dateMillis = now - (day * 6),
                billNumber = "INV-102",
                note = "Occasional order"
            ),
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p3.id,
                amount = 3200.0,
                type = KhataEntryType.YOU_GOT,
                dateMillis = now - (day * 2),
                billNumber = "CASH-991",
                note = "Settled full in cash"
            ),
            // Vikram Patel: Blocked customer, took 8,400, unpaid
            KhataEntry(
                id = UUID.randomUUID().toString(),
                partyId = p5.id,
                amount = 8400.0,
                type = KhataEntryType.YOU_GAVE,
                dateMillis = now - (day * 40),
                billNumber = "INV-071",
                note = "Overdue credit - phone unanswered"
            )
        )

        return Pair(parties, entries)
    }

    companion object {
        private const val KEY_PARTIES = "parties_data_list"
        private const val KEY_ENTRIES = "khata_entries_list"

        @Volatile
        private var INSTANCE: PartyRepository? = null

        fun getInstance(context: Context): PartyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PartyRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
