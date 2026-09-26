package com.hisabpro.app.ui.reports

import android.widget.Toast
import com.hisabpro.app.util.ShareHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate700
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReconciliationState(
    val isReconciled: Boolean = false,
    val clearanceDateMillis: Long = System.currentTimeMillis(),
    val bankRefNumber: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankReconciliationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val txRepo = remember { TransactionRepository.getInstance(context) }
    val transactions by txRepo.transactions.collectAsStateWithLifecycle()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val businessProfile by settingsRepo.profile.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Uncleared, 2: Reconciled

    // In-memory reconciliation tracker for active transactions
    val reconciliationMap = remember {
        mutableStateMapOf<String, ReconciliationState>()
    }

    // Filter to bank/UPI/cheque transactions
    val bankTransactions = remember(transactions) {
        transactions.filter {
            it.paymentMode == PaymentMode.BANK_TRANSFER ||
            it.paymentMode == PaymentMode.ONLINE_UPI ||
            it.paymentMode == PaymentMode.CARD
        }
    }

    // Calculate balances
    val totalInflow = bankTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalOutflow = bankTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val bookBalance = totalInflow - totalOutflow

    val reconciledInflow = bankTransactions.filter { it.type == TransactionType.INCOME && (reconciliationMap[it.id]?.isReconciled == true) }.sumOf { it.amount }
    val reconciledOutflow = bankTransactions.filter { it.type == TransactionType.EXPENSE && (reconciliationMap[it.id]?.isReconciled == true) }.sumOf { it.amount }
    val reconciledBalance = reconciledInflow - reconciledOutflow
    val unclearedDifference = bookBalance - reconciledBalance

    val filteredList = remember(bankTransactions, selectedTab, reconciliationMap) {
        when (selectedTab) {
            1 -> bankTransactions.filter { reconciliationMap[it.id]?.isReconciled != true }
            2 -> bankTransactions.filter { reconciliationMap[it.id]?.isReconciled == true }
            else -> bankTransactions
        }
    }

    var selectedTxForReconciliation by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bank Reconciliation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "${businessProfile.bankName.ifBlank { "Primary Bank Account" }}",
                            fontSize = 11.sp,
                            color = PureWhite.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sb = StringBuilder()
                        sb.append("🏦 *BANK RECONCILIATION STATEMENT*\n")
                        sb.append("🏢 Business: ${businessProfile.shopName}\n")
                        sb.append("🏛️ Bank: ${businessProfile.bankName} (A/c: ${businessProfile.accountNumber})\n")
                        sb.append("📅 Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())}\n\n")
                        sb.append("• Book Balance (Ledger): ₹${HisabViewModel.formatAmount(bookBalance)}\n")
                        sb.append("• Reconciled Bank Balance: ₹${HisabViewModel.formatAmount(reconciledBalance)}\n")
                        sb.append("• Uncleared Float: ₹${HisabViewModel.formatAmount(unclearedDifference)}\n\n")
                        sb.append("Total Bank Transactions: ${bankTransactions.size}\n")
                        sb.append("Reconciled: ${bankTransactions.count { reconciliationMap[it.id]?.isReconciled == true }}\n")
                        sb.append("Pending Clearance: ${bankTransactions.count { reconciliationMap[it.id]?.isReconciled != true }}\n")
                        
                        ShareHelper.shareText(context, sb.toString(), "Share Reconciliation via", "Bank Reconciliation Statement")
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DeepNavyBlue)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_bank_recon_summary"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LEDGER BOOK BALANCE", fontSize = 11.sp, color = PureWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                                Text("₹${HisabViewModel.formatAmount(bookBalance)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald700.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("A/c: ${businessProfile.accountNumber.takeLast(4).padStart(8, '•')}", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = PureWhite.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Cleared in Bank", fontSize = 11.sp, color = PureWhite.copy(alpha = 0.7f))
                                Text("₹${HisabViewModel.formatAmount(reconciledBalance)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Uncleared / Float", fontSize = 11.sp, color = PureWhite.copy(alpha = 0.7f))
                                Text("₹${HisabViewModel.formatAmount(unclearedDifference)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SaffronOrange)
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All (${bankTransactions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Uncleared (${bankTransactions.count { reconciliationMap[it.id]?.isReconciled != true }})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) SaffronOrange else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Reconciled (${bankTransactions.count { reconciliationMap[it.id]?.isReconciled == true }})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selectedTab == 2) Emerald800 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }

            // Transaction List
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Emerald800, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No bank transactions found for this filter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("All your bank receipts and payments are fully reconciled.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredList) { tx ->
                    val recon = reconciliationMap[tx.id]
                    val isReconciled = recon?.isReconciled == true
                    val isIncome = tx.type == TransactionType.INCOME

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_recon_tx_${tx.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isIncome) Emerald800.copy(alpha = 0.12f) else Color(0xFFDC2626).copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isIncome) "Cr" else "Dr",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isIncome) Emerald800 else Color(0xFFDC2626)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tx.title.ifBlank { if (isIncome) "Payment Received" else "Bank Payment" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(tx.dateMillis))
                                    Text(
                                        text = "$dateStr • ${tx.paymentMode.name}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isReconciled && recon != null && recon.bankRefNumber.isNotBlank()) {
                                        Text(
                                            text = "UTR: ${recon.bankRefNumber}",
                                            fontSize = 10.sp,
                                            color = Emerald800,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isIncome) "+" else "-"}₹${HisabViewModel.formatAmount(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isIncome) Emerald800 else Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isReconciled) Emerald800.copy(alpha = 0.15f) else SaffronOrange.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable {
                                        selectedTxForReconciliation = tx
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isReconciled) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = if (isReconciled) Emerald800 else SaffronOrange,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isReconciled) "Reconciled" else "Mark Cleared",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReconciled) Emerald800 else SaffronOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reconcile Dialog
    if (selectedTxForReconciliation != null) {
        val tx = selectedTxForReconciliation!!
        val currentRecon = reconciliationMap[tx.id]
        var utrNo by remember { mutableStateOf(currentRecon?.bankRefNumber ?: "") }
        var isMarkedReconciled by remember { mutableStateOf(currentRecon?.isReconciled ?: true) }

        AlertDialog(
            onDismissRequest = { selectedTxForReconciliation = null },
            title = {
                Text(
                    text = "Bank Clearance / UTR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${tx.title} (₹${HisabViewModel.formatAmount(tx.amount)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = utrNo,
                        onValueChange = { utrNo = it },
                        label = { Text("Bank UTR / Transaction Ref No.") },
                        placeholder = { Text("e.g. UTR4928172901") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_bank_utr")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mark as Reconciled", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Switch(
                            checked = isMarkedReconciled,
                            onCheckedChange = { isMarkedReconciled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reconciliationMap[tx.id] = ReconciliationState(
                            isReconciled = isMarkedReconciled,
                            clearanceDateMillis = System.currentTimeMillis(),
                            bankRefNumber = utrNo.trim()
                        )
                        Toast.makeText(context, if (isMarkedReconciled) "Transaction reconciled with bank" else "Marked uncleared", Toast.LENGTH_SHORT).show()
                        selectedTxForReconciliation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedTxForReconciliation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
