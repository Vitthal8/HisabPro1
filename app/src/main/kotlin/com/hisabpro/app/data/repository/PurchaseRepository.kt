package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.PurchaseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class PurchaseRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_pro_purchases_v1", Context.MODE_PRIVATE)

    private val _purchases = MutableStateFlow<List<PurchaseBill>>(emptyList())
    val purchases: StateFlow<List<PurchaseBill>> = _purchases.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val json = prefs.getString(KEY_PURCHASES, null)
        if (json.isNullOrBlank()) {
            val initial = createInitialPurchases()
            saveInternal(initial)
        } else {
            try {
                val list = mutableListOf<PurchaseBill>()
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val itemsList = mutableListOf<PurchaseItem>()
                    val itemsArray = obj.getJSONArray("items")
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        itemsList.add(
                            PurchaseItem(
                                id = itemObj.getString("id"),
                                itemId = if (itemObj.isNull("itemId")) null else itemObj.getString("itemId"),
                                description = itemObj.getString("description"),
                                hsnCode = itemObj.optString("hsnCode", ""),
                                quantity = itemObj.getDouble("quantity"),
                                unit = itemObj.optString("unit", "Pcs"),
                                unitPrice = itemObj.getDouble("unitPrice"),
                                gstRate = itemObj.optDouble("gstRate", 18.0)
                            )
                        )
                    }

                    list.add(
                        PurchaseBill(
                            id = obj.getString("id"),
                            purchaseNumber = obj.getString("purchaseNumber"),
                            vendorBillNumber = obj.optString("vendorBillNumber", ""),
                            supplierId = if (obj.isNull("supplierId")) null else obj.getString("supplierId"),
                            supplierName = obj.optString("supplierName", "Distributor"),
                            supplierPhone = obj.optString("supplierPhone", ""),
                            supplierAddress = obj.optString("supplierAddress", ""),
                            supplierGstin = obj.optString("supplierGstin", ""),
                            dateMillis = obj.getLong("dateMillis"),
                            dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
                            items = itemsList,
                            discountAmount = obj.optDouble("discountAmount", 0.0),
                            notes = obj.optString("notes", ""),
                            paymentStatus = InvoiceStatus.fromString(obj.optString("paymentStatus", "PAID")),
                            paidAmount = obj.optDouble("paidAmount", 0.0),
                            paymentMode = obj.optString("paymentMode", "Bank Transfer"),
                            itcEligible = obj.optBoolean("itcEligible", true),
                            gstMode = GstMode.fromString(obj.optString("gstMode", "INTRA_STATE")),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _purchases.value = list.sortedByDescending { it.dateMillis }
            } catch (e: Exception) {
                val initial = createInitialPurchases()
                saveInternal(initial)
            }
        }
    }

    private fun saveInternal(list: List<PurchaseBill>) {
        val array = JSONArray()
        for (bill in list) {
            val obj = JSONObject().apply {
                put("id", bill.id)
                put("purchaseNumber", bill.purchaseNumber)
                put("vendorBillNumber", bill.vendorBillNumber)
                if (bill.supplierId != null) put("supplierId", bill.supplierId) else put("supplierId", JSONObject.NULL)
                put("supplierName", bill.supplierName)
                put("supplierPhone", bill.supplierPhone)
                put("supplierAddress", bill.supplierAddress)
                put("supplierGstin", bill.supplierGstin)
                put("dateMillis", bill.dateMillis)
                put("dueDateMillis", bill.dueDateMillis)
                put("discountAmount", bill.discountAmount)
                put("notes", bill.notes)
                put("paymentStatus", bill.paymentStatus.name)
                put("paidAmount", bill.paidAmount)
                put("paymentMode", bill.paymentMode)
                put("itcEligible", bill.itcEligible)
                put("gstMode", bill.gstMode.name)
                put("createdAt", bill.createdAt)

                val itemsArray = JSONArray()
                for (item in bill.items) {
                    val itemObj = JSONObject().apply {
                        put("id", item.id)
                        if (item.itemId != null) put("itemId", item.itemId) else put("itemId", JSONObject.NULL)
                        put("description", item.description)
                        put("hsnCode", item.hsnCode)
                        put("quantity", item.quantity)
                        put("unit", item.unit)
                        put("unitPrice", item.unitPrice)
                        put("gstRate", item.gstRate)
                    }
                    itemsArray.put(itemObj)
                }
                put("items", itemsArray)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PURCHASES, array.toString()).apply()
        _purchases.value = list.sortedByDescending { it.dateMillis }
    }

    fun generateNextPurchaseNumber(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR) % 100
        val nextYear = year + 1
        val fy = "$year-$nextYear"

        val count = _purchases.value.size + 1
        val padded = String.format("%03d", count)
        return "PUR-$fy-$padded"
    }

    fun addPurchase(bill: PurchaseBill): PurchaseBill {
        val updated = listOf(bill) + _purchases.value
        saveInternal(updated)
        return bill
    }

    fun updatePurchase(bill: PurchaseBill) {
        val updated = _purchases.value.map { if (it.id == bill.id) bill else it }
        saveInternal(updated)
    }

    fun deletePurchase(billId: String) {
        val updated = _purchases.value.filter { it.id != billId }
        saveInternal(updated)
    }

    fun markAsPaid(billId: String, amount: Double) {
        val updated = _purchases.value.map { bill ->
            if (bill.id == billId) {
                val newPaid = bill.grandTotal
                bill.copy(
                    paidAmount = newPaid,
                    paymentStatus = InvoiceStatus.PAID
                )
            } else bill
        }
        saveInternal(updated)
    }

    private fun createInitialPurchases(): List<PurchaseBill> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        return listOf(
            PurchaseBill(
                id = UUID.randomUUID().toString(),
                purchaseNumber = "PUR-26-001",
                vendorBillNumber = "SBT/2026/1029",
                supplierName = "Shree Balaji Traders",
                supplierPhone = "+91 98221 54321",
                supplierGstin = "27AAACB1122D1Z4",
                supplierAddress = "Sector 18, Vashi APMC Market, Navi Mumbai",
                dateMillis = now - (2 * day),
                dueDateMillis = now + (13 * day),
                items = listOf(
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Basmati Rice Premium 25kg",
                        hsnCode = "1006",
                        quantity = 10.0,
                        unit = "Bag",
                        unitPrice = 1100.0,
                        gstRate = 5.0
                    ),
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Fortune Sunflower Oil 15L Tin",
                        hsnCode = "1512",
                        quantity = 12.0,
                        unit = "Tin",
                        unitPrice = 1350.0,
                        gstRate = 5.0
                    ),
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Tata Iodized Salt 1kg",
                        hsnCode = "2501",
                        quantity = 50.0,
                        unit = "Packet",
                        unitPrice = 22.0,
                        gstRate = 0.0
                    )
                ),
                discountAmount = 250.0,
                notes = "Delivered via Tempo MH-12-AB-4321. Stock verified.",
                paymentStatus = InvoiceStatus.PAID,
                paidAmount = 29435.0,
                paymentMode = "NEFT / Bank Transfer",
                itcEligible = true,
                gstMode = GstMode.INTRA_STATE
            ),
            PurchaseBill(
                id = UUID.randomUUID().toString(),
                purchaseNumber = "PUR-26-002",
                vendorBillNumber = "MLW/9812",
                supplierName = "Mahalaxmi Wholesalers",
                supplierPhone = "+91 94220 87654",
                supplierGstin = "27AABCM8765E1Z8",
                supplierAddress = "Gultekdi Market Yard, Pune",
                dateMillis = now - (6 * day),
                dueDateMillis = now + (9 * day),
                items = listOf(
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Britannia Good Day Biscuits 200g (Carton of 24)",
                        hsnCode = "1905",
                        quantity = 15.0,
                        unit = "Box",
                        unitPrice = 450.0,
                        gstRate = 18.0
                    ),
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Aashirvaad Shudh Chakki Atta 10kg",
                        hsnCode = "1101",
                        quantity = 20.0,
                        unit = "Bag",
                        unitPrice = 370.0,
                        gstRate = 5.0
                    )
                ),
                discountAmount = 150.0,
                notes = "Invoice with E-Way Bill 54128903. 15 days credit terms.",
                paymentStatus = InvoiceStatus.PARTIAL,
                paidAmount = 10000.0,
                paymentMode = "Cheque",
                itcEligible = true,
                gstMode = GstMode.INTRA_STATE
            ),
            PurchaseBill(
                id = UUID.randomUUID().toString(),
                purchaseNumber = "PUR-26-003",
                vendorBillNumber = "APX/540",
                supplierName = "Apex Electricals & Hardware",
                supplierPhone = "+91 97654 32190",
                supplierGstin = "27AAZPA9876F1Z1",
                supplierAddress = "Budhwar Peth, Pune",
                dateMillis = now - (11 * day),
                dueDateMillis = now + (4 * day),
                items = listOf(
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Havells LED Bulb 9W Cool White (Pack of 10)",
                        hsnCode = "8539",
                        quantity = 10.0,
                        unit = "Box",
                        unitPrice = 620.0,
                        gstRate = 18.0
                    ),
                    PurchaseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Anchor Roma 6A Switch (Box of 20)",
                        hsnCode = "8536",
                        quantity = 5.0,
                        unit = "Box",
                        unitPrice = 420.0,
                        gstRate = 18.0
                    )
                ),
                discountAmount = 0.0,
                notes = "Warranty cards included in package.",
                paymentStatus = InvoiceStatus.UNPAID,
                paidAmount = 0.0,
                paymentMode = "Credit / Due",
                itcEligible = true,
                gstMode = GstMode.INTRA_STATE
            )
        )
    }

    companion object {
        private const val KEY_PURCHASES = "hisab_pro_purchases_key"
    }
}
