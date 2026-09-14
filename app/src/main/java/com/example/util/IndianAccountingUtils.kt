package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object IndianAccountingUtils {

    /**
     * Formats amount in Indian numbering system:
     * e.g. 1234567.89 -> ₹12,34,567.89 or ₹12,34,568
     */
    fun formatCurrency(amount: Double, showDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val absAmount = abs(amount)

        val longPart = absAmount.toLong()
        val decimalPart = String.format(Locale.US, "%.2f", absAmount - longPart).substring(1) // .xx

        val s = longPart.toString()
        val formattedNumber = if (s.length <= 3) {
            s
        } else {
            val lastThree = s.substring(s.length - 3)
            val rest = s.substring(0, s.length - 3)
            val sb = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count == 2 && i > 0) {
                    sb.append(",")
                    count = 0
                }
            }
            sb.reverse().toString() + "," + lastThree
        }

        val result = if (showDecimals) "$formattedNumber$decimalPart" else formattedNumber
        return if (isNegative) "-₹$result" else "₹$result"
    }

    /**
     * Formats balance with Dr/Cr indicator:
     * Positive = Dr (Customer owes us / Receivable)
     * Negative = Cr (We owe supplier / Payable)
     * Zero = Settled
     */
    fun formatBalanceDrCr(balance: Double): String {
        return when {
            balance > 0.01 -> "${formatCurrency(balance)} Dr"
            balance < -0.01 -> "${formatCurrency(abs(balance))} Cr"
            else -> "₹0.00"
        }
    }

    /**
     * Get current Indian Financial Year string (April 1 to March 31):
     * e.g. 2025-26 or 2026-27
     */
    fun getCurrentFinancialYear(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0 = Jan, 3 = April
        return if (month >= Calendar.APRIL) {
            val nextYear = (year + 1) % 100
            "$year-${String.format(Locale.US, "%02d", nextYear)}"
        } else {
            val prevYear = year - 1
            val thisYear = year % 100
            "$prevYear-${String.format(Locale.US, "%02d", thisYear)}"
        }
    }

    /**
     * Format timestamp to Indian standard DD/MM/YYYY
     */
    fun formatDate(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    /**
     * Generate standard Invoice Number:
     * e.g. 2025-26/INV/001
     */
    fun generateInvoiceNumber(sequence: Int, fy: String = getCurrentFinancialYear(), prefix: String = "INV"): String {
        return "$fy/$prefix/${String.format(Locale.US, "%03d", sequence)}"
    }
}
