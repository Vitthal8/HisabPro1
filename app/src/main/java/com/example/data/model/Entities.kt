package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PartyType {
    CUSTOMER,
    SUPPLIER,
    BOTH
}

enum class InvoiceType {
    SALE,
    PURCHASE,
    CREDIT_NOTE,
    DEBIT_NOTE
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK,
    CHEQUE
}

enum class AccountType {
    CASH,
    BANK,
    CAPITAL,
    LOAN
}

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val pan: String = "",
    @ColumnInfo(name = "logo_path") val logoPath: String = "",
    @ColumnInfo(name = "gst_enabled") val gstEnabled: Boolean = false,
    @ColumnInfo(name = "fy_start") val fyStart: String = "01/04", // 1st April
    val state: String = "Maharashtra",
    @ColumnInfo(name = "composition_scheme") val isCompositionScheme: Boolean = false,
    val language: String = "en", // en, hi, mr
    @ColumnInfo(name = "upi_id") val upiId: String = "mali.kirana@okhdfcbank"
)

@Entity(
    tableName = "parties",
    indices = [Index("business_id")]
)
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val type: PartyType = PartyType.CUSTOMER,
    @ColumnInfo(name = "opening_balance") val openingBalance: Double = 0.0, // Positive = Receivable (Dr), Negative = Payable (Cr)
    @ColumnInfo(name = "current_balance") val currentBalance: Double = 0.0 // Running balance
)

@Entity(
    tableName = "items",
    indices = [Index("business_id")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    val name: String,
    val unit: String = "PCS", // PCS, KG, MTR, BOX, LTR, PKT
    @ColumnInfo(name = "hsn_code") val hsnCode: String = "",
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double = 0.0,
    @ColumnInfo(name = "sell_price") val sellPrice: Double = 0.0,
    @ColumnInfo(name = "gst_rate") val gstRate: Double = 0.0, // e.g. 0, 5, 12, 18, 28
    val category: String = "General",
    @ColumnInfo(name = "stock_qty") val stockQty: Double = 0.0
)

@Entity(
    tableName = "invoices",
    indices = [Index("business_id"), Index("party_id")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    @ColumnInfo(name = "invoice_no") val invoiceNo: String,
    val date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "party_id") val partyId: Long,
    @ColumnInfo(name = "party_name") val partyName: String = "",
    val type: InvoiceType = InvoiceType.SALE,
    val subtotal: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    @ColumnInfo(name = "paid_amount") val paidAmount: Double = 0.0,
    @ColumnInfo(name = "payment_mode") val paymentMode: PaymentMode = PaymentMode.CASH,
    val notes: String = "",
    @ColumnInfo(name = "is_gst") val isGst: Boolean = false
)

@Entity(
    tableName = "invoice_items",
    indices = [Index("invoice_id"), Index("item_id")]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "invoice_id") val invoiceId: Long,
    @ColumnInfo(name = "item_id") val itemId: Long = 0,
    @ColumnInfo(name = "item_name") val itemName: String,
    val qty: Double = 1.0,
    val unit: String = "PCS",
    val rate: Double = 0.0,
    val discount: Double = 0.0,
    @ColumnInfo(name = "cgst_rate") val cgstRate: Double = 0.0,
    @ColumnInfo(name = "sgst_rate") val sgstRate: Double = 0.0,
    val amount: Double = 0.0
)

@Entity(
    tableName = "payments",
    indices = [Index("business_id"), Index("party_id")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    @ColumnInfo(name = "party_id") val partyId: Long,
    @ColumnInfo(name = "party_name") val partyName: String = "",
    val date: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val mode: PaymentMode = PaymentMode.CASH,
    @ColumnInfo(name = "reference_no") val referenceNo: String = "",
    val notes: String = "",
    @ColumnInfo(name = "linked_invoice_id") val linkedInvoiceId: Long? = null,
    @ColumnInfo(name = "is_received") val isReceived: Boolean = true // true = Received from customer, false = Paid to supplier
)

@Entity(
    tableName = "expenses",
    indices = [Index("business_id")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    val date: Long = System.currentTimeMillis(),
    val category: String, // Rent, Salary, Tea & Snacks, Electricity, Transport, Stationery, Misc
    val amount: Double = 0.0,
    val description: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    @ColumnInfo(name = "receipt_path") val receiptPath: String = ""
)

@Entity(
    tableName = "accounts",
    indices = [Index("business_id")]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    val name: String,
    val type: AccountType = AccountType.CASH,
    @ColumnInfo(name = "opening_balance") val openingBalance: Double = 0.0,
    @ColumnInfo(name = "current_balance") val currentBalance: Double = 0.0
)

data class JournalLine(
    val accountName: String,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0
)

@Entity(
    tableName = "journal_entries",
    indices = [Index("business_id")]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long = 1,
    val date: Long = System.currentTimeMillis(),
    val narration: String = "",
    val entries: List<JournalLine> = emptyList()
)
