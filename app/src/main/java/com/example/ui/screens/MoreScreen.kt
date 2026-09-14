package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMode
import com.example.ui.AccountingViewModel
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.AppLanguage
import com.example.util.IndianAccountingUtils

@Composable
fun MoreScreen(
    viewModel: AccountingViewModel,
    onNavigateToBusinessSetup: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.business.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("more_screen"),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Profile Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepNavy),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToBusinessSetup() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Saffron),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (business?.name?.take(1) ?: "H").uppercase(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = business?.name?.ifBlank { "HisabPro Business" } ?: "HisabPro Business",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (business?.gstEnabled == true) "GST Enabled (${business?.gstin ?: "Registered"})" else "Non-GST Mode Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Tap to edit settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = Saffron
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Language Switcher Strip
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = DeepNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Language (भाषा)", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                FilterChip(
                                    selected = currentLang == lang.code,
                                    onClick = { viewModel.setLanguage(lang.code) },
                                    label = { Text(lang.displayName) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Menu Items
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        MoreMenuItem(
                            title = "Business Profile & GST Setup",
                            subtitle = "Name, Address, PAN, GSTIN & Non-GST Toggle",
                            icon = Icons.Default.Business,
                            onClick = onNavigateToBusinessSetup
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        MoreMenuItem(
                            title = "Add Expense (खर्च नोंदवा)",
                            subtitle = "Shop rent, electricity, transport, tea/snacks",
                            icon = Icons.Default.ShoppingBag,
                            onClick = { showExpenseDialog = true }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        MoreMenuItem(
                            title = "Subscription & Plans",
                            subtitle = "Free plan active • Upgrade to Pro / Premium",
                            icon = Icons.Default.Star,
                            onClick = { showSubscriptionDialog = true }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        MoreMenuItem(
                            title = "Data & Offline Backup",
                            subtitle = "100% Offline-First with local SQLite database",
                            icon = Icons.Default.CloudDone,
                            onClick = {
                                Toast.makeText(context, "Local Room database automatically synchronized!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Expense Dialog
    if (showExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { category, amount, notes, mode ->
                viewModel.addExpense(category, amount, notes, mode) {
                    Toast.makeText(context, "Expense of ${IndianAccountingUtils.formatCurrency(amount)} recorded", Toast.LENGTH_SHORT).show()
                    showExpenseDialog = false
                }
            }
        )
    }

    // Subscription Plans Dialog
    if (showSubscriptionDialog) {
        SubscriptionPlansDialog(
            onDismiss = { showSubscriptionDialog = false }
        )
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DeepNavy.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double, notes: String, mode: PaymentMode) -> Unit
) {
    var category by remember { mutableStateOf("Shop Rent / Electricity") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PaymentMode.CASH) }

    val categories = listOf("Shop Rent / Electricity", "Tea & Refreshments", "Staff Salary", "Transport / Courier", "Repairs & Maintenance", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense (खर्च नोंदवा)", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = category == "Shop Rent / Electricity",
                        onClick = { category = "Shop Rent / Electricity" },
                        label = { Text("Rent/Elec", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = category == "Tea & Refreshments",
                        onClick = { category = "Tea & Refreshments" },
                        label = { Text("Tea/Food", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = category == "Other",
                        onClick = { category = "Other" },
                        label = { Text("Other", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Description / Paid to") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(category, amt, notes, mode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SubscriptionPlansDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HisabPro Plans & Pricing", fontWeight = FontWeight.Bold, color = DeepNavy) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FREE PLAN", fontWeight = FontWeight.Bold, color = CreditGreen)
                            Text("Current Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CreditGreen)
                        }
                        Text("• Unlimited parties & items\n• 50 invoices/month\n• Full Non-GST & GST support\n• 100% Offline SQLite DB", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Saffron.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PRO PLAN", fontWeight = FontWeight.Bold, color = Saffron)
                            Text("₹99/month", fontWeight = FontWeight.Bold, color = DeepNavy)
                        }
                        Text("• Unlimited invoices & PDF export\n• Cloud sync & auto WhatsApp reminders\n• Remove ads & multi-staff login", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Google Play Billing upgrade flow ready!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Saffron)
            ) {
                Text("Upgrade to Pro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
