package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
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
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.ui.AccountingViewModel
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DeepNavy
import com.example.util.IndianAccountingUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReceivedScreen(
    viewModel: AccountingViewModel,
    preselectedPartyId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val parties by viewModel.parties.collectAsState()

    var selectedParty by remember(parties, preselectedPartyId) {
        mutableStateOf(parties.find { it.id == preselectedPartyId } ?: parties.firstOrNull { it.currentBalance > 0 })
    }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.UPI) }
    var referenceNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val currentDue = selectedParty?.currentBalance ?: 0.0
    val remainingDue = (currentDue - amount).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Record Payment Received (जमा पावती)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreditGreen)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("payment_received_screen")
        ) {
            // Customer Selector
            Text(
                text = "Received From (कोणाकडून मिळाले/किससे मिला) *",
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
                    placeholder = { Text("Select customer...") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("payment_customer_dropdown"),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = partyDropdownExpanded,
                    onDismissRequest = { partyDropdownExpanded = false }
                ) {
                    parties.forEach { p ->
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

            // Customer outstanding status card
            selectedParty?.let { party ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                        Column {
                            Text("Current Ledger Due", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = IndianAccountingUtils.formatBalanceDrCr(party.currentBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (party.currentBalance > 0) DebitRed else CreditGreen
                            )
                        }
                        if (party.currentBalance > 0) {
                            Button(
                                onClick = {
                                    amountText = party.currentBalance.toInt().toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("fill_full_due_button")
                            ) {
                                Text("Full Amount (${IndianAccountingUtils.formatCurrency(party.currentBalance)})", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Received Input
            Text(
                text = "Amount Received (जमा रक्कम) *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                placeholder = { Text("Enter amount in ₹...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Text(
                        text = "₹",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("received_amount_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Quick chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(500.0, 1000.0, 2000.0, 5000.0).forEach { quickAmt ->
                    FilterChip(
                        selected = amount == quickAmt,
                        onClick = { amountText = quickAmt.toInt().toString() },
                        label = { Text("₹${quickAmt.toInt()}", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Mode
            Text(
                text = "Payment Mode (कसे मिळाले)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(PaymentMode.UPI, PaymentMode.CASH, PaymentMode.BANK, PaymentMode.CHEQUE).forEach { mode ->
                    FilterChip(
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode },
                        label = { Text(mode.name, fontSize = 12.sp) },
                        modifier = Modifier.testTag("mode_${mode.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = referenceNo,
                onValueChange = { referenceNo = it },
                label = { Text("Reference / UPI / Cheque Number") },
                placeholder = { Text("e.g. UPI Ref 329104829 or Cheque #00412") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reference_no_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Narration") },
                placeholder = { Text("e.g. Received via GPay for past bill") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_notes_input"),
                shape = RoundedCornerShape(10.dp),
                maxLines = 2
            )

            if (amount > 0 && selectedParty != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = CreditGreenLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✓ Ledger will be credited by ${IndianAccountingUtils.formatCurrency(amount)}",
                            fontWeight = FontWeight.Bold,
                            color = CreditGreen,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Remaining balance after payment: ${IndianAccountingUtils.formatCurrency(remainingDue)} Dr",
                            color = CreditGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedParty == null) {
                        Toast.makeText(context, "Please select a customer!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (amount <= 0) {
                        Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.recordPayment(
                        partyId = selectedParty!!.id,
                        partyName = selectedParty!!.name,
                        amount = amount,
                        mode = paymentMode,
                        referenceNo = referenceNo,
                        notes = notes,
                        onSuccess = {
                            Toast.makeText(context, "Payment of ${IndianAccountingUtils.formatCurrency(amount)} recorded!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_record_payment_button")
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Record Payment (${IndianAccountingUtils.formatCurrency(amount)})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
