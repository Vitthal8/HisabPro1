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
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.repository.SettingsRepository
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    fun generatePdf(context: Context, invoice: Invoice, profileOverride: BusinessProfile? = null): File {
        val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeNumber = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(invoicesDir, "${safeNumber}.pdf")

        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawInvoice(canvas, invoice, pageWidth, pageHeight, profile)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    private fun drawInvoice(canvas: Canvas, invoice: Invoice, width: Int, height: Int, profile: BusinessProfile) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 36f // 0.5 inch margin

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Top Header Accent Bar
        paint.color = Color.parseColor("#1B5E20") // Deep Forest Green
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, paint)

        var y = margin + 15f

        // Business Name & Title
        paint.color = Color.parseColor("#111827")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 17f
        val shopDisplayName = profile.shopName.ifBlank { "HISAB PRO ENTERPRISES" }
        canvas.drawText(shopDisplayName.uppercase(), margin, y, paint)

        // Title on Right
        val titleText = when (invoice.type) {
            InvoiceType.TAX_INVOICE -> "TAX INVOICE"
            InvoiceType.NON_GST_BILL -> "BILL OF SUPPLY"
            InvoiceType.PROFORMA -> "PROFORMA INVOICE"
        }
        paint.textSize = 16f
        paint.color = Color.parseColor("#1B5E20")
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, width - margin - titleWidth, y, paint)

        y += 16f
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")
        val gstinLine = if (profile.gstin.isNotBlank()) "GSTIN: ${profile.gstin.uppercase()}" else "TAX STATUS: COMPOSITION / REGULAR"
        val stateLine = if (profile.state.isNotBlank()) " | STATE: ${profile.state}${if (profile.stateCode.isNotBlank()) " (${profile.stateCode})" else ""}" else ""
        canvas.drawText("$gstinLine$stateLine", margin, y, paint)

        val origText = "ORIGINAL FOR RECIPIENT"
        val origWidth = paint.measureText(origText)
        canvas.drawText(origText, width - margin - origWidth, y, paint)

        y += 13f
        val fullAddress = listOf(profile.address, profile.city, "${profile.state} ${profile.pincode}".trim())
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Commercial Center, Main Road" }
        canvas.drawText(fullAddress.take(65), margin, y, paint)

        val contactLine = listOfNotNull(
            if (profile.phone.isNotBlank()) "Phone: ${profile.phone}" else null,
            if (profile.email.isNotBlank()) profile.email else null
        ).joinToString(" | ").ifBlank { "Contact: Support & Billing Desk" }
        canvas.drawText(contactLine.take(65), margin, y + 12f, paint)

        y += 24f

        // Divider
        paint.color = Color.parseColor("#E5E7EB")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, y, width - margin, y, paint)

        y += 16f

        // Two Column Meta: Left = Bill To, Right = Invoice Details
        val colWidth = (width - (margin * 2) - 16f) / 2f
        val leftX = margin
        val rightX = margin + colWidth + 16f

        // Box backgrounds
        paint.color = Color.parseColor("#F9FAFB")
        val boxHeight = 78f
        canvas.drawRoundRect(RectF(leftX, y, leftX + colWidth, y + boxHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(rightX, y, rightX + colWidth, y + boxHeight), 6f, 6f, paint)

        // Box Borders
        paint.color = Color.parseColor("#E5E7EB")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(leftX, y, leftX + colWidth, y + boxHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(rightX, y, rightX + colWidth, y + boxHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Left box content (Customer)
        paint.color = Color.parseColor("#6B7280")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO / CUSTOMER:", leftX + 10f, y + 16f, paint)

        paint.color = Color.parseColor("#111827")
        paint.textSize = 10.5f
        canvas.drawText(invoice.customerName, leftX + 10f, y + 31f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")
        paint.textSize = 8.5f
        val phoneText = if (invoice.customerPhone.isNotBlank()) "Phone: ${invoice.customerPhone}" else "Walk-in Counter Customer"
        canvas.drawText(phoneText, leftX + 10f, y + 45f, paint)

        val gstinText = if (invoice.customerGstin.isNotBlank()) "GSTIN: ${invoice.customerGstin}" else if (invoice.customerAddress.isNotBlank()) invoice.customerAddress else "Cash Transaction"
        canvas.drawText(gstinText.take(45), leftX + 10f, y + 59f, paint)

        // Right box content (Invoice Details)
        paint.color = Color.parseColor("#6B7280")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE DETAILS:", rightX + 10f, y + 16f, paint)

        paint.color = Color.parseColor("#111827")
        paint.textSize = 9.5f
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", rightX + 10f, y + 31f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")
        paint.textSize = 8.5f
        canvas.drawText("Date: ${dateFormat.format(Date(invoice.dateMillis))}", rightX + 10f, y + 45f, paint)

        val taxModeLabel = when (invoice.gstMode) {
            GstMode.INTRA_STATE -> "Tax: CGST + SGST"
            GstMode.INTER_STATE -> "Tax: IGST"
            GstMode.EXEMPT -> "Tax: Exempt / Non-GST"
        }
        canvas.drawText("Place of Supply: Maharashtra (27) | $taxModeLabel", rightX + 10f, y + 59f, paint)

        y += boxHeight + 16f

        // Table Header
        val thHeight = 22f
        paint.color = Color.parseColor("#E8F5E9") // Subtle light green table header
        canvas.drawRect(margin, y, width - margin, y + thHeight, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colSno = margin + 8f
        val colDesc = margin + 35f
        val colHsn = margin + 220f
        val colQty = margin + 285f
        val colRate = margin + 355f
        val colGst = margin + 430f
        val colTotal = width - margin - 8f

        canvas.drawText("#", colSno, y + 15f, paint)
        canvas.drawText("Item / Description", colDesc, y + 15f, paint)
        canvas.drawText("HSN", colHsn, y + 15f, paint)
        canvas.drawText("Qty", colQty, y + 15f, paint)
        canvas.drawText("Rate", colRate, y + 15f, paint)
        canvas.drawText("GST", colGst, y + 15f, paint)
        val thTotalText = "Total (₹)"
        val thTotalWidth = paint.measureText(thTotalText)
        canvas.drawText(thTotalText, colTotal - thTotalWidth, y + 15f, paint)

        y += thHeight

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        val rowHeight = 22f
        invoice.items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(margin, y, width - margin, y + rowHeight, paint)
            }

            paint.color = Color.parseColor("#374151")
            canvas.drawText("${index + 1}", colSno, y + 15f, paint)

            paint.color = Color.parseColor("#111827")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val desc = if (item.description.length > 32) item.description.take(30) + ".." else item.description
            canvas.drawText(desc, colDesc, y + 15f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText(if (item.hsnCode.isNotBlank()) item.hsnCode else "-", colHsn, y + 15f, paint)
            canvas.drawText("${item.quantity.toIntIfWhole()} ${item.unit}", colQty, y + 15f, paint)
            canvas.drawText("₹${String.format(Locale.ENGLISH, "%.2f", item.unitPrice)}", colRate, y + 15f, paint)
            canvas.drawText(if (invoice.gstMode != GstMode.EXEMPT && invoice.type != InvoiceType.NON_GST_BILL) "${item.gstRate.toIntIfWhole()}%" else "0%", colGst, y + 15f, paint)

            val itemTotalStr = "₹${String.format(Locale.ENGLISH, "%.2f", item.getTotal(invoice.gstMode))}"
            val itemTotalW = paint.measureText(itemTotalStr)
            paint.color = Color.parseColor("#111827")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(itemTotalStr, colTotal - itemTotalW, y + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // subtle row bottom line
            paint.color = Color.parseColor("#F3F4F6")
            canvas.drawLine(margin, y + rowHeight, width - margin, y + rowHeight, paint)

            y += rowHeight
        }

        // Table Bottom Line
        paint.color = Color.parseColor("#D1D5DB")
        canvas.drawLine(margin, y, width - margin, y, paint)

        y += 16f

        // Bottom section: Left = Notes & Bank Details, Right = Summary Calculation
        val summaryBoxWidth = 220f
        val summaryLeft = width - margin - summaryBoxWidth

        // Notes & Terms on Left
        val notesLeft = margin
        val notesWidth = summaryLeft - margin - 20f

        paint.color = Color.parseColor("#F9FAFB")
        val notesHeight = 110f
        canvas.drawRoundRect(RectF(notesLeft, y, notesLeft + notesWidth, y + notesHeight), 6f, 6f, paint)
        paint.color = Color.parseColor("#E5E7EB")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(notesLeft, y, notesLeft + notesWidth, y + notesHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#6B7280")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TERMS & BANK DETAILS:", notesLeft + 10f, y + 15f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")
        paint.textSize = 8f
        val bankLine = if (profile.bankName.isNotBlank() && profile.accountNumber.isNotBlank()) {
            "Bank: ${profile.bankName} | A/C: ${profile.accountNumber}"
        } else "Payment: Cash / UPI / Bank Transfer"
        canvas.drawText(bankLine.take(45), notesLeft + 10f, y + 30f, paint)

        val ifscUpiLine = listOfNotNull(
            if (profile.ifscCode.isNotBlank()) "IFSC: ${profile.ifscCode}" else null,
            if (profile.upiId.isNotBlank()) "UPI: ${profile.upiId}" else null
        ).joinToString(" | ")
        if (ifscUpiLine.isNotBlank()) {
            canvas.drawText(ifscUpiLine.take(45), notesLeft + 10f, y + 43f, paint)
        }

        val customNote = if (invoice.notes.isNotBlank()) {
            "Note: ${invoice.notes}"
        } else if (profile.termsAndConditions.isNotBlank()) {
            "Terms: ${profile.termsAndConditions.lines().firstOrNull() ?: ""}"
        } else "1. Goods once sold will not be taken back."
        canvas.drawText(customNote.take(50), notesLeft + 10f, y + 56f, paint)

        // Draw UPI QR Code on PDF if enabled
        if (profile.showUpiQrOnInvoice && profile.upiId.isNotBlank() && invoice.dueAmount > 0) {
            try {
                val upiUri = UpiPaymentHelper.buildUpiUri(
                    upiId = profile.upiId,
                    payeeName = profile.shopName.ifBlank { profile.ownerName.ifBlank { "Merchant" } },
                    amount = invoice.dueAmount,
                    invoiceNumber = invoice.invoiceNumber,
                    notes = "Bill ${invoice.invoiceNumber}"
                )
                val qrBitmap = UpiPaymentHelper.generateQrBitmap(upiUri, 120)
                if (qrBitmap != null) {
                    val qrSize = 42f
                    val qrX = notesLeft + notesWidth - qrSize - 8f
                    val qrY = y + 10f
                    val destRect = RectF(qrX, qrY, qrX + qrSize, qrY + qrSize)
                    canvas.drawBitmap(qrBitmap, null, destRect, null)
                    paint.textSize = 6f
                    paint.color = Color.parseColor("#1B5E20")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val scanText = "SCAN TO PAY"
                    val stW = paint.measureText(scanText)
                    canvas.drawText(scanText, qrX + (qrSize - stW) / 2f, qrY + qrSize + 8f, paint)
                }
            } catch (e: Exception) {
                // Ignore QR draw error
            }
        } else {
            canvas.drawText("2. Subject to local state jurisdiction.", notesLeft + 10f, y + 69f, paint)
        }

        // Right Summary Calculation
        var sumY = y + 10f
        fun drawSummaryLine(label: String, value: String, isBold: Boolean = false, color: Int = Color.parseColor("#374151")) {
            paint.textSize = if (isBold) 10f else 8.5f
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = color

            canvas.drawText(label, summaryLeft, sumY, paint)
            val valW = paint.measureText(value)
            canvas.drawText(value, width - margin - valW, sumY, paint)
            sumY += 16f
        }

        drawSummaryLine("Subtotal (Taxable):", "₹${String.format(Locale.ENGLISH, "%.2f", invoice.subtotal)}")

        if (invoice.discountAmount > 0) {
            drawSummaryLine("Discount:", "-₹${String.format(Locale.ENGLISH, "%.2f", invoice.discountAmount)}", color = Color.parseColor("#DC2626"))
        }

        if (invoice.type == InvoiceType.TAX_INVOICE) {
            if (invoice.gstMode == GstMode.INTRA_STATE) {
                drawSummaryLine("CGST:", "+₹${String.format(Locale.ENGLISH, "%.2f", invoice.cgstTotal)}")
                drawSummaryLine("SGST:", "+₹${String.format(Locale.ENGLISH, "%.2f", invoice.sgstTotal)}")
            } else if (invoice.gstMode == GstMode.INTER_STATE) {
                drawSummaryLine("IGST:", "+₹${String.format(Locale.ENGLISH, "%.2f", invoice.igstTotal)}")
            }
        }

        // Divider
        paint.color = Color.parseColor("#1B5E20")
        paint.strokeWidth = 1.5f
        canvas.drawLine(summaryLeft, sumY, width - margin, sumY, paint)
        sumY += 14f

        drawSummaryLine(
            label = "GRAND TOTAL:",
            value = "₹${String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)}",
            isBold = true,
            color = Color.parseColor("#1B5E20")
        )

        drawSummaryLine("Paid Amount:", "₹${String.format(Locale.ENGLISH, "%.2f", invoice.paidAmount)}", isBold = false)
        val dueColor = if (invoice.dueAmount > 0) Color.parseColor("#DC2626") else Color.parseColor("#1B5E20")
        drawSummaryLine(
            label = if (invoice.dueAmount > 0) "Balance Due:" else "Status:",
            value = if (invoice.dueAmount > 0) "₹${String.format(Locale.ENGLISH, "%.2f", invoice.dueAmount)}" else "PAID FULL",
            isBold = true,
            color = dueColor
        )

        // Signatory box at bottom
        val sigY = height - margin - 45f
        paint.color = Color.parseColor("#9CA3AF")
        paint.strokeWidth = 1f
        canvas.drawLine(width - margin - 150f, sigY, width - margin, sigY, paint)

        paint.textSize = 8.5f
        paint.color = Color.parseColor("#374151")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val sigText = "For ${profile.shopName.ifBlank { "HisabPro Enterprises" }}"
        val sigW = paint.measureText(sigText)
        canvas.drawText(sigText, width - margin - sigW, sigY + 14f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#6B7280")
        val authLabel = if (profile.ownerName.isNotBlank()) "(${profile.ownerName}) Authorized Signatory" else "Authorized Signatory"
        val authW = paint.measureText(authLabel)
        canvas.drawText(authLabel, width - margin - authW, sigY + 26f, paint)

        // Bottom Footer
        paint.textSize = 8f
        paint.color = Color.parseColor("#9CA3AF")
        canvas.drawText("Generated electronically via HisabPro - Simplified Business Accounting", margin, height - margin, paint)
    }

    private fun Double.toIntIfWhole(): String {
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.ENGLISH, "%.1f", this)
    }

    fun sharePdf(
        context: Context,
        invoice: Invoice,
        targetWhatsApp: Boolean = false,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        try {
            val file = generatePdf(context, invoice, profile)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.hisabpro.app.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice.invoiceNumber} from ${profile.shopName.ifBlank { "HisabPro" }}")
                putExtra(Intent.EXTRA_TEXT, generateInvoiceWhatsAppText(invoice, profile))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF (${invoice.invoiceNumber})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback if WhatsApp package is missing or chooser fails
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, generateInvoiceWhatsAppText(invoice, profile))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Share Invoice Summary"))
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not launch share: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateInvoiceWhatsAppText(invoice: Invoice, profileOverride: BusinessProfile? = null): String {
        val totalFormatted = String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)
        val dueFormatted = String.format(Locale.ENGLISH, "%.2f", invoice.dueAmount)

        val itemsSummary = invoice.items.joinToString("\n") {
            "• ${it.description} x ${it.quantity} ${it.unit} = ₹${String.format(Locale.ENGLISH, "%.2f", it.getTotal(invoice.gstMode))}"
        }

        val shopName = profileOverride?.shopName?.ifBlank { "HisabPro Enterprises" } ?: "HisabPro Enterprises"
        val upiInfo = if (!profileOverride?.upiId.isNullOrBlank() && invoice.dueAmount > 0) {
            "\n*Pay via UPI:* ${profileOverride?.upiId}"
        } else ""

        return """
📄 *${invoice.type.label.uppercase()}*
*From:* $shopName
*Invoice No:* ${invoice.invoiceNumber}
*Date:* ${dateFormat.format(Date(invoice.dateMillis))}
*Customer:* ${invoice.customerName}

*Items:*
$itemsSummary

*Grand Total:* ₹$totalFormatted
*Paid:* ₹${String.format(Locale.ENGLISH, "%.2f", invoice.paidAmount)}
*Balance Due:* ₹$dueFormatted$upiInfo

Thank you for your business!
_$shopName _
        """.trimIndent()
    }
}
