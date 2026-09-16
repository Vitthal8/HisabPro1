package com.hisabpro.app.ui.party

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
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyTag
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartyDialog(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    partyToEdit: Party? = null,
    onSave: (
        name: String,
        phone: String,
        address: String,
        gstin: String,
        type: PartyType,
        tag: PartyTag
    ) -> Unit
) {
    var name by remember(partyToEdit) { mutableStateOf(partyToEdit?.name ?: "") }
    var phone by remember(partyToEdit) { mutableStateOf(partyToEdit?.phone ?: "") }
    var address by remember(partyToEdit) { mutableStateOf(partyToEdit?.address ?: "") }
    var gstin by remember(partyToEdit) { mutableStateOf(partyToEdit?.gstin ?: "") }
    var partyType by remember(partyToEdit) { mutableStateOf(partyToEdit?.type ?: PartyType.CUSTOMER) }
    var partyTag by remember(partyToEdit) { mutableStateOf(partyToEdit?.tag ?: PartyTag.REGULAR) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                    text = if (partyToEdit != null) "Edit Party / Contact" else "Add Party / Contact",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_add_party_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close dialog"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Party Type Toggle (Customer vs Supplier)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (partyType == PartyType.CUSTOMER) Emerald700 else Color.Transparent
                        )
                        .clickable { partyType = PartyType.CUSTOMER }
                        .padding(vertical = 12.dp)
                        .testTag("select_party_customer"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Customer (Client)",
                        fontWeight = FontWeight.Bold,
                        color = if (partyType == PartyType.CUSTOMER) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (partyType == PartyType.SUPPLIER) Slate700 else Color.Transparent
                        )
                        .clickable { partyType = PartyType.SUPPLIER }
                        .padding(vertical = 12.dp)
                        .testTag("select_party_supplier"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Supplier (Vendor)",
                        fontWeight = FontWeight.Bold,
                        color = if (partyType == PartyType.SUPPLIER) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text(if (partyType == PartyType.CUSTOMER) "Customer Name *" else "Supplier / Firm Name *") },
                placeholder = { Text("e.g. Ramesh Kumar, Om Enterprises") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("party_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    errorMessage = null
                },
                label = { Text("Phone / WhatsApp Number *") },
                placeholder = { Text("e.g. 9876543210") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("party_phone_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Address (optional)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Billing Address (Optional)") },
                placeholder = { Text("Shop no, Street, City") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("party_address_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // GSTIN (optional)
            OutlinedTextField(
                value = gstin,
                onValueChange = { gstin = it.uppercase() },
                label = { Text("GSTIN (Optional)") },
                placeholder = { Text("e.g. 27AAAAA0000A1Z5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("party_gstin_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tag Selection (Regular / Occasional / Blocked)
            Text(
                text = "Account Tag",
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
                PartyTag.entries.forEach { tag ->
                    val isSelected = partyTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = { partyTag = tag },
                        label = { Text(tag.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (tag) {
                                PartyTag.REGULAR -> Color(0xFFE0F2FE)
                                PartyTag.OCCASIONAL -> Color(0xFFFEF3C7)
                                PartyTag.BLOCKED -> Color(0xFFFEE4E2)
                            },
                            selectedLabelColor = when (tag) {
                                PartyTag.REGULAR -> Color(0xFF0369A1)
                                PartyTag.OCCASIONAL -> Color(0xFFB45309)
                                PartyTag.BLOCKED -> ExpenseRed
                            }
                        ),
                        modifier = Modifier.testTag("tag_chip_${tag.name}")
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMessage = "Please enter party name"
                        return@Button
                    }
                    if (phone.trim().isEmpty()) {
                        errorMessage = "Please enter contact phone number"
                        return@Button
                    }

                    onSave(
                        name.trim(),
                        phone.trim(),
                        address.trim(),
                        gstin.trim(),
                        partyType,
                        partyTag
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_party_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald700
                )
            ) {
                Text(
                    text = if (partyToEdit != null) "Update Party Details" else "Save Party",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
        }
    }
}
