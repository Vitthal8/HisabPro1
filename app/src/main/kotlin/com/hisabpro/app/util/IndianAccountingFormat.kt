package com.hisabpro.app.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Standard Indian Accounting Utilities:
 * 1. Financial Year (April 1 to March 31)
 * 2. Indian Numbering System (Lakhs & Crores e.g. ₹1,23,456.00)
 * 3. Dr / Cr (Debit / Credit) Ledger labels
 * 4. DD/MM/YYYY Indian Date formatting
 * 5. Multi-language dictionary (English, Hindi, Marathi)
 */
object IndianAccountingFormat {

    /**
     * Calculates the Indian Financial Year string, e.g. "2025-26".
     * In India, FY starts on April 1 (Month index 3 in Calendar) and ends on March 31.
     */
    fun getFinancialYear(dateMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-indexed: 3 is April

        return if (month >= Calendar.APRIL) {
            val nextYearShort = (year + 1) % 100
            "$year-${String.format(Locale.ROOT, "%02d", nextYearShort)}"
        } else {
            val prevYear = year - 1
            val currentYearShort = year % 100
            "$prevYear-${String.format(Locale.ROOT, "%02d", currentYearShort)}"
        }
    }

    /**
     * Generates a standard FY-prefixed invoice number e.g. "2025-26/INV/001" or "2025-26/BILL/001"
     */
    fun formatInvoiceNumberWithFY(
        prefix: String,
        sequenceNumber: Int,
        dateMillis: Long = System.currentTimeMillis()
    ): String {
        val fy = getFinancialYear(dateMillis)
        val seqPadded = String.format(Locale.ROOT, "%03d", sequenceNumber)
        return "$fy/$prefix/$seqPadded"
    }

    /**
     * Formats amount according to Indian numbering system (Lakhs, Crores):
     * e.g. 1234567.50 -> ₹12,34,567.50
     */
    fun formatIndianCurrency(amount: Double, showSymbol: Boolean = true): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val longPart = absAmount.toLong()
        val decimalPart = kotlin.math.round((absAmount - longPart) * 100).toInt()

        val s = longPart.toString()
        val formattedLong = if (s.length <= 3) {
            s
        } else {
            val lastThree = s.substring(s.length - 3)
            val rest = s.substring(0, s.length - 3)
            val sb = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count % 2 == 0 && i > 0) {
                    sb.append(',')
                }
            }
            sb.reverse().append(',').append(lastThree).toString()
        }

        val formatted = if (decimalPart > 0) {
            "$formattedLong.${String.format(Locale.ROOT, "%02d", decimalPart)}"
        } else {
            formattedLong
        }

        val symbolPrefix = if (showSymbol) "₹" else ""
        return if (isNegative) "-$symbolPrefix$formatted" else "$symbolPrefix$formatted"
    }

    /**
     * Formats date as DD/MM/YYYY (Indian Standard)
     */
    fun formatIndianDate(dateMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        return sdf.format(Date(dateMillis))
    }

    /**
     * Returns Dr / Cr descriptor for party running balances:
     * - Customer positive balance: Dr (Debit = Money to Receive)
     * - Supplier positive balance: Cr (Credit = Money to Pay)
     */
    fun getDrCrBalanceLabel(balance: Double, isCustomer: Boolean): String {
        val formatted = formatIndianCurrency(kotlin.math.abs(balance))
        return when {
            balance > 0 -> {
                if (isCustomer) "$formatted Dr (To Collect)" else "$formatted Cr (To Pay)"
            }
            balance < 0 -> {
                if (isCustomer) "$formatted Cr (Advance Paid)" else "$formatted Dr (Advance Given)"
            }
            else -> "₹0 (Settled)"
        }
    }

    /**
     * Multi-language dictionary for Indian SMB accounting terms (English, Hindi, Marathi)
     */
    fun t(key: String, lang: String = "en"): String {
        val dict = mapOf(
            "dashboard" to mapOf("en" to "Dashboard", "hi" to "डैशबोर्ड", "mr" to "डॅशबोर्ड"),
            "today_sales" to mapOf("en" to "Today's Sales", "hi" to "आज की बिक्री", "mr" to "आजची विक्री"),
            "to_collect" to mapOf("en" to "To Collect (Receivables)", "hi" to "लेना बाकी (उधार)", "mr" to "येणे बाकी (उधारी)"),
            "to_pay" to mapOf("en" to "To Pay (Payables)", "hi" to "देना बाकी", "mr" to "देणे बाकी"),
            "cash_in_hand" to mapOf("en" to "Cash in Hand", "hi" to "रोकड़ (नकद)", "mr" to "हातातील रोख"),
            "bank_balance" to mapOf("en" to "Bank Balance", "hi" to "बैंक बैलेंस", "mr" to "बँक शिल्लक"),
            "new_sale" to mapOf("en" to "New Sale", "hi" to "नई बिक्री", "mr" to "नवीन विक्री"),
            "payment_in" to mapOf("en" to "Payment In", "hi" to "पेमेंट जमा", "mr" to "पेमेंट जमा"),
            "add_expense" to mapOf("en" to "Expense Out", "hi" to "खर्च", "mr" to "खर्च"),
            "add_party" to mapOf("en" to "Add Party", "hi" to "पार्टी जोड़ें", "mr" to "पार्टी जोडा"),
            "daybook" to mapOf("en" to "Day Book (Roznamcha)", "hi" to "रोजनामचा", "mr" to "रोजकीर्द"),
            "cashbook" to mapOf("en" to "Cash Book", "hi" to "रोकड़ बही", "mr" to "रोख वही"),
            "parties" to mapOf("en" to "Parties / Khata", "hi" to "पार्टियां (खाता)", "mr" to "खातेदार (खाते)"),
            "sales" to mapOf("en" to "Sales / Invoices", "hi" to "बिक्री / बिल", "mr" to "विक्री / बिले"),
            "reports" to mapOf("en" to "Reports & GST", "hi" to "रिपोर्ट्स और जीएसटी", "mr" to "अहवाल व जीएसटी"),
            "items" to mapOf("en" to "Items & Stock", "hi" to "आइटम और स्टॉक", "mr" to "वस्तू व स्टॉक"),
            "non_gst_mode" to mapOf("en" to "Non-GST Business", "hi" to "नॉन-जीएसटी व्यापार", "mr" to "विना-जीएसटी व्यवसाय"),
            "gst_registered" to mapOf("en" to "GST Registered", "hi" to "जीएसटी पंजीकृत", "mr" to "जीएसटी नोंदणीकृत")
        )
        return dict[key]?.get(lang) ?: dict[key]?.get("en") ?: key
    }
}
