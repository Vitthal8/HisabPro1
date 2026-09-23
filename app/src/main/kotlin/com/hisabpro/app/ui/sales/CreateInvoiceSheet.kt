package com.hisabpro.app.ui.sales

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.ui.theme.Emerald50
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate50
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate400
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
import androidx.compose.foundation.BorderStroke
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceSheet(
    sheetState: SheetState,
    parties: List<Party>,
    availableItems: List<Item> = emptyList(),
    initialInvoiceType: InvoiceType = InvoiceType.TAX_INVOICE,
    invoiceToEdit: Invoice? = null,
    onDismiss: () -> Unit,
    onSaveInvoice: (Invoice, saveAction: SaveAction) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(invoiceToEdit?.type ?: initialInvoiceType) }
    var gstMode by remember { mutableStateOf(invoiceToEdit?.gstMode ?: GstMode.INTRA_STATE) }

    // Item picker state
    var showItemPickerDialog by remember { mutableStateOf(false) }
    var itemPickerSearch by remember { mutableStateOf("") }

    // Quick cash mode shortcut
    var isQuickSaleMode by remember {
        mutableStateOf(
            if (invoiceToEdit != null) invoiceToEdit.type == InvoiceType.NON_GST_BILL
            else initialInvoiceType == InvoiceType.NON_GST_BILL
        )
    }

    // Customer
    var selectedParty by remember { mutableStateOf<Party?>(parties.find { it.id == invoiceToEdit?.customerId }) }
    var showPartyDropdown by remember { mutableStateOf(false) }
    var customerName by remember {
        mutableStateOf(
            invoiceToEdit?.customerName ?: if (isQuickSaleMode) "Cash Customer" else ""
        )
    }
    var customerPhone by remember { mutableStateOf(invoiceToEdit?.customerPhone ?: "") }
    var customerAddress by remember { mutableStateOf(invoiceToEdit?.customerAddress ?: "") }
    var customerGstin by remember { mutableStateOf(invoiceToEdit?.customerGstin ?: "") }

    // Line items
    val items = remember { mutableStateListOf<InvoiceItem>() }

    // Line item entry fields
    var itemDesc by remember { mutableStateOf("") }
    var itemHsn by remember { mutableStateOf("") }
    var itemQtyText by remember { mutableStateOf("1") }
    var itemUnit by remember { mutableStateOf("Pcs") }
    var itemPriceText by remember { mutableStateOf("") }
    var itemGstRate by remember { mutableDoubleStateOf(18.0) }
    var showUnitDropdown by remember { mutableStateOf(false) }

    // Financial
    var discountText by remember {
        mutableStateOf(
            invoiceToEdit?.discountAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "0"
        )
    }
    var paymentStatus by remember { mutableStateOf(invoiceToEdit?.paymentStatus ?: InvoiceStatus.PAID) }
    var paidAmountText by remember {
        mutableStateOf(
            invoiceToEdit?.paidAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        )
    }
    var notes by remember { mutableStateOf(invoiceToEdit?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonUnits = listOf("Pcs", "Nos", "Kg", "Box", "Mtr", "Ltr", "Pkt", "Set")
    val gstSlabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

    // Prepopulate items
    LaunchedEffect(invoiceToEdit) {
        if (invoiceToEdit != null) {
            items.clear()
            items.addAll(invoiceToEdit.items)
        } else if (items.isEmpty()) {
            items.add(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    description = if (isQuickSaleMode) "Retail Counter Sale" else "Goods / Services",
                    hsnCode = "9983",
                    quantity = 1.0,
                    unit = "Pcs",
                    unitPrice = 1000.0,
                    gstRate = if (selectedType == InvoiceType.TAX_INVOICE) 18.0 else 0.0
                )
            )
        }
    }

    // Calculations
    val subtotal = items.sumOf { it.taxableAmount }
    val totalTax = if (selectedType == InvoiceType.NON_GST_BILL || gstMode == GstMode.EXEMPT) {
        0.0
    } else {
        items.sumOf { it.getTaxAmount(gstMode) }
    }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val grandTotal = kotlin.math.max(0.0, subtotal + totalTax - discount)
    val autoPaid = when (paymentStatus) {
        InvoiceStatus.PAID -> grandTotal
        InvoiceStatus.UNPAID -> 0.0
        InvoiceStatus.PARTIAL -> paidAmountText.toDoubleOrNull() ?: 0.0
    }
    val dueAmount = kotlin.math.max(0.0, grandTotal - autoPaid)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isQuickSaleMode) Emerald700.copy(alpha = 0.14f) else Emerald700.copy(alpha = 0.1f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isQuickSaleMode) Icons.Default.FlashOn else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = when {
                                    invoiceToEdit != null -> "Edit ${invoiceToEdit.invoiceNumber}"
                                    isQuickSaleMode -> "Quick Counter Sale"
                                    else -> "New Invoice / Bill"
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (invoiceToEdit != null) "Modify items, totals, or customer details"
                                else if (isQuickSaleMode) "Instant walk-in cash billing"
                                else "Complete GST/Non-GST tax invoice",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            HorizontalDivider(color = Slate200)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Sale Mode Switch Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isQuickSaleMode) Emerald50 else Slate50
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isQuickSaleMode) Emerald700.copy(alpha = 0.35f) else Slate200
                    ),
                    shape = RoundedCornerShape(12.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isQuickSaleMode) Emerald700 else Slate200,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = if (isQuickSaleMode) PureWhite else Slate600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Quick Counter Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isQuickSaleMode) Emerald900 else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isQuickSaleMode) "Active: Skip customer info for walk-in cash sales" else "Switch on for rapid counter bills",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilterChip(
                            selected = isQuickSaleMode,
                            onClick = {
                                isQuickSaleMode = !isQuickSaleMode
                                if (isQuickSaleMode) {
                                    customerName = "Cash Customer"
                                    customerPhone = ""
                                    selectedParty = null
                                    paymentStatus = InvoiceStatus.PAID
                                }
                            },
                            label = {
                                Text(
                                    text = if (isQuickSaleMode) "ON" else "OFF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald700,
                                selectedLabelColor = PureWhite,
                                containerColor = Slate200,
                                labelColor = Slate700
                            )
                        )
                    }
                }

                // Document Type Chips
                Text(
                    text = "Document Type",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InvoiceType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == InvoiceType.NON_GST_BILL) {
                                    gstMode = GstMode.EXEMPT
                                } else if (type == InvoiceType.TAX_INVOICE && gstMode == GstMode.EXEMPT) {
                                    gstMode = GstMode.INTRA_STATE
                                }
                            },
                            label = {
                                Text(
                                    text = when (type) {
                                        InvoiceType.TAX_INVOICE -> "GST Tax Invoice"
                                        InvoiceType.NON_GST_BILL -> "Non-GST Bill"
                                        InvoiceType.PROFORMA -> "Quotation / Estimate"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald700,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.testTag("chip_type_${type.name.lowercase()}")
                        )
                    }
                }

                // GST Mode Selection (Only if TAX_INVOICE)
                if (selectedType == InvoiceType.TAX_INVOICE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = gstMode == GstMode.INTRA_STATE,
                            onClick = { gstMode = GstMode.INTRA_STATE },
                            label = { Text("Intra-State (CGST + SGST)") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                                selectedLabelColor = Emerald800
                            )
                        )
                        FilterChip(
                            selected = gstMode == GstMode.INTER_STATE,
                            onClick = { gstMode = GstMode.INTER_STATE },
                            label = { Text("Inter-State (IGST)") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                                selectedLabelColor = Emerald800
                            )
                        )
                    }
                }

                HorizontalDivider(color = Slate200)

                // Customer Details Section
                Text(
                    text = "Customer Details",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                if (isQuickSaleMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Emerald700)
                            Column {
                                Text("Cash Customer (Counter Walk-in)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Customer profile not required for cash bills.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    // Pick existing party dropdown button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showPartyDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedParty?.name ?: "Select from Existing Parties (Khata)",
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showPartyDropdown,
                            onDismissRequest = { showPartyDropdown = false }
                        ) {
                            parties.forEach { party ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(party.name, fontWeight = FontWeight.SemiBold)
                                            if (party.phone.isNotBlank()) {
                                                Text(party.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedParty = party
                                        customerName = party.name
                                        customerPhone = party.phone
                                        customerAddress = party.address
                                        customerGstin = party.gstin
                                        showPartyDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer / Party Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_customer_phone"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (selectedType == InvoiceType.TAX_INVOICE) {
                            OutlinedTextField(
                                value = customerGstin,
                                onValueChange = { customerGstin = it.uppercase() },
                                label = { Text("GSTIN (Optional)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_customer_gstin"),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("Billing Address (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(color = Slate200)

                // Itemized List Header & Current Items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invoice Items (${items.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Existing items cards
                items.forEachIndexed { index, item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${item.description}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.quantity} ${item.unit} @ ₹${item.unitPrice} | GST: ${item.gstRate}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Total: ₹${String.format(Locale.ENGLISH, "%.2f", item.getTotal(gstMode))}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Emerald800
                                )
                            }
                            IconButton(
                                onClick = { items.removeAt(index) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove item",
                                    tint = ExpenseRed
                                )
                            }
                        }
                    }
                }

                // Add New Item Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+ Add Item",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Emerald800
                            )

                            if (availableItems.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        itemPickerSearch = ""
                                        showItemPickerDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Emerald800
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick from Inventory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = itemDesc,
                            onValueChange = { itemDesc = it },
                            label = { Text("Item Name / Description") },
                            placeholder = { Text("e.g. Copper Wire 90m, Rice Bag") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_item_desc"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = itemQtyText,
                                onValueChange = { itemQtyText = it },
                                label = { Text("Qty") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_item_qty"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Unit picker box
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showUnitDropdown = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(itemUnit, maxLines = 1)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = showUnitDropdown,
                                    onDismissRequest = { showUnitDropdown = false }
                                ) {
                                    commonUnits.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                itemUnit = u
                                                showUnitDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = itemPriceText,
                                onValueChange = { itemPriceText = it },
                                label = { Text("Rate (₹)") },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("input_item_price"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedType == InvoiceType.TAX_INVOICE) {
                                OutlinedTextField(
                                    value = itemHsn,
                                    onValueChange = { itemHsn = it },
                                    label = { Text("HSN / SAC") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            // GST Slab chips
                            if (selectedType == InvoiceType.TAX_INVOICE && gstMode != GstMode.EXEMPT) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text("GST Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        gstSlabs.forEach { slab ->
                                            FilterChip(
                                                selected = itemGstRate == slab,
                                                onClick = { itemGstRate = slab },
                                                label = { Text("${slab.toInt()}%", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Emerald700,
                                                    selectedLabelColor = PureWhite
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val desc = itemDesc.trim()
                                val qty = itemQtyText.toDoubleOrNull() ?: 1.0
                                val price = itemPriceText.toDoubleOrNull() ?: 0.0

                                if (desc.isBlank()) {
                                    errorMessage = "Please enter an item description."
                                    return@Button
                                }
                                if (price <= 0) {
                                    errorMessage = "Please enter a valid unit rate."
                                    return@Button
                                }

                                items.add(
                                    InvoiceItem(
                                        id = UUID.randomUUID().toString(),
                                        description = desc,
                                        hsnCode = itemHsn.trim(),
                                        quantity = qty,
                                        unit = itemUnit,
                                        unitPrice = price,
                                        gstRate = if (selectedType == InvoiceType.TAX_INVOICE && gstMode != GstMode.EXEMPT) itemGstRate else 0.0
                                    )
                                )

                                // Reset item form
                                itemDesc = ""
                                itemHsn = ""
                                itemPriceText = ""
                                itemQtyText = "1"
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_add_item_row"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Line Item")
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Discount & Payment Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount (₹)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_discount"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Payment status selector
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Payment Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InvoiceStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = paymentStatus == status,
                                    onClick = { paymentStatus = status },
                                    label = { Text(status.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (status) {
                                            InvoiceStatus.PAID -> IncomeGreen
                                            InvoiceStatus.PARTIAL -> Color(0xFFF59E0B)
                                            InvoiceStatus.UNPAID -> ExpenseRed
                                        },
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }
                    }
                }

                if (paymentStatus == InvoiceStatus.PARTIAL) {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("Paid Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Payment Terms (Optional)") },
                    placeholder = { Text("e.g. Thanks for your visit! Payment via UPI.") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Live Financial Summary Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BILL SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal (Taxable):", color = Slate600, fontSize = 13.sp)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", subtotal)}",
                                color = Slate900,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedType == InvoiceType.TAX_INVOICE && gstMode != GstMode.EXEMPT) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (gstMode == GstMode.INTRA_STATE) "CGST + SGST:" else "IGST:",
                                    color = Slate600,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "+₹${String.format(Locale.ENGLISH, "%.2f", totalTax)}",
                                    color = Emerald800,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (discount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount:", color = ExpenseRed, fontSize = 13.sp)
                                Text(
                                    "-₹${String.format(Locale.ENGLISH, "%.2f", discount)}",
                                    color = ExpenseRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Grand Total Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald50,
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "GRAND TOTAL",
                                        color = Emerald900,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (dueAmount > 0) {
                                        Text(
                                            text = "Due: ₹${String.format(Locale.ENGLISH, "%.2f", dueAmount)}",
                                            color = ExpenseRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Fully Paid",
                                            color = IncomeGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = "₹${String.format(Locale.ENGLISH, "%.2f", grandTotal)}",
                                    color = Emerald900,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Save, Save & WhatsApp, Save & PDF
                fun buildInvoice(): Invoice? {
                    if (items.isEmpty()) {
                        errorMessage = "Please add at least one line item."
                        return null
                    }
                    val cName = if (isQuickSaleMode) {
                        "Cash Customer"
                    } else if (customerName.isNotBlank()) {
                        customerName.trim()
                    } else {
                        "Cash Customer"
                    }

                    return Invoice(
                        id = invoiceToEdit?.id ?: UUID.randomUUID().toString(),
                        invoiceNumber = invoiceToEdit?.invoiceNumber ?: "", // Will be assigned by repository if blank
                        type = selectedType,
                        gstMode = gstMode,
                        customerId = selectedParty?.id,
                        customerName = cName,
                        customerPhone = customerPhone.trim(),
                        customerAddress = customerAddress.trim(),
                        customerGstin = customerGstin.trim(),
                        dateMillis = invoiceToEdit?.dateMillis ?: System.currentTimeMillis(),
                        items = items.toList(),
                        discountAmount = discount,
                        notes = notes.trim(),
                        paymentStatus = paymentStatus,
                        paidAmount = autoPaid,
                        createdAt = invoiceToEdit?.createdAt ?: System.currentTimeMillis()
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val inv = buildInvoice() ?: return@Button
                            onSaveInvoice(inv, SaveAction.SAVE_ONLY)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_save_invoice")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (invoiceToEdit != null) "Update Invoice" else "Save & Complete Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val inv = buildInvoice() ?: return@OutlinedButton
                                onSaveInvoice(inv, SaveAction.SAVE_AND_WHATSAPP)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_save_and_whatsapp")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF1EBE5D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & WhatsApp", fontSize = 12.sp, color = Color(0xFF128C7E), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                val inv = buildInvoice() ?: return@OutlinedButton
                                onSaveInvoice(inv, SaveAction.SAVE_AND_PDF)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Slate400),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_save_and_pdf")
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & PDF", fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (showItemPickerDialog) {
            AlertDialog(
                onDismissRequest = { showItemPickerDialog = false },
                title = {
                    Column {
                        Text(
                            text = "Select from Inventory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any product to auto-fill line item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        OutlinedTextField(
                            value = itemPickerSearch,
                            onValueChange = { itemPickerSearch = it },
                            placeholder = { Text("Search product name, SKU...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredPickerItems = availableItems.filter {
                            itemPickerSearch.isBlank() ||
                                it.name.contains(itemPickerSearch, ignoreCase = true) ||
                                it.itemCode.contains(itemPickerSearch, ignoreCase = true) ||
                                it.category.contains(itemPickerSearch, ignoreCase = true)
                        }

                        if (filteredPickerItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching items",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredPickerItems.forEach { prod ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                itemDesc = prod.name
                                                itemPriceText = if (prod.salePrice > 0) prod.salePrice.toString() else ""
                                                itemUnit = prod.unit
                                                itemHsn = prod.hsnCode
                                                itemGstRate = prod.gstRate
                                                showItemPickerDialog = false
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = prod.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${prod.category} • In Stock: ${prod.currentStock.toInt()} ${prod.unit}",
                                                    fontSize = 11.sp,
                                                    color = if (prod.isOutOfStock) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "₹${prod.salePrice}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Emerald800
                                                )
                                                if (prod.gstRate > 0) {
                                                    Text(
                                                        text = "${prod.gstRate.toInt()}% GST",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showItemPickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

enum class SaveAction {
    SAVE_ONLY,
    SAVE_AND_WHATSAPP,
    SAVE_AND_PDF
}
