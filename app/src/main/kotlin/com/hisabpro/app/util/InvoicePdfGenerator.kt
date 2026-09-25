package com.hisabpro.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.repository.SettingsRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

/**
 * Professional Indian Invoice PDF Generator.
 *
 * Implements:
 * 1. Business logo (loaded from logoPath or rendered as executive business emblem)
 * 2. Business name
 * 3. Complete Address (Street, City, State, State code, Pincode)
 * 4. Contact Phone & Email
 * 5. GSTIN (prominently displayed when applicable; completely removed for Non-GST businesses)
 * 6. Invoice number (e.g. 2025-26/INV/001)
 * 7. Invoice date (Strictly DD/MM/YYYY format)
 * 8. Customer details (Name, Phone, Address, GSTIN when applicable)
 * 9. Item table (Structured rows with zebra striping and crisp borders)
 * 10. Quantity
 * 11. Unit (Pcs, Bags, Kg, Box, etc.)
 * 12. Rate (₹ per unit)
 * 13. Discount (Item discount + Invoice discount)
 * 14. Taxable amount
 * 15. CGST (Intra-state rate and amount)
 * 16. SGST (Intra-state rate and amount)
 * 17. IGST (Inter-state rate and amount)
 * 18. Grand total (Indian format ₹1,23,456.00 + Amount in Words in Indian numbering)
 * 19. Amount paid
 * 20. Balance due (with visual status badge)
 * 21. Payment mode (Cash / UPI / Bank Transfer / Cheque)
 * 22. Notes (Custom notes, terms & conditions, bank details & dynamic UPI QR code)
 *
 * Fully Offline - uses Android's native android.graphics.pdf.PdfDocument.
 * Handles errors gracefully, suitable for A4 printing and WhatsApp sharing.
 */
object InvoicePdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN = 36f // 0.5 inch (36 points) margin
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2) // 523 points

    // Colors adhering to HisabPro guidelines (Navy #1A237E, Saffron #FF6B00)
    private val COLOR_PRIMARY = 0xFF1A237E.toInt() // Deep Navy Blue
    private val COLOR_SECONDARY = 0xFFFF6B00.toInt() // Saffron Accent
    private val COLOR_DARK_TEXT = 0xFF0F172A.toInt() // Slate 900
    private val COLOR_MUTED_TEXT = 0xFF475569.toInt() // Slate 600
    private val COLOR_LIGHT_BORDER = 0xFFCBD5E1.toInt() // Slate 300
    private val COLOR_ROW_ALT = 0xFFF8FAFC.toInt() // Slate 50
    private val COLOR_HEADER_BG = 0xFFF1F5F9.toInt() // Slate 100
    private val COLOR_SUCCESS = 0xFF16A34A.toInt() // Green 600
    private val COLOR_DANGER = 0xFFDC2626.toInt() // Red 600

    sealed class PdfResult {
        data class Success(val file: File) : PdfResult()
        data class Error(val message: String, val cause: Throwable? = null) : PdfResult()
    }

    /**
     * Generates invoice PDF and returns the saved File.
     * Throws an exception on critical failure.
     */
    fun generatePdf(
        context: Context,
        invoice: Invoice,
        profileOverride: BusinessProfile? = null
    ): File {
        val result = generatePdfSafe(context, invoice, profileOverride)
        return when (result) {
            is PdfResult.Success -> result.file
            is PdfResult.Error -> throw IllegalStateException(result.message, result.cause)
        }
    }

    /**
     * Safely generates invoice PDF catching all exceptions and returning a sealed PdfResult.
     */
    fun generatePdfSafe(
        context: Context,
        invoice: Invoice,
        profileOverride: BusinessProfile? = null
    ): PdfResult {
        return try {
            val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val safeNumber = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(invoicesDir, "${safeNumber}.pdf")

            val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
            val logoBitmap = loadLogoBitmap(context, profile.logoPath)

            val document = PdfDocument()
            val isGst = (profile.isGstRegistered || invoice.type == InvoiceType.TAX_INVOICE) &&
                    invoice.type != InvoiceType.NON_GST_BILL &&
                    invoice.gstMode != GstMode.EXEMPT

            // Calculate pagination: max items per page
            val items = invoice.items
            val itemsPerPage = if (items.size <= 12) items.size else 10
            val totalPages = if (items.isEmpty()) 1 else ((items.size - 1) / itemsPerPage) + 1

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val startIndex = pageIndex * itemsPerPage
                val endIndex = kotlin.math.min(items.size, startIndex + itemsPerPage)
                val pageItems = if (items.isEmpty()) emptyList() else items.subList(startIndex, endIndex)
                val isLastPage = (pageIndex == totalPages - 1)

                drawInvoicePage(
                    canvas = canvas,
                    invoice = invoice,
                    profile = profile,
                    pageItems = pageItems,
                    pageStartIndex = startIndex,
                    isGst = isGst,
                    currentPage = pageIndex + 1,
                    totalPages = totalPages,
                    isLastPage = isLastPage,
                    logoBitmap = logoBitmap
                )

                document.finishPage(page)
            }

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            PdfResult.Success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            PdfResult.Error("Failed to generate PDF: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    private fun drawInvoicePage(
        canvas: Canvas,
        invoice: Invoice,
        profile: BusinessProfile,
        pageItems: List<InvoiceItem>,
        pageStartIndex: Int,
        isGst: Boolean,
        currentPage: Int,
        totalPages: Int,
        isLastPage: Boolean,
        logoBitmap: Bitmap?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Page Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // 2. Top Color Bars (Navy primary + Saffron accent stripe)
        paint.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, paint)
        paint.color = COLOR_SECONDARY
        canvas.drawRect(0f, 8f, PAGE_WIDTH.toFloat(), 11f, paint)

        var y = MARGIN + 12f

        // 3. Business Header & Title
        val logoBoxSize = 48f
        val logoX = MARGIN
        val textStartX = if (logoBitmap != null || profile.logoPath.isNotBlank()) MARGIN + logoBoxSize + 12f else MARGIN + logoBoxSize + 12f

        // Draw Business Logo or Fallback Emblem
        if (logoBitmap != null) {
            val destRect = RectF(logoX, y - 2f, logoX + logoBoxSize, y - 2f + logoBoxSize)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
        } else {
            // Executive Monogram Emblem
            val emblemRect = RectF(logoX, y - 2f, logoX + logoBoxSize, y - 2f + logoBoxSize)
            paint.color = COLOR_PRIMARY
            canvas.drawRoundRect(emblemRect, 8f, 8f, paint)

            paint.color = COLOR_SECONDARY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(emblemRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val initials = getBusinessInitials(profile.shopName)
            paint.color = Color.WHITE
            paint.textSize = 17f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val initW = paint.measureText(initials)
            canvas.drawText(initials, logoX + (logoBoxSize - initW) / 2f, y + 28f, paint)
        }

        // Business Name
        paint.color = COLOR_DARK_TEXT
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val businessName = profile.shopName.ifBlank { "HISABPRO ENTERPRISES" }
        canvas.drawText(businessName.uppercase(), textStartX, y + 10f, paint)

        // Invoice Title on Right
        val titleText = when {
            invoice.type == InvoiceType.PROFORMA -> "PROFORMA INVOICE"
            isGst -> "TAX INVOICE"
            else -> "BILL OF SUPPLY"
        }
        paint.textSize = 15f
        paint.color = COLOR_PRIMARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, PAGE_WIDTH - MARGIN - titleWidth, y + 10f, paint)

        // Subtitle on Right
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_MUTED_TEXT
        val copyText = "ORIGINAL FOR RECIPIENT"
        val copyW = paint.measureText(copyText)
        canvas.drawText(copyText, PAGE_WIDTH - MARGIN - copyW, y + 21f, paint)

        // Business Contact & Address
        y += 24f
        paint.textSize = 8.5f
        paint.color = COLOR_MUTED_TEXT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val fullAddress = listOf(profile.address, profile.city, "${profile.state} - ${profile.pincode}".trim())
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Main Commercial Market, Maharashtra" }
        canvas.drawText(fullAddress.take(65), textStartX, y, paint)

        val contactLine = listOfNotNull(
            if (profile.phone.isNotBlank()) "Ph: ${profile.phone}" else null,
            if (profile.email.isNotBlank()) "Email: ${profile.email}" else null
        ).joinToString("  |  ").ifBlank { "Contact: Support & Billing" }
        canvas.drawText(contactLine.take(65), textStartX, y + 11f, paint)

        // GSTIN Line (ONLY when GST is enabled!)
        if (isGst) {
            val gstinText = "GSTIN: ${profile.gstin.ifBlank { "UNREGISTERED" }.uppercase()}  |  State: ${profile.state} (${profile.stateCode.ifBlank { "27" }})"
            paint.color = COLOR_PRIMARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(gstinText, textStartX, y + 22f, paint)
        } else if (profile.pan.isNotBlank()) {
            paint.color = COLOR_MUTED_TEXT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("PAN: ${profile.pan.uppercase()}", textStartX, y + 22f, paint)
        }

        y += 34f

        // 4. Divider Line
        paint.color = COLOR_LIGHT_BORDER
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        y += 12f

        // 5. Two Cards: "BILLED TO" (Left) and "INVOICE DETAILS" (Right)
        val colGap = 12f
        val colWidth = (USABLE_WIDTH - colGap) / 2f
        val leftX = MARGIN
        val rightX = MARGIN + colWidth + colGap
        val boxHeight = 74f

        // Backgrounds
        paint.color = COLOR_ROW_ALT
        canvas.drawRoundRect(RectF(leftX, y, leftX + colWidth, y + boxHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(rightX, y, rightX + colWidth, y + boxHeight), 6f, 6f, paint)

        // Borders
        paint.color = COLOR_LIGHT_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(leftX, y, leftX + colWidth, y + boxHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(rightX, y, rightX + colWidth, y + boxHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Left Card: Customer Details
        paint.color = COLOR_PRIMARY
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO / CUSTOMER", leftX + 10f, y + 14f, paint)

        paint.color = COLOR_DARK_TEXT
        paint.textSize = 10f
        val custName = invoice.customerName.ifBlank { "Walk-in Cash Customer" }
        canvas.drawText(custName, leftX + 10f, y + 29f, paint)

        paint.color = COLOR_MUTED_TEXT
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val phoneStr = if (invoice.customerPhone.isNotBlank()) "Ph: ${invoice.customerPhone}" else "Phone: Not Provided"
        canvas.drawText(phoneStr, leftX + 10f, y + 43f, paint)

        val custAddrOrGstin = when {
            isGst && invoice.customerGstin.isNotBlank() -> "GSTIN: ${invoice.customerGstin.uppercase()}"
            invoice.customerAddress.isNotBlank() -> invoice.customerAddress.take(45)
            else -> if (isGst) "GSTIN: Unregistered Consumer" else "Local Customer"
        }
        canvas.drawText(custAddrOrGstin, leftX + 10f, y + 57f, paint)

        // Right Card: Invoice Details
        paint.color = COLOR_PRIMARY
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE & PAYMENT DETAILS", rightX + 10f, y + 14f, paint)

        paint.color = COLOR_DARK_TEXT
        paint.textSize = 9.5f
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", rightX + 10f, y + 29f, paint)

        paint.color = COLOR_MUTED_TEXT
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        // Date strictly DD/MM/YYYY
        val formattedDate = IndianAccountingFormat.formatIndianDate(invoice.dateMillis)
        canvas.drawText("Date: $formattedDate", rightX + 10f, y + 43f, paint)

        val payModeLine = "Payment Mode: ${invoice.paymentMode.ifBlank { "Cash" }}"
        val gstModeLine = if (isGst) "  |  ${invoice.gstMode.label.take(16)}" else ""
        canvas.drawText("$payModeLine$gstModeLine", rightX + 10f, y + 57f, paint)

        y += boxHeight + 14f

        // 6. Item Table
        val thHeight = 22f
        paint.color = COLOR_HEADER_BG
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + thHeight, paint)

        paint.color = COLOR_PRIMARY
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        if (isGst) {
            // GST Table Columns (10 columns, precise point layout)
            val c1 = MARGIN + 6f        // # (18)
            val c2 = MARGIN + 26f       // Description (120)
            val c3 = MARGIN + 148f      // HSN (42)
            val c4 = MARGIN + 192f      // Qty & Unit (46)
            val c5 = MARGIN + 242f      // Rate (48)
            val c6 = MARGIN + 294f      // Disc (35)
            val c7 = MARGIN + 332f      // Taxable (55)
            val c8 = MARGIN + 390f      // CGST (45)
            val c9 = MARGIN + 438f      // SGST / IGST (45)
            val c10 = PAGE_WIDTH - MARGIN - 6f // Total (Right aligned)

            canvas.drawText("#", c1, y + 15f, paint)
            canvas.drawText("Item / Description", c2, y + 15f, paint)
            canvas.drawText("HSN", c3, y + 15f, paint)
            canvas.drawText("Qty/Unit", c4, y + 15f, paint)
            canvas.drawText("Rate(₹)", c5, y + 15f, paint)
            canvas.drawText("Disc(₹)", c6, y + 15f, paint)
            canvas.drawText("Taxable", c7, y + 15f, paint)

            if (invoice.gstMode == GstMode.INTER_STATE) {
                canvas.drawText("IGST", c8, y + 15f, paint)
            } else {
                canvas.drawText("CGST", c8, y + 15f, paint)
                canvas.drawText("SGST", c9, y + 15f, paint)
            }
            val thTot = "Total (₹)"
            val thTotW = paint.measureText(thTot)
            canvas.drawText(thTot, c10 - thTotW, y + 15f, paint)

            y += thHeight

            // Table Rows
            val rowHeight = 22f
            pageItems.forEachIndexed { i, item ->
                val absoluteIndex = pageStartIndex + i
                if (i % 2 == 1) {
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, paint)
                }

                // Row bottom separator
                paint.color = COLOR_LIGHT_BORDER
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, paint)

                paint.color = COLOR_MUTED_TEXT
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${absoluteIndex + 1}", c1, y + 15f, paint)

                paint.color = COLOR_DARK_TEXT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val desc = item.description.take(24)
                canvas.drawText(desc, c2, y + 15f, paint)

                paint.color = COLOR_MUTED_TEXT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(if (item.hsnCode.isNotBlank()) item.hsnCode else "-", c3, y + 15f, paint)

                val qtyUnit = "${item.quantity.toIntIfWhole()} ${item.unit.take(4)}"
                canvas.drawText(qtyUnit, c4, y + 15f, paint)

                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.unitPrice), c5, y + 15f, paint)
                canvas.drawText(if (item.discount > 0) String.format(Locale.ENGLISH, "%.1f", item.discount) else "-", c6, y + 15f, paint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.taxableAmount), c7, y + 15f, paint)

                if (invoice.gstMode == GstMode.INTER_STATE) {
                    val igstAmt = item.getIgst(invoice.gstMode)
                    canvas.drawText("${item.gstRate.toIntIfWhole()}% (₹${String.format(Locale.ENGLISH, "%.0f", igstAmt)})", c8, y + 15f, paint)
                } else {
                    val cgstAmt = item.getCgst(invoice.gstMode)
                    val sgstAmt = item.getSgst(invoice.gstMode)
                    canvas.drawText("${(item.gstRate / 2).toIntIfWhole()}%", c8, y + 15f, paint)
                    canvas.drawText("${(item.gstRate / 2).toIntIfWhole()}%", c9, y + 15f, paint)
                }

                val rowTotal = String.format(Locale.ENGLISH, "%.2f", item.getTotal(invoice.gstMode))
                paint.color = COLOR_DARK_TEXT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val rtW = paint.measureText(rowTotal)
                canvas.drawText(rowTotal, c10 - rtW, y + 15f, paint)

                y += rowHeight
            }

        } else {
            // NON-GST Table Columns (Strictly No GST/HSN columns!)
            // Requirement 4: Item | Qty | Rate | Amount (+ #, Unit, Disc)
            val c1 = MARGIN + 8f        // # (25)
            val c2 = MARGIN + 36f       // Description (210)
            val c3 = MARGIN + 250f      // Qty (45)
            val c4 = MARGIN + 298f      // Unit (40)
            val c5 = MARGIN + 345f      // Rate (60)
            val c6 = MARGIN + 412f      // Disc (45)
            val c7 = PAGE_WIDTH - MARGIN - 8f // Amount (Right aligned)

            canvas.drawText("#", c1, y + 15f, paint)
            canvas.drawText("Item / Description", c2, y + 15f, paint)
            canvas.drawText("Qty", c3, y + 15f, paint)
            canvas.drawText("Unit", c4, y + 15f, paint)
            canvas.drawText("Rate (₹)", c5, y + 15f, paint)
            canvas.drawText("Disc (₹)", c6, y + 15f, paint)
            val thAmt = "Amount (₹)"
            val thAmtW = paint.measureText(thAmt)
            canvas.drawText(thAmt, c7 - thAmtW, y + 15f, paint)

            y += thHeight

            // Non-GST Table Rows
            val rowHeight = 22f
            pageItems.forEachIndexed { i, item ->
                val absoluteIndex = pageStartIndex + i
                if (i % 2 == 1) {
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, paint)
                }

                paint.color = COLOR_LIGHT_BORDER
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, paint)

                paint.color = COLOR_MUTED_TEXT
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${absoluteIndex + 1}", c1, y + 15f, paint)

                paint.color = COLOR_DARK_TEXT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val desc = item.description.take(38)
                canvas.drawText(desc, c2, y + 15f, paint)

                paint.color = COLOR_MUTED_TEXT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(item.quantity.toIntIfWhole(), c3, y + 15f, paint)
                canvas.drawText(item.unit, c4, y + 15f, paint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", item.unitPrice), c5, y + 15f, paint)
                canvas.drawText(if (item.discount > 0) String.format(Locale.ENGLISH, "%.2f", item.discount) else "-", c6, y + 15f, paint)

                val lineAmt = String.format(Locale.ENGLISH, "%.2f", item.taxableAmount)
                paint.color = COLOR_DARK_TEXT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val amtW = paint.measureText(lineAmt)
                canvas.drawText(lineAmt, c7 - amtW, y + 15f, paint)

                y += rowHeight
            }
        }

        // Table Bottom Border
        paint.color = COLOR_PRIMARY
        paint.strokeWidth = 1.2f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        // 7. Bottom Section (Rendered on the last page)
        if (isLastPage) {
            y += 12f
            val summaryWidth = 210f
            val summaryLeft = PAGE_WIDTH - MARGIN - summaryWidth
            val notesWidth = summaryLeft - MARGIN - 14f

            // --- Left Column: Amount in Words, Bank Details, Notes & UPI QR ---
            val leftBoxHeight = 150f
            paint.color = COLOR_ROW_ALT
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + notesWidth, y + leftBoxHeight), 6f, 6f, paint)
            paint.color = COLOR_LIGHT_BORDER
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + notesWidth, y + leftBoxHeight), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            var notesY = y + 14f

            // Amount in Words
            paint.color = COLOR_PRIMARY
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAL AMOUNT IN WORDS:", MARGIN + 8f, notesY, paint)

            notesY += 12f
            paint.color = COLOR_DARK_TEXT
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val words = IndianAccountingFormat.numberToWordsIndian(invoice.grandTotal)
            canvas.drawText(words.take(54), MARGIN + 8f, notesY, paint)
            if (words.length > 54) {
                notesY += 10f
                canvas.drawText(words.substring(54).take(54), MARGIN + 8f, notesY, paint)
            }

            notesY += 14f
            paint.color = COLOR_PRIMARY
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BANK ACCOUNT & UPI DETAILS:", MARGIN + 8f, notesY, paint)

            notesY += 11f
            paint.color = COLOR_MUTED_TEXT
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val bankStr = if (profile.bankName.isNotBlank() && profile.accountNumber.isNotBlank()) {
                "Bank: ${profile.bankName}  |  A/C: ${profile.accountNumber}"
            } else "Mode: Cash / UPI Transfer"
            canvas.drawText(bankStr.take(45), MARGIN + 8f, notesY, paint)

            notesY += 10f
            val ifscStr = listOfNotNull(
                if (profile.ifscCode.isNotBlank()) "IFSC: ${profile.ifscCode}" else null,
                if (profile.upiId.isNotBlank()) "UPI ID: ${profile.upiId}" else null
            ).joinToString("  |  ")
            if (ifscStr.isNotBlank()) {
                canvas.drawText(ifscStr.take(45), MARGIN + 8f, notesY, paint)
                notesY += 10f
            }

            // Notes / Terms
            val noteStr = if (invoice.notes.isNotBlank()) "Note: ${invoice.notes}" else "Terms: ${profile.termsAndConditions.lines().firstOrNull() ?: "Goods once sold cannot be returned."}"
            canvas.drawText(noteStr.take(48), MARGIN + 8f, notesY, paint)

            // Dynamic UPI QR Code (if configured & due balance > 0)
            if (profile.showUpiQrOnInvoice && profile.upiId.isNotBlank() && invoice.dueAmount > 0) {
                try {
                    val upiUri = UpiPaymentHelper.buildUpiUri(
                        upiId = profile.upiId,
                        payeeName = profile.shopName.ifBlank { profile.ownerName.ifBlank { "Merchant" } },
                        amount = invoice.dueAmount,
                        invoiceNumber = invoice.invoiceNumber,
                        notes = "Bill ${invoice.invoiceNumber}"
                    )
                    val qrBitmap = UpiPaymentHelper.generateQrBitmap(upiUri, 150)
                    if (qrBitmap != null) {
                        val qrSize = 50f
                        val qrX = MARGIN + notesWidth - qrSize - 8f
                        val qrY = y + 8f
                        canvas.drawBitmap(qrBitmap, null, RectF(qrX, qrY, qrX + qrSize, qrY + qrSize), paint)

                        paint.color = COLOR_PRIMARY
                        paint.textSize = 6f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        val qrLabel = "SCAN & PAY"
                        val qlW = paint.measureText(qrLabel)
                        canvas.drawText(qrLabel, qrX + (qrSize - qlW) / 2f, qrY + qrSize + 7f, paint)
                    }
                } catch (e: Exception) {
                    // Ignore QR drawing failure gracefully
                }
            }

            // --- Right Column: Financial Summary Table ---
            var sumY = y + 10f
            fun drawSumRow(label: String, value: String, isBold: Boolean = false, color: Int = COLOR_DARK_TEXT) {
                paint.textSize = if (isBold) 9.5f else 8f
                paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = color

                canvas.drawText(label, summaryLeft, sumY, paint)
                val valW = paint.measureText(value)
                canvas.drawText(value, PAGE_WIDTH - MARGIN - valW, sumY, paint)
                sumY += 14f
            }

            // Subtotal
            val subLabel = if (isGst) "Taxable Subtotal:" else "Subtotal Amount:"
            drawSumRow(subLabel, IndianAccountingFormat.formatIndianCurrency(invoice.subtotal))

            // Discount
            if (invoice.discountAmount > 0) {
                drawSumRow("Discount Amount:", "-${IndianAccountingFormat.formatIndianCurrency(invoice.discountAmount)}", color = COLOR_DANGER)
            }

            // GST Breakdowns (Strictly ONLY when isGst == true)
            if (isGst) {
                if (invoice.gstMode == GstMode.INTRA_STATE) {
                    drawSumRow("CGST Total:", "+${IndianAccountingFormat.formatIndianCurrency(invoice.cgstTotal)}")
                    drawSumRow("SGST Total:", "+${IndianAccountingFormat.formatIndianCurrency(invoice.sgstTotal)}")
                } else if (invoice.gstMode == GstMode.INTER_STATE) {
                    drawSumRow("IGST Total:", "+${IndianAccountingFormat.formatIndianCurrency(invoice.igstTotal)}")
                }
            }

            // Separator before Grand Total
            paint.color = COLOR_PRIMARY
            paint.strokeWidth = 1f
            canvas.drawLine(summaryLeft, sumY, PAGE_WIDTH - MARGIN, sumY, paint)
            sumY += 12f

            // Grand Total
            drawSumRow(
                label = "GRAND TOTAL:",
                value = IndianAccountingFormat.formatIndianCurrency(invoice.grandTotal),
                isBold = true,
                color = COLOR_PRIMARY
            )

            // Amount Paid
            drawSumRow("Amount Paid (${invoice.paymentMode}):", IndianAccountingFormat.formatIndianCurrency(invoice.paidAmount))

            // Balance Due
            val dueColor = if (invoice.dueAmount > 0.01) COLOR_DANGER else COLOR_SUCCESS
            val dueLabel = if (invoice.dueAmount > 0.01) "Balance Due:" else "Payment Status:"
            val dueValue = if (invoice.dueAmount > 0.01) IndianAccountingFormat.formatIndianCurrency(invoice.dueAmount) else "PAID IN FULL"
            drawSumRow(dueLabel, dueValue, isBold = true, color = dueColor)

            // Authorized Signatory
            val sigY = PAGE_HEIGHT - MARGIN - 36f
            paint.color = COLOR_LIGHT_BORDER
            paint.strokeWidth = 0.8f
            canvas.drawLine(PAGE_WIDTH - MARGIN - 140f, sigY, PAGE_WIDTH - MARGIN, sigY, paint)

            paint.textSize = 8f
            paint.color = COLOR_DARK_TEXT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val forText = "For ${profile.shopName.ifBlank { "HisabPro Enterprises" }}"
            val forW = paint.measureText(forText)
            canvas.drawText(forText, PAGE_WIDTH - MARGIN - forW, sigY + 12f, paint)

            paint.color = COLOR_MUTED_TEXT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val authText = if (profile.ownerName.isNotBlank()) "(${profile.ownerName}) Authorized Signatory" else "Authorized Signatory"
            val authW = paint.measureText(authText)
            canvas.drawText(authText, PAGE_WIDTH - MARGIN - authW, sigY + 22f, paint)
        } else {
            // Multi-page notice
            paint.color = COLOR_MUTED_TEXT
            paint.textSize = 8.5f
            val contText = "Continued on next page..."
            canvas.drawText(contText, PAGE_WIDTH - MARGIN - paint.measureText(contText), y + 25f, paint)
        }

        // 8. Footer (Page Numbering & Legal)
        paint.textSize = 7.5f
        paint.color = COLOR_MUTED_TEXT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated electronically via HisabPro • Simplified Accounting", MARGIN, PAGE_HEIGHT - MARGIN + 8f, paint)

        val pageStr = "Page $currentPage of $totalPages"
        val pW = paint.measureText(pageStr)
        canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - pW, PAGE_HEIGHT - MARGIN + 8f, paint)
    }

    private fun Double.toIntIfWhole(): String {
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", this)
    }

    private fun getBusinessInitials(name: String): String {
        val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
            words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
            words.size == 1 -> words[0].take(1).uppercase()
            else -> "HP"
        }
    }

    private fun loadLogoBitmap(context: Context, logoPath: String): Bitmap? {
        if (logoPath.isBlank()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            if (logoPath.startsWith("content://") || logoPath.startsWith("file://")) {
                val uri = Uri.parse(logoPath)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                options.inSampleSize = calculateInSampleSize(options, 256, 256)
                options.inJustDecodeBounds = false
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } else {
                val file = File(logoPath)
                if (file.exists() && file.canRead()) {
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, 256, 256)
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeFile(file.absolutePath, options)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int = 256, reqHeight: Int = 256): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Shares invoice PDF via Android Share Sheet or directly targets WhatsApp.
     */
    fun sharePdf(
        context: Context,
        invoice: Invoice,
        targetWhatsApp: Boolean = false,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        try {
            val fileResult = generatePdfSafe(context, invoice, profile)
            if (fileResult is PdfResult.Error) {
                Toast.makeText(context, "Could not generate PDF: ${fileResult.message}", Toast.LENGTH_LONG).show()
                return
            }

            val file = (fileResult as PdfResult.Success).file
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

            if (targetWhatsApp) {
                try {
                    val chooser = Intent.createChooser(shareIntent, "Share Invoice via WhatsApp")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    return
                } catch (e: ActivityNotFoundException) {
                    // WhatsApp not installed, fallback to standard chooser
                    shareIntent.setPackage(null)
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share Invoice (${invoice.invoiceNumber})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Native Android Print Integration.
     * Direct print to WiFi/Bluetooth printers or Save to PDF via Android Print Framework.
     */
    fun printPdf(
        context: Context,
        invoice: Invoice,
        profileOverride: BusinessProfile? = null
    ) {
        val profile = profileOverride ?: SettingsRepository.getInstance(context).profile.value
        try {
            val fileResult = generatePdfSafe(context, invoice, profile)
            if (fileResult is PdfResult.Error) {
                Toast.makeText(context, "Could not generate PDF for printing: ${fileResult.message}", Toast.LENGTH_LONG).show()
                return
            }

            val file = (fileResult as PdfResult.Success).file
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder("${invoice.invoiceNumber}.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            FileInputStream(file).use { input ->
                                FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.localizedMessage)
                        }
                    }
                }

                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()

                printManager.print("Invoice_${invoice.invoiceNumber}", printAdapter, printAttributes)
            } else {
                openPdfViewer(context, file)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the generated PDF in an external viewer.
     */
    fun openPdfViewer(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "com.hisabpro.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Invoice PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a formatted WhatsApp message with Indian conventions.
     */
    fun generateInvoiceWhatsAppText(invoice: Invoice, profileOverride: BusinessProfile? = null): String {
        val totalFormatted = IndianAccountingFormat.formatIndianCurrency(invoice.grandTotal)
        val dueFormatted = IndianAccountingFormat.formatIndianCurrency(invoice.dueAmount)
        val dateFormatted = IndianAccountingFormat.formatIndianDate(invoice.dateMillis)

        val itemsSummary = invoice.items.joinToString("\n") {
            "• ${it.description} x ${it.quantity.toIntIfWhole()} ${it.unit} = ${IndianAccountingFormat.formatIndianCurrency(it.getTotal(invoice.gstMode))}"
        }

        val shopName = profileOverride?.shopName?.ifBlank { "HisabPro Enterprises" } ?: "HisabPro Enterprises"
        val upiInfo = if (!profileOverride?.upiId.isNullOrBlank() && invoice.dueAmount > 0.01) {
            "\n💳 *Pay via UPI:* ${profileOverride?.upiId}\n_Scan the QR on the invoice or pay via PhonePe / GPay / Paytm._"
        } else ""

        val statusBadge = if (invoice.dueAmount <= 0.01) "✅ *PAID IN FULL*" else "⚠️ *BALANCE DUE:* $dueFormatted"

        return """
📄 *${if (invoice.type == InvoiceType.TAX_INVOICE) "TAX INVOICE" else "BILL OF SUPPLY"}*
*From:* $shopName
*Invoice No:* ${invoice.invoiceNumber}
*Date:* $dateFormatted
*Customer:* ${invoice.customerName}

*Items:*
$itemsSummary

*Total Amount:* $totalFormatted
*Amount Paid:* ${IndianAccountingFormat.formatIndianCurrency(invoice.paidAmount)}
$statusBadge$upiInfo

Thank you for your business!
_$shopName _
        """.trimIndent()
    }
}
