package com.hisabpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Material 3 confirmation dialog for permanently deleting a company/business profile.
 * Requires explicit user confirmation by typing the exact company name and checking the acknowledgment box.
 */
@Composable
fun DeleteBusinessDialog(
    businessName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var typedName by remember { mutableStateOf("") }
    var isAcknowledged by remember { mutableStateOf(false) }

    val isConfirmEnabled = remember(typedName, businessName, isAcknowledged) {
        isAcknowledged && typedName.trim() == businessName.trim()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(
                    text = "Delete Company Profile?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Warning Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Permanent Data Loss Warning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Deleting \"$businessName\" will permanently erase all associated invoices, line items, inventory, expenses, payments, parties, and ledger entries.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Checkbox Acknowledgment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isAcknowledged,
                        onCheckedChange = { isAcknowledged = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("chk_acknowledge_delete")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "I understand all invoices, expenses, and ledger entries for this company will be permanently erased.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Typed Confirmation Input
                Column {
                    Text(
                        text = "To confirm, type \"$businessName\" below:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = typedName,
                        onValueChange = { typedName = it },
                        placeholder = { Text(businessName, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_delete_name")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isConfirmEnabled) {
                        onConfirm()
                    }
                },
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_delete_company")
            ) {
                Text("Delete Company Permanently", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
