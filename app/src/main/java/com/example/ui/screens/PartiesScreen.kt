package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.ui.AccountingViewModel
import com.example.ui.components.BalanceBadge
import com.example.ui.components.PartyLedgerDialog
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesScreen(
    viewModel: AccountingViewModel,
    onNavigateToReceivePayment: (Long) -> Unit
) {
    val context = LocalContext.current
    val allParties by viewModel.parties.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Customers, 1 = Suppliers
    var searchQuery by remember { mutableStateOf("") }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var selectedPartyForLedger by remember { mutableStateOf<Party?>(null) }

    val filteredParties = allParties.filter { party ->
        val matchesTab = when (selectedTab) {
            0 -> party.type == PartyType.CUSTOMER || party.type == PartyType.BOTH
            else -> party.type == PartyType.SUPPLIER || party.type == PartyType.BOTH
        }
        val matchesSearch = party.name.contains(searchQuery, ignoreCase = true) ||
                party.phone.contains(searchQuery)
        matchesTab && matchesSearch
    }

    val totalCustomerReceivables = allParties
        .filter { (it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH) && it.currentBalance > 0 }
        .sumOf { it.currentBalance }

    val totalSupplierPayables = allParties
        .filter { (it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH) && it.currentBalance < 0 }
        .sumOf { kotlin.math.abs(it.currentBalance) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPartyDialog = true },
                containerColor = DeepNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_party")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Party")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("parties_screen")
        ) {
            // Header Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DeepNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Customers (${allParties.count { it.type != PartyType.SUPPLIER }})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_customers")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Suppliers (${allParties.count { it.type != PartyType.CUSTOMER }})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_suppliers")
                )
            }

            // Overview Summary Strip
            Surface(
                color = if (selectedTab == 0) DebitRed.copy(alpha = 0.08f) else CreditGreen.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "Total Due from Customers:" else "Total to Pay to Suppliers:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (selectedTab == 0)
                            "${IndianAccountingUtils.formatCurrency(totalCustomerReceivables)} Dr"
                        else
                            "${IndianAccountingUtils.formatCurrency(totalSupplierPayables)} Cr",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) DebitRed else CreditGreen
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer / supplier by name or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_party_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Parties List
            if (filteredParties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No party matches '$searchQuery'" else "No parties in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { showAddPartyDialog = true }) {
                            Text("+ Add Party Now")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredParties) { party ->
                        PartyCard(
                            party = party,
                            onCardClick = { selectedPartyForLedger = party },
                            onCallClick = {
                                if (party.phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "No phone number saved for ${party.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPaymentClick = {
                                onNavigateToReceivePayment(party.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Party Ledger Dialog
    selectedPartyForLedger?.let { party ->
        PartyLedgerDialog(
            party = party,
            viewModel = viewModel,
            onDismiss = { selectedPartyForLedger = null },
            onReceivePaymentClick = {
                onNavigateToReceivePayment(party.id)
            }
        )
    }

    // Add Party Dialog
    if (showAddPartyDialog) {
        AddPartyDialog(
            defaultType = if (selectedTab == 0) PartyType.CUSTOMER else PartyType.SUPPLIER,
            onDismiss = { showAddPartyDialog = false },
            onSave = { name, phone, address, gstin, type, openingBal, isReceivable ->
                viewModel.addParty(name, phone, address, gstin, type, openingBal, isReceivable)
                showAddPartyDialog = false
                Toast.makeText(context, "$name added successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun PartyCard(
    party: Party,
    onCardClick: () -> Unit,
    onCallClick: () -> Unit,
    onPaymentClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("party_card_${party.id}")
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DeepNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = party.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = party.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (party.phone.isNotBlank()) {
                    Text(
                        text = "📱 ${party.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (party.address.isNotBlank()) {
                    Text(
                        text = party.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                BalanceBadge(balance = party.currentBalance)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (party.phone.isNotBlank()) {
                        IconButton(
                            onClick = onCallClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = DeepNavy, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = onCardClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Ledger", tint = Saffron, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddPartyDialog(
    defaultType: PartyType,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, gstin: String, type: PartyType, openingBal: Double, isReceivable: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(defaultType) }
    var openingBalanceText by remember { mutableStateOf("0") }
    var isReceivable by remember { mutableStateOf(defaultType != PartyType.SUPPLIER) } // true = Dr (Customer owes), false = Cr (We owe)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Party (नवीन खातेदार)",
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party / Customer Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_party_name_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_party_phone_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("City / Address (e.g. Pune)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_party_address_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it.uppercase() },
                    label = { Text("GSTIN (Optional, 15 chars)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_party_gstin_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Party Type selection
                Text("Party Type:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = PartyType.CUSTOMER }
                    ) {
                        RadioButton(
                            selected = selectedType == PartyType.CUSTOMER,
                            onClick = { selectedType = PartyType.CUSTOMER },
                            colors = RadioButtonDefaults.colors(selectedColor = DeepNavy)
                        )
                        Text("Customer", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = PartyType.SUPPLIER }
                    ) {
                        RadioButton(
                            selected = selectedType == PartyType.SUPPLIER,
                            onClick = { selectedType = PartyType.SUPPLIER },
                            colors = RadioButtonDefaults.colors(selectedColor = DeepNavy)
                        )
                        Text("Supplier", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Opening Balance with Dr/Cr toggle
                Text("Opening Balance (प्रारंभिक शिल्लक):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = openingBalanceText,
                        onValueChange = { openingBalanceText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_party_opening_bal_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isReceivable = !isReceivable }
                    ) {
                        Button(
                            onClick = { isReceivable = !isReceivable },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReceivable) DebitRed else CreditGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isReceivable) "Dr (Receivable)" else "Cr (Payable)", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val bal = openingBalanceText.toDoubleOrNull() ?: 0.0
                    onSave(name, phone, address, gstin, selectedType, bal, isReceivable)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                modifier = Modifier.testTag("confirm_add_party_button")
            ) {
                Text("Save Party")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
