package com.hisabpro.app.data.local

import android.content.Context
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.util.toPaise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

object DatabaseMigrationHelper {

    fun migrateIfNecessary(context: Context, database: AppDatabase, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            try {
                // Remove any rogue/accidental dummy businesses from previous migrations
                database.businessDao().deleteBusiness("biz_ganesh_traders")

                val settingsPrefs = context.getSharedPreferences("hisab_pro_settings_v1", Context.MODE_PRIVATE)
                val businessName = settingsPrefs.getString("business_name", null)
                if (businessName.isNullOrBlank() || businessName.equals("Ganesh Traders", ignoreCase = true)) {
                    // No legitimate legacy business name found; skip migration to prevent dummy company creation
                    return@launch
                }
                val sanitized = businessName.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { return@launch }
                val targetBusinessId = "biz_$sanitized"

                // 1. Business Profile
                val existingBusiness = database.businessDao().getBusinessSync(targetBusinessId)
                if (existingBusiness == null) {
                    val isGst = settingsPrefs.getBoolean("is_gst_registered", false)
                    val gstin = settingsPrefs.getString("business_gstin", "") ?: ""
                    val phone = settingsPrefs.getString("business_phone", "") ?: ""
                    val address = settingsPrefs.getString("business_address", "") ?: ""
                    val upiId = settingsPrefs.getString("business_upi_id", "") ?: ""

                    database.businessDao().insertOrUpdate(
                        BusinessEntity(
                            id = targetBusinessId,
                            name = businessName,
                            phone = phone,
                            address = address,
                            gstin = gstin,
                            gstEnabled = isGst,
                            upiId = upiId
                        )
                    )
                }

                // 2. Parties and Khata Entries
                val existingParties = database.partyDao().getAllPartiesSync(targetBusinessId)
                if (existingParties.isEmpty()) {
                    val partyPrefs = context.getSharedPreferences("hisab_pro_parties_v1", Context.MODE_PRIVATE)
                    val partiesJson = partyPrefs.getString("parties_list_v1", null)
                    val entriesJson = partyPrefs.getString("khata_entries_v1", null)

                    val partyEntities = mutableListOf<PartyEntity>()
                    val entryEntities = mutableListOf<KhataEntryEntity>()

                    if (!partiesJson.isNullOrBlank()) {
                        val pArray = JSONArray(partiesJson)
                        for (i in 0 until pArray.length()) {
                            val obj = pArray.getJSONObject(i)
                            partyEntities.add(
                                PartyEntity(
                                    id = obj.getString("id"),
                                    businessId = targetBusinessId,
                                    name = obj.getString("name"),
                                    phone = obj.getString("phone"),
                                    address = obj.optString("address", ""),
                                    gstin = obj.optString("gstin", ""),
                                    type = obj.optString("type", "CUSTOMER"),
                                    tag = obj.optString("tag", "REGULAR"),
                                    openingBalance = obj.optDouble("openingBalance", 0.0).toPaise(),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    if (!entriesJson.isNullOrBlank()) {
                        val eArray = JSONArray(entriesJson)
                        for (i in 0 until eArray.length()) {
                            val obj = eArray.getJSONObject(i)
                            entryEntities.add(
                                KhataEntryEntity(
                                    id = obj.getString("id"),
                                    businessId = targetBusinessId,
                                    partyId = obj.getString("partyId"),
                                    amount = obj.getDouble("amount").toPaise(),
                                    type = obj.getString("type"),
                                    date = obj.getLong("dateMillis"),
                                    billNumber = obj.optString("billNumber", ""),
                                    note = obj.optString("note", "")
                                )
                            )
                        }
                    }

                    if (partyEntities.isNotEmpty()) {
                        database.partyDao().insertAllParties(partyEntities)
                    }
                    if (entryEntities.isNotEmpty()) {
                        database.khataDao().insertAllEntries(entryEntities)
                    }
                }

                // 3. Items / Products
                val existingItems = database.itemDao().getAllItemsSync(targetBusinessId)
                if (existingItems.isEmpty()) {
                    val itemPrefs = context.getSharedPreferences("hisab_pro_items_v1", Context.MODE_PRIVATE)
                    val itemsJson = itemPrefs.getString("inventory_items_v1", null)
                    if (!itemsJson.isNullOrBlank()) {
                        val array = JSONArray(itemsJson)
                        val itemEntities = mutableListOf<ItemEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            itemEntities.add(
                                ItemEntity(
                                    id = obj.getString("id"),
                                    businessId = targetBusinessId,
                                    name = obj.getString("name"),
                                    itemCode = obj.optString("itemCode", ""),
                                    category = obj.optString("category", "General"),
                                    unit = obj.optString("unit", "Pcs"),
                                    sellPrice = obj.optDouble("salePrice", 0.0).toPaise(),
                                    purchasePrice = obj.optDouble("purchasePrice", 0.0).toPaise(),
                                    gstRate = obj.optDouble("gstRate", 18.0),
                                    hsnCode = obj.optString("hsnCode", ""),
                                    stockQty = obj.optDouble("currentStock", 0.0),
                                    lowStockThreshold = obj.optDouble("minStockAlert", 5.0),
                                    createdAt = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                                )
                            )
                        }
                        if (itemEntities.isNotEmpty()) {
                            database.itemDao().insertAllItems(itemEntities)
                        }
                    }
                }

                // 4. Invoices
                val existingInvoices = database.invoiceDao().getAllInvoicesSync(targetBusinessId)
                if (existingInvoices.isEmpty()) {
                    val invoicePrefs = context.getSharedPreferences("hisab_pro_invoices_v1", Context.MODE_PRIVATE)
                    val invoicesJson = invoicePrefs.getString("invoices_list_v1", null)
                    if (!invoicesJson.isNullOrBlank()) {
                        val array = JSONArray(invoicesJson)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val invoiceId = obj.getString("id")
                            val isGst = obj.optBoolean("isGst", false)
                            val invoiceEntity = InvoiceEntity(
                                id = invoiceId,
                                businessId = targetBusinessId,
                                invoiceNo = obj.getString("invoiceNumber"),
                                date = obj.optLong("dateMillis", System.currentTimeMillis()),
                                customerName = obj.optString("customerName", ""),
                                customerPhone = obj.optString("customerPhone", ""),
                                customerAddress = obj.optString("customerAddress", ""),
                                customerGstin = obj.optString("customerGstin", ""),
                                type = obj.optString("invoiceType", if (isGst) "TAX_INVOICE" else "NON_GST_BILL"),
                                gstMode = obj.optString("gstMode", if (isGst) "INTRA_STATE" else "EXEMPT"),
                                discount = obj.optDouble("discountAmount", 0.0).toPaise(),
                                notes = obj.optString("notes", ""),
                                paymentStatus = obj.optString("paymentStatus", "PAID"),
                                paymentMode = obj.optString("paymentMode", "Cash"),
                                isGst = isGst
                            )
                            database.invoiceDao().insertInvoice(invoiceEntity)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
