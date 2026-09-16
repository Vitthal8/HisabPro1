package com.hisabpro.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import java.util.Locale

object UpiPaymentHelper {

    /**
     * Constructs a NPCI standard UPI payment URI compliant with PhonePe, Google Pay, Paytm, BHIM.
     */
    fun buildUpiUri(
        upiId: String,
        payeeName: String,
        amount: Double,
        invoiceNumber: String,
        notes: String = ""
    ): String {
        val cleanUpi = upiId.trim()
        val noteText = notes.ifBlank { "Invoice $invoiceNumber" }.take(50)
        val formattedAmount = String.format(Locale.ENGLISH, "%.2f", amount)

        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", cleanUpi)
            .appendQueryParameter("pn", payeeName.trim().ifBlank { "Merchant" })
            .appendQueryParameter("am", formattedAmount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", noteText)
            .build()
            .toString()
    }

    /**
     * Generates a high-contrast QR Code Bitmap using ZXing.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = AndroidColor.BLACK,
        lightColor: Int = AndroidColor.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launches UPI Intent or opens chooser for UPI payment apps installed on device.
     */
    fun launchUpiPaymentIntent(context: Context, upiUriString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upiUriString)
            }
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI app found on device", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceUpiQrSheet(
    invoiceNumber: String,
    amount: Double,
    customerName: String,
    merchantName: String,
    merchantUpiId: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val fallbackUpi = merchantUpiId.ifBlank { "merchant@upi" }
    val upiUri = remember(fallbackUpi, amount, invoiceNumber) {
        UpiPaymentHelper.buildUpiUri(
            upiId = fallbackUpi,
            payeeName = merchantName,
            amount = amount,
            invoiceNumber = invoiceNumber
        )
    }

    val qrBitmap = remember(upiUri) {
        UpiPaymentHelper.generateQrBitmap(upiUri, sizePx = 480)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Surface(
                color = Emerald800,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Scan & Pay via UPI",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "PhonePe • GPay • Paytm • BHIM",
                                color = PureWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_upi_qr")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Display Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Amount to Pay for $invoiceNumber",
                    fontSize = 13.sp,
                    color = Slate700
                )
                Text(
                    text = "₹${String.format(Locale.ENGLISH, "%.2f", amount)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Emerald800
                )
                if (customerName.isNotBlank()) {
                    Text(
                        text = "Customer: $customerName",
                        fontSize = 12.sp,
                        color = Slate700
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .border(2.dp, Emerald700.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(18.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Dynamic UPI QR Code",
                            modifier = Modifier
                                .size(220.dp)
                                .testTag("image_upi_qr_code")
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(Slate100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unable to generate QR code",
                                color = Slate700,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // UPI ID Pill with Copy
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clickable {
                        clipboardManager.setText(AnnotatedString(fallbackUpi))
                        Toast.makeText(context, "UPI ID copied: $fallbackUpi", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 24.dp)
                    .testTag("btn_copy_upi_id")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "UPI ID: $fallbackUpi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate800
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy UPI ID",
                        modifier = Modifier.size(14.dp),
                        tint = Emerald700
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open Installed UPI Apps
                Button(
                    onClick = {
                        UpiPaymentHelper.launchUpiPaymentIntent(context, upiUri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_open_upi_app")
                ) {
                    Text(
                        text = "Pay using UPI App (GPay / PhonePe)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Confirm Payment Received
                if (onPaymentConfirmed != null) {
                    OutlinedButton(
                        onClick = {
                            onPaymentConfirmed()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_confirm_upi_received")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payment Received (Mark as Paid)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }
    }
}
