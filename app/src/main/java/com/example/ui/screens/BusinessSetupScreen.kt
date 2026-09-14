package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AccountingViewModel
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.AppLanguage
import com.example.util.IndianAccountingUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    viewModel: AccountingViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentBusiness by viewModel.business.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()

    var name by remember(currentBusiness) { mutableStateOf(currentBusiness?.name ?: "Mali General Stores") }
    var phone by remember(currentBusiness) { mutableStateOf(currentBusiness?.phone ?: "9876543210") }
    var address by remember(currentBusiness) { mutableStateOf(currentBusiness?.address ?: "Shop No. 4, Shivaji Chowk, Pune") }
    var state by remember(currentBusiness) { mutableStateOf(currentBusiness?.state ?: "Maharashtra") }

    // CRITICAL MANDATE: "GST Registered Business? Yes / No" toggle
    var gstEnabled by remember(currentBusiness) { mutableStateOf(currentBusiness?.gstEnabled ?: false) }
    var gstin by remember(currentBusiness) { mutableStateOf(currentBusiness?.gstin ?: "") }
    var pan by remember(currentBusiness) { mutableStateOf(currentBusiness?.pan ?: "") }
    var isCompositionScheme by remember(currentBusiness) { mutableStateOf(currentBusiness?.isCompositionScheme ?: false) }

    val indianStates = listOf("Maharashtra", "Gujarat", "Karnataka", "Goa", "Madhya Pradesh", "Rajasthan", "Delhi", "Telangana", "Other")
    var stateDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Business Profile & GST Setup", fontWeight = FontWeight.Bold, color = Color.White)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("business_setup_screen")
        ) {
            // Language Selection
            Text("Preferred Language (भाषा निवडा / चुनें)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.values().forEach { lang ->
                    FilterChip(
                        selected = currentLang == lang.code,
                        onClick = { viewModel.setLanguage(lang.code) },
                        label = { Text(lang.displayName, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shop / Business Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Business / Shop Name (दुकान/व्यवसायाचे नाव) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_name_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Business Mobile / WhatsApp Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_phone_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Address
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Shop Address / Market Location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_address_input"),
                shape = RoundedCornerShape(10.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // State Selector
            ExposedDropdownMenuBox(
                expanded = stateDropdownExpanded,
                onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("State (राज्य)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("state_dropdown"),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = stateDropdownExpanded,
                    onDismissRequest = { stateDropdownExpanded = false }
                ) {
                    indianStates.forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st) },
                            onClick = {
                                state = st
                                stateDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CRITICAL CORE REQUIREMENT: GST Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (gstEnabled) DeepNavy.copy(alpha = 0.08f) else Color(0xFFEEEEEE)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
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
                                color = if (gstEnabled) DeepNavy else Color.Black
                            )
                            Text(
                                text = if (gstEnabled) "Yes (Tax Invoices with CGST/SGST/HSN)" else "No (Simple bills without taxes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (gstEnabled) DeepNavy else Color.DarkGray
                            )
                        }
                        Switch(
                            checked = gstEnabled,
                            onCheckedChange = { gstEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavy,
                                checkedTrackColor = DeepNavy.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("setup_gst_toggle")
                        )
                    }

                    // Conditional GST Fields
                    if (gstEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it.uppercase() },
                            label = { Text("GSTIN (15 Digits e.g. 27AAAAA0000A1Z5)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_gstin_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pan,
                            onValueChange = { pan = it.uppercase() },
                            label = { Text("PAN Number (10 Digits)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_pan_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Composition Scheme?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "1% flat tax for traders, 6% for service providers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isCompositionScheme,
                                onCheckedChange = { isCompositionScheme = it }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 In Non-GST mode, all GST columns, HSN codes, and tax calculations are hidden to keep billing ultra-fast and simple for your shop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Details Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Business name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveBusiness(
                        name = name,
                        phone = phone,
                        address = address,
                        state = state,
                        gstEnabled = gstEnabled,
                        gstin = gstin,
                        pan = pan,
                        isCompositionScheme = isCompositionScheme
                    )
                    Toast.makeText(context, "Business Profile Saved!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_business_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Business Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
