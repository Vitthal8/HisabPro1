package com.hisabpro.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.ui.HisabViewModel

object ShareHelper {

    fun shareBalanceStatement(
        context: Context,
        party: Party,
        netBalance: Double,
        businessName: String = "HisabPro Store"
    ) {
        val dueAmountFormatted = HisabViewModel.formatAmount(kotlin.math.abs(netBalance))
        val message = when {
            netBalance > 0.009 -> {
                if (party.type == PartyType.CUSTOMER) {
                    "Namaste ${party.name},\nYour outstanding pending balance with $businessName is ₹$dueAmountFormatted.\nKindly clear the payment at your earliest convenience.\n\nThank you for your business!"
                } else {
                    "Namaste ${party.name},\nYour account with $businessName has an advance credit balance of ₹$dueAmountFormatted.\n\nThank you!"
                }
            }
            netBalance < -0.009 -> {
                if (party.type == PartyType.SUPPLIER) {
                    "Namaste ${party.name},\nOur outstanding balance payable to you from $businessName is ₹$dueAmountFormatted. Payment is scheduled shortly.\n\nThank you!"
                } else {
                    "Namaste ${party.name},\nYour account with $businessName has an advance balance of ₹$dueAmountFormatted.\n\nThank you!"
                }
            }
            else -> {
                "Namaste ${party.name},\nYour account with $businessName is completely settled with ₹0.00 balance.\n\nThank you for doing business with us!"
            }
        }

        val cleanPhone = party.phone.replace(Regex("[^0-9]"), "")
        val uri = if (cleanPhone.isNotBlank()) {
            val phoneWithCountry = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
            Uri.parse("https://api.whatsapp.com/send?phone=$phoneWithCountry&text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }

        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(waIntent)
        } catch (e: Exception) {
            // Graceful fallback to Android Share Sheet
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "Hisab Balance Statement - ${party.name}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(shareIntent, "Share Hisab Balance via")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(chooser)
            } catch (_: Exception) {}
        }
    }
}
