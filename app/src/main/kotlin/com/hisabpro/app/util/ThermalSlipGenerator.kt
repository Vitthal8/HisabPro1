package com.hisabpro.app.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalSlipGenerator {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)

    /**
     * Formats an invoice specifically for 58mm (32 cols) or 80mm (48 cols) Bluetooth thermal POS printers
     * and sharing via WhatsApp/text with clean monospace aligned receipt layout.
     */
    fun formatThermalSlipText(
        invoice: Invoice,
        profile: BusinessProfile,
        widthChars: Int = 32
    ): String {
        val lineDivider = "-".repeat(widthChars)
        val doubleDivider = "=".repeat(widthChars)

        val sb = StringBuilder()

        // Store Header (Centered)
        val shopName = profile.shopName.ifBlank { "HISABPRO STORE" }
        sb.appendLine(centerText(shopName.uppercase(), widthChars))
        if (profile.address.isNotBlank()) {
            sb.appendLine(centerText(profile.address, widthChars))
        }
        if (profile.city.isNotBlank() || profile.pincode.isNotBlank()) {
            val loc = listOf(profile.city, profile.pincode).filter { it.isNotBlank() }.joinToString(" - ")
            sb.appendLine(centerText(loc, widthChars))
        }
        if (profile.phone.isNotBlank()) {
            sb.appendLine(centerText("Ph: ${profile.phone}", widthChars))
        }
        if (profile.gstin.isNotBlank()) {
            sb.appendLine(centerText("GSTIN: ${profile.gstin.uppercase()}", widthChars))
        }

        sb.appendLine(doubleDivider)

        // Title and Meta
        val title = when (invoice.type) {
            InvoiceType.TAX_INVOICE -> "TAX INVOICE"
            InvoiceType.NON_GST_BILL -> "RETAIL BILL / CASH SLIP"
            InvoiceType.PROFORMA -> "ESTIMATE / QUOTATION"
        }
        sb.appendLine(centerText(title, widthChars))
        sb.appendLine(lineDivider)

        sb.appendLine(leftRightText("Bill No: ${invoice.invoiceNumber}", "", widthChars))
        sb.appendLine(leftRightText("Date: ${dateFormat.format(Date(invoice.dateMillis))}", "", widthChars))
        if (invoice.customerName.isNotBlank() && invoice.customerName != "Cash Customer") {
            sb.appendLine(leftRightText("Cust: ${invoice.customerName}", "", widthChars))
        }
        if (invoice.customerPhone.isNotBlank()) {
            sb.appendLine(leftRightText("Phone: ${invoice.customerPhone}", "", widthChars))
        }
        if (invoice.customerGstin.isNotBlank()) {
            sb.appendLine(leftRightText("GSTIN: ${invoice.customerGstin}", "", widthChars))
        }

        sb.appendLine(lineDivider)

        // Item Header
        // Format: Item [Qty x Rate]       Total
        sb.appendLine(leftRightText("Item Description", "Total", widthChars))
        sb.appendLine(lineDivider)

        invoice.items.forEachIndexed { idx, item ->
            val desc = "${idx + 1}. ${item.description}"
            val itemTotal = String.format(Locale.ENGLISH, "%.2f", item.getTotal(invoice.gstMode))
            sb.appendLine(leftRightText(desc, itemTotal, widthChars))

            val qtyRate = "   ${item.quantity} ${item.unit} @ ${String.format(Locale.ENGLISH, "%.2f", item.unitPrice)}"
            val taxInfo = if (invoice.gstMode != GstMode.EXEMPT && item.gstRate > 0) " (GST ${item.gstRate.toInt()}%)" else ""
            sb.appendLine(qtyRate + taxInfo)
        }

        sb.appendLine(lineDivider)

        // Summary Calculations
        sb.appendLine(leftRightText("Sub Total:", String.format(Locale.ENGLISH, "%.2f", invoice.subtotal), widthChars))
        if (invoice.gstMode != GstMode.EXEMPT && invoice.totalTax > 0) {
            sb.appendLine(leftRightText("Total GST:", String.format(Locale.ENGLISH, "%.2f", invoice.totalTax), widthChars))
        }
        if (invoice.discountAmount > 0) {
            sb.appendLine(leftRightText("Discount:", "-${String.format(Locale.ENGLISH, "%.2f", invoice.discountAmount)}", widthChars))
        }

        sb.appendLine(doubleDivider)
        sb.appendLine(leftRightText("GRAND TOTAL:", "Rs ${String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)}", widthChars))
        sb.appendLine(leftRightText("Paid Amount:", "Rs ${String.format(Locale.ENGLISH, "%.2f", invoice.paidAmount)}", widthChars))
        if (invoice.dueAmount > 0) {
            sb.appendLine(leftRightText("BALANCE DUE:", "Rs ${String.format(Locale.ENGLISH, "%.2f", invoice.dueAmount)}", widthChars))
        }
        sb.appendLine(doubleDivider)

        // Payment status & Mode
        sb.appendLine(leftRightText("Payment Status:", invoice.paymentStatus.label, widthChars))

        // UPI QR / ID if enabled and due
        if (profile.showUpiQrOnInvoice && profile.upiId.isNotBlank() && invoice.dueAmount > 0) {
            sb.appendLine(lineDivider)
            sb.appendLine(centerText("PAY VIA UPI / GPAY / PHONEPE", widthChars))
            sb.appendLine(centerText("VPA: ${profile.upiId}", widthChars))
            sb.appendLine(centerText("Scan QR or Pay to VPA", widthChars))
        }

        // Terms
        if (profile.termsAndConditions.isNotBlank()) {
            sb.appendLine(lineDivider)
            sb.appendLine(centerText("Terms & Conditions:", widthChars))
            sb.appendLine(profile.termsAndConditions)
        }

        sb.appendLine(doubleDivider)
        sb.appendLine(centerText("*** THANK YOU, VISIT AGAIN ***", widthChars))
        sb.appendLine(centerText("Powered by HisabPro", widthChars))
        sb.appendLine("\n\n") // Printer feed spacing

        return sb.toString()
    }

    fun shareThermalSlip(
        context: Context,
        invoice: Invoice,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        val slipText = formatThermalSlipText(invoice, profile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Thermal Slip - ${invoice.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, slipText)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Print / Share Thermal Slip (Bluetooth POS)"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats party khata ledger statement specifically for 58mm (32 cols) or 80mm (48 cols) Bluetooth thermal POS printers
     */
    fun formatPartyKhataSlipText(
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>,
        profile: BusinessProfile,
        widthChars: Int = 32
    ): String {
        val party = partyWithBalance.party
        val lineDivider = "-".repeat(widthChars)
        val doubleDivider = "=".repeat(widthChars)

        val sb = StringBuilder()

        // Store Header (Centered)
        val shopName = profile.shopName.ifBlank { "HISABPRO STORE" }
        sb.appendLine(centerText(shopName.uppercase(), widthChars))
        if (profile.address.isNotBlank()) {
            sb.appendLine(centerText(profile.address, widthChars))
        }
        if (profile.phone.isNotBlank()) {
            sb.appendLine(centerText("Ph: ${profile.phone}", widthChars))
        }
        if (profile.gstin.isNotBlank()) {
            sb.appendLine(centerText("GSTIN: ${profile.gstin.uppercase()}", widthChars))
        }

        sb.appendLine(doubleDivider)
        sb.appendLine(centerText("KHATA LEDGER STATEMENT", widthChars))
        sb.appendLine(lineDivider)

        sb.appendLine(leftRightText("Party: ${party.name}", "", widthChars))
        if (party.phone.isNotBlank()) {
            sb.appendLine(leftRightText("Phone: ${party.phone}", "", widthChars))
        }
        sb.appendLine(leftRightText("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date())}", "", widthChars))

        sb.appendLine(doubleDivider)
        sb.appendLine(leftRightText("Date/Ref", "Gave(-)  Got(+)", widthChars))
        sb.appendLine(lineDivider)

        val entryDateFormat = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
        entries.takeLast(15).forEach { entry ->
            val dateStr = entryDateFormat.format(Date(entry.dateMillis))
            val refStr = if (entry.billNumber.isNotBlank()) " #${entry.billNumber}" else ""
            val left = "$dateStr$refStr"

            val amtFormatted = String.format(Locale.ENGLISH, "%.2f", entry.amount)
            val right = if (entry.type == KhataEntryType.YOU_GAVE) {
                "-$amtFormatted"
            } else {
                "+$amtFormatted"
            }
            sb.appendLine(leftRightText(left, right, widthChars))
            if (entry.note.isNotBlank()) {
                sb.appendLine("  ${entry.note.take(widthChars - 2)}")
            }
        }

        sb.appendLine(lineDivider)
        sb.appendLine(leftRightText("Total Debit (Gave):", "Rs ${String.format(Locale.ENGLISH, "%.2f", partyWithBalance.totalGave)}", widthChars))
        sb.appendLine(leftRightText("Total Credit (Got):", "Rs ${String.format(Locale.ENGLISH, "%.2f", partyWithBalance.totalGot)}", widthChars))
        sb.appendLine(doubleDivider)

        val balanceLabel = if (partyWithBalance.isSettled) {
            "ACCOUNT SETTLED:"
        } else if (partyWithBalance.isReceivable) {
            "NET RECEIVABLE (DUE):"
        } else {
            "NET PAYABLE (ADVANCE):"
        }
        val balanceVal = "Rs ${String.format(Locale.ENGLISH, "%.2f", partyWithBalance.dueAmount)}"
        sb.appendLine(leftRightText(balanceLabel, balanceVal, widthChars))
        sb.appendLine(doubleDivider)

        // UPI instructions if receivable
        if (partyWithBalance.isReceivable && profile.upiId.isNotBlank()) {
            sb.appendLine(centerText("PAYMENT VIA UPI / GPAY / PHONEPE", widthChars))
            sb.appendLine(centerText("UPI ID: ${profile.upiId}", widthChars))
            sb.appendLine(lineDivider)
        }

        sb.appendLine(centerText("*** HISABPRO KHATA ***", widthChars))
        sb.appendLine("\n\n")

        return sb.toString()
    }

    fun sharePartyKhataThermalSlip(
        context: Context,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        val slipText = formatPartyKhataSlipText(partyWithBalance, entries, profile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Khata Statement - ${partyWithBalance.party.name}")
            putExtra(Intent.EXTRA_TEXT, slipText)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Print / Share Khata POS Slip"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats Inventory Stock audit / Low-stock reorder slip for 58mm/80mm thermal POS printers
     */
    fun formatInventoryStockThermalSlipText(
        items: List<Item>,
        category: String = "All",
        lowStockOnly: Boolean = false,
        profile: BusinessProfile,
        widthChars: Int = 32
    ): String {
        val lineDivider = "-".repeat(widthChars)
        val doubleDivider = "=".repeat(widthChars)
        val sb = StringBuilder()

        val shopName = profile.shopName.ifBlank { "HISABPRO STORE" }
        sb.appendLine(centerText(shopName.uppercase(), widthChars))
        if (profile.address.isNotBlank()) sb.appendLine(centerText(profile.address, widthChars))
        if (profile.phone.isNotBlank()) sb.appendLine(centerText("Ph: ${profile.phone}", widthChars))
        if (profile.gstin.isNotBlank()) sb.appendLine(centerText("GSTIN: ${profile.gstin.uppercase()}", widthChars))

        sb.appendLine(doubleDivider)
        val slipTitle = if (lowStockOnly) "LOW STOCK REORDER SLIP" else "STOCK INVENTORY AUDIT"
        sb.appendLine(centerText(slipTitle, widthChars))
        sb.appendLine(lineDivider)

        sb.appendLine(leftRightText("Category: $category", "SKUs: ${items.size}", widthChars))
        sb.appendLine(leftRightText("Date: ${dateFormat.format(Date())}", "", widthChars))
        sb.appendLine(doubleDivider)

        sb.appendLine(leftRightText("Item / SKU", "Stock / Rate", widthChars))
        sb.appendLine(lineDivider)

        items.forEach { item ->
            val itemName = item.name.take(widthChars - 10)
            val stockStr = "${item.currentStock.toInt()} ${item.unit}"
            sb.appendLine(leftRightText(itemName, stockStr, widthChars))

            val priceInfo = "Rs ${String.format(Locale.ENGLISH, "%.2f", item.salePrice)}"
            val skuInfo = if (item.itemCode.isNotBlank()) "SKU:${item.itemCode}" else "Cat:${item.category}"
            val alertInfo = if (item.isOutOfStock) " [OUT OF STOCK]" else if (item.isLowStock) " [LOW STOCK]" else ""
            sb.appendLine(leftRightText("  $skuInfo$alertInfo", priceInfo, widthChars))
        }

        sb.appendLine(lineDivider)
        val totalRetailVal = items.sumOf { it.stockValueSale }
        val totalCostVal = items.sumOf { it.stockValuePurchase }
        sb.appendLine(leftRightText("Total Stock Val (Sale):", "Rs ${String.format(Locale.ENGLISH, "%.2f", totalRetailVal)}", widthChars))
        if (totalCostVal > 0) {
            sb.appendLine(leftRightText("Total Cost Val (Pur):", "Rs ${String.format(Locale.ENGLISH, "%.2f", totalCostVal)}", widthChars))
        }
        sb.appendLine(doubleDivider)
        sb.appendLine(centerText("*** INVENTORY AUDIT VERIFIED ***", widthChars))
        sb.appendLine("\n\n")

        return sb.toString()
    }

    fun shareInventoryStockThermalSlip(
        context: Context,
        items: List<Item>,
        category: String = "All",
        lowStockOnly: Boolean = false,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        val slipText = formatInventoryStockThermalSlipText(items, category, lowStockOnly, profile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Stock Audit - ${profile.shopName.ifBlank { "HisabPro Store" }}")
            putExtra(Intent.EXTRA_TEXT, slipText)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Print / Share Stock POS Slip"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats a single item barcode / shelf price label for 58mm thermal label/receipt printers
     */
    fun formatSingleItemThermalLabel(
        item: Item,
        profile: BusinessProfile,
        widthChars: Int = 32
    ): String {
        val lineDivider = "-".repeat(widthChars)
        val doubleDivider = "=".repeat(widthChars)
        val sb = StringBuilder()

        val shopName = profile.shopName.ifBlank { "HISABPRO STORE" }
        sb.appendLine(centerText(shopName.uppercase(), widthChars))
        sb.appendLine(doubleDivider)
        sb.appendLine(centerText(item.name.uppercase(), widthChars))
        sb.appendLine(lineDivider)

        sb.appendLine(centerText("MRP: Rs ${String.format(Locale.ENGLISH, "%.2f", item.salePrice)}", widthChars))
        sb.appendLine(centerText("(Incl. of all taxes)", widthChars))

        if (item.itemCode.isNotBlank()) {
            sb.appendLine(lineDivider)
            sb.appendLine(centerText("BARCODE / SKU: ${item.itemCode}", widthChars))
            sb.appendLine(centerText("*${item.itemCode}*", widthChars))
        }
        if (item.hsnCode.isNotBlank()) {
            sb.appendLine(centerText("HSN: ${item.hsnCode} | GST: ${item.gstRate.toInt()}%", widthChars))
        }

        sb.appendLine(doubleDivider)
        sb.appendLine(centerText("PACK: 1 ${item.unit.uppercase()}", widthChars))
        sb.appendLine("\n\n")

        return sb.toString()
    }

    fun shareSingleItemThermalLabel(
        context: Context,
        item: Item,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        val labelText = formatSingleItemThermalLabel(item, profile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Price Label - ${item.name}")
            putExtra(Intent.EXTRA_TEXT, labelText)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Print Thermal Shelf Label"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats Daily Daybook / Cash Drawer Closing Settlement Slip for 58mm/80mm thermal POS printers
     */
    fun formatDailyCashbookThermalSlipText(
        transactions: List<Transaction>,
        dateMillis: Long = System.currentTimeMillis(),
        profile: BusinessProfile,
        widthChars: Int = 32
    ): String {
        val lineDivider = "-".repeat(widthChars)
        val doubleDivider = "=".repeat(widthChars)
        val sb = StringBuilder()

        val shopName = profile.shopName.ifBlank { "HISABPRO STORE" }
        sb.appendLine(centerText(shopName.uppercase(), widthChars))
        if (profile.address.isNotBlank()) sb.appendLine(centerText(profile.address, widthChars))
        if (profile.phone.isNotBlank()) sb.appendLine(centerText("Ph: ${profile.phone}", widthChars))

        sb.appendLine(doubleDivider)
        sb.appendLine(centerText("DAILY CASH DRAWER CLOSING", widthChars))
        sb.appendLine(lineDivider)

        val dayFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.ENGLISH)
        sb.appendLine(leftRightText("Date: ${dayFormat.format(Date(dateMillis))}", "", widthChars))
        sb.appendLine(leftRightText("Printed: ${dateFormat.format(Date())}", "", widthChars))
        sb.appendLine(doubleDivider)

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense

        // Breakdown by payment mode
        val cashIn = transactions.filter { it.type == TransactionType.INCOME && it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val cashOut = transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val netCashDrawer = cashIn - cashOut

        val upiIn = transactions.filter { it.type == TransactionType.INCOME && it.paymentMode == PaymentMode.ONLINE_UPI }.sumOf { it.amount }
        val bankIn = transactions.filter { it.type == TransactionType.INCOME && it.paymentMode == PaymentMode.BANK_TRANSFER }.sumOf { it.amount }

        sb.appendLine(leftRightText("Total Collections (In):", "Rs ${String.format(Locale.ENGLISH, "%.2f", totalIncome)}", widthChars))
        sb.appendLine(leftRightText("Total Expenses (Out):", "Rs ${String.format(Locale.ENGLISH, "%.2f", totalExpense)}", widthChars))
        sb.appendLine(lineDivider)
        sb.appendLine(leftRightText("NET DAY CASHFLOW:", "Rs ${String.format(Locale.ENGLISH, "%+.2f", netCashFlow)}", widthChars))
        sb.appendLine(doubleDivider)

        sb.appendLine(centerText("DRAWER CASH SETTLEMENT", widthChars))
        sb.appendLine(leftRightText("Cash Received (In):", "Rs ${String.format(Locale.ENGLISH, "%.2f", cashIn)}", widthChars))
        sb.appendLine(leftRightText("Cash Paid (Out):", "Rs ${String.format(Locale.ENGLISH, "%.2f", cashOut)}", widthChars))
        sb.appendLine(lineDivider)
        sb.appendLine(leftRightText("CLOSING CASH IN HAND:", "Rs ${String.format(Locale.ENGLISH, "%.2f", netCashDrawer)}", widthChars))

        if (upiIn > 0 || bankIn > 0) {
            sb.appendLine(lineDivider)
            if (upiIn > 0) sb.appendLine(leftRightText("UPI/QR Collections:", "Rs ${String.format(Locale.ENGLISH, "%.2f", upiIn)}", widthChars))
            if (bankIn > 0) sb.appendLine(leftRightText("Bank Transfer In:", "Rs ${String.format(Locale.ENGLISH, "%.2f", bankIn)}", widthChars))
        }

        sb.appendLine(doubleDivider)
        sb.appendLine(centerText("Entries Count: ${transactions.size}", widthChars))
        sb.appendLine(centerText("*** DRAWER BALANCED & CLOSED ***", widthChars))
        sb.appendLine("\n\n")

        return sb.toString()
    }

    fun shareDailyCashbookThermalSlip(
        context: Context,
        transactions: List<Transaction>,
        dateMillis: Long = System.currentTimeMillis(),
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        val slipText = formatDailyCashbookThermalSlipText(transactions, dateMillis, profile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Daily Cash Settlement - ${profile.shopName.ifBlank { "HisabPro Store" }}")
            putExtra(Intent.EXTRA_TEXT, slipText)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Print / Share Cash Drawer Slip"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val leftPadding = (width - text.length) / 2
        return " ".repeat(leftPadding) + text
    }

    private fun leftRightText(left: String, right: String, width: Int): String {
        val totalLen = left.length + right.length
        if (totalLen >= width) {
            val maxLeft = width - right.length - 1
            val truncatedLeft = if (maxLeft > 0) left.take(maxLeft) else left
            val spaces = 1.coerceAtLeast(width - truncatedLeft.length - right.length)
            return truncatedLeft + " ".repeat(spaces) + right
        }
        val spaces = width - totalLen
        return left + " ".repeat(spaces) + right
    }
}
