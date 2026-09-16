package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TransactionRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_pro_prefs", Context.MODE_PRIVATE)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        val jsonString = prefs.getString(KEY_TRANSACTIONS, null)
        if (jsonString.isNullOrBlank()) {
            val initial = getInitialTransactions()
            saveTransactions(initial)
            _transactions.value = initial
        } else {
            try {
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<Transaction>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Transaction(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            type = TransactionType.valueOf(obj.getString("type")),
                            category = Category.fromString(obj.getString("category")),
                            dateMillis = obj.getLong("dateMillis"),
                            paymentMode = PaymentMode.fromString(obj.optString("paymentMode", "CASH")),
                            note = obj.optString("note", "")
                        )
                    )
                }
                _transactions.value = list
            } catch (e: Exception) {
                val initial = getInitialTransactions()
                saveTransactions(initial)
                _transactions.value = initial
            }
        }
    }

    private fun saveTransactions(list: List<Transaction>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("amount", item.amount)
                put("type", item.type.name)
                put("category", item.category.name)
                put("dateMillis", item.dateMillis)
                put("paymentMode", item.paymentMode.name)
                put("note", item.note)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
        _transactions.value = list
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        dateMillis: Long,
        paymentMode: PaymentMode,
        note: String
    ) {
        val newTx = Transaction(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            amount = amount,
            type = type,
            category = category,
            dateMillis = dateMillis,
            paymentMode = paymentMode,
            note = note.trim()
        )
        val updated = listOf(newTx) + _transactions.value
        saveTransactions(updated)
    }

    fun updateTransaction(transaction: Transaction) {
        val updated = _transactions.value.map {
            if (it.id == transaction.id) transaction else it
        }
        saveTransactions(updated)
    }

    fun deleteTransaction(id: String) {
        val updated = _transactions.value.filterNot { it.id == id }
        saveTransactions(updated)
    }

    fun resetToDemo() {
        val demo = getInitialTransactions()
        saveTransactions(demo)
    }

    private fun getInitialTransactions(): List<Transaction> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        return listOf(
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Client Project Invoice",
                amount = 45000.0,
                type = TransactionType.INCOME,
                category = Category.BUSINESS,
                dateMillis = now - (day * 0),
                paymentMode = PaymentMode.BANK_TRANSFER,
                note = "Full milestone payment for mobile application development"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "High-speed Office Internet",
                amount = 1499.0,
                type = TransactionType.EXPENSE,
                category = Category.UTILITIES,
                dateMillis = now - (day * 1),
                paymentMode = PaymentMode.ONLINE_UPI,
                note = "Fiber broadband monthly renewal"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Team Project Lunch",
                amount = 2850.0,
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                dateMillis = now - (day * 2),
                paymentMode = PaymentMode.CARD,
                note = "Celebration dinner with UI design team"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Cloud Infrastructure & Hosting",
                amount = 3200.0,
                type = TransactionType.EXPENSE,
                category = Category.BUSINESS,
                dateMillis = now - (day * 3),
                paymentMode = PaymentMode.CARD,
                note = "Server hosting and database instances"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Consulting Retainer",
                amount = 25000.0,
                type = TransactionType.INCOME,
                category = Category.BUSINESS,
                dateMillis = now - (day * 5),
                paymentMode = PaymentMode.BANK_TRANSFER,
                note = "Technical architecture consultation"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Supermarket Groceries",
                amount = 3450.0,
                type = TransactionType.EXPENSE,
                category = Category.GROCERIES,
                dateMillis = now - (day * 6),
                paymentMode = PaymentMode.ONLINE_UPI,
                note = "Weekly pantry restocking"
            )
        )
    }

    companion object {
        private const val KEY_TRANSACTIONS = "transactions_data_v1"

        @Volatile
        private var INSTANCE: TransactionRepository? = null

        fun getInstance(context: Context): TransactionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransactionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
