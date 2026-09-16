package com.hisabpro.app.data.model

enum class TransactionType {
    INCOME,
    EXPENSE;

    fun displayName(): String = when (this) {
        INCOME -> "Income (Credit)"
        EXPENSE -> "Expense (Debit)"
    }
}

enum class Category(val label: String, val iconName: String) {
    BUSINESS("Business / Client", "business"),
    SALARY("Salary / Wages", "salary"),
    FOOD("Food & Dining", "food"),
    GROCERIES("Groceries", "groceries"),
    SHOPPING("Shopping", "shopping"),
    TRANSPORT("Travel & Transport", "transport"),
    UTILITIES("Bills & Utilities", "utilities"),
    HEALTH("Health & Medical", "health"),
    ENTERTAINMENT("Entertainment", "entertainment"),
    INVESTMENT("Investment", "investment"),
    OTHER("General / Other", "other");

    companion object {
        fun fromString(value: String): Category {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class PaymentMode(val label: String) {
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    ONLINE_UPI("UPI / Online"),
    CARD("Card");

    companion object {
        fun fromString(value: String): PaymentMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val dateMillis: Long,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val note: String = ""
)
