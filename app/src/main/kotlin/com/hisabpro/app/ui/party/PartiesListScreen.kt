package com.hisabpro.app.ui.party

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.PartyTag
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.payments.PaymentDirection
import com.hisabpro.app.ui.payments.RecordPaymentSheet
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.ExpenseRedContainer
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.IncomeGreenContainer
import androidx.compose.ui.res.stringResource
import com.hisabpro.app.R
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate700
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesListScreen(
    viewModel: PartyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddPartySheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    // Payment & Receipt Sheet State
    var showRecordPaymentSheet by remember { mutableStateOf(false) }
    var recordPaymentDirection by remember { mutableStateOf(PaymentDirection.RECEIPT_IN) }
    var quickPaymentPartyId by remember { mutableStateOf<String?>(null) }
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val businessProfile by settingsRepo.profile.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // If a party is selected, show their full PartyKhataScreen
    if (uiState.selectedParty != null) {
        PartyKhataScreen(
            partyWithBalance = uiState.selectedParty!!,
            entries = uiState.selectedPartyEntries,
            onBack = { viewModel.selectParty(null) },
            onAddEntry = { amount, type, dateMillis, billNumber, note ->
                viewModel.addKhataEntry(
                    partyId = uiState.selectedParty!!.party.id,
                    amount = amount,
                    type = type,
                    dateMillis = dateMillis,
                    billNumber = billNumber,
                    note = note
                )
            },
            onDeleteEntry = { entryId ->
                viewModel.deleteKhataEntry(entryId)
            },
            onDeleteParty = { partyId ->
                viewModel.deleteParty(partyId)
            },
            onUpdateParty = { updatedParty ->
                viewModel.updateParty(updatedParty)
            },
            onRecordPayment = { direction, amount, mode, ref, notes, linkedInvId ->
                viewModel.recordPartyPayment(
                    partyId = uiState.selectedParty!!.party.id,
                    amount = amount,
                    direction = direction,
                    paymentMode = mode,
                    referenceNo = ref,
                    notes = notes,
                    linkedInvoiceId = linkedInvId
                )
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald700,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.parties),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("toggle_party_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search parties"
                        )
                    }

                    var showSortMenu by remember { mutableStateOf(false) }

                    // Sort button
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("btn_sort_parties")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort parties"
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        PartySortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.sortOption == option) Emerald700 else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Export CSV
                    IconButton(
                        onClick = { viewModel.exportAllPartiesCsv(context) },
                        modifier = Modifier.testTag("btn_export_parties_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Khata summary CSV"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.resetToDemo() },
                        modifier = Modifier.testTag("reset_parties_demo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset demo parties"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        showAddPartySheet = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Party", fontWeight = FontWeight.Bold) },
                containerColor = Emerald700,
                contentColor = PureWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_party")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by name, phone, or GSTIN...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("party_search_input")
                    )
                }
            }

            // Summary Card: Total Receivable & Total Payable
            item {
                PartiesSummaryCard(
                    receivable = uiState.totalReceivable,
                    payable = uiState.totalPayable,
                    onReceive = {
                        recordPaymentDirection = PaymentDirection.RECEIPT_IN
                        showRecordPaymentSheet = true
                    },
                    onPay = {
                        recordPaymentDirection = PaymentDirection.PAYMENT_OUT
                        showRecordPaymentSheet = true
                    }
                )
            }

            // Filter Chips (Type & Tag)
            item {
                PartyFilterSection(
                    selectedType = uiState.typeFilter,
                    onTypeSelected = { viewModel.setTypeFilter(it) },
                    selectedTag = uiState.tagFilter,
                    onTagSelected = { viewModel.setTagFilter(it) }
                )
            }

            // Header Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Parties (${uiState.filteredParties.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.typeFilter != null || uiState.tagFilter != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Filtered",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Parties List
            if (uiState.filteredParties.isEmpty()) {
                item {
                    EmptyPartiesPlaceholder(
                        isFiltered = uiState.searchQuery.isNotEmpty() ||
                                uiState.typeFilter != null ||
                                uiState.tagFilter != null
                    )
                }
            } else {
                items(
                    items = uiState.filteredParties,
                    key = { it.party.id }
                ) { partyWithBalance ->
                    PartyCard(
                        partyWithBalance = partyWithBalance,
                        onClick = {
                            viewModel.selectParty(partyWithBalance.party.id)
                        },
                        onQuickPayment = {
                            quickPaymentPartyId = partyWithBalance.party.id
                            recordPaymentDirection = if (partyWithBalance.isReceivable) PaymentDirection.RECEIPT_IN else PaymentDirection.PAYMENT_OUT
                            showRecordPaymentSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showAddPartySheet) {
        AddPartyDialog(
            sheetState = sheetState,
            onDismiss = { showAddPartySheet = false },
            onSave = { name, phone, address, gstin, type, tag ->
                viewModel.addParty(
                    name = name,
                    phone = phone,
                    address = address,
                    gstin = gstin,
                    type = type,
                    tag = tag
                )
            }
        )
    }

    if (showRecordPaymentSheet) {
        RecordPaymentSheet(
            parties = uiState.parties.map { it.party },
            initialDirection = recordPaymentDirection,
            initialPartyId = quickPaymentPartyId,
            merchantUpiId = businessProfile.upiId,
            merchantName = businessProfile.shopName.ifBlank { "HisabPro Merchant" },
            sheetState = paymentSheetState,
            onDismiss = {
                showRecordPaymentSheet = false
                quickPaymentPartyId = null
            },
            onSavePayment = { data ->
                viewModel.recordPartyPayment(
                    partyId = data.partyId,
                    amount = data.amount,
                    direction = data.direction,
                    paymentMode = data.paymentMode,
                    referenceNo = data.referenceNo,
                    notes = data.notes,
                    linkedInvoiceId = data.linkedInvoiceId
                )
                quickPaymentPartyId = null
            }
        )
    }
}

@Composable
private fun PartiesSummaryCard(
    receivable: Double,
    payable: Double,
    onReceive: () -> Unit = {},
    onPay: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parties_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Emerald800, Emerald900)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // You'll Get (Receivable)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onReceive() },
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YOU'LL GET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = IncomeGreenContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IncomeGreen.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "+ Receive",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${HisabViewModel.formatAmount(receivable)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Customer dues",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                // You'll Give (Payable)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPay() },
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YOU'LL GIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = ExpenseRedContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ExpenseRed.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "- Pay",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${HisabViewModel.formatAmount(payable)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Supplier dues",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyFilterSection(
    selectedType: PartyType?,
    onTypeSelected: (PartyType?) -> Unit,
    selectedTag: PartyTag?,
    onTagSelected: (PartyTag?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Type Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("All Parties") },
                modifier = Modifier.testTag("filter_all_parties")
            )
            FilterChip(
                selected = selectedType == PartyType.CUSTOMER,
                onClick = { onTypeSelected(if (selectedType == PartyType.CUSTOMER) null else PartyType.CUSTOMER) },
                label = { Text("Customers") },
                modifier = Modifier.testTag("filter_customers")
            )
            FilterChip(
                selected = selectedType == PartyType.SUPPLIER,
                onClick = { onTypeSelected(if (selectedType == PartyType.SUPPLIER) null else PartyType.SUPPLIER) },
                label = { Text("Suppliers") },
                modifier = Modifier.testTag("filter_suppliers")
            )
        }

        // Tag Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tags:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            PartyTag.entries.forEach { tag ->
                val isSelected = selectedTag == tag
                FilterChip(
                    selected = isSelected,
                    onClick = { onTagSelected(if (isSelected) null else tag) },
                    label = { Text(tag.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (tag) {
                            PartyTag.REGULAR -> Color(0xFFE0F2FE)
                            PartyTag.OCCASIONAL -> Color(0xFFFEF3C7)
                            PartyTag.BLOCKED -> ExpenseRedContainer
                        },
                        selectedLabelColor = when (tag) {
                            PartyTag.REGULAR -> Color(0xFF0369A1)
                            PartyTag.OCCASIONAL -> Color(0xFFB45309)
                            PartyTag.BLOCKED -> ExpenseRed
                        }
                    ),
                    modifier = Modifier.testTag("filter_tag_${tag.name}")
                )
            }
        }
    }
}

@Composable
private fun EmptyPartiesPlaceholder(isFiltered: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isFiltered) "No Matching Parties" else "No Parties Added",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFiltered) {
                    "Try adjusting your search query or filters."
                } else {
                    "Tap '+ Add Party' below to add your first customer or supplier."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
