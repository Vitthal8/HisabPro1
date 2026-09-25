package com.hisabpro.app.ui.purchases

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.PurchaseItem
import androidx.compose.foundation.BorderStroke
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
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseSheet(
    suppliers: List<Party>,
    inventoryItems: List<Item>,
    nextPurchaseNumber: String,
    sheetState: SheetState? = null,
    onDismiss: () -> Unit,
    onSavePurchase: (PurchaseBill) -> Unit
) {
    val filteredSuppliers = remember(suppliers) {
        suppliers.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }.ifEmpty { suppliers }
    }

    var selectedSupplier by remember { mutableStateOf<Party?>(filteredSuppliers.firstOrNull()) }
    var customSupplierName by remember { mutableStateOf(selectedSupplier?.name ?: "") }
    var supplierPhone by remember { mutableStateOf(selectedSupplier?.phone ?: "") }
    var supplierGstin by remember { mutableStateOf(selectedSupplier?.gstin ?: "") }
    var vendorBillNumber by remember { mutableStateOf("") }

    var gstMode by remember { mutableStateOf(GstMode.INTRA_STATE) }
    var itcEligible by remember { mutableStateOf(true) }
    var discountInput by remember { mutableStateOf("") }
    var paidAmountInput by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Bank Transfer") }
    var notes by remember { mutableStateOf("") }

    // Draft Purchase Items
    val initialItem = inventoryItems.firstOrNull()
    val initialPrice = initialItem?.purchasePrice?.takeIf { it > 0 } ?: 0.0
    val initialPriceText = if (initialPrice > 0) {
        if (initialPrice % 1.0 == 0.0) initialPrice.toInt().toString() else initialPrice.toString()
    } else ""
    val initialDesc = initialItem?.name ?: ""
    val initialUnit = initialItem?.unit ?: "Pcs"
    val initialGst = initialItem?.gstRate ?: 18.0

    val draftItems = remember {
        mutableStateListOf(
            DraftPurchaseLine(
                id = UUID.randomUUID().toString(),
                initialDescription = initialDesc,
                initialItemId = initialItem?.id,
                initialHsnCode = initialItem?.hsnCode ?: "",
                initialQuantityText = "1",
                initialUnit = initialUnit,
                initialUnitPriceText = initialPriceText,
                initialGstRate = initialGst
            )
        )
    }

    // Calculations
    val subtotal by remember {
        derivedStateOf {
            draftItems.sumOf { it.quantity * it.unitPrice }
        }
    }

    val totalTax by remember {
        derivedStateOf {
            if (gstMode == GstMode.EXEMPT) 0.0 else {
                draftItems.sumOf { (it.quantity * it.unitPrice * it.gstRate) / 100.0 }
            }
        }
    }

    val discountAmount = discountInput.toDoubleOrNull() ?: 0.0
    val grandTotal = (subtotal + totalTax - discountAmount).coerceAtLeast(0.0)
    val enteredPaid = paidAmountInput.toDoubleOrNull() ?: grandTotal
    val dueAmount = (grandTotal - enteredPaid).coerceAtLeast(0.0)

    val firstItem = draftItems.firstOrNull()
    val initialSupplierName = selectedSupplier?.name ?: ""
    val hasUnsavedChanges = remember(
        customSupplierName, supplierPhone, supplierGstin, vendorBillNumber, notes,
        draftItems.size, discountInput, paidAmountInput,
        firstItem?.description, firstItem?.quantityText, firstItem?.unitPriceText, firstItem?.unit, firstItem?.gstRate
    ) {
        (customSupplierName.isNotBlank() && customSupplierName != initialSupplierName) ||
                supplierPhone.isNotBlank() ||
                supplierGstin.isNotBlank() ||
                vendorBillNumber.isNotBlank() ||
                notes.isNotBlank() ||
                discountInput.isNotBlank() ||
                paidAmountInput.isNotBlank() ||
                draftItems.size > 1 ||
                (firstItem != null && firstItem.isModifiedFrom(
                    initialDesc,
                    "1",
                    initialUnit,
                    initialPriceText,
                    initialGst
                ))
    }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    val currentHasUnsavedChanges by rememberUpdatedState(hasUnsavedChanges)
    val coroutineScope = rememberCoroutineScope()

    val defaultSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                if (currentHasUnsavedChanges) {
                    showDiscardConfirmDialog = true
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }
    )
    val effectiveSheetState = sheetState ?: defaultSheetState

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val isImeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0

    fun handleDismissAttempt() {
        if (isImeVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
            return
        }
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = true) {
        handleDismissAttempt()
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardConfirmDialog = false
                coroutineScope.launch {
                    try {
                        effectiveSheetState.expand()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes in this purchase bill form. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Discard", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        coroutineScope.launch {
                            try {
                                effectiveSheetState.expand()
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { handleDismissAttempt() },
        sheetState = effectiveSheetState,
        properties = androidx.compose.material3.ModalBottomSheetDefaults.properties(
            shouldDismissOnBackPress = false
        ),
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        BackHandler(enabled = true) {
            handleDismissAttempt()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald50,
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.2f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "New Purchase Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Inward Stock & Tax Credit ($nextPurchaseNumber)",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
                IconButton(onClick = { handleDismissAttempt() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }

            HorizontalDivider(color = Slate200)

            // Supplier Selection Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SUPPLIER / DISTRIBUTOR DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    // Quick supplier chips
                    if (filteredSuppliers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredSuppliers.take(3).forEach { sup ->
                                val isSel = selectedSupplier?.id == sup.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedSupplier = sup
                                        customSupplierName = sup.name
                                        supplierPhone = sup.phone
                                        supplierGstin = sup.gstin
                                    },
                                    label = {
                                        Text(
                                            text = sup.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald700,
                                        selectedLabelColor = PureWhite,
                                        containerColor = Slate50,
                                        labelColor = Slate700
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSel) Emerald700 else Slate200,
                                        selectedBorderColor = Emerald700,
                                        enabled = true,
                                        selected = isSel
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customSupplierName,
                        onValueChange = {
                            customSupplierName = it
                            if (selectedSupplier?.name != it) selectedSupplier = null
                        },
                        label = { Text("Supplier Business Name *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Emerald700)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_purchase_supplier_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = supplierGstin,
                            onValueChange = { supplierGstin = it.uppercase() },
                            label = { Text("Supplier GSTIN") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_purchase_supplier_gstin"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = vendorBillNumber,
                            onValueChange = { vendorBillNumber = it },
                            label = { Text("Vendor Bill No.") },
                            placeholder = { Text("e.g. MKT/892") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_purchase_vendor_bill_no"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // GST Supply Mode
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GST Supply Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate700
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isIntra = gstMode == GstMode.INTRA_STATE
                    FilterChip(
                        selected = isIntra,
                        onClick = { gstMode = GstMode.INTRA_STATE },
                        label = {
                            Text(
                                "Intra-State (CGST + SGST)",
                                fontSize = 12.sp,
                                fontWeight = if (isIntra) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = PureWhite,
                            containerColor = Slate50,
                            labelColor = Slate700
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isIntra) Emerald700 else Slate200,
                            selectedBorderColor = Emerald700,
                            enabled = true,
                            selected = isIntra
                        )
                    )

                    val isInter = gstMode == GstMode.INTER_STATE
                    FilterChip(
                        selected = isInter,
                        onClick = { gstMode = GstMode.INTER_STATE },
                        label = {
                            Text(
                                "Inter-State (IGST)",
                                fontSize = 12.sp,
                                fontWeight = if (isInter) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = PureWhite,
                            containerColor = Slate50,
                            labelColor = Slate700
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isInter) Emerald700 else Slate200,
                            selectedBorderColor = Emerald700,
                            enabled = true,
                            selected = isInter
                        )
                    )
                }
            }

            // Items List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Purchased Items (Adds to Stock)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Button(
                    onClick = {
                        draftItems.add(
                            DraftPurchaseLine(
                                id = UUID.randomUUID().toString(),
                                initialDescription = "",
                                initialQuantityText = "1",
                                initialUnit = "Pcs",
                                initialUnitPriceText = "",
                                initialGstRate = 18.0
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Item", color = Emerald700, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            draftItems.forEachIndexed { index, line ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item #${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Emerald800
                            )
                            if (draftItems.size > 1) {
                                IconButton(
                                    onClick = { draftItems.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Select from Inventory quick chips
                        if (inventoryItems.isNotEmpty() && line.description.isBlank()) {
                            Text(
                                text = "Pick from inventory:",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                inventoryItems.take(3).forEach { invItem ->
                                    Surface(
                                        color = Slate50,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .clickable {
                                                line.description = invItem.name
                                                line.itemId = invItem.id
                                                line.hsnCode = invItem.hsnCode
                                                line.unit = invItem.unit
                                                val p = if (invItem.purchasePrice > 0) invItem.purchasePrice else invItem.salePrice * 0.8
                                                line.unitPriceText = if (p > 0) {
                                                    if (p % 1.0 == 0.0) p.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", p)
                                                } else ""
                                                line.gstRate = invItem.gstRate
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(invItem.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Slate700)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = line.description,
                            onValueChange = { line.description = it },
                            label = { Text("Item Description *") },
                            placeholder = { Text("e.g. Basmati Rice 25kg") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = line.quantityText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        line.quantityText = input
                                    }
                                },
                                label = { Text("Inward Qty *") },
                                placeholder = { Text("1") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald700,
                                    unfocusedBorderColor = Slate200
                                ),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("input_purchase_item_qty_${index}"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = line.unit,
                                onValueChange = { line.unit = it },
                                label = { Text("Unit") },
                                placeholder = { Text("Pcs") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald700,
                                    unfocusedBorderColor = Slate200
                                ),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("input_purchase_item_unit_${index}"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = line.unitPriceText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        line.unitPriceText = input
                                    }
                                },
                                label = { Text("Rate (₹) *") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald700,
                                    unfocusedBorderColor = Slate200
                                ),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("input_purchase_item_rate_${index}"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Quick Quantity Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Qty:",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Medium
                            )
                            listOf(-1, 1, 5, 10, 50).forEach { delta ->
                                Surface(
                                    color = if (delta > 0) Emerald50 else Slate100,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (delta > 0) Emerald700.copy(alpha = 0.3f) else Slate200),
                                    modifier = Modifier
                                        .clickable {
                                            val current = line.quantity
                                            val updated = (current + delta).coerceAtLeast(1.0)
                                            line.quantityText = if (updated % 1.0 == 0.0) updated.toInt().toString() else updated.toString()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (delta > 0) "+$delta" else "$delta",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (delta > 0) Emerald800 else Slate700
                                    )
                                }
                            }
                        }

                        // GST Slab for item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "GST Rate:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                    FilterChip(
                                        selected = line.gstRate == rate,
                                        onClick = { line.gstRate = rate },
                                        label = { Text("${rate.toInt()}%", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Emerald700,
                                            selectedLabelColor = PureWhite
                                        )
                                    )
                                }
                            }
                        }

                        // Line Total
                        val lineTax = line.lineTax(gstMode)
                        val lineTotal = line.lineTotal(gstMode)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Item Subtotal: ₹${String.format(Locale.ENGLISH, "%.2f", line.lineSubtotal)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ₹${String.format(Locale.ENGLISH, "%.2f", lineTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Emerald800
                            )
                        }
                    }
                }
            }

            // ITC Eligibility Card
            Surface(
                color = if (itcEligible) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (itcEligible) Color(0xFFBFDBFE) else Slate200),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = itcEligible,
                        onCheckedChange = { itcEligible = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1D4ED8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Input Tax Credit (ITC) Eligible",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (itcEligible) Color(0xFF1E40AF) else Slate800
                        )
                        Text(
                            text = "Claim ₹${String.format(Locale.ENGLISH, "%.2f", totalTax)} ITC to reduce Net GST Payable in GSTR-3B",
                            fontSize = 11.sp,
                            color = if (itcEligible) Color(0xFF2563EB) else Slate500
                        )
                    }
                }
            }

            // Summary Card
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 13.sp, color = Slate600)
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total GST Tax:", fontSize = 13.sp, color = Slate600)
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", totalTax)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Emerald800)
                    }

                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("Supplier Discount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

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
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald900)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", grandTotal)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald900
                            )
                        }
                    }
                }
            }

            // Payment Settlement
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PAYMENT TO SUPPLIER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bank Transfer", "UPI", "Cash", "Cheque").forEach { mode ->
                            val isSel = paymentMode == mode
                            FilterChip(
                                selected = isSel,
                                onClick = { paymentMode = mode },
                                label = {
                                    Text(
                                        mode,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald700,
                                    selectedLabelColor = PureWhite,
                                    containerColor = Slate50,
                                    labelColor = Slate700
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSel) Emerald700 else Slate200,
                                    selectedBorderColor = Emerald700,
                                    enabled = true,
                                    selected = isSel
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = paidAmountInput,
                        onValueChange = { paidAmountInput = it },
                        label = { Text("Paid Amount (Leave blank for Full Paid)") },
                        placeholder = { Text("₹${String.format(Locale.ENGLISH, "%.2f", grandTotal)}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_purchase_paid_amount"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (dueAmount > 0.01) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pending Payable to Supplier:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                    Text(
                                        "₹${String.format(Locale.ENGLISH, "%.2f", dueAmount)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                Text(
                                    text = "Will be added to ${customSupplierName.ifBlank { "Supplier" }} Khata as Credit Payable.",
                                    fontSize = 11.sp,
                                    color = ExpenseRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Transport / E-Way Bill Details") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald700,
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Submit Button
            Button(
                onClick = {
                    val finalItems = draftItems.map { line ->
                        val qty = if (line.quantity > 0) line.quantity else 1.0
                        PurchaseItem(
                            id = line.id,
                            itemId = line.itemId,
                            description = line.description.ifBlank { "Goods / Stock" },
                            hsnCode = line.hsnCode,
                            quantity = qty,
                            unit = line.unit.ifBlank { "Pcs" },
                            unitPrice = line.unitPrice,
                            gstRate = line.gstRate
                        )
                    }

                    val finalPaid = paidAmountInput.toDoubleOrNull() ?: grandTotal
                    val status = when {
                        finalPaid >= grandTotal - 0.01 -> InvoiceStatus.PAID
                        finalPaid > 0.0 -> InvoiceStatus.PARTIAL
                        else -> InvoiceStatus.UNPAID
                    }

                    val bill = PurchaseBill(
                        id = UUID.randomUUID().toString(),
                        purchaseNumber = nextPurchaseNumber,
                        vendorBillNumber = vendorBillNumber,
                        supplierId = selectedSupplier?.id,
                        supplierName = customSupplierName.ifBlank { "Distributor / Supplier" },
                        supplierPhone = supplierPhone,
                        supplierGstin = supplierGstin,
                        items = finalItems,
                        discountAmount = discountAmount,
                        notes = notes,
                        paymentStatus = status,
                        paidAmount = finalPaid.coerceAtMost(grandTotal),
                        paymentMode = paymentMode,
                        itcEligible = itcEligible,
                        gstMode = gstMode
                    )
                    onSavePurchase(bill)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_purchase_bill"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Purchase Bill (₹${String.format(Locale.ENGLISH, "%.2f", grandTotal)})",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

class DraftPurchaseLine(
    val id: String = UUID.randomUUID().toString(),
    initialDescription: String = "",
    initialItemId: String? = null,
    initialHsnCode: String = "",
    initialQuantityText: String = "1",
    initialUnit: String = "Pcs",
    initialUnitPriceText: String = "",
    initialGstRate: Double = 18.0
) {
    var description by mutableStateOf(initialDescription)
    var itemId by mutableStateOf(initialItemId)
    var hsnCode by mutableStateOf(initialHsnCode)
    var quantityText by mutableStateOf(initialQuantityText)
    var unit by mutableStateOf(initialUnit)
    var unitPriceText by mutableStateOf(initialUnitPriceText)
    var gstRate by mutableDoubleStateOf(initialGstRate)

    val quantity: Double
        get() = quantityText.toDoubleOrNull() ?: 0.0

    val unitPrice: Double
        get() = unitPriceText.toDoubleOrNull() ?: 0.0

    val lineSubtotal: Double
        get() = quantity * unitPrice

    fun lineTax(gstMode: GstMode): Double =
        if (gstMode == GstMode.EXEMPT) 0.0 else (lineSubtotal * gstRate) / 100.0

    fun lineTotal(gstMode: GstMode): Double =
        lineSubtotal + lineTax(gstMode)

    fun isModifiedFrom(
        desc: String,
        qty: String,
        u: String,
        price: String,
        gst: Double
    ): Boolean {
        return description != desc ||
                quantityText != qty ||
                unit != u ||
                unitPriceText != price ||
                gstRate != gst
    }
}
