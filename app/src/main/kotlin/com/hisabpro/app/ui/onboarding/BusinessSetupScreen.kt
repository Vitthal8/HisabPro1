package com.hisabpro.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.DeepNavyLight
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronLight
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    currentProfile: BusinessProfile,
    isInitialOnboarding: Boolean = false,
    onSaveProfile: (BusinessProfile) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var shopName by remember { mutableStateOf(currentProfile.shopName) }
    var ownerName by remember { mutableStateOf(currentProfile.ownerName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var selectedLanguage by remember { mutableStateOf(currentProfile.appLanguage) }

    // Core Requirement: GST vs Non-GST
    var isGstRegistered by remember { mutableStateOf(currentProfile.isGstRegistered) }
    var gstin by remember { mutableStateOf(currentProfile.gstin) }
    var pan by remember { mutableStateOf(currentProfile.pan) }
    var isCompositionScheme by remember { mutableStateOf(currentProfile.isCompositionScheme) }
    var compositionType by remember { mutableStateOf(currentProfile.compositionType) }

    // Location
    val indianStates = remember {
        listOf(
            "Maharashtra (27)",
            "Gujarat (24)",
            "Karnataka (29)",
            "Madhya Pradesh (23)",
            "Goa (30)",
            "Delhi (07)",
            "Rajasthan (08)",
            "Uttar Pradesh (09)",
            "Tamil Nadu (33)",
            "Telangana (36)",
            "Andhra Pradesh (37)",
            "West Bengal (19)",
            "Punjab (03)",
            "Haryana (06)",
            "Kerala (32)",
            "Bihar (10)"
        )
    }
    var selectedStateWithCode by remember {
        val match = indianStates.find { it.startsWith(currentProfile.state, ignoreCase = true) }
        mutableStateOf(match ?: "Maharashtra (27)")
    }
    var isStateExpanded by remember { mutableStateOf(false) }

    var city by remember { mutableStateOf(currentProfile.city) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var pincode by remember { mutableStateOf(currentProfile.pincode) }

    // UPI & Bank
    var upiId by remember { mutableStateOf(currentProfile.upiId) }
    var bankName by remember { mutableStateOf(currentProfile.bankName) }
    var accountNumber by remember { mutableStateOf(currentProfile.accountNumber) }
    var ifscCode by remember { mutableStateOf(currentProfile.ifscCode) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 90.dp)
        ) {
            // Header Banner with Saffron & Deep Navy Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepNavyBlue, Color(0xFF0F1442))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SaffronOrange,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isInitialOnboarding) "Setup Your Business" else "Business & GST Profile",
                                    color = PureWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "HisabPro • Indian SMB Accounting",
                                    color = PureWhite.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (onDismiss != null && !isInitialOnboarding) {
                            Surface(
                                shape = CircleShape,
                                color = PureWhite.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clickable { onDismiss() }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Language Selector Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = PureWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Language:",
                            color = PureWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        listOf("en" to "English", "hi" to "हिंदी", "mr" to "मराठी").forEach { (code, label) ->
                            FilterChip(
                                selected = selectedLanguage == code,
                                onClick = { selectedLanguage = code },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronOrange,
                                    selectedLabelColor = PureWhite,
                                    containerColor = PureWhite.copy(alpha = 0.1f),
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CORE REQUIREMENT: GST vs Non-GST Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGstRegistered) DeepNavyLight.copy(alpha = 0.5f) else SaffronLight.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GST Registered Business?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGstRegistered) DeepNavyBlue else SaffronOrange
                                )
                                Text(
                                    text = if (isGstRegistered) "Yes — Enable CGST/SGST/IGST & HSN" else "No — Simple Non-GST Billing Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }
                            Switch(
                                checked = isGstRegistered,
                                onCheckedChange = { isGstRegistered = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = DeepNavyBlue,
                                    uncheckedThumbColor = PureWhite,
                                    uncheckedTrackColor = SaffronOrange
                                ),
                                modifier = Modifier.testTag("gst_registered_toggle")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        if (!isGstRegistered) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Non-GST Mode Activated: Invoices show only Item, Qty, Rate, and Amount. All tax columns, HSN code fields, and GST returns are cleanly hidden for simpler billing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = DeepNavyBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "GST Registered Mode Activated: Invoices show CGST, SGST, IGST columns, HSN codes, and auto-tax calculation based on state codes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700,
                                        lineHeight = 16.sp
                                    )
                                }

                                OutlinedTextField(
                                    value = gstin,
                                    onValueChange = { input ->
                                        val cleaned = input.uppercase().trim().take(15)
                                        gstin = cleaned
                                        if (cleaned.length >= 10 && pan.isBlank()) {
                                            pan = cleaned.substring(2, kotlin.math.min(12, cleaned.length))
                                        }
                                    },
                                    label = { Text("GSTIN (15 characters) *") },
                                    placeholder = { Text("e.g. 27AAAAA0000A1Z5") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_gstin"),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters
                                    ),
                                    supportingText = {
                                        Text(
                                            text = if (gstin.length == 15) "Valid length (15-digits)" else "${gstin.length}/15 characters",
                                            color = if (gstin.length == 15) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )

                                OutlinedTextField(
                                    value = pan,
                                    onValueChange = { pan = it.uppercase().take(10) },
                                    label = { Text("PAN Number (Optional)") },
                                    placeholder = { Text("e.g. AAAAA0000A") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                                )

                                // Composition Scheme Option
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Composition Scheme?",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Pay flat turnover tax (1% trader / 6% service) without ITC",
                                                    fontSize = 11.sp,
                                                    color = Slate700
                                                )
                                            }
                                            Switch(
                                                checked = isCompositionScheme,
                                                onCheckedChange = { isCompositionScheme = it }
                                            )
                                        }

                                        if (isCompositionScheme) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable { compositionType = "TRADER" }
                                                ) {
                                                    RadioButton(
                                                        selected = compositionType == "TRADER",
                                                        onClick = { compositionType = "TRADER" },
                                                        colors = RadioButtonDefaults.colors(selectedColor = Emerald700)
                                                    )
                                                    Text("Trader (1%)", fontSize = 12.sp)
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable { compositionType = "SERVICE" }
                                                ) {
                                                    RadioButton(
                                                        selected = compositionType == "SERVICE",
                                                        onClick = { compositionType = "SERVICE" },
                                                        colors = RadioButtonDefaults.colors(selectedColor = Emerald700)
                                                    )
                                                    Text("Services (6%)", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Business Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Business Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Shop / Business Name *") },
                            placeholder = { Text("e.g. Mali Kirana & General Stores") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_shop_name")
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Proprietor Name") },
                            placeholder = { Text("e.g. Vittal Mali") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number (+91) *") },
                            placeholder = { Text("e.g. 9876543210") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_phone")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email (Optional)") },
                            placeholder = { Text("e.g. business@gmail.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Location Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Location & Jurisdiction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // State Dropdown (Defaults to Maharashtra 27)
                        ExposedDropdownMenuBox(
                            expanded = isStateExpanded,
                            onExpandedChange = { isStateExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedStateWithCode,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("State & GST State Code *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStateExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isStateExpanded,
                                onDismissRequest = { isStateExpanded = false }
                            ) {
                                indianStates.forEach { stateItem ->
                                    DropdownMenuItem(
                                        text = { Text(stateItem) },
                                        onClick = {
                                            selectedStateWithCode = stateItem
                                            isStateExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City / Town *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { pincode = it.take(6) },
                                label = { Text("PIN Code") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address / Shop Address") },
                            placeholder = { Text("e.g. Shop No. 12, Main Market") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Bank & UPI Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Payment & Bank Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prints dynamic UPI QR on invoices for instant payment collection via PhonePe, GPay, Paytm",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it.trim() },
                            label = { Text("UPI ID / VPA") },
                            placeholder = { Text("e.g. yourshop@okhdfcbank") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            placeholder = { Text("e.g. State Bank of India") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it.trim() },
                                label = { Text("Account No.") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase().trim().take(11) },
                                label = { Text("IFSC Code") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Error Message if any
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (shopName.isBlank()) {
                            errorMessage = "Please enter your Shop or Business Name"
                            return@Button
                        }
                        if (isGstRegistered && gstin.length < 15) {
                            errorMessage = "GSTIN must be 15 alphanumeric characters"
                            return@Button
                        }

                        val stateName = selectedStateWithCode.substringBefore(" (").trim()
                        val stateCodeStr = selectedStateWithCode.substringAfter("(").substringBefore(")").trim()

                        val updated = currentProfile.copy(
                            shopName = shopName.trim(),
                            ownerName = ownerName.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            isGstRegistered = isGstRegistered,
                            gstin = if (isGstRegistered) gstin.trim().uppercase() else "",
                            pan = pan.trim().uppercase(),
                            isCompositionScheme = isCompositionScheme,
                            compositionType = compositionType,
                            state = stateName,
                            stateCode = stateCodeStr,
                            city = city.trim(),
                            address = address.trim(),
                            pincode = pincode.trim(),
                            upiId = upiId.trim(),
                            bankName = bankName.trim(),
                            accountNumber = accountNumber.trim(),
                            ifscCode = ifscCode.trim(),
                            appLanguage = selectedLanguage,
                            hasCompletedOnboarding = true
                        )

                        onSaveProfile(updated)
                        onDismiss?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_business_setup_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                ) {
                    Text(
                        text = if (isInitialOnboarding) "Complete Setup & Launch HisabPro" else "Save Business Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}
