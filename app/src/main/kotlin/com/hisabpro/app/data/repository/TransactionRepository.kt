package com.hisabpro.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
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

class TransactionRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

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
        scope.launch {
            syncToDatabase()
        }
    }

    private fun saveTransactions(list: List<Transaction>, syncAllRoom: Boolean = false) {
        _transactions.value = list
        scope.launch {
            try {
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

                if (syncAllRoom) {
                    syncToDatabase()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncToDatabase() {
        try {
            ensureDefaultBusiness()
            val currentList = _transactions.value
            for (item in currentList) {
                saveSingleTransactionDbInternal(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reloadFromDatabase() {
        try {
            val payments = db.paymentDao().getAllPaymentsGlobalSync()
            val expenses = db.expenseDao().getAllExpensesGlobalSync()
            if (payments.isNotEmpty() || expenses.isNotEmpty()) {
                val list = mutableListOf<Transaction>()
                for (p in payments) {
                    val mode = try { PaymentMode.valueOf(p.mode) } catch (e: Exception) { PaymentMode.CASH }
                    list.add(
                        Transaction(
                            id = p.id,
                            title = p.referenceNo.ifBlank { "Payment Received" },
                            amount = p.amount.toRupees(),
                            type = TransactionType.INCOME,
                            category = Category.BUSINESS,
                            dateMillis = p.date,
                            paymentMode = mode,
                            note = p.notes
                        )
                    )
                }
                for (exp in expenses) {
                    val cat = try { Category.valueOf(exp.category) } catch (e: Exception) { Category.OTHER }
                    val mode = try { PaymentMode.valueOf(exp.mode) } catch (e: Exception) { PaymentMode.CASH }
                    list.add(
                        Transaction(
                            id = exp.id,
                            title = exp.description.ifBlank { "Expense" },
                            amount = exp.amount.toRupees(),
                            type = TransactionType.EXPENSE,
                            category = cat,
                            dateMillis = exp.date,
                            paymentMode = mode,
                            note = ""
                        )
                    )
                }
                val sorted = list.sortedByDescending { it.dateMillis }
                _transactions.value = sorted
                saveTransactions(sorted, syncAllRoom = false)
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

    private suspend fun saveSingleTransactionDbInternal(item: Transaction) {
        try {
            ensureDefaultBusiness()
            if (item.type == TransactionType.EXPENSE) {
                db.expenseDao().insertExpense(
                    ExpenseEntity(
                        id = item.id,
                        businessId = "default_business",
                        date = item.dateMillis,
                        category = item.category.name,
                        amount = item.amount.toPaise(),
                        description = item.title + (if (item.note.isNotBlank()) " - ${item.note}" else ""),
                        mode = item.paymentMode.name,
                        receiptPath = ""
                    )
                )
            } else {
                db.paymentDao().insertPayment(
                    PaymentEntity(
                        id = item.id,
                        businessId = "default_business",
                        partyId = null,
                        date = item.dateMillis,
                        amount = item.amount.toPaise(),
                        mode = item.paymentMode.name,
                        referenceNo = item.title,
                        notes = item.note,
                        linkedInvoiceId = null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        saveTransactions(updated, syncAllRoom = false)
        scope.launch {
            saveSingleTransactionDbInternal(newTx)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        val updated = _transactions.value.map {
            if (it.id == transaction.id) transaction else it
        }
        saveTransactions(updated, syncAllRoom = false)
        scope.launch {
            saveSingleTransactionDbInternal(transaction)
        }
    }

    fun deleteTransaction(id: String) {
        val updated = _transactions.value.filterNot { it.id == id }
        saveTransactions(updated, syncAllRoom = false)
        scope.launch {
            try {
                db.expenseDao().deleteExpense(id)
                db.paymentDao().deletePayment(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetToDemo() {
        val demo = getInitialTransactions()
        saveTransactions(demo, syncAllRoom = true)
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
