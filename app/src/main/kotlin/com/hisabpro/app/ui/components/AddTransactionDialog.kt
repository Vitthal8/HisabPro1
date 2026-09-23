package com.hisabpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    sheetState: SheetState,
    initialTransaction: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        dateMillis: Long,
        paymentMode: PaymentMode,
        note: String
    ) -> Unit
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var amountText by remember {
        mutableStateOf(
            initialTransaction?.amount?.let { amt ->
                if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } ?: ""
        )
    }
    var titleText by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTransaction?.category ?: Category.FOOD) }
    var selectedPaymentMode by remember { mutableStateOf(initialTransaction?.paymentMode ?: PaymentMode.ONLINE_UPI) }
    var noteText by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = initialTransaction != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Hisab Entry" else "New Hisab Entry",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sheet"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Toggle (Income / Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                // Expense Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent
                        )
                        .clickable {
                            type = TransactionType.EXPENSE
                            if (selectedCategory == Category.SALARY) {
                                selectedCategory = Category.FOOD
                            }
                        }
                        .padding(vertical = 12.dp)
                        .testTag("type_toggle_expense"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense (Debit)",
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.EXPENSE) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Income Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent
                        )
                        .clickable {
                            type = TransactionType.INCOME
                            if (selectedCategory == Category.FOOD) {
                                selectedCategory = Category.SALARY
                            }
                        }
                        .padding(vertical = 12.dp)
                        .testTag("type_toggle_income"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income (Credit)",
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.INCOME) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Amount (₹)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Amount Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 500, 1000, 2000, 5000, 10000).forEach { quickVal ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            val current = amountText.toDoubleOrNull() ?: 0.0
                            amountText = (current + quickVal).toInt().toString()
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = "+₹$quickVal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Party Name
            OutlinedTextField(
                value = titleText,
                onValueChange = {
                    titleText = it
                    errorMessage = null
                },
                label = { Text("Title / Description") },
                placeholder = { Text("e.g. Grocery store, Client payment, Dinner") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availableCategories = if (type == TransactionType.INCOME) {
                    listOf(Category.SALARY, Category.BUSINESS, Category.INVESTMENT, Category.OTHER)
                } else {
                    Category.entries.toList()
                }

                availableCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chip_category_${cat.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Mode
            Text(
                text = "Payment Mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMode.entries.forEach { mode ->
                    val isSelected = selectedPaymentMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPaymentMode = mode },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Optional Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (Optional)") },
                placeholder = { Text("Add transaction remarks or invoice numbers") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (titleText.trim().isEmpty()) {
                        errorMessage = "Please enter a title for this entry"
                        return@Button
                    }

                    onSave(
                        titleText.trim(),
                        amt,
                        type,
                        selectedCategory,
                        initialTransaction?.dateMillis ?: System.currentTimeMillis(),
                        selectedPaymentMode,
                        noteText.trim()
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                )
            ) {
                Text(
                    text = when {
                        isEditing -> "Save Changes"
                        type == TransactionType.EXPENSE -> "Record Expense"
                        else -> "Record Income"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
        }
    }
}
