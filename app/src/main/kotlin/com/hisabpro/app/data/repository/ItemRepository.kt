package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.StockHistoryEntry
import com.hisabpro.app.data.model.StockReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ItemRepository(context: Context) {

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
                            timestampMillis = obj.getLong("timestampMillis")
                        )
                    )
                }
                _stockHistory.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _stockHistory.value = emptyList()
            }
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
                note = "Initial opening stock"
            )
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
                recordStockHistory(
                    itemId = item.id,
                    changeQty = diff,
                    prevStock = oldItem.currentStock,
                    newStock = item.currentStock,
                    reason = StockReason.MANUAL_ADJUSTMENT,
                    note = "Updated via item edit"
                )
            }
        }
    }

    fun deleteItem(itemId: String) {
        val current = _items.value.filter { it.id != itemId }
        saveItemsInternal(current)

        val hist = _stockHistory.value.filter { it.itemId != itemId }
        saveHistoryInternal(hist)
    }

    fun adjustStock(
        itemId: String,
        changeQty: Double,
        reason: StockReason,
        note: String
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

        recordStockHistory(
            itemId = itemId,
            changeQty = changeQty,
            prevStock = prevStock,
            newStock = newStock,
            reason = reason,
            note = note
        )
        return true
    }

    /**
     * Called when an invoice is created: deducts stock for matched items
     */
    fun deductStockForInvoiceItem(itemNameOrId: String, quantity: Double, invoiceNumber: String) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst {
            it.id == itemNameOrId || it.name.equals(itemNameOrId, ignoreCase = true)
        }
        if (index != -1) {
            val item = current[index]
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
                note = "Sold on Invoice #$invoiceNumber"
            )
        }
    }

    /**
     * Called when an invoice is cancelled or deleted: restores stock for matched items
     */
    fun restoreStockForInvoiceItem(itemNameOrId: String, quantity: Double, invoiceNumber: String) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst {
            it.id == itemNameOrId || it.name.equals(itemNameOrId, ignoreCase = true)
        }
        if (index != -1) {
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
                reason = StockReason.RETURN_IN,
                note = "Restored from Deleted Invoice #$invoiceNumber"
            )
        }
    }

    private fun recordStockHistory(
        itemId: String,
        changeQty: Double,
        prevStock: Double,
        newStock: Double,
        reason: StockReason,
        note: String
    ) {
        val entry = StockHistoryEntry(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            changeQty = changeQty,
            previousStock = prevStock,
            newStock = newStock,
            reason = reason,
            note = note,
            timestampMillis = System.currentTimeMillis()
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
