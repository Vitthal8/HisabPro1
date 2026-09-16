package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class InvoiceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_pro_invoices_v1", Context.MODE_PRIVATE)

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val json = prefs.getString(KEY_INVOICES, null)
        if (json.isNullOrBlank()) {
            val initial = createInitialInvoices()
            saveInternal(initial)
        } else {
            try {
                val list = mutableListOf<Invoice>()
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val itemsList = mutableListOf<InvoiceItem>()
                    val itemsArray = obj.getJSONArray("items")
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        itemsList.add(
                            InvoiceItem(
                                id = itemObj.getString("id"),
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
                        Invoice(
                            id = obj.getString("id"),
                            invoiceNumber = obj.getString("invoiceNumber"),
                            type = InvoiceType.fromString(obj.optString("type", "TAX_INVOICE")),
                            gstMode = GstMode.fromString(obj.optString("gstMode", "INTRA_STATE")),
                            customerId = if (obj.isNull("customerId")) null else obj.getString("customerId"),
                            customerName = obj.optString("customerName", "Cash Customer"),
                            customerPhone = obj.optString("customerPhone", ""),
                            customerAddress = obj.optString("customerAddress", ""),
                            customerGstin = obj.optString("customerGstin", ""),
                            dateMillis = obj.getLong("dateMillis"),
                            items = itemsList,
                            discountAmount = obj.optDouble("discountAmount", 0.0),
                            notes = obj.optString("notes", ""),
                            paymentStatus = InvoiceStatus.fromString(obj.optString("paymentStatus", "PAID")),
                            paidAmount = obj.optDouble("paidAmount", 0.0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _invoices.value = list.sortedByDescending { it.dateMillis }
            } catch (e: Exception) {
                val initial = createInitialInvoices()
                saveInternal(initial)
            }
        }
    }

    private fun saveInternal(list: List<Invoice>) {
        val array = JSONArray()
        for (inv in list) {
            val obj = JSONObject().apply {
                put("id", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("type", inv.type.name)
                put("gstMode", inv.gstMode.name)
                if (inv.customerId != null) put("customerId", inv.customerId) else put("customerId", JSONObject.NULL)
                put("customerName", inv.customerName)
                put("customerPhone", inv.customerPhone)
                put("customerAddress", inv.customerAddress)
                put("customerGstin", inv.customerGstin)
                put("dateMillis", inv.dateMillis)
                put("discountAmount", inv.discountAmount)
                put("notes", inv.notes)
                put("paymentStatus", inv.paymentStatus.name)
                put("paidAmount", inv.paidAmount)
                put("createdAt", inv.createdAt)

                val itemsArray = JSONArray()
                for (item in inv.items) {
                    val itemObj = JSONObject().apply {
                        put("id", item.id)
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
        prefs.edit().putString(KEY_INVOICES, array.toString()).apply()
        _invoices.value = list.sortedByDescending { it.dateMillis }
    }

    fun generateNextInvoiceNumber(type: InvoiceType, prefixOverride: String? = null): String {
        val year = Calendar.getInstance().get(Calendar.YEAR) % 100
        val nextYear = year + 1
        val fy = "$year-$nextYear"

        val countForType = _invoices.value.count { it.type == type } + 1
        val padded = String.format("%03d", countForType)
        val prefix = if (!prefixOverride.isNullOrBlank()) prefixOverride.trim() else type.prefix
        return "$prefix-$fy-$padded"
    }

    fun updateCustomerDetails(
        customerId: String,
        name: String,
        phone: String,
        address: String,
        gstin: String
    ) {
        val updated = _invoices.value.map { inv ->
            if (inv.customerId == customerId) {
                inv.copy(
                    customerName = name.ifBlank { inv.customerName },
                    customerPhone = phone,
                    customerAddress = address,
                    customerGstin = gstin
                )
            } else {
                inv
            }
        }
        saveInternal(updated)
    }

    fun addInvoice(invoice: Invoice): Invoice {
        val updated = listOf(invoice) + _invoices.value
        saveInternal(updated)
        return invoice
    }

    fun updateInvoice(invoice: Invoice) {
        val updated = _invoices.value.map {
            if (it.id == invoice.id) invoice else it
        }
        saveInternal(updated)
    }

    fun deleteInvoice(invoiceId: String) {
        val updated = _invoices.value.filterNot { it.id == invoiceId }
        saveInternal(updated)
    }

    fun duplicateInvoice(invoiceId: String): Invoice? {
        val original = _invoices.value.find { it.id == invoiceId } ?: return null
        val nextNumber = generateNextInvoiceNumber(original.type)
        val duplicated = original.copy(
            id = UUID.randomUUID().toString(),
            invoiceNumber = nextNumber,
            dateMillis = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            paidAmount = original.paidAmount,
            paymentStatus = original.paymentStatus,
            items = original.items.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        addInvoice(duplicated)
        return duplicated
    }

    fun resetToDemo() {
        val initial = createInitialInvoices()
        saveInternal(initial)
    }

    private fun createInitialInvoices(): List<Invoice> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        // 1. Quick Sale Cash Bill (Walk-in customer)
        val inv1 = Invoice(
            id = "inv_demo_quick_1",
            invoiceNumber = "BILL-24-25-001",
            type = InvoiceType.NON_GST_BILL,
            gstMode = GstMode.EXEMPT,
            customerId = null,
            customerName = "Cash Customer",
            customerPhone = "",
            dateMillis = now - (day * 1),
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    description = "Basmati Rice (Premium)",
                    hsnCode = "1006",
                    quantity = 5.0,
                    unit = "Kg",
                    unitPrice = 140.0,
                    gstRate = 0.0
                ),
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    description = "Sunflower Oil 1L",
                    hsnCode = "1512",
                    quantity = 2.0,
                    unit = "Pcs",
                    unitPrice = 160.0,
                    gstRate = 0.0
                )
            ),
            discountAmount = 20.0,
            notes = "Counter quick sale - Thank you!",
            paymentStatus = InvoiceStatus.PAID,
            paidAmount = 1000.0,
            createdAt = now - (day * 1)
        )

        // 2. Full GST Tax Invoice with Ramesh Sharma (intra-state CGST + SGST)
        val inv2 = Invoice(
            id = "inv_demo_gst_2",
            invoiceNumber = "INV-24-25-001",
            type = InvoiceType.TAX_INVOICE,
            gstMode = GstMode.INTRA_STATE,
            customerId = "p_sharma_kirana",
            customerName = "Ramesh Sharma (Kirana Store)",
            customerPhone = "+91 98765 43210",
            customerAddress = "Shop 12, Main Market, Mumbai",
            customerGstin = "27AAAAA1234A1Z5",
            dateMillis = now - (day * 3),
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    description = "Industrial LED Flood Light 50W",
                    hsnCode = "9405",
                    quantity = 4.0,
                    unit = "Nos",
                    unitPrice = 1200.0,
                    gstRate = 18.0
                ),
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    description = "Copper Wiring Cable 90m Coil",
                    hsnCode = "8544",
                    quantity = 2.0,
                    unit = "Coil",
                    unitPrice = 2800.0,
                    gstRate = 18.0
                )
            ),
            discountAmount = 400.0,
            notes = "Payment due within 15 days of invoice date.",
            paymentStatus = InvoiceStatus.PARTIAL,
            paidAmount = 5000.0,
            createdAt = now - (day * 3)
        )

        // 3. Proforma Quotation for Anjali Verma
        val inv3 = Invoice(
            id = "inv_demo_quot_3",
            invoiceNumber = "QUOT-24-25-001",
            type = InvoiceType.PROFORMA,
            gstMode = GstMode.INTRA_STATE,
            customerId = "p_anjali_verma",
            customerName = "Anjali Verma",
            customerPhone = "+91 97654 32109",
            customerAddress = "B-204, Green Heights, Andheri",
            dateMillis = now - (day * 5),
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    description = "Office Ergonomic Mesh Chair",
                    hsnCode = "9403",
                    quantity = 2.0,
                    unit = "Nos",
                    unitPrice = 4500.0,
                    gstRate = 18.0
                )
            ),
            discountAmount = 0.0,
            notes = "Quotation valid for 7 days. 100% advance on order confirmation.",
            paymentStatus = InvoiceStatus.UNPAID,
            paidAmount = 0.0,
            createdAt = now - (day * 5)
        )

        return listOf(inv1, inv2, inv3)
    }

    companion object {
        private const val KEY_INVOICES = "invoices_data_list"
    }
}
