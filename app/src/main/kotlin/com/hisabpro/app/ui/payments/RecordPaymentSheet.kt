package com.hisabpro.app.ui.payments

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import androidx.compose.ui.res.stringResource
import com.hisabpro.app.R
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.util.InvoiceUpiQrSheet
import java.util.Locale

enum class PaymentDirection {
    RECEIPT_IN,  // Money In from Customer
    PAYMENT_OUT  // Money Out to Supplier
}

data class PaymentRecordData(
    val direction: PaymentDirection,
    val partyId: String,
    val partyName: String,
    val amount: Double,
    val paymentMode: PaymentMode,
    val referenceNo: String,
    val notes: String,
    val linkedInvoiceId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentSheet(
    parties: List<Party>,
    initialDirection: PaymentDirection = PaymentDirection.RECEIPT_IN,
    initialPartyId: String? = null,
    merchantUpiId: String = "",
    merchantName: String = "",
    sheetState: SheetState? = null,
    onDismiss: () -> Unit,
    onSavePayment: (PaymentRecordData) -> Unit
) {
    val context = LocalContext.current
    var direction by remember { mutableStateOf(initialDirection) }

    val filteredParties = remember(direction, parties) {
        when (direction) {
            PaymentDirection.RECEIPT_IN -> parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
            PaymentDirection.PAYMENT_OUT -> parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
        }
    }

    var selectedParty by remember(initialPartyId, filteredParties) {
        mutableStateOf(
            filteredParties.find { it.id == initialPartyId }
                ?: filteredParties.firstOrNull()
        )
    }

    val invoiceRepo = remember { com.hisabpro.app.data.repository.InvoiceRepository.getInstance(context) }
    val allInvoices by invoiceRepo.invoices.collectAsStateWithLifecycle()

    val openInvoicesForParty = remember(selectedParty, direction, allInvoices) {
        if (selectedParty == null) emptyList()
        else allInvoices.filter { inv ->
            inv.customerId == selectedParty?.id &&
            inv.dueAmount > 0.01 &&
            inv.paymentStatus != com.hisabpro.app.data.model.InvoiceStatus.PAID
        }.sortedByDescending { it.dateMillis }
    }

    var selectedInvoice by remember { mutableStateOf<com.hisabpro.app.data.model.Invoice?>(null) }
    var showInvoiceDropdown by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var referenceNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showPartyDropdown by remember { mutableStateOf(false) }

    // Auto populate reference invoice & due amount when party or direction changes
    androidx.compose.runtime.LaunchedEffect(selectedParty, direction, openInvoicesForParty) {
        if (direction == PaymentDirection.RECEIPT_IN && openInvoicesForParty.isNotEmpty()) {
            val autoInv = openInvoicesForParty.first()
            selectedInvoice = autoInv
            referenceNo = autoInv.invoiceNumber
            if (amountText.isBlank()) {
                amountText = if (autoInv.dueAmount % 1.0 == 0.0) autoInv.dueAmount.toInt().toString() else autoInv.dueAmount.toString()
            }
        }
    }

    // Instant UPI QR dialog trigger
    var showUpiQrSheet by remember { mutableStateOf(false) }
    val upiQrSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isUserEdited by remember { mutableStateOf(false) }

    val initialParty = remember { parties.find { it.id == initialPartyId } }
    val hasUnsavedChanges = remember(amountText, referenceNo, notes, selectedParty, selectedMode, isUserEdited) {
        amountText.isNotBlank() ||
                referenceNo.isNotBlank() ||
                notes.isNotBlank() ||
                (selectedParty != null && selectedParty != initialParty) ||
                selectedMode != PaymentMode.CASH ||
                isUserEdited
    }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    val currentHasUnsavedChanges by rememberUpdatedState(hasUnsavedChanges)
    val coroutineScope = rememberCoroutineScope()

    val defaultSheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == androidx.compose.material3.SheetValue.Hidden) {
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
        if (showUpiQrSheet) {
            showUpiQrSheet = false
            return
        }
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
            text = { Text("You have unsaved changes in this payment form. Are you sure you want to discard them?") },
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
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BackHandler(enabled = true) {
            handleDismissAttempt()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Surface(
                color = if (direction == PaymentDirection.RECEIPT_IN) IncomeGreen else ExpenseRed,
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
                                imageVector = if (direction == PaymentDirection.RECEIPT_IN)
                                    Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (direction == PaymentDirection.RECEIPT_IN)
                                    stringResource(R.string.receive_payment) else stringResource(R.string.make_payment),
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = if (direction == PaymentDirection.RECEIPT_IN)
                                    "Customer Receipt • Settle Balance" else "Supplier Payment • Clear Due",
                                color = PureWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { handleDismissAttempt() },
                        modifier = Modifier.testTag("btn_close_record_payment")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PureWhite
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Direction Toggle (Payment In vs Payment Out)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = direction == PaymentDirection.RECEIPT_IN,
                        onClick = {
                            direction = PaymentDirection.RECEIPT_IN
                            selectedParty = parties.firstOrNull { it.type == PartyType.CUSTOMER }
                        },
                        label = { Text("Payment In (Customer)") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                            selectedLabelColor = IncomeGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_direction_receipt")
                    )

                    FilterChip(
                        selected = direction == PaymentDirection.PAYMENT_OUT,
                        onClick = {
                            direction = PaymentDirection.PAYMENT_OUT
                            selectedParty = parties.firstOrNull { it.type == PartyType.SUPPLIER }
                        },
                        label = { Text("Payment Out (Supplier)") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                            selectedLabelColor = ExpenseRed
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_direction_payment")
                    )
                }

                // Party Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = showPartyDropdown,
                    onExpandedChange = { showPartyDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedParty?.name ?: "Select Party...",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (direction == PaymentDirection.RECEIPT_IN) "From Customer" else "To Supplier")
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPartyDropdown) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Emerald700) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("input_payment_party"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = showPartyDropdown,
                        onDismissRequest = { showPartyDropdown = false }
                    ) {
                        if (filteredParties.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No ${if (direction == PaymentDirection.RECEIPT_IN) "customers" else "suppliers"} found") },
                                onClick = { showPartyDropdown = false }
                            )
                        } else {
                            filteredParties.forEach { party ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(party.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (party.phone.isNotBlank()) {
                                                Text(party.phone, fontSize = 12.sp, color = Slate700)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedParty = party
                                        showPartyDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Reference Invoice Selection (Auto-populated for unpaid invoices)
                if (direction == PaymentDirection.RECEIPT_IN && openInvoicesForParty.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = showInvoiceDropdown,
                        onExpandedChange = { showInvoiceDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedInvoice?.let { "${it.invoiceNumber} (Due: ₹${String.format(Locale.ENGLISH, "%.2f", it.dueAmount)})" } ?: "General Credit / Custom Ref",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Against Reference Invoice *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showInvoiceDropdown) },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = Emerald700) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("input_payment_invoice_ref"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = showInvoiceDropdown,
                            onDismissRequest = { showInvoiceDropdown = false }
                        ) {
                            openInvoicesForParty.forEach { inv ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Due Amount: ₹${String.format(Locale.ENGLISH, "%.2f", inv.dueAmount)}", fontSize = 12.sp, color = ExpenseRed)
                                        }
                                    },
                                    onClick = {
                                        selectedInvoice = inv
                                        referenceNo = inv.invoiceNumber
                                        amountText = if (inv.dueAmount % 1.0 == 0.0) inv.dueAmount.toInt().toString() else inv.dueAmount.toString()
                                        showInvoiceDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("None / Custom Ref (General Credit)", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedInvoice = null
                                    referenceNo = ""
                                    showInvoiceDropdown = false
                                }
                            )
                        }
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amountText = input
                            isUserEdited = true
                        }
                    },
                    label = { Text("Amount Paid (₹) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text("₹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald800, modifier = Modifier.padding(start = 12.dp))
                    },
                    trailingIcon = {
                        if (direction == PaymentDirection.RECEIPT_IN && amountText.toDoubleOrNull() != null && amountText.toDouble() > 0) {
                            IconButton(
                                onClick = { showUpiQrSheet = true },
                                modifier = Modifier.testTag("btn_payment_show_qr")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Show UPI QR",
                                    tint = Emerald700
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_payment_amount"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Payment Mode Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMode.entries.forEach { mode ->
                            val isSelected = selectedMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Emerald800 else Slate100,
                                modifier = Modifier
                                    .clickable { selectedMode = mode }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Emerald800 else Slate200,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("mode_${mode.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val icon = when (mode) {
                                        PaymentMode.CASH -> Icons.Default.Money
                                        PaymentMode.ONLINE_UPI -> Icons.Default.Payments
                                        PaymentMode.BANK_TRANSFER -> Icons.Default.AccountBalance
                                        PaymentMode.CARD -> Icons.Default.CreditCard
                                        PaymentMode.CHEQUE -> Icons.Default.Payments
                                        PaymentMode.CREDIT -> Icons.Default.Person
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) PureWhite else Slate700,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = mode.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PureWhite else Slate800
                                    )
                                }
                            }
                        }
                    }
                }

                // Reference / Cheque No.
                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text("Reference / Cheque # / UPI UTR") },
                    placeholder = { Text("e.g. UTR-982173 or CHQ-00123") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_payment_ref"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Payment Notes / Remarks") },
                    placeholder = { Text("e.g. Cleared bill invoice") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_payment_notes"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            Toast.makeText(context, "Please enter a valid payment amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val party = selectedParty
                        if (party == null) {
                            Toast.makeText(context, "Please select a party", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        onSavePayment(
                            PaymentRecordData(
                                direction = direction,
                                partyId = party.id,
                                partyName = party.name,
                                amount = amt,
                                paymentMode = selectedMode,
                                referenceNo = referenceNo.trim(),
                                notes = notes.trim(),
                                linkedInvoiceId = selectedInvoice?.id
                            )
                        )
                        Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (direction == PaymentDirection.RECEIPT_IN) IncomeGreen else ExpenseRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_payment")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (direction == PaymentDirection.RECEIPT_IN)
                            "Record Receipt (₹${amountText.ifBlank { "0" }})"
                        else
                            "Record Payment (₹${amountText.ifBlank { "0" }})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PureWhite
                    )
                }
            }
        }
    }

    // Dynamic UPI QR Sheet for immediate in-person scanning
    if (showUpiQrSheet) {
        val amt = amountText.toDoubleOrNull() ?: 0.0
        InvoiceUpiQrSheet(
            invoiceNumber = "PAYMENT-${selectedParty?.name ?: "CLIENT"}",
            amount = amt,
            customerName = selectedParty?.name ?: "",
            merchantName = merchantName.ifBlank { "HisabPro Merchant" },
            merchantUpiId = merchantUpiId,
            sheetState = upiQrSheetState,
            onDismiss = { showUpiQrSheet = false },
            onPaymentConfirmed = {
                // Auto fill mode to UPI
                selectedMode = PaymentMode.ONLINE_UPI
            }
        )
    }
}
