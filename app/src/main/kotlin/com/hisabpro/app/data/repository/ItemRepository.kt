package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.StockHistoryEntry
import com.hisabpro.app.data.model.StockReason
import com.hisabpro.app.util.toPaise
import com.hisabpro.app.util.toRupees
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ItemRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_pro_items_v1", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _stockHistory = MutableStateFlow<List<StockHistoryEntry>>(emptyList())
    val stockHistory: StateFlow<List<StockHistoryEntry>> = _stockHistory.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val itemsJson = prefs.getString(KEY_ITEMS, null)
        if (itemsJson.isNullOrBlank()) {
            val initialItems = createInitialItems()
            saveItemsInternal(initialItems)
        } else {
            try {
                val list = mutableListOf<Item>()
                val array = JSONArray(itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Item(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            itemCode = obj.optString("itemCode", ""),
                            category = obj.optString("category", "General"),
                            unit = obj.optString("unit", "Pcs"),
                            salePrice = obj.optDouble("salePrice", 0.0),
                            purchasePrice = obj.optDouble("purchasePrice", 0.0),
                            gstRate = obj.optDouble("gstRate", 18.0),
                            hsnCode = obj.optString("hsnCode", ""),
                            currentStock = obj.optDouble("currentStock", 0.0),
                            minStockAlert = obj.optDouble("minStockAlert", 5.0),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
                _items.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _items.value = emptyList()
            }
        }

        val historyJson = prefs.getString(KEY_STOCK_HISTORY, null)
        if (!historyJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<StockHistoryEntry>()
                val array = JSONArray(historyJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        StockHistoryEntry(
                            id = obj.getString("id"),
                            itemId = obj.getString("itemId"),
                            changeQty = obj.getDouble("changeQty"),
                            previousStock = obj.getDouble("previousStock"),
                            newStock = obj.getDouble("newStock"),
                            reason = StockReason.fromString(obj.getString("reason")),
                            note = obj.optString("note", ""),
                            timestampMillis = obj.getLong("timestampMillis"),
                            sourceTransactionId = obj.optString("sourceTransactionId").ifEmpty { null },
                            sourceTransactionType = obj.optString("sourceTransactionType").ifEmpty { null },
                            sourceRefNumber = obj.optString("sourceRefNumber").ifEmpty { null }
                        )
                    )
                }
                _stockHistory.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _stockHistory.value = emptyList()
            }
        }

        scope.launch {
            syncToDatabase()
        }
    }

    fun addItem(item: Item): Item {
        val current = _items.value.toMutableList()
        current.add(0, item)
        saveItemsInternal(current)

        if (item.currentStock > 0) {
            recordStockHistory(
                itemId = item.id,
                changeQty = item.currentStock,
                prevStock = 0.0,
                newStock = item.currentStock,
                reason = StockReason.OPENING_STOCK,
                note = "Initial opening stock",
                sourceTransactionId = item.id,
                sourceTransactionType = "OPENING_STOCK",
                sourceRefNumber = "OPN-${item.itemCode.ifBlank { item.id.takeLast(6).uppercase() }}"
            )
        }

        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("id", item.id)
                    put("business_id", "default_business")
                    put("name", item.name)
                    put("item_code", item.itemCode)
                    put("category", item.category)
                    put("unit", item.unit)
                    put("sell_price", item.salePrice.toPaise())
                    put("purchase_price", item.purchasePrice.toPaise())
                    put("gst_rate", item.gstRate)
                    put("hsn_code", item.hsnCode)
                    put("stock_qty", item.currentStock)
                    put("low_stock_threshold", item.minStockAlert)
                    put("created_at", item.updatedAtMillis)
                    put("updated_at", item.updatedAtMillis)
                }
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "item",
                    entityId = item.id,
                    operation = "UPSERT",
                    payloadJson = payload.toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return item
    }

    fun updateItem(item: Item) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val oldItem = current[index]
            current[index] = item.copy(updatedAtMillis = System.currentTimeMillis())
            saveItemsInternal(current)

            // If stock changed directly via item edit
            if (oldItem.currentStock != item.currentStock) {
                val diff = item.currentStock - oldItem.currentStock
                val isPositive = diff > 0
                recordStockHistory(
                    itemId = item.id,
                    changeQty = diff,
                    prevStock = oldItem.currentStock,
                    newStock = item.currentStock,
                    reason = if (isPositive) StockReason.MANUAL_ADJUSTMENT else StockReason.DAMAGE_LOSS,
                    note = "Updated via product edit screen",
                    sourceTransactionId = item.id,
                    sourceTransactionType = "STOCK_ADJUSTMENT",
                    sourceRefNumber = "ADJ-EDIT-${item.itemCode.ifBlank { item.id.takeLast(4).uppercase() }}"
                )
            }

            scope.launch {
                try {
                    val payload = JSONObject().apply {
                        put("id", item.id)
                        put("business_id", "default_business")
                        put("name", item.name)
                        put("item_code", item.itemCode)
                        put("category", item.category)
                        put("unit", item.unit)
                        put("sell_price", item.salePrice.toPaise())
                        put("purchase_price", item.purchasePrice.toPaise())
                        put("gst_rate", item.gstRate)
                        put("hsn_code", item.hsnCode)
                        put("stock_qty", item.currentStock)
                        put("low_stock_threshold", item.minStockAlert)
                        put("created_at", item.updatedAtMillis)
                        put("updated_at", System.currentTimeMillis())
                    }
                    com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                        entityType = "item",
                        entityId = item.id,
                        operation = "UPSERT",
                        payloadJson = payload.toString()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteItem(itemId: String) {
        val current = _items.value.filter { it.id != itemId }
        saveItemsInternal(current)

        val hist = _stockHistory.value.filter { it.itemId != itemId }
        saveHistoryInternal(hist)

        scope.launch {
            try {
                db.itemDao().deleteItem(itemId)
                com.hisabpro.app.data.sync.CloudSyncManager.getInstance(context).enqueueChange(
                    entityType = "item",
                    entityId = itemId,
                    operation = "DELETE",
                    payloadJson = "{}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adjustStock(
        itemId: String,
        changeQty: Double,
        reason: StockReason,
        note: String,
        sourceRefNumber: String? = null,
        sourceTransactionId: String? = null,
        sourceTransactionType: String? = null
    ): Boolean {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index == -1) return false

        val item = current[index]
        val prevStock = item.currentStock
        val newStock = (prevStock + changeQty).coerceAtLeast(0.0)

        current[index] = item.copy(
            currentStock = newStock,
            updatedAtMillis = System.currentTimeMillis()
        )
        saveItemsInternal(current)

        val resolvedRef = if (!sourceRefNumber.isNullOrBlank()) {
            sourceRefNumber
        } else {
            when (reason) {
                StockReason.OPENING_STOCK -> "OPN-${item.itemCode.ifBlank { item.id.takeLast(4).uppercase() }}"
                StockReason.PURCHASE_IN -> "PUR-${System.currentTimeMillis() % 10000}"
                StockReason.SALES_RETURN, StockReason.RETURN_IN -> "SR-${System.currentTimeMillis() % 10000}"
                StockReason.PURCHASE_RETURN -> "PR-${System.currentTimeMillis() % 10000}"
                StockReason.SALE_OUT -> "DISP-${System.currentTimeMillis() % 10000}"
                StockReason.DAMAGE_LOSS -> "SCRAP-${System.currentTimeMillis() % 10000}"
                StockReason.MANUAL_ADJUSTMENT -> "AUDIT-${System.currentTimeMillis() % 10000}"
            }
        }

        val resolvedType = sourceTransactionType ?: when (reason) {
            StockReason.OPENING_STOCK -> "OPENING_STOCK"
            StockReason.PURCHASE_IN -> "PURCHASE"
            StockReason.SALES_RETURN, StockReason.RETURN_IN -> "SALES_RETURN"
            StockReason.PURCHASE_RETURN -> "PURCHASE_RETURN"
            StockReason.SALE_OUT -> "SALE_OUT"
            StockReason.DAMAGE_LOSS -> "DAMAGE_LOSS"
            StockReason.MANUAL_ADJUSTMENT -> "STOCK_ADJUSTMENT"
        }

        recordStockHistory(
            itemId = itemId,
            changeQty = changeQty,
            prevStock = prevStock,
            newStock = newStock,
            reason = reason,
            note = note,
            sourceTransactionId = sourceTransactionId ?: UUID.randomUUID().toString(),
            sourceTransactionType = resolvedType,
            sourceRefNumber = resolvedRef
        )
        return true
    }

    /**
     * Called when an invoice is created: deducts stock for matched items.
     * Prevents double stock deduction by checking whether this invoice transaction
     * has already had stock deducted for this item.
     */
    fun deductStockForInvoiceItem(
        itemNameOrId: String,
        quantity: Double,
        invoiceNumber: String,
        sourceTransactionId: String? = null
    ): Boolean {
        if (quantity <= 0.0) return false

        val current = _items.value.toMutableList()
        val index = current.indexOfFirst {
            it.id == itemNameOrId || it.name.equals(itemNameOrId, ignoreCase = true)
        }
        if (index == -1) return false

        val item = current[index]

        // Double deduction guard: ensure not deducted twice for the same invoice transaction
        val alreadyDeducted = _stockHistory.value.any { entry ->
            entry.itemId == item.id &&
            entry.reason == StockReason.SALE_OUT &&
            ((sourceTransactionId != null && entry.sourceTransactionId == sourceTransactionId) ||
             (entry.sourceRefNumber == invoiceNumber))
        }
        if (alreadyDeducted) {
            return false // Prevent double stock deduction
        }

        val prevStock = item.currentStock
        val newStock = (prevStock - quantity).coerceAtLeast(0.0)
        current[index] = item.copy(
            currentStock = newStock,
            updatedAtMillis = System.currentTimeMillis()
        )
        saveItemsInternal(current)

        recordStockHistory(
            itemId = item.id,
            changeQty = -quantity,
            prevStock = prevStock,
            newStock = newStock,
            reason = StockReason.SALE_OUT,
            note = "Sold on Invoice #$invoiceNumber",
            sourceTransactionId = sourceTransactionId,
            sourceTransactionType = "INVOICE",
            sourceRefNumber = invoiceNumber
        )
        return true
    }

    /**
     * Called when an invoice is cancelled or deleted: restores stock for matched items
     */
    fun restoreStockForInvoiceItem(
        itemNameOrId: String,
        quantity: Double,
        invoiceNumber: String,
        sourceTransactionId: String? = null,
        reason: StockReason = StockReason.SALES_RETURN,
        notePrefix: String = "Restored: Cancelled Invoice #"
    ): Boolean {
        if (quantity <= 0.0) return false

        val current = _items.value.toMutableList()
        val index = current.indexOfFirst {
            it.id == itemNameOrId || it.name.equals(itemNameOrId, ignoreCase = true)
        }
        if (index == -1) return false

        val item = current[index]
        val prevStock = item.currentStock
        val newStock = prevStock + quantity
        current[index] = item.copy(
            currentStock = newStock,
            updatedAtMillis = System.currentTimeMillis()
        )
        saveItemsInternal(current)

        recordStockHistory(
            itemId = item.id,
            changeQty = quantity,
            prevStock = prevStock,
            newStock = newStock,
            reason = reason,
            note = "$notePrefix$invoiceNumber",
            sourceTransactionId = sourceTransactionId,
            sourceTransactionType = "INVOICE_CANCEL",
            sourceRefNumber = invoiceNumber
        )
        return true
    }

    /**
     * Reconciles inventory stock whenever an existing invoice is updated/edited.
     * Accurately calculates net differences per line item and updates stock
     * with an audit trail, and handles invoice cancellation / reopening.
     */
    fun adjustStockForInvoiceUpdate(
        oldInvoice: com.hisabpro.app.data.model.Invoice,
        newInvoice: com.hisabpro.app.data.model.Invoice
    ) {
        val wasCancelled = oldInvoice.paymentStatus == com.hisabpro.app.data.model.InvoiceStatus.CANCELLED
        val isCancelled = newInvoice.paymentStatus == com.hisabpro.app.data.model.InvoiceStatus.CANCELLED

        // Transition 1: Invoice marked as CANCELLED -> restore all items to inventory
        if (!wasCancelled && isCancelled) {
            oldInvoice.items.forEach { oldLine ->
                restoreStockForInvoiceItem(
                    itemNameOrId = oldLine.description,
                    quantity = oldLine.quantity,
                    invoiceNumber = newInvoice.invoiceNumber,
                    sourceTransactionId = newInvoice.id,
                    reason = StockReason.SALES_RETURN,
                    notePrefix = "Restored: Cancelled Invoice #"
                )
            }
            return
        }

        // Transition 2: Invoice reopened from CANCELLED -> deduct all items again
        if (wasCancelled && !isCancelled) {
            newInvoice.items.forEach { newLine ->
                deductStockForInvoiceItem(
                    itemNameOrId = newLine.description,
                    quantity = newLine.quantity,
                    invoiceNumber = newInvoice.invoiceNumber,
                    sourceTransactionId = newInvoice.id
                )
            }
            return
        }

        // Transition 3: Both were cancelled, no stock movement
        if (wasCancelled && isCancelled) {
            return
        }

        // Transition 4: Active invoice edited with altered line items/quantities
        val oldQuantities = mutableMapOf<String, Double>()
        for (item in oldInvoice.items) {
            val key = item.description.trim().lowercase()
            oldQuantities[key] = (oldQuantities[key] ?: 0.0) + item.quantity
        }

        val newQuantities = mutableMapOf<String, Double>()
        for (item in newInvoice.items) {
            val key = item.description.trim().lowercase()
            newQuantities[key] = (newQuantities[key] ?: 0.0) + item.quantity
        }

        val allKeys = oldQuantities.keys + newQuantities.keys
        for (key in allKeys) {
            val oldQty = oldQuantities[key] ?: 0.0
            val newQty = newQuantities[key] ?: 0.0
            val delta = oldQty - newQty // > 0: customer bought less/removed item; < 0: customer bought more/added item

            if (delta > 0.0001) {
                // Return stock back to inventory
                val current = _items.value.toMutableList()
                val idx = current.indexOfFirst {
                    it.name.trim().lowercase() == key || it.id.lowercase() == key
                }
                if (idx != -1) {
                    val itm = current[idx]
                    val prev = itm.currentStock
                    val next = prev + delta
                    current[idx] = itm.copy(currentStock = next, updatedAtMillis = System.currentTimeMillis())
                    saveItemsInternal(current)

                    recordStockHistory(
                        itemId = itm.id,
                        changeQty = delta,
                        prevStock = prev,
                        newStock = next,
                        reason = StockReason.SALES_RETURN,
                        note = "Restored: Invoice #${newInvoice.invoiceNumber} updated (-${delta.toInt()} sold)",
                        sourceTransactionId = newInvoice.id,
                        sourceTransactionType = "INVOICE_EDIT",
                        sourceRefNumber = newInvoice.invoiceNumber
                    )
                }
            } else if (delta < -0.0001) {
                // Deduct additional items from stock
                val additionalSold = -delta
                val current = _items.value.toMutableList()
                val idx = current.indexOfFirst {
                    it.name.trim().lowercase() == key || it.id.lowercase() == key
                }
                if (idx != -1) {
                    val itm = current[idx]
                    val prev = itm.currentStock
                    val next = (prev - additionalSold).coerceAtLeast(0.0)
                    current[idx] = itm.copy(currentStock = next, updatedAtMillis = System.currentTimeMillis())
                    saveItemsInternal(current)

                    recordStockHistory(
                        itemId = itm.id,
                        changeQty = -additionalSold,
                        prevStock = prev,
                        newStock = next,
                        reason = StockReason.SALE_OUT,
                        note = "Deducted: Invoice #${newInvoice.invoiceNumber} updated (+${additionalSold.toInt()} sold)",
                        sourceTransactionId = newInvoice.id,
                        sourceTransactionType = "INVOICE_EDIT",
                        sourceRefNumber = newInvoice.invoiceNumber
                    )
                }
            }
        }
    }

    private fun recordStockHistory(
        itemId: String,
        changeQty: Double,
        prevStock: Double,
        newStock: Double,
        reason: StockReason,
        note: String,
        sourceTransactionId: String? = null,
        sourceTransactionType: String? = null,
        sourceRefNumber: String? = null
    ) {
        val entry = StockHistoryEntry(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            changeQty = changeQty,
            previousStock = prevStock,
            newStock = newStock,
            reason = reason,
            note = note,
            timestampMillis = System.currentTimeMillis(),
            sourceTransactionId = sourceTransactionId,
            sourceTransactionType = sourceTransactionType,
            sourceRefNumber = sourceRefNumber
        )
        val current = _stockHistory.value.toMutableList()
        current.add(0, entry)
        saveHistoryInternal(current)
    }

    fun getHistoryForItem(itemId: String): List<StockHistoryEntry> {
        return _stockHistory.value.filter { it.itemId == itemId }
    }

    private fun saveItemsInternal(list: List<Item>) {
        _items.value = list
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("itemCode", item.itemCode)
                put("category", item.category)
                put("unit", item.unit)
                put("salePrice", item.salePrice)
                put("purchasePrice", item.purchasePrice)
                put("gstRate", item.gstRate)
                put("hsnCode", item.hsnCode)
                put("currentStock", item.currentStock)
                put("minStockAlert", item.minStockAlert)
                put("updatedAtMillis", item.updatedAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()

        scope.launch {
            try {
                ensureDefaultBusiness()
                val entities = list.map { item ->
                    ItemEntity(
                        id = item.id,
                        businessId = "default_business",
                        name = item.name,
                        itemCode = item.itemCode,
                        category = item.category,
                        unit = item.unit,
                        sellPrice = item.salePrice.toPaise(),
                        purchasePrice = item.purchasePrice.toPaise(),
                        gstRate = item.gstRate,
                        hsnCode = item.hsnCode,
                        stockQty = item.currentStock,
                        lowStockThreshold = item.minStockAlert,
                        createdAt = item.updatedAtMillis
                    )
                }
                if (entities.isNotEmpty()) {
                    db.itemDao().insertAllItems(entities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncToDatabase() {
        try {
            ensureDefaultBusiness()
            val entities = _items.value.map { item ->
                ItemEntity(
                    id = item.id,
                    businessId = "default_business",
                    name = item.name,
                    itemCode = item.itemCode,
                    category = item.category,
                    unit = item.unit,
                    sellPrice = item.salePrice.toPaise(),
                    purchasePrice = item.purchasePrice.toPaise(),
                    gstRate = item.gstRate,
                    hsnCode = item.hsnCode,
                    stockQty = item.currentStock,
                    lowStockThreshold = item.minStockAlert,
                    createdAt = item.updatedAtMillis
                )
            }
            if (entities.isNotEmpty()) {
                db.itemDao().insertAllItems(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reloadFromDatabase() {
        try {
            val dbItems = db.itemDao().getAllItemsGlobalSync()
            if (dbItems.isNotEmpty()) {
                val itemsList = dbItems.map { item ->
                    Item(
                        id = item.id,
                        name = item.name,
                        itemCode = item.itemCode,
                        category = item.category,
                        unit = item.unit,
                        salePrice = item.sellPrice.toRupees(),
                        purchasePrice = item.purchasePrice.toRupees(),
                        gstRate = item.gstRate,
                        hsnCode = item.hsnCode,
                        currentStock = item.stockQty,
                        minStockAlert = item.lowStockThreshold,
                        updatedAtMillis = item.createdAt
                    )
                }
                _items.value = itemsList
                saveItemsInternal(itemsList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun ensureDefaultBusiness() {
        try {
            val existing = db.businessDao().getBusinessSync("default_business")
            if (existing == null) {
                db.businessDao().insertOrUpdate(
                    BusinessEntity(
                        id = "default_business",
                        name = "HisabPro Business",
                        phone = "",
                        address = "",
                        gstin = "",
                        gstEnabled = false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistoryInternal(list: List<StockHistoryEntry>) {
        _stockHistory.value = list
        val array = JSONArray()
        for (entry in list) {
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("itemId", entry.itemId)
                put("changeQty", entry.changeQty)
                put("previousStock", entry.previousStock)
                put("newStock", entry.newStock)
                put("reason", entry.reason.name)
                put("note", entry.note)
                put("timestampMillis", entry.timestampMillis)
                put("sourceTransactionId", entry.sourceTransactionId ?: "")
                put("sourceTransactionType", entry.sourceTransactionType ?: "")
                put("sourceRefNumber", entry.sourceRefNumber ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_STOCK_HISTORY, array.toString()).apply()
    }

    private fun createInitialItems(): List<Item> {
        return listOf(
            Item(
                id = "item_1",
                name = "Daawat Basmati Rice 5kg",
                itemCode = "BR-501",
                category = "Groceries",
                unit = "Bag",
                salePrice = 480.0,
                purchasePrice = 390.0,
                gstRate = 5.0,
                hsnCode = "1006",
                currentStock = 32.0,
                minStockAlert = 8.0
            ),
            Item(
                id = "item_2",
                name = "Syska 9W LED Bulb (Cool White)",
                itemCode = "SYS-LED-9W",
                category = "Electricals",
                unit = "Pcs",
                salePrice = 120.0,
                purchasePrice = 75.0,
                gstRate = 18.0,
                hsnCode = "8539",
                currentStock = 4.0, // Low stock trigger
                minStockAlert = 10.0
            ),
            Item(
                id = "item_3",
                name = "Amul Pure Ghee 1 Litre",
                itemCode = "AG-1000",
                category = "Dairy & FMCG",
                unit = "Pcs",
                salePrice = 640.0,
                purchasePrice = 560.0,
                gstRate = 12.0,
                hsnCode = "0405",
                currentStock = 18.0,
                minStockAlert = 5.0
            ),
            Item(
                id = "item_4",
                name = "Tata Salt Iodized 1kg",
                itemCode = "TS-101",
                category = "Groceries",
                unit = "Packet",
                salePrice = 28.0,
                purchasePrice = 22.0,
                gstRate = 0.0,
                hsnCode = "2501",
                currentStock = 65.0,
                minStockAlert = 15.0
            ),
            Item(
                id = "item_5",
                name = "Fastrack USB-C Fast Charging Cable (1.2m)",
                itemCode = "FT-CB-01",
                category = "Electronics",
                unit = "Pcs",
                salePrice = 299.0,
                purchasePrice = 140.0,
                gstRate = 18.0,
                hsnCode = "8544",
                currentStock = 2.0, // Low stock alert
                minStockAlert = 6.0
            ),
            Item(
                id = "item_6",
                name = "Fortune Sunlite Refined Oil 1L",
                itemCode = "FS-RO-01",
                category = "Groceries",
                unit = "Packet",
                salePrice = 155.0,
                purchasePrice = 135.0,
                gstRate = 5.0,
                hsnCode = "1512",
                currentStock = 0.0, // Out of stock
                minStockAlert = 10.0
            )
        )
    }

    companion object {
        private const val KEY_ITEMS = "key_items_list"
        private const val KEY_STOCK_HISTORY = "key_items_stock_history"

        @Volatile
        private var INSTANCE: ItemRepository? = null

        fun getInstance(context: Context): ItemRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ItemRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
