package com.hisabpro.app.data.local

import android.content.Context
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object DatabaseMigrationHelper {

    fun migrateIfNecessary(context: Context, database: AppDatabase, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            try {
                // 1. Business Profile
                val existingBusiness = database.businessDao().getBusinessSync("default_business")
                if (existingBusiness == null) {
                    val settingsPrefs = context.getSharedPreferences("hisab_pro_settings_v1", Context.MODE_PRIVATE)
                    val businessName = settingsPrefs.getString("business_name", "Ganesh Traders") ?: "Ganesh Traders"
                    val isGst = settingsPrefs.getBoolean("is_gst_registered", false)
                    val gstin = settingsPrefs.getString("business_gstin", "") ?: ""
                    val phone = settingsPrefs.getString("business_phone", "9822012345") ?: "9822012345"
                    val address = settingsPrefs.getString("business_address", "Market Yard, Pune, Maharashtra 411037") ?: ""
                    val upiId = settingsPrefs.getString("business_upi_id", "ganeshtraders@okaxis") ?: ""

                    database.businessDao().insertOrUpdate(
                        BusinessEntity(
                            id = "default_business",
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
                val existingParties = database.partyDao().getAllPartiesSync("default_business")
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
                                    businessId = "default_business",
                                    name = obj.getString("name"),
                                    phone = obj.getString("phone"),
                                    address = obj.optString("address", ""),
                                    gstin = obj.optString("gstin", ""),
                                    type = obj.optString("type", "CUSTOMER"),
                                    tag = obj.optString("tag", "REGULAR"),
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
                                    partyId = obj.getString("partyId"),
                                    amount = obj.getDouble("amount"),
                                    type = obj.getString("type"),
                                    dateMillis = obj.getLong("dateMillis"),
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
                val existingItems = database.itemDao().getAllItemsSync("default_business")
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
                                    businessId = "default_business",
                                    name = obj.getString("name"),
                                    itemCode = obj.optString("itemCode", ""),
                                    category = obj.optString("category", "General"),
                                    unit = obj.optString("unit", "Pcs"),
                                    sellPrice = obj.optDouble("salePrice", 0.0),
                                    purchasePrice = obj.optDouble("purchasePrice", 0.0),
                                    gstRate = obj.optDouble("gstRate", 18.0),
                                    hsnCode = obj.optString("hsnCode", ""),
                                    stockQty = obj.optDouble("currentStock", 0.0),
                                    minStockAlert = obj.optDouble("minStockAlert", 5.0),
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
                val existingInvoices = database.invoiceDao().getAllInvoicesSync("default_business")
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
                                businessId = "default_business",
                                invoiceNo = obj.getString("invoiceNumber"),
                                dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                                customerName = obj.optString("customerName", ""),
                                customerPhone = obj.optString("customerPhone", ""),
                                customerAddress = obj.optString("customerAddress", ""),
                                customerGstin = obj.optString("customerGstin", ""),
                                type = obj.optString("invoiceType", if (isGst) "TAX_INVOICE" else "NON_GST_BILL"),
                                gstMode = obj.optString("gstMode", if (isGst) "INTRA_STATE" else "EXEMPT"),
                                discountAmount = obj.optDouble("discountAmount", 0.0),
                                notes = obj.optString("notes", ""),
                                paymentStatus = obj.optString("paymentStatus", "PAID"),
                                paidAmount = obj.optDouble("paidAmount", 0.0),
                                paymentMode = obj.optString("paymentMode", "Cash"),
                                isGst = isGst,
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )

                            val itemsList = mutableListOf<InvoiceItemEntity>()
                            val itemsArray = obj.optJSONArray("items")
                            if (itemsArray != null) {
                                for (j in 0 until itemsArray.length()) {
                                    val itemObj = itemsArray.getJSONObject(j)
                                    val qty = itemObj.optDouble("quantity", 1.0)
                                    val rate = itemObj.optDouble("unitPrice", 0.0)
                                    itemsList.add(
                                        InvoiceItemEntity(
                                            id = itemObj.getString("id"),
                                            invoiceId = invoiceId,
                                            itemName = itemObj.optString("description", ""),
                                            hsnCode = itemObj.optString("hsnCode", ""),
                                            qty = qty,
                                            unit = itemObj.optString("unit", "Pcs"),
                                            rate = rate,
                                            amount = qty * rate
                                        )
                                    )
                                }
                            }
                            database.invoiceDao().insertInvoiceWithItems(invoiceEntity, itemsList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
