package com.hisabpro.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.DeepNavyLight
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.KhataGold
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronLight
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.util.IndianAccountingFormat

@Composable
fun MoreScreen(
    profile: BusinessProfile,
    onOpenBusinessSetup: () -> Unit,
    onOpenItems: () -> Unit,
    onOpenCashbook: () -> Unit,
    onUpdateProfile: (BusinessProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPricingModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Business Profile Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepNavyBlue, Color(0xFF0D1244))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SaffronOrange,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = profile.shopName.take(1).uppercase(),
                                color = PureWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.shopName,
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile.city}, ${profile.state} • ${profile.phone}",
                            color = PureWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (profile.isGstRegistered) Emerald700 else SaffronOrange
                            ) {
                                Text(
                                    text = if (profile.isGstRegistered) "GST Registered" else "Non-GST Shop",
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PureWhite.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "FY ${IndianAccountingFormat.getFinancialYear()}",
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenBusinessSetup,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("edit_business_profile_btn")
                    ) {
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        }

        // Pro Upgrade Monetization Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { showPricingModal = true }
                    .testTag("pro_upgrade_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SaffronLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SaffronOrange,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "HisabPro Pro & Premium Plans",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate800
                            )
                            Text(
                                text = "₹99/mo • Custom Logo, Cloud Sync & GSTR-1",
                                fontSize = 12.sp,
                                color = Slate700
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SaffronOrange
                    ) {
                        Text(
                            text = "Upgrade",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section: Core Management
        item {
            SectionHeader(title = "Accounting & Inventory Masters")
            SettingsItemRow(
                icon = Icons.Default.Store,
                title = "Business Setup & GST Toggle",
                subtitle = if (profile.isGstRegistered) "GST Mode Active (CGST/SGST/IGST & HSN)" else "Non-GST Mode (Clean, Simple Invoices)",
                tag = "more_item_business_setup",
                onClick = onOpenBusinessSetup
            )
            SettingsItemRow(
                icon = Icons.Default.Inventory2,
                title = "Items & Stock Management",
                subtitle = "Product master, reorder levels, purchase/sale pricing",
                tag = "more_item_inventory",
                onClick = onOpenItems
            )
            SettingsItemRow(
                icon = Icons.Default.MenuBook,
                title = "Day Book (Roznamcha) & Cash Book",
                subtitle = "Daily journal, Cash in Hand vs Bank Book ledger",
                tag = "more_item_cashbook",
                onClick = onOpenCashbook
            )
        }

        // Section: Language & Localization
        item {
            SectionHeader(title = "Language & Regional")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = DeepNavyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("App Language", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("English • हिंदी • मराठी", fontSize = 12.sp, color = Slate700)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("en" to "English", "hi" to "हिंदी", "mr" to "मराठी").forEach { (code, label) ->
                            val isSelected = profile.appLanguage == code
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) SaffronOrange else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateProfile(profile.copy(appLanguage = code))
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PureWhite else Slate800,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Hardware & Printing
        item {
            SectionHeader(title = "Hardware & POS Invoicing")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text("58mm Thermal POS Printer Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Format bills as compact ESC/POS slips for Bluetooth printers", fontSize = 11.sp, color = Slate700)
                            }
                        }
                        Switch(
                            checked = profile.isThermalPrinterMode,
                            onCheckedChange = {
                                onUpdateProfile(profile.copy(isThermalPrinterMode = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = Emerald700)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = DeepNavyBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text("UPI Dynamic QR on Invoices", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Print PhonePe/GPay QR with exact bill amount for instant payment", fontSize = 11.sp, color = Slate700)
                            }
                        }
                        Switch(
                            checked = profile.showUpiQrOnInvoice,
                            onCheckedChange = {
                                onUpdateProfile(profile.copy(showUpiQrOnInvoice = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = DeepNavyBlue)
                        )
                    }
                }
            }
        }

        // Section: Cloud & Local Storage
        item {
            SectionHeader(title = "Data & Cloud Architecture")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Offline-First Room (SQLite) Database",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "100% of your data is stored securely on your phone. Ready for Supabase PostgreSQL cloud sync.",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }
        }

        // Indian Accounting Standards Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Indian Accounting Standards Compliant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Financial Year: April 1 to March 31\n" +
                                "• Indian numbering system (Lakhs / Crores)\n" +
                                "• Double-entry Dr/Cr party ledger & Roznamcha Daybook\n" +
                                "• Automatic GST / Non-GST billing mode segregation\n" +
                                "• Invoice numbering prefix: ${IndianAccountingFormat.getFinancialYear()}/INV/001",
                        fontSize = 11.sp,
                        color = Slate700,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Pricing & Plans Modal
    if (showPricingModal) {
        PricingPlansSheet(onDismiss = { showPricingModal = false })
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Slate700,
        modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = DeepNavyLight,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = DeepNavyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = subtitle, fontSize = 11.sp, color = Slate700)
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Slate700,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PricingPlansSheet(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HisabPro Plans",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyBlue
                        )
                        Surface(
                            shape = CircleShape,
                            color = Slate200,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(6.dp)
                        ) {
                            Text("✕", modifier = Modifier.padding(horizontal = 6.dp), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Free Tier
                    PlanCard(
                        name = "Free Tier",
                        price = "₹0 / Lifetime",
                        features = listOf("Unlimited Parties & Items", "Up to 50 invoices/month", "1 Business Profile", "Local Backup"),
                        isCurrent = true,
                        accentColor = Slate700
                    )

                    // Pro Tier
                    PlanCard(
                        name = "Pro Tier",
                        price = "₹99 / month (or ₹799/yr)",
                        features = listOf("Unlimited Invoices", "Custom Logo on PDF", "WhatsApp Share & Excel Export", "Ad-Free Experience"),
                        isCurrent = false,
                        accentColor = SaffronOrange
                    )

                    // Premium Tier
                    PlanCard(
                        name = "Premium Tier",
                        price = "₹199 / month (or ₹1499/yr)",
                        features = listOf("Up to 5 Business Profiles", "Supabase Cloud Sync & Backup", "GSTR-1 & GSTR-3B Tax Filing Exports", "Priority WhatsApp Support"),
                        isCurrent = false,
                        accentColor = DeepNavyBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    name: String,
    price: String,
    features: List<String>,
    isCurrent: Boolean,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 15.sp)
                Text(text = price, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Slate800)
            }
            Spacer(modifier = Modifier.height(4.dp))
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(text = feature, fontSize = 11.sp, color = Slate700)
                }
            }
        }
    }
}
