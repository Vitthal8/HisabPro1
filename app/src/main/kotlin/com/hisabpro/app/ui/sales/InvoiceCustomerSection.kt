package com.hisabpro.app.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.R
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Slate100

/**
 * Customer Selection and Party Details Input Section for Invoice Creation.
 */
@Composable
fun InvoiceCustomerSection(
    isQuickSaleMode: Boolean,
    parties: List<Party>,
    selectedParty: Party?,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    customerGstin: String,
    isGstRegistered: Boolean,
    selectedType: InvoiceType,
    businessProfile: BusinessProfile,
    isEditing: Boolean,
    onPartySelected: (Party) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onCustomerAddressChange: (String) -> Unit,
    onCustomerGstinChange: (String) -> Unit,
    onGstModeUpdated: (GstMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPartyDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isQuickSaleMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Emerald700)
                    Column {
                        Text(
                            text = stringResource(R.string.cash_customer),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Customer profile not required for cash bills.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Pick existing party dropdown button
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showPartyDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedParty?.name ?: stringResource(R.string.select_customer_optional),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showPartyDropdown,
                    onDismissRequest = { showPartyDropdown = false }
                ) {
                    parties.forEach { party ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(party.name, fontWeight = FontWeight.SemiBold)
                                    if (party.phone.isNotBlank()) {
                                        Text(party.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                onPartySelected(party)
                                showPartyDropdown = false

                                if (isGstRegistered && selectedType == InvoiceType.TAX_INVOICE && !isEditing) {
                                    val mode = com.hisabpro.app.util.IndianAccountingFormat.determineGstMode(
                                        businessState = businessProfile.state,
                                        businessStateCode = businessProfile.stateCode,
                                        businessGstin = businessProfile.gstin,
                                        customerGstin = party.gstin,
                                        customerAddress = party.address
                                    )
                                    onGstModeUpdated(mode)
                                }
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                label = { Text("Customer / Party Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_customer_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onCustomerPhoneChange,
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_customer_phone"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (isGstRegistered && selectedType == InvoiceType.TAX_INVOICE) {
                    OutlinedTextField(
                        value = customerGstin,
                        onValueChange = { input ->
                            val upper = input.uppercase()
                            onCustomerGstinChange(upper)
                            if (!isEditing && upper.length >= 2) {
                                val mode = com.hisabpro.app.util.IndianAccountingFormat.determineGstMode(
                                    businessState = businessProfile.state,
                                    businessStateCode = businessProfile.stateCode,
                                    businessGstin = businessProfile.gstin,
                                    customerGstin = upper,
                                    customerAddress = customerAddress
                                )
                                onGstModeUpdated(mode)
                            }
                        },
                        label = { Text("GSTIN (Optional)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_customer_gstin"),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = customerAddress,
                onValueChange = { input ->
                    onCustomerAddressChange(input)
                    if (isGstRegistered && selectedType == InvoiceType.TAX_INVOICE && !isEditing && customerGstin.isBlank()) {
                        val mode = com.hisabpro.app.util.IndianAccountingFormat.determineGstMode(
                            businessState = businessProfile.state,
                            businessStateCode = businessProfile.stateCode,
                            businessGstin = businessProfile.gstin,
                            customerGstin = customerGstin,
                            customerAddress = input
                        )
                        onGstModeUpdated(mode)
                    }
                },
                label = { Text("Billing Address (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_customer_address"),
                maxLines = 2,
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
