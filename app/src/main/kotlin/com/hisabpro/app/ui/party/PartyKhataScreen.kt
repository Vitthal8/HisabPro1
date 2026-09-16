package com.hisabpro.app.ui.party

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.ExpenseRedContainer
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.IncomeGreenContainer
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.util.ShareHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyKhataScreen(
    partyWithBalance: PartyWithBalance,
    entries: List<KhataEntry>,
    onBack: () -> Unit,
    onAddEntry: (amount: Double, type: KhataEntryType, dateMillis: Long, billNumber: String, note: String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onDeleteParty: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val party = partyWithBalance.party
    val context = LocalContext.current

    var showAddEntrySheet by remember { mutableStateOf(false) }
    var entryTypeToAdd by remember { mutableStateOf(KhataEntryType.YOU_GAVE) }
    var showDeletePartyDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = party.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("khata_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to party list"
                        )
                    }
                },
                actions = {
                    // Call party
                    IconButton(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(callIntent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.testTag("call_party_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call party",
                            tint = Emerald700
                        )
                    }

                    // Share on WhatsApp
                    IconButton(
                        onClick = {
                            ShareHelper.shareBalanceStatement(
                                context = context,
                                party = party,
                                netBalance = partyWithBalance.netBalance
                            )
                        },
                        modifier = Modifier.testTag("share_khata_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share balance reminder on WhatsApp",
                            tint = IncomeGreen
                        )
                    }

                    // More Menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete Party", color = ExpenseRed) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ExpenseRed
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDeletePartyDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // High-visibility dual action ledger buttons
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // YOU GAVE Button (Red)
                    Button(
                        onClick = {
                            entryTypeToAdd = KhataEntryType.YOU_GAVE
                            showAddEntrySheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_you_gave"),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "YOU GAVE ₹",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = PureWhite
                        )
                    }

                    // YOU GOT Button (Green)
                    Button(
                        onClick = {
                            entryTypeToAdd = KhataEntryType.YOU_GOT
                            showAddEntrySheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_you_got"),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "YOU GOT ₹",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = PureWhite
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Party Info & Running Balance Card
            item {
                PartySummaryHeroCard(
                    partyWithBalance = partyWithBalance,
                    onShareWhatsApp = {
                        ShareHelper.shareBalanceStatement(
                            context = context,
                            party = party,
                            netBalance = partyWithBalance.netBalance
                        )
                    }
                )
            }

            // Ledger Entries Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Khata Ledger (${entries.size} entries)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Ledger Entries List
            if (entries.isEmpty()) {
                item {
                    EmptyKhataPlaceholder()
                }
            } else {
                items(
                    items = entries,
                    key = { it.id }
                ) { entry ->
                    KhataEntryCard(
                        entry = entry,
                        onDelete = { onDeleteEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showAddEntrySheet) {
        AddKhataEntryDialog(
            party = party,
            initialType = entryTypeToAdd,
            sheetState = sheetState,
            onDismiss = { showAddEntrySheet = false },
            onSave = { amount, type, dateMillis, billNumber, note ->
                onAddEntry(amount, type, dateMillis, billNumber, note)
            }
        )
    }

    if (showDeletePartyDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePartyDialog = false },
            title = { Text("Delete ${party.name}?") },
            text = { Text("This will permanently remove this party and all associated Khata ledger records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePartyDialog = false
                        onDeleteParty(party.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_party_btn")
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePartyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PartySummaryHeroCard(
    partyWithBalance: PartyWithBalance,
    onShareWhatsApp: () -> Unit
) {
    val party = partyWithBalance.party
    val statusLabel = partyWithBalance.getStatusLabel()
    val balanceText = "₹${HisabViewModel.formatAmount(partyWithBalance.dueAmount)}"

    val cardBgColor = when {
        partyWithBalance.isSettled -> MaterialTheme.colorScheme.surfaceVariant
        partyWithBalance.isReceivable -> IncomeGreenContainer
        else -> ExpenseRedContainer
    }

    val primaryTextColor = when {
        partyWithBalance.isSettled -> MaterialTheme.colorScheme.onSurface
        partyWithBalance.isReceivable -> Color(0xFF027A48)
        else -> Color(0xFFB42318)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("party_summary_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = statusLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primaryTextColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = balanceText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp
                        ),
                        color = primaryTextColor
                    )
                }

                PartyTagBadge(tag = party.tag)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Totals row: Total Gave vs Total Got
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PureWhite.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Gave (Debit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${HisabViewModel.formatAmount(partyWithBalance.totalGave)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Got (Credit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${HisabViewModel.formatAmount(partyWithBalance.totalGot)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = IncomeGreen
                        )
                    }
                }
            }

            if (party.address.isNotBlank() || party.gstin.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                if (party.address.isNotBlank()) {
                    Text(
                        text = "Address: ${party.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryTextColor.copy(alpha = 0.9f)
                    )
                }
                if (party.gstin.isNotBlank()) {
                    Text(
                        text = "GSTIN: ${party.gstin}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = primaryTextColor.copy(alpha = 0.9f)
                    )
                }
            }

            if (!partyWithBalance.isSettled) {
                Spacer(modifier = Modifier.height(14.dp))

                // WhatsApp Balance Share Reminder Button
                Button(
                    onClick = onShareWhatsApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("send_whatsapp_reminder_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send WhatsApp Reminder",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun KhataEntryCard(
    entry: KhataEntry,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isGave = entry.type == KhataEntryType.YOU_GAVE
    val amountColor = if (isGave) ExpenseRed else IncomeGreen
    val amountPrefix = if (isGave) "- ₹" else "+ ₹"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("khata_entry_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatEntryDate(entry.dateMillis),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (entry.billNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "#${entry.billNumber}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (entry.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${HisabViewModel.formatAmount(entry.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = amountColor
                )
                Text(
                    text = if (isGave) "You Gave" else "You Got",
                    style = MaterialTheme.typography.labelSmall,
                    color = amountColor
                )
            }

            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 6.dp)
                    .testTag("delete_entry_btn_${entry.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to remove this ${if (isGave) "You Gave" else "You Got"} entry of ₹${HisabViewModel.formatAmount(entry.amount)}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyKhataPlaceholder() {
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
            Text(
                text = "No Khata Transactions Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Use 'YOU GAVE' or 'YOU GOT' buttons below to start recording credit and payments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatEntryDate(millis: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
}
