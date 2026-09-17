package com.hisabpro.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.SettingsRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CashbookPdfGenerator {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

    fun generatePdf(
        context: Context,
        transactions: List<Transaction>,
        dateRangeLabel: String = "All Time",
        profileOverride: BusinessProfile? = null
    ): File {
        val statementsDir = File(context.cacheDir, "cashbook").apply { mkdirs() }
        val file = File(statementsDir, "Cashbook_${System.currentTimeMillis()}.pdf")

        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width
        val pageHeight = 842 // A4 standard height
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawCashbook(canvas, transactions, dateRangeLabel, pageWidth, pageHeight, profile)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    private fun drawCashbook(
        canvas: Canvas,
        transactions: List<Transaction>,
        dateRangeLabel: String,
        width: Int,
        height: Int,
        profile: BusinessProfile
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 36f

        // White background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Top Accent Stripe
        paint.color = Color.parseColor("#1B5E20") // Emerald Primary
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, paint)

        var y = margin + 15f

        // Business Header (Left)
        paint.color = Color.parseColor("#111827")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 17f
        val shopDisplayName = profile.shopName.ifBlank { "HISAB PRO STORE" }
        canvas.drawText(shopDisplayName.uppercase(), margin, y, paint)

        // Title on Right
        val titleText = "CASHBOOK / DAYBOOK"
        paint.textSize = 16f
        paint.color = Color.parseColor("#1B5E20")
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, width - margin - titleWidth, y, paint)

        y += 16f
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")

        if (profile.address.isNotBlank()) {
            canvas.drawText(profile.address, margin, y, paint)
            y += 13f
        }
        val phoneGst = buildString {
            if (profile.phone.isNotBlank()) append("Ph: ${profile.phone}")
            if (profile.gstin.isNotBlank()) {
                if (isNotEmpty()) append("  |  ")
                append("GSTIN: ${profile.gstin}")
            }
        }
        if (phoneGst.isNotBlank()) {
            canvas.drawText(phoneGst, margin, y, paint)
            y += 13f
        }

        // Subtitle Date & Range on Right
        val rangeText = "Period: $dateRangeLabel"
        val rangeWidth = paint.measureText(rangeText)
        canvas.drawText(rangeText, width - margin - rangeWidth, margin + 31f, paint)

        val genText = "Generated: ${dateFormat.format(Date())}"
        val genWidth = paint.measureText(genText)
        canvas.drawText(genText, width - margin - genWidth, margin + 44f, paint)

        y += 6f
        // Divider
        paint.color = Color.parseColor("#E5E7EB")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, y, width - margin, y, paint)
        y += 16f

        // Calculations
        var totalInflow = 0.0
        var totalOutflow = 0.0
        var cashInflow = 0.0
        var cashOutflow = 0.0
        var bankInflow = 0.0
        var bankOutflow = 0.0

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalInflow += tx.amount
                if (tx.paymentMode == PaymentMode.CASH) cashInflow += tx.amount else bankInflow += tx.amount
            } else {
                totalOutflow += tx.amount
                if (tx.paymentMode == PaymentMode.CASH) cashOutflow += tx.amount else bankOutflow += tx.amount
            }
        }

        val netCash = cashInflow - cashOutflow
        val netBank = bankInflow - bankOutflow
        val netTotal = totalInflow - totalOutflow

        // 3 Summary Cards Row: Total Balance, Cash In Hand, Bank & Online
        val gap = 12f
        val colWidth = (width - margin * 2 - gap * 2) / 3f
        val cardHeight = 72f

        // 1. Total Net Card
        drawSummaryCard(
            canvas = canvas,
            paint = paint,
            left = margin,
            top = y,
            width = colWidth,
            height = cardHeight,
            title = "NET DAYBOOK BALANCE",
            amount = netTotal,
            subtext = "In: ₹${formatAmt(totalInflow)} | Out: ₹${formatAmt(totalOutflow)}",
            isNet = true
        )

        // 2. Cash In Hand Card
        drawSummaryCard(
            canvas = canvas,
            paint = paint,
            left = margin + colWidth + gap,
            top = y,
            width = colWidth,
            height = cardHeight,
            title = "CASH IN HAND",
            amount = netCash,
            subtext = "In: ₹${formatAmt(cashInflow)} | Out: ₹${formatAmt(cashOutflow)}",
            isNet = false
        )

        // 3. Bank / UPI Card
        drawSummaryCard(
            canvas = canvas,
            paint = paint,
            left = margin + (colWidth + gap) * 2,
            top = y,
            width = colWidth,
            height = cardHeight,
            title = "BANK / UPI / ONLINE",
            amount = netBank,
            subtext = "In: ₹${formatAmt(bankInflow)} | Out: ₹${formatAmt(bankOutflow)}",
            isNet = false
        )

        y += cardHeight + 20f

        // Table Header
        val thHeight = 24f
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawRect(margin, y, width - margin, y + thHeight, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colDate = margin + 10f
        val colDesc = margin + 85f
        val colCat = margin + 225f
        val colMode = margin + 300f
        val colInflow = width - margin - 150f
        val colOutflow = width - margin - 85f
        val colBalance = width - margin - 15f

        val thY = y + 16f
        canvas.drawText("DATE & TIME", colDate, thY, paint)
        canvas.drawText("DESCRIPTION / PARTY", colDesc, thY, paint)
        canvas.drawText("CATEGORY", colCat, thY, paint)
        canvas.drawText("MODE", colMode, thY, paint)

        val inHdr = "INFLOW (₹)"
        canvas.drawText(inHdr, colInflow - paint.measureText(inHdr) / 2, thY, paint)
        val outHdr = "OUTFLOW (₹)"
        canvas.drawText(outHdr, colOutflow - paint.measureText(outHdr) / 2, thY, paint)
        val balHdr = "BALANCE"
        canvas.drawText(balHdr, colBalance - paint.measureText(balHdr), thY, paint)

        y += thHeight

        // Chronological order for running balance
        val chronological = transactions.sortedBy { it.dateMillis }
        var running = 0.0
        val rowHeight = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        for ((index, tx) in chronological.withIndex()) {
            if (y + rowHeight > height - 80f) {
                break
            }

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F9FAFB")
                canvas.drawRect(margin, y, width - margin, y + rowHeight, paint)
            }

            val isIncome = tx.type == TransactionType.INCOME
            if (isIncome) running += tx.amount else running -= tx.amount

            val rY = y + 15f

            // Date & Time
            paint.color = Color.parseColor("#374151")
            val dtText = "${dateFormat.format(Date(tx.dateMillis))} ${timeFormat.format(Date(tx.dateMillis))}"
            canvas.drawText(dtText, colDate, rY, paint)

            // Description
            paint.color = Color.parseColor("#111827")
            canvas.drawText(tx.title.take(24), colDesc, rY, paint)

            // Category
            paint.color = Color.parseColor("#4B5563")
            canvas.drawText(tx.category.label.take(14), colCat, rY, paint)

            // Mode
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText(tx.paymentMode.label.take(12), colMode, rY, paint)

            // Inflow
            if (isIncome) {
                paint.color = Color.parseColor("#047857")
                val inStr = formatAmt(tx.amount)
                canvas.drawText(inStr, colInflow - paint.measureText(inStr) / 2, rY, paint)
            } else {
                paint.color = Color.parseColor("#9CA3AF")
                canvas.drawText("-", colInflow, rY, paint)
            }

            // Outflow
            if (!isIncome) {
                paint.color = Color.parseColor("#B91C1C")
                val outStr = formatAmt(tx.amount)
                canvas.drawText(outStr, colOutflow - paint.measureText(outStr) / 2, rY, paint)
            } else {
                paint.color = Color.parseColor("#9CA3AF")
                canvas.drawText("-", colOutflow, rY, paint)
            }

            // Running Balance
            paint.color = if (running >= 0) Color.parseColor("#111827") else Color.parseColor("#B91C1C")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val balText = "₹${formatAmt(running)}"
            canvas.drawText(balText, colBalance - paint.measureText(balText), rY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Light horizontal line
            paint.color = Color.parseColor("#F3F4F6")
            paint.strokeWidth = 0.8f
            canvas.drawLine(margin, y + rowHeight, width - margin, y + rowHeight, paint)

            y += rowHeight
        }

        // Totals Row
        y += 8f
        val totalsRect = RectF(margin, y, width - margin, y + 28f)
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(totalsRect, 6f, 6f, paint)

        paint.color = Color.parseColor("#111827")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("TOTALS", colDate, y + 18f, paint)

        // Total Inflow
        paint.color = Color.parseColor("#047857")
        val totInStr = "₹${formatAmt(totalInflow)}"
        canvas.drawText(totInStr, colInflow - paint.measureText(totInStr) / 2, y + 18f, paint)

        // Total Outflow
        paint.color = Color.parseColor("#B91C1C")
        val totOutStr = "₹${formatAmt(totalOutflow)}"
        canvas.drawText(totOutStr, colOutflow - paint.measureText(totOutStr) / 2, y + 18f, paint)

        // Net Balance
        paint.color = if (netTotal >= 0) Color.parseColor("#065F46") else Color.parseColor("#991B1B")
        val finalBalStr = "₹${formatAmt(netTotal)}"
        canvas.drawText(finalBalStr, colBalance - paint.measureText(finalBalStr), y + 18f, paint)

        // Footer
        val footerY = height - margin - 20f
        paint.color = Color.parseColor("#E5E7EB")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, footerY, width - margin, footerY, paint)

        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("This is an official Cashbook report generated by HisabPro.", margin, footerY + 14f, paint)

        val signText = "For ${shopDisplayName.uppercase()} (Verified & Approved)"
        val signWidth = paint.measureText(signText)
        canvas.drawText(signText, width - margin - signWidth, footerY + 14f, paint)
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        paint: Paint,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        title: String,
        amount: Double,
        subtext: String,
        isNet: Boolean
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = if (isNet) Color.parseColor("#ECFDF5") else Color.parseColor("#F9FAFB")
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.color = if (isNet) Color.parseColor("#A7F3D0") else Color.parseColor("#E5E7EB")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var cy = top + 15f
        paint.color = if (isNet) Color.parseColor("#065F46") else Color.parseColor("#6B7280")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, left + 10f, cy, paint)

        cy += 19f
        paint.textSize = 13.5f
        paint.color = when {
            amount >= 0 -> Color.parseColor("#111827")
            else -> Color.parseColor("#B91C1C")
        }
        val sign = if (amount < 0) "- ₹" else "₹"
        canvas.drawText("$sign${formatAmt(kotlin.math.abs(amount))}", left + 10f, cy, paint)

        cy += 14f
        paint.textSize = 7.5f
        paint.color = Color.parseColor("#6B7280")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(subtext, left + 10f, cy, paint)
    }

    private fun formatAmt(amount: Double): String {
        return String.format(Locale.ENGLISH, "%,.2f", amount)
    }

    fun sharePdf(
        context: Context,
        transactions: List<Transaction>,
        dateRangeLabel: String = "All Time",
        targetWhatsApp: Boolean = false,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        try {
            val file = generatePdf(context, transactions, dateRangeLabel, profile)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.hisabpro.app.fileprovider",
                file
            )

            val subject = "Cashbook Statement - ${profile.shopName.ifBlank { "HisabPro Store" }} ($dateRangeLabel)"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Sharing Cashbook/Daybook Statement for $dateRangeLabel from ${profile.shopName.ifBlank { "HisabPro" }}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share Cashbook PDF ($dateRangeLabel)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    fun exportCsv(
        context: Context,
        transactions: List<Transaction>,
        dateRangeLabel: String = "All Time"
    ) {
        try {
            val exportDir = File(context.cacheDir, "csv_exports").apply { mkdirs() }
            val file = File(exportDir, "Cashbook_${System.currentTimeMillis()}.csv")

            val chronological = transactions.sortedBy { it.dateMillis }
            var running = 0.0

            file.bufferedWriter().use { writer ->
                writer.write("Date,Time,Title / Description,Category,Payment Mode,Type,Inflow (Income),Outflow (Expense),Running Balance,Note\n")
                for (tx in chronological) {
                    val isIncome = tx.type == TransactionType.INCOME
                    val inAmt = if (isIncome) tx.amount else 0.0
                    val outAmt = if (!isIncome) tx.amount else 0.0
                    if (isIncome) running += inAmt else running -= outAmt

                    val dateStr = dateFormat.format(Date(tx.dateMillis))
                    val timeStr = timeFormat.format(Date(tx.dateMillis))
                    val titleEsc = "\"${tx.title.replace("\"", "\"\"")}\""
                    val catStr = tx.category.label
                    val modeStr = tx.paymentMode.label
                    val typeStr = if (isIncome) "INCOME" else "EXPENSE"
                    val noteEsc = "\"${tx.note.replace("\"", "\"\"")}\""

                    writer.write("$dateStr,$timeStr,$titleEsc,$catStr,$modeStr,$typeStr,$inAmt,$outAmt,$running,$noteEsc\n")
                }
            }

            val uri = FileProvider.getUriForFile(context, "com.hisabpro.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cashbook Daybook CSV - $dateRangeLabel")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Cashbook CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }
}
