package com.hisabpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSwitcherSheet(
    sheetState: SheetState,
    businesses: List<BusinessProfile>,
    activeBusiness: BusinessProfile,
    onSelectBusiness: (BusinessProfile) -> Unit,
    onAddNewBusiness: () -> Unit,
    onEditBusiness: (BusinessProfile) -> Unit,
    onDeleteBusiness: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sheet_business_switcher")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SaffronOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = SaffronOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Switch Business / Shop",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${businesses.size} registered company profile(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(businesses) { biz ->
                    val isActive = biz.shopName == activeBusiness.shopName
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive)
                                Emerald700.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isActive) 1.5.dp else 0.5.dp,
                                color = if (isActive) Emerald700 else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectBusiness(biz)
                                onDismiss()
                            }
                            .testTag("biz_item_${biz.shopName}")
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
                                    color = if (isActive) Emerald700 else DeepNavyBlue,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = biz.shopName.take(1).uppercase(),
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = biz.shopName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Emerald700,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    color = PureWhite,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    val gstTag = if (biz.isGstRegistered && biz.gstin.isNotBlank()) "GST: ${biz.gstin}" else "Non-GST Business"
                                    Text(
                                        text = "${biz.city.ifBlank { "India" }} • $gstTag",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditBusiness(biz) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Business",
                                        tint = DeepNavyBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (businesses.size > 1 && !isActive) {
                                    IconButton(
                                        onClick = { onDeleteBusiness(biz.shopName) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Business",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onDismiss()
                    onAddNewBusiness()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_add_another_business")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Another Business / Branch", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AddEditBusinessDialog(
    initialProfile: BusinessProfile? = null,
    onSave: (BusinessProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var shopName by remember { mutableStateOf(initialProfile?.shopName ?: "") }
    var ownerName by remember { mutableStateOf(initialProfile?.ownerName ?: "") }
    var phone by remember { mutableStateOf(initialProfile?.phone ?: "") }
    var email by remember { mutableStateOf(initialProfile?.email ?: "") }
    var isGst by remember { mutableStateOf(initialProfile?.isGstRegistered ?: false) }
    var gstin by remember { mutableStateOf(initialProfile?.gstin ?: "") }
    var address by remember { mutableStateOf(initialProfile?.address ?: "") }
    var city by remember { mutableStateOf(initialProfile?.city ?: "") }
    var state by remember { mutableStateOf(initialProfile?.state ?: "Maharashtra") }
    var upiId by remember { mutableStateOf(initialProfile?.upiId ?: "") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProfile == null) "Add New Company / Shop" else "Edit Business Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it; errorMsg = null },
                    label = { Text("Shop / Business Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_biz_name")
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("GST Registered Business?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(if (isGst) "Tax Invoices with GSTIN" else "Non-GST Bills", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isGst,
                        onCheckedChange = { isGst = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = SaffronOrange)
                    )
                }

                if (isGst) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        label = { Text("GSTIN Number (15 Characters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (for QR Code on Bills)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shopName.isBlank()) {
                        errorMsg = "Please enter business name"
                        return@Button
                    }
                    val updated = (initialProfile ?: BusinessProfile()).copy(
                        shopName = shopName.trim(),
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        isGstRegistered = isGst,
                        gstin = if (isGst) gstin.trim().uppercase() else "",
                        address = address.trim(),
                        city = city.trim(),
                        state = state.trim(),
                        upiId = upiId.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
            ) {
                Text("Save Business", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
