package com.hisabpro.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileSheet(
    profile: BusinessProfile,
    sheetState: SheetState? = null,
    onDismiss: () -> Unit,
    onSaveProfile: (BusinessProfile) -> Unit
) {
    val context = LocalContext.current

    var shopName by remember { mutableStateOf(profile.shopName) }
    var ownerName by remember { mutableStateOf(profile.ownerName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var email by remember { mutableStateOf(profile.email) }
    var isGstRegistered by remember { mutableStateOf(profile.isGstRegistered) }
    var gstin by remember { mutableStateOf(profile.gstin) }
    var address by remember { mutableStateOf(profile.address) }
    var city by remember { mutableStateOf(profile.city) }
    var state by remember { mutableStateOf(profile.state) }
    var stateCode by remember { mutableStateOf(profile.stateCode) }
    var pincode by remember { mutableStateOf(profile.pincode) }

    var upiId by remember { mutableStateOf(profile.upiId) }
    var bankName by remember { mutableStateOf(profile.bankName) }
    var accountNumber by remember { mutableStateOf(profile.accountNumber) }
    var ifscCode by remember { mutableStateOf(profile.ifscCode) }

    var invoicePrefix by remember { mutableStateOf(profile.invoicePrefix) }
    var purchasePrefix by remember { mutableStateOf(profile.purchasePrefix) }
    var termsAndConditions by remember { mutableStateOf(profile.termsAndConditions) }
    var isThermalPrinterMode by remember { mutableStateOf(profile.isThermalPrinterMode) }
    var showUpiQrOnInvoice by remember { mutableStateOf(profile.showUpiQrOnInvoice) }

    androidx.compose.runtime.LaunchedEffect(profile) {
        shopName = profile.shopName
        ownerName = profile.ownerName
        phone = profile.phone
        email = profile.email
        isGstRegistered = profile.isGstRegistered
        gstin = profile.gstin
        address = profile.address
        city = profile.city
        state = profile.state
        stateCode = profile.stateCode
        pincode = profile.pincode
        upiId = profile.upiId
        bankName = profile.bankName
        accountNumber = profile.accountNumber
        ifscCode = profile.ifscCode
        invoicePrefix = profile.invoicePrefix
        purchasePrefix = profile.purchasePrefix
        termsAndConditions = profile.termsAndConditions
        isThermalPrinterMode = profile.isThermalPrinterMode
        showUpiQrOnInvoice = profile.showUpiQrOnInvoice
    }

    val hasUnsavedChanges = remember(
        shopName, ownerName, phone, email, isGstRegistered, gstin, address, city, state, stateCode, pincode,
        upiId, bankName, accountNumber, ifscCode, invoicePrefix, purchasePrefix, termsAndConditions,
        isThermalPrinterMode, showUpiQrOnInvoice, profile
    ) {
        shopName != profile.shopName ||
                ownerName != profile.ownerName ||
                phone != profile.phone ||
                email != profile.email ||
                isGstRegistered != profile.isGstRegistered ||
                gstin != profile.gstin ||
                address != profile.address ||
                city != profile.city ||
                state != profile.state ||
                stateCode != profile.stateCode ||
                pincode != profile.pincode ||
                upiId != profile.upiId ||
                bankName != profile.bankName ||
                accountNumber != profile.accountNumber ||
                ifscCode != profile.ifscCode ||
                invoicePrefix != profile.invoicePrefix ||
                purchasePrefix != profile.purchasePrefix ||
                termsAndConditions != profile.termsAndConditions ||
                isThermalPrinterMode != profile.isThermalPrinterMode ||
                showUpiQrOnInvoice != profile.showUpiQrOnInvoice
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
            text = { Text("You have unsaved changes in your business profile. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Discard", color = androidx.compose.ui.graphics.Color.Red, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                    Text("Cancel", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Emerald700.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Business Profile & Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Store Details, GSTIN, UPI QR & Bill Customizer",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { handleDismissAttempt() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = Slate200)

            // Section 1: Business Identity
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "STORE & TAX IDENTITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Business / Shop Name *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = Emerald700)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_settings_shop_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Prop.") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GST Registered Business?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isGstRegistered) "YES — Enable CGST/SGST/IGST & HSN" else "NO — Simple Non-GST Billing Mode",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isGstRegistered,
                            onCheckedChange = { isGstRegistered = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = Emerald700
                            )
                        )
                    }

                    if (isGstRegistered) {
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it.uppercase() },
                            label = { Text("GSTIN (15 Digits)") },
                            supportingText = {
                                Text(
                                    text = if (gstin.length == 15) "Valid 15-digit GSTIN format" else "Enter 15-digit GSTIN",
                                    fontSize = 11.sp,
                                    color = if (gstin.length == 15) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_settings_gstin"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        Surface(
                            color = Slate200.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Non-GST Mode: GSTIN, HSN/SAC codes, and GST rates are cleanly hidden on invoices and reports.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Market Yard") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Section 2: UPI Payments & QR Code
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BHARAT UPI & INSTANT QR CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = Emerald700)
                    }

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.trim().lowercase() },
                        label = { Text("Business UPI VPA ID *") },
                        placeholder = { Text("e.g. yourname@okhdfcbank or paytm") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        trailingIcon = {
                            if (upiId.isNotBlank()) {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                                    Toast.makeText(context, "UPI ID copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = Emerald700)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_settings_upi_id"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Dynamic UPI QR Visualizer Card
                    if (upiId.isNotBlank()) {
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Stylized Bharat UPI QR Canvas
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .border(1.dp, Slate200, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UpiQrCanvas(sizeDp = 64)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = Emerald700.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "SCAN & PAY UPI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald800,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = shopName.ifBlank { "HisabPro Store" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = upiId,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Works with GPay, PhonePe, Paytm, BHIM",
                                        fontSize = 10.sp,
                                        color = Emerald700
                                    )
                                }
                            }
                        }
                    }

                    // Bank Account details
                    Text(
                        text = "Bank Account for NEFT / RTGS / Cheques",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        placeholder = { Text("e.g. State Bank of India, HDFC Bank") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = Emerald700)
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_settings_bank_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it.trim() },
                            label = { Text("Account No.") },
                            placeholder = { Text("e.g. 1234567890") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_settings_account_no"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase().trim().take(11) },
                            label = { Text("IFSC Code") },
                            placeholder = { Text("SBIN0001234") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_settings_ifsc_code"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Section 3: Billing & Print Customization
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BILLING & PRINT PREFERENCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it.uppercase() },
                            label = { Text("Invoice Prefix") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = purchasePrefix,
                            onValueChange = { purchasePrefix = it.uppercase() },
                            label = { Text("Purchase Prefix") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Print UPI QR on Bills", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                "Prints dynamic payment QR code on PDF and receipts",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showUpiQrOnInvoice,
                            onCheckedChange = { showUpiQrOnInvoice = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = Emerald700)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Thermal 2\" / 3\" POS Slip Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                "Optimizes invoice sharing for Bluetooth thermal slip printers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isThermalPrinterMode,
                            onCheckedChange = { isThermalPrinterMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = Emerald700)
                        )
                    }

                    OutlinedTextField(
                        value = termsAndConditions,
                        onValueChange = { termsAndConditions = it },
                        label = { Text("Bill Terms & Conditions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Save Action
            Button(
                onClick = {
                    val updated = BusinessProfile(
                        shopName = shopName.ifBlank { "HisabPro Store" },
                        ownerName = ownerName,
                        phone = phone,
                        email = email,
                        isGstRegistered = isGstRegistered,
                        gstin = if (isGstRegistered) gstin else "",
                        address = address,
                        city = city,
                        state = state,
                        stateCode = stateCode,
                        pincode = pincode,
                        upiId = upiId,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        ifscCode = ifscCode,
                        invoicePrefix = invoicePrefix.ifBlank { "INV" },
                        purchasePrefix = purchasePrefix.ifBlank { "PUR" },
                        termsAndConditions = termsAndConditions,
                        isThermalPrinterMode = isThermalPrinterMode,
                        showUpiQrOnInvoice = showUpiQrOnInvoice
                    )
                    onSaveProfile(updated)
                    Toast.makeText(context, "Business Profile & Settings Saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_business_profile"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile & Settings", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun UpiQrCanvas(sizeDp: Int) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val w = size.width
        val h = size.height
        val dot = w / 7f

        // Top-left finder pattern
        drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(dot * 2.2f, dot * 2.2f))
        drawRect(Color.White, topLeft = Offset(dot * 0.4f, dot * 0.4f), size = Size(dot * 1.4f, dot * 1.4f))
        drawRect(Color.Black, topLeft = Offset(dot * 0.7f, dot * 0.7f), size = Size(dot * 0.8f, dot * 0.8f))

        // Top-right finder pattern
        drawRect(Color.Black, topLeft = Offset(w - dot * 2.2f, 0f), size = Size(dot * 2.2f, dot * 2.2f))
        drawRect(Color.White, topLeft = Offset(w - dot * 1.8f, dot * 0.4f), size = Size(dot * 1.4f, dot * 1.4f))
        drawRect(Color.Black, topLeft = Offset(w - dot * 1.5f, dot * 0.7f), size = Size(dot * 0.8f, dot * 0.8f))

        // Bottom-left finder pattern
        drawRect(Color.Black, topLeft = Offset(0f, h - dot * 2.2f), size = Size(dot * 2.2f, dot * 2.2f))
        drawRect(Color.White, topLeft = Offset(dot * 0.4f, h - dot * 1.8f), size = Size(dot * 1.4f, dot * 1.4f))
        drawRect(Color.Black, topLeft = Offset(dot * 0.7f, h - dot * 1.5f), size = Size(dot * 0.8f, dot * 0.8f))

        // Center simulated data blocks
        drawRect(Color.Black, topLeft = Offset(dot * 3f, dot * 1.2f), size = Size(dot * 0.9f, dot * 0.9f))
        drawRect(Color.Black, topLeft = Offset(dot * 2.6f, dot * 2.8f), size = Size(dot * 1.6f, dot * 1.4f))
        drawRect(Color.Black, topLeft = Offset(dot * 4.6f, dot * 3.4f), size = Size(dot * 1.0f, dot * 1.0f))
        drawRect(Color.Black, topLeft = Offset(dot * 3.2f, dot * 4.8f), size = Size(dot * 1.2f, dot * 1.0f))
        drawRect(Color.Black, topLeft = Offset(dot * 5.0f, dot * 5.0f), size = Size(dot * 1.2f, dot * 1.2f))
    }
}
