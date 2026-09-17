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
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.repository.SettingsRepository
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PartyStatementPdfGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

    fun generatePdf(
        context: Context,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>,
        profileOverride: BusinessProfile? = null
    ): File {
        val statementsDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val safeName = partyWithBalance.party.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(statementsDir, "Statement_${safeName}_${System.currentTimeMillis()}.pdf")

        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width
        val pageHeight = 842 // A4 standard height
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawStatement(canvas, partyWithBalance, entries, pageWidth, pageHeight, profile)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    private fun drawStatement(
        canvas: Canvas,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>,
        width: Int,
        height: Int,
        profile: BusinessProfile
    ) {
        val party = partyWithBalance.party
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
        val titleText = "PARTY STATEMENT"
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

        // Subtitle Date on Right
        val dateText = "Generated: ${dateFormat.format(Date())}"
        val dateWidth = paint.measureText(dateText)
        canvas.drawText(dateText, width - margin - dateWidth, margin + 31f, paint)

        y += 6f
        // Divider
        paint.color = Color.parseColor("#E5E7EB")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, y, width - margin, y, paint)
        y += 16f

        // Two Cards Row: Party Details (Left) + Balance Summary (Right)
        val colWidth = (width - margin * 2 - 16f) / 2f
        val leftX = margin
        val rightX = margin + colWidth + 16f
        val cardHeight = 90f

        // Left Card: Party Details
        paint.color = Color.parseColor("#F9FAFB")
        val partyCardRect = RectF(leftX, y, leftX + colWidth, y + cardHeight)
        canvas.drawRoundRect(partyCardRect, 8f, 8f, paint)
        paint.color = Color.parseColor("#E5E7EB")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(partyCardRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var py = y + 18f
        paint.color = Color.parseColor("#6B7280")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PARTY / ACCOUNT DETAILS", leftX + 12f, py, paint)

        py += 16f
        paint.color = Color.parseColor("#111827")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(party.name, leftX + 12f, py, paint)

        py += 14f
        paint.color = Color.parseColor("#374151")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Phone: ${party.phone.ifBlank { "N/A" }}  |  Type: ${party.type.label}", leftX + 12f, py, paint)

        py += 14f
        val gstinStr = if (party.gstin.isNotBlank()) "GSTIN: ${party.gstin}" else "Tag: ${party.tag.label}"
        canvas.drawText(gstinStr, leftX + 12f, py, paint)

        // Right Card: Balance Summary
        val isReceivable = partyWithBalance.isReceivable
        val isSettled = partyWithBalance.isSettled
        val statusBg = when {
            isSettled -> Color.parseColor("#F3F4F6")
            isReceivable -> Color.parseColor("#ECFDF5")
            else -> Color.parseColor("#FEF2F2")
        }
        val statusBorder = when {
            isSettled -> Color.parseColor("#E5E7EB")
            isReceivable -> Color.parseColor("#A7F3D0")
            else -> Color.parseColor("#FECACA")
        }
        val statusTextCol = when {
            isSettled -> Color.parseColor("#374151")
            isReceivable -> Color.parseColor("#065F46")
            else -> Color.parseColor("#991B1B")
        }

        paint.color = statusBg
        val balCardRect = RectF(rightX, y, rightX + colWidth, y + cardHeight)
        canvas.drawRoundRect(balCardRect, 8f, 8f, paint)
        paint.color = statusBorder
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(balCardRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var by = y + 18f
        paint.color = statusTextCol
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NET OUTSTANDING STATUS", rightX + 12f, by, paint)

        by += 22f
        paint.textSize = 17f
        val balStr = "₹${formatAmt(partyWithBalance.dueAmount)}"
        canvas.drawText(balStr, rightX + 12f, by, paint)

        by += 15f
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val statusExplanation = when {
            isSettled -> "Account is fully settled (₹0.00)"
            isReceivable -> "You will receive from ${party.name}"
            else -> "You need to pay to ${party.name}"
        }
        canvas.drawText(statusExplanation, rightX + 12f, by, paint)

        y += cardHeight + 20f

        // Ledger Table Header
        val thHeight = 24f
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawRect(margin, y, width - margin, y + thHeight, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colDate = margin + 10f
        val colRef = margin + 85f
        val colDesc = margin + 175f
        val colDebit = width - margin - 200f
        val colCredit = width - margin - 110f
        val colBalance = width - margin - 20f

        val thY = y + 16f
        canvas.drawText("DATE", colDate, thY, paint)
        canvas.drawText("REF / BILL #", colRef, thY, paint)
        canvas.drawText("DETAILS / REMARKS", colDesc, thY, paint)

        val debHdr = "DEBIT (₹)"
        canvas.drawText(debHdr, colDebit - paint.measureText(debHdr) / 2, thY, paint)
        val credHdr = "CREDIT (₹)"
        canvas.drawText(credHdr, colCredit - paint.measureText(credHdr) / 2, thY, paint)
        val balHdr = "BALANCE"
        canvas.drawText(balHdr, colBalance - paint.measureText(balHdr), thY, paint)

        y += thHeight

        // Chronological order for ledger statement (oldest to newest)
        val chronologicalEntries = entries.sortedBy { it.dateMillis }

        var runningBalance = 0.0
        val rowHeight = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        for ((index, entry) in chronologicalEntries.withIndex()) {
            if (y + rowHeight > height - 80f) {
                // End of page buffer
                break
            }

            // Zebra background
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F9FAFB")
                canvas.drawRect(margin, y, width - margin, y + rowHeight, paint)
            }

            // Calculations
            val isGave = entry.type == KhataEntryType.YOU_GAVE
            if (isGave) {
                runningBalance += entry.amount
            } else {
                runningBalance -= entry.amount
            }

            val rY = y + 15f

            // Date
            paint.color = Color.parseColor("#374151")
            canvas.drawText(dateFormat.format(Date(entry.dateMillis)), colDate, rY, paint)

            // Bill / Ref
            val refText = entry.billNumber.ifBlank { "-" }
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText(refText.take(14), colRef, rY, paint)

            // Description
            val descText = entry.note.ifBlank { if (isGave) "Goods / Services" else "Payment Received" }
            paint.color = Color.parseColor("#111827")
            canvas.drawText(descText.take(28), colDesc, rY, paint)

            // Debit (You Gave)
            if (isGave) {
                paint.color = Color.parseColor("#B91C1C")
                val amtStr = formatAmt(entry.amount)
                canvas.drawText(amtStr, colDebit - paint.measureText(amtStr) / 2, rY, paint)
            } else {
                paint.color = Color.parseColor("#9CA3AF")
                canvas.drawText("-", colDebit, rY, paint)
            }

            // Credit (You Got)
            if (!isGave) {
                paint.color = Color.parseColor("#047857")
                val amtStr = formatAmt(entry.amount)
                canvas.drawText(amtStr, colCredit - paint.measureText(amtStr) / 2, rY, paint)
            } else {
                paint.color = Color.parseColor("#9CA3AF")
                canvas.drawText("-", colCredit, rY, paint)
            }

            // Running Balance
            paint.color = if (runningBalance >= 0) Color.parseColor("#111827") else Color.parseColor("#B91C1C")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val balText = "₹${formatAmt(kotlin.math.abs(runningBalance))}"
            canvas.drawText(balText, colBalance - paint.measureText(balText), rY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Light horizontal rule
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

        // Total Debit
        paint.color = Color.parseColor("#B91C1C")
        val totDebitStr = "₹${formatAmt(partyWithBalance.totalGave)}"
        canvas.drawText(totDebitStr, colDebit - paint.measureText(totDebitStr) / 2, y + 18f, paint)

        // Total Credit
        paint.color = Color.parseColor("#047857")
        val totCreditStr = "₹${formatAmt(partyWithBalance.totalGot)}"
        canvas.drawText(totCreditStr, colCredit - paint.measureText(totCreditStr) / 2, y + 18f, paint)

        // Net Balance
        paint.color = statusTextCol
        val finalBalStr = "₹${formatAmt(partyWithBalance.dueAmount)}"
        canvas.drawText(finalBalStr, colBalance - paint.measureText(finalBalStr), y + 18f, paint)

        // Footer
        val footerY = height - margin - 20f
        paint.color = Color.parseColor("#E5E7EB")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, footerY, width - margin, footerY, paint)

        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("This is a computer-generated statement prepared via HisabPro.", margin, footerY + 14f, paint)

        val signText = "For ${shopDisplayName.uppercase()} (Authorized Signatory)"
        val signWidth = paint.measureText(signText)
        canvas.drawText(signText, width - margin - signWidth, footerY + 14f, paint)
    }

    private fun formatAmt(amount: Double): String {
        return String.format(Locale.ENGLISH, "%,.2f", amount)
    }

    fun sharePdf(
        context: Context,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>,
        targetWhatsApp: Boolean = false,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        try {
            val file = generatePdf(context, partyWithBalance, entries, profile)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.hisabpro.app.fileprovider",
                file
            )

            val subject = "Khata Statement: ${partyWithBalance.party.name} (${profile.shopName.ifBlank { "HisabPro" }})"
            val textSummary = buildString {
                append("Namaste ${partyWithBalance.party.name},\n")
                append("Please find attached your official Account Statement from ${profile.shopName.ifBlank { "HisabPro Store" }}.\n")
                val balFormatted = formatAmt(partyWithBalance.dueAmount)
                if (partyWithBalance.isSettled) {
                    append("Current Balance: ₹0.00 (Fully Settled)\n")
                } else if (partyWithBalance.isReceivable) {
                    append("Net Pending Balance: ₹$balFormatted\n")
                } else {
                    append("Net Advance/Payable: ₹$balFormatted\n")
                }
                append("\nThank you for doing business with us!")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, textSummary)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share Khata Statement (${partyWithBalance.party.name})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text message
            ShareHelper.shareBalanceStatement(
                context = context,
                party = partyWithBalance.party,
                netBalance = partyWithBalance.netBalance,
                businessName = profile.shopName.ifBlank { "HisabPro Store" }
            )
        }
    }

    fun exportCsv(
        context: Context,
        partyWithBalance: PartyWithBalance,
        entries: List<KhataEntry>
    ) {
        try {
            val exportDir = File(context.cacheDir, "csv_exports").apply { mkdirs() }
            val safeName = partyWithBalance.party.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(exportDir, "Ledger_${safeName}_${System.currentTimeMillis()}.csv")

            val chronologicalEntries = entries.sortedBy { it.dateMillis }
            var running = 0.0

            file.bufferedWriter().use { writer ->
                writer.write("Date,Time,Reference / Bill No,Description,Type,Debit (You Gave),Credit (You Got),Running Balance\n")
                for (entry in chronologicalEntries) {
                    val isGave = entry.type == KhataEntryType.YOU_GAVE
                    val debit = if (isGave) entry.amount else 0.0
                    val credit = if (!isGave) entry.amount else 0.0
                    if (isGave) running += debit else running -= credit

                    val dateStr = dateFormat.format(Date(entry.dateMillis))
                    val timeStr = timeFormat.format(Date(entry.dateMillis))
                    val refStr = "\"${entry.billNumber.replace("\"", "\"\"")}\""
                    val descStr = "\"${entry.note.replace("\"", "\"\"")}\""
                    val typeStr = if (isGave) "DEBIT (YOU GAVE)" else "CREDIT (YOU GOT)"

                    writer.write("$dateStr,$timeStr,$refStr,$descStr,$typeStr,$debit,$credit,$running\n")
                }
            }

            val uri: Uri = FileProvider.getUriForFile(context, "com.hisabpro.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Ledger Export - ${partyWithBalance.party.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Ledger CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }
}
