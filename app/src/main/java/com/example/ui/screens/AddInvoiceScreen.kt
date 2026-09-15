package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceType
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.ui.AccountingViewModel
import com.example.ui.components.InvoiceBillPreviewDialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedLight
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils

data class LineItemForm(
    var itemId: Long = 0L,
    var name: String = "",
    var qty: String = "1",
    var unit: String = "PCS",
    var rate: String = "0",
    var hsnCode: String = "",
    var gstRate: String = "18" // e.g. 0, 5, 12, 18
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvoiceScreen(
    viewModel: AccountingViewModel,
    initialInvoiceType: InvoiceType = InvoiceType.SALE,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.business.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val availableItems by viewModel.items.collectAsState()

    var invoiceType by remember { mutableStateOf(initialInvoiceType) }

    var invoiceNo by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        invoiceNo = viewModel.getNextInvoiceNumber()
    }

    // Default GST mode from business setup
    var isGstMode by remember(business) { mutableStateOf(business?.gstEnabled == true) }

    // Customer selection
    var selectedParty by remember { mutableStateOf<Party?>(null) }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    // Line items list
    val lineItems = remember {
        mutableStateListOf(
            LineItemForm(name = "Basmati Rice 10kg", qty = "1", unit = "BAG", rate = "850", gstRate = "0")
        )
    }

    // Payment info
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var paidAmountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Preview after saving
    var savedInvoiceForPreview by remember { mutableStateOf<Invoice?>(null) }
    var savedItemsForPreview by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    // Calculate totals
    val subtotal = lineItems.sumOf { item ->
        val q = item.qty.toDoubleOrNull() ?: 0.0
        val r = item.rate.toDoubleOrNull() ?: 0.0
        q * r
    }

    val totalGst = if (isGstMode) {
        lineItems.sumOf { item ->
            val q = item.qty.toDoubleOrNull() ?: 0.0
            val r = item.rate.toDoubleOrNull() ?: 0.0
            val g = item.gstRate.toDoubleOrNull() ?: 0.0
            (q * r) * (g / 100.0)
        }
    } else 0.0

    val grandTotal = subtotal + totalGst
    val cgst = totalGst / 2.0
    val sgst = totalGst / 2.0

    val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
    val balanceDue = (grandTotal - paidAmount).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (invoiceType == InvoiceType.PURCHASE) {
                                if (isGstMode) "Purchase Tax Invoice" else "Purchase Bill (ख़रीद)"
                            } else {
                                if (isGstMode) "New GST Tax Invoice" else "New Sale Bill (बिक्री)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = invoiceNo.ifBlank { "Generating..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (invoiceType == InvoiceType.PURCHASE) DeepNavy else DeepNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("add_invoice_screen")
        ) {
            // Bill Type Switcher: Sale vs Purchase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = invoiceType == InvoiceType.SALE,
                    onClick = {
                        invoiceType = InvoiceType.SALE
                        selectedParty = null
                    },
                    label = { Text("Sale Bill (बिक्री / विक्री)", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Saffron,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).testTag("select_sale_type")
                )
                FilterChip(
                    selected = invoiceType == InvoiceType.PURCHASE,
                    onClick = {
                        invoiceType = InvoiceType.PURCHASE
                        selectedParty = null
                    },
                    label = { Text("Purchase Bill (ख़रीद / खरेदी)", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepNavy,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).testTag("select_purchase_type")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CRITICAL REQUIREMENT: GST Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isGstMode) DeepNavy.copy(alpha = 0.08f) else Saffron.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGstMode) "GST Mode: Enabled" else "Non-GST Mode: Simple Bill",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isGstMode) DeepNavy else Saffron
                        )
                        Text(
                            text = if (isGstMode)
                                "HSN, CGST, SGST columns visible."
                            else
                                "GST fields hidden. Only Item, Qty, Rate, Amount shown.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isGstMode,
                        onCheckedChange = { isGstMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavy,
                            checkedTrackColor = DeepNavy.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("gst_mode_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Select Party Dropdown (Customer for Sale, Supplier for Purchase)
            Text(
                text = if (invoiceType == InvoiceType.PURCHASE) "Select Supplier / Vendor (व्यापारी निवडा/चुनें) *" else "Select Customer (ग्राहक निवडा/चुनें) *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = partyDropdownExpanded,
                onExpandedChange = { partyDropdownExpanded = !partyDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedParty?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(if (invoiceType == InvoiceType.PURCHASE) "Choose supplier from list..." else "Choose customer from list...") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("customer_dropdown"),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = partyDropdownExpanded,
                    onDismissRequest = { partyDropdownExpanded = false }
                ) {
                    val filteredPartyList = if (invoiceType == InvoiceType.PURCHASE) {
                        parties.filter { it.type != PartyType.CUSTOMER }
                    } else {
                        parties.filter { it.type != PartyType.SUPPLIER }
                    }
                    if (filteredPartyList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(if (invoiceType == InvoiceType.PURCHASE) "No suppliers found in directory" else "No customers found in directory") },
                            onClick = { partyDropdownExpanded = false }
                        )
                    } else {
                        filteredPartyList.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(p.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = IndianAccountingUtils.formatBalanceDrCr(p.currentBalance),
                                            color = if (p.currentBalance > 0) DebitRed else CreditGreen,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedParty = p
                                    partyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            selectedParty?.let { party ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (party.phone.isNotBlank()) "Phone: ${party.phone}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Current Due: ${IndianAccountingUtils.formatBalanceDrCr(party.currentBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (party.currentBalance > 0) DebitRed else CreditGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Pick from Inventory (Stock Products)
            if (availableItems.isNotEmpty()) {
                Text(
                    text = "Quick Pick from Stock (इन्व्हेंटरी सामान)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableItems.forEach { itm ->
                        val price = if (invoiceType == InvoiceType.PURCHASE) itm.purchasePrice else itm.sellPrice
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                // If first item is blank, overwrite it; otherwise add new
                                val firstBlank = lineItems.indexOfFirst { it.name.isBlank() }
                                val lineItem = LineItemForm(
                                    itemId = itm.id,
                                    name = itm.name,
                                    qty = "1",
                                    unit = itm.unit,
                                    rate = price.toInt().toString(),
                                    hsnCode = itm.hsnCode,
                                    gstRate = itm.gstRate.toInt().toString()
                                )
                                if (firstBlank >= 0) {
                                    lineItems[firstBlank] = lineItem
                                } else {
                                    lineItems.add(lineItem)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(itm.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${IndianAccountingUtils.formatCurrency(price)} • Stock: ${itm.stockQty.toInt()} ${itm.unit}",
                                        fontSize = 10.sp,
                                        color = if (itm.stockQty <= 5.0) DebitRed else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items / Products (सामान सूची)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        lineItems.add(LineItemForm())
                    },
                    modifier = Modifier.testTag("add_item_line_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Item")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Items List
            lineItems.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { item.name = it },
                                label = { Text("Item Name #${index + 1}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("item_name_${index}"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            if (lineItems.size > 1) {
                                IconButton(
                                    onClick = { lineItems.removeAt(index) },
                                    modifier = Modifier.testTag("delete_item_${index}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DebitRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = item.qty,
                                onValueChange = { item.qty = it },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("item_qty_${index}"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.unit,
                                onValueChange = { item.unit = it },
                                label = { Text("Unit") },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("item_unit_${index}"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.rate,
                                onValueChange = { item.rate = it },
                                label = { Text("Rate (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("item_rate_${index}"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }

                        // CONDITIONAL: Only show GST fields if GST mode is active
                        if (isGstMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.hsnCode,
                                    onValueChange = { item.hsnCode = it },
                                    label = { Text("HSN Code") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("item_hsn_${index}"),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = item.gstRate,
                                    onValueChange = { item.gstRate = it },
                                    label = { Text("GST % (e.g. 18)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("item_gst_rate_${index}"),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Line Total
                        val lineTotal = (item.qty.toDoubleOrNull() ?: 0.0) * (item.rate.toDoubleOrNull() ?: 0.0)
                        val lineGst = if (isGstMode) lineTotal * ((item.gstRate.toDoubleOrNull() ?: 0.0) / 100.0) else 0.0
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Item Amount: ${IndianAccountingUtils.formatCurrency(lineTotal + lineGst)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bill Total & Tax Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                        Text(IndianAccountingUtils.formatCurrency(subtotal), fontWeight = FontWeight.SemiBold)
                    }

                    if (isGstMode && totalGst > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CGST:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(IndianAccountingUtils.formatCurrency(cgst), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SGST:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(IndianAccountingUtils.formatCurrency(sgst), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Grand Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            IndianAccountingUtils.formatCurrency(grandTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Receipt Details
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Payment Settlement (रक्कम जमा / उधारी)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick buttons for paid amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paidAmount == grandTotal && grandTotal > 0,
                            onClick = { paidAmountText = grandTotal.toInt().toString() },
                            label = { Text("Full Paid", fontSize = 11.sp) },
                            modifier = Modifier.testTag("quick_full_paid")
                        )
                        FilterChip(
                            selected = paidAmount == 0.0,
                            onClick = { paidAmountText = "0" },
                            label = { Text("Credit (उधारी)", fontSize = 11.sp) },
                            modifier = Modifier.testTag("quick_credit_unpaid")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("Amount Paid Now (₹)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paid_amount_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Payment mode
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Payment Mode", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(PaymentMode.CASH, PaymentMode.UPI).forEach { mode ->
                                    FilterChip(
                                        selected = paymentMode == mode,
                                        onClick = { paymentMode = mode },
                                        label = { Text(mode.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    if (balanceDue > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = DebitRedLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Remaining Balance Due: ${IndianAccountingUtils.formatCurrency(balanceDue)} will be added to Customer's Dr ledger.",
                                color = DebitRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Narration (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_notes_input"),
                shape = RoundedCornerShape(8.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save & Generate Bill Button
            Button(
                onClick = {
                    if (selectedParty == null) {
                        Toast.makeText(context, if (invoiceType == InvoiceType.PURCHASE) "Please select a supplier first!" else "Please select a customer first!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (lineItems.isEmpty() || lineItems.any { it.name.isBlank() }) {
                        Toast.makeText(context, "Please enter valid item name(s)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val entityItems = lineItems.map { itm ->
                        val q = itm.qty.toDoubleOrNull() ?: 1.0
                        val r = itm.rate.toDoubleOrNull() ?: 0.0
                        val g = if (isGstMode) (itm.gstRate.toDoubleOrNull() ?: 0.0) else 0.0
                        val lineAmt = q * r
                        val lineTax = lineAmt * (g / 100.0)
                        InvoiceItem(
                            invoiceId = 0,
                            itemId = itm.itemId,
                            itemName = itm.name,
                            qty = q,
                            unit = itm.unit,
                            rate = r,
                            cgstRate = if (isGstMode) g / 2.0 else 0.0,
                            sgstRate = if (isGstMode) g / 2.0 else 0.0,
                            amount = lineAmt + lineTax
                        )
                    }

                    val finalPaid = paidAmountText.toDoubleOrNull() ?: 0.0

                    viewModel.saveInvoice(
                        invoiceNo = invoiceNo,
                        partyId = selectedParty!!.id,
                        partyName = selectedParty!!.name,
                        items = entityItems,
                        paidAmount = finalPaid,
                        paymentMode = paymentMode,
                        isGst = isGstMode,
                        notes = notes,
                        type = invoiceType,
                        onSuccess = { invoiceId ->
                            val msg = if (invoiceType == InvoiceType.PURCHASE) "Purchase Bill Saved Successfully!" else "Sale Bill Saved Successfully!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            savedInvoiceForPreview = Invoice(
                                id = invoiceId,
                                invoiceNo = invoiceNo,
                                partyId = selectedParty!!.id,
                                partyName = selectedParty!!.name,
                                subtotal = subtotal,
                                cgst = cgst,
                                sgst = sgst,
                                igst = 0.0,
                                total = grandTotal,
                                paidAmount = finalPaid,
                                paymentMode = paymentMode,
                                isGst = isGstMode,
                                notes = notes,
                                type = invoiceType
                            )
                            savedItemsForPreview = entityItems
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (invoiceType == InvoiceType.PURCHASE) DeepNavy else Saffron),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_invoice_button")
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (invoiceType == InvoiceType.PURCHASE) {
                        "Save Purchase Bill (${IndianAccountingUtils.formatCurrency(grandTotal)})"
                    } else {
                        "Save & Print Bill (${IndianAccountingUtils.formatCurrency(grandTotal)})"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // Invoice Bill Preview Dialog
    savedInvoiceForPreview?.let { inv ->
        InvoiceBillPreviewDialog(
            invoice = inv,
            items = savedItemsForPreview,
            business = business,
            onDismiss = {
                savedInvoiceForPreview = null
                onNavigateBack()
            }
        )
    }
}
