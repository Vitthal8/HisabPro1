package com.hisabpro.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.DeepNavyLight
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronLight
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    currentProfile: BusinessProfile,
    isInitialOnboarding: Boolean = false,
    onSaveProfile: (BusinessProfile) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var shopName by remember { mutableStateOf(currentProfile.shopName) }
    var ownerName by remember { mutableStateOf(currentProfile.ownerName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var selectedLanguage by remember { mutableStateOf(currentProfile.appLanguage) }

    var logoPath by remember { mutableStateOf(currentProfile.logoPath) }
    var logoAspectRatio by remember { mutableStateOf("Square (1:1)") }
    var logoScaleMode by remember { mutableStateOf("Fit") }
    var isDarkMode by remember { mutableStateOf(currentProfile.isDarkMode) }
    var themeAccent by remember { mutableStateOf(currentProfile.themeAccent) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            logoPath = uri.toString()
        }
    }

    // Core Requirement: GST vs Non-GST
    var isGstRegistered by remember { mutableStateOf(currentProfile.isGstRegistered) }
    var gstin by remember { mutableStateOf(currentProfile.gstin) }
    var pan by remember { mutableStateOf(currentProfile.pan) }
    var isCompositionScheme by remember { mutableStateOf(currentProfile.isCompositionScheme) }
    var compositionType by remember { mutableStateOf(currentProfile.compositionType) }

    // Location
    val indianStates = remember {
        listOf(
            "Maharashtra (27)",
            "Gujarat (24)",
            "Karnataka (29)",
            "Madhya Pradesh (23)",
            "Goa (30)",
            "Delhi (07)",
            "Rajasthan (08)",
            "Uttar Pradesh (09)",
            "Tamil Nadu (33)",
            "Telangana (36)",
            "Andhra Pradesh (37)",
            "West Bengal (19)",
            "Punjab (03)",
            "Haryana (06)",
            "Kerala (32)",
            "Bihar (10)"
        )
    }
    var selectedStateWithCode by remember {
        val match = indianStates.find { it.startsWith(currentProfile.state, ignoreCase = true) }
        mutableStateOf(match ?: "Maharashtra (27)")
    }
    var isStateExpanded by remember { mutableStateOf(false) }

    var city by remember { mutableStateOf(currentProfile.city) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var pincode by remember { mutableStateOf(currentProfile.pincode) }

    // UPI & Bank
    var upiId by remember { mutableStateOf(currentProfile.upiId) }
    var bankName by remember { mutableStateOf(currentProfile.bankName) }
    var accountNumber by remember { mutableStateOf(currentProfile.accountNumber) }
    var ifscCode by remember { mutableStateOf(currentProfile.ifscCode) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val errShopNameEmpty = stringResource(R.string.err_shop_name_empty)
    val errInvalidGstin = stringResource(R.string.err_invalid_gstin)

    val hasUnsavedChanges = remember(
        shopName, ownerName, phone, email, isGstRegistered, gstin, city, address, upiId, bankName
    ) {
        if (!currentProfile.hasCompletedOnboarding) {
            shopName.isNotBlank() || phone.isNotBlank() || ownerName.isNotBlank()
        } else {
            shopName != currentProfile.shopName ||
                    ownerName != currentProfile.ownerName ||
                    phone != currentProfile.phone ||
                    email != currentProfile.email ||
                    isGstRegistered != currentProfile.isGstRegistered ||
                    gstin != currentProfile.gstin ||
                    city != currentProfile.city ||
                    address != currentProfile.address ||
                    upiId != currentProfile.upiId
        }
    }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    fun handleDismissAttempt() {
        if (onDismiss == null) return
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = onDismiss != null) {
        handleDismissAttempt()
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes in your business profile form. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss?.invoke()
                    }
                ) {
                    Text("Discard", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Surface(
                color = DeepNavyBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (onDismiss != null) {
                                IconButton(
                                    onClick = { handleDismissAttempt() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        tint = PureWhite
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SaffronOrange,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = if (isInitialOnboarding) stringResource(R.string.setup_your_business) else stringResource(R.string.business_profile),
                                    color = PureWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "HisabPro • Indian SMB Accounting",
                                    color = PureWhite.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (onDismiss != null && !isInitialOnboarding) {
                            TextButton(onClick = { handleDismissAttempt() }) {
                                Text(
                                    text = stringResource(R.string.close),
                                    color = PureWhite.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = ExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (shopName.isBlank()) {
                                errorMessage = errShopNameEmpty
                                return@Button
                            }
                            if (isGstRegistered && gstin.length < 15) {
                                errorMessage = errInvalidGstin
                                return@Button
                            }

                            val stateName = selectedStateWithCode.substringBefore(" (").trim()
                            val stateCodeStr = selectedStateWithCode.substringAfter("(").substringBefore(")").trim()

                            val updated = currentProfile.copy(
                                shopName = shopName.trim(),
                                ownerName = ownerName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                isGstRegistered = isGstRegistered,
                                gstin = if (isGstRegistered) gstin.trim().uppercase() else "",
                                pan = pan.trim().uppercase(),
                                isCompositionScheme = isCompositionScheme,
                                compositionType = compositionType,
                                state = stateName,
                                stateCode = stateCodeStr,
                                city = city.trim(),
                                address = address.trim(),
                                pincode = pincode.trim(),
                                upiId = upiId.trim(),
                                bankName = bankName.trim(),
                                accountNumber = accountNumber.trim(),
                                ifscCode = ifscCode.trim(),
                                appLanguage = selectedLanguage,
                                hasCompletedOnboarding = true
                            )

                            onSaveProfile(updated)
                            onDismiss?.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_business_setup_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                    ) {
                        Text(
                            text = if (isInitialOnboarding) stringResource(R.string.save) else stringResource(R.string.save_settings),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavyBlue.copy(alpha = 0.06f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = DeepNavyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "en" to stringResource(R.string.english),
                            "hi" to stringResource(R.string.hindi),
                            "mr" to stringResource(R.string.marathi)
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = selectedLanguage == code,
                                onClick = { selectedLanguage = code },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronOrange,
                                    selectedLabelColor = PureWhite,
                                    containerColor = PureWhite,
                                    labelColor = Slate800
                                )
                            )
                        }
                    }
                }
            }

            // Advanced Company Logo & Aspect Ratio Studio Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Company Logo & Ratio Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Upload your business logo for invoices and PDFs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (logoPath.isBlank()) {
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Choose / Upload Logo from Gallery", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aspect Ratio & Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            OutlinedButton(
                                onClick = {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Change Logo", fontSize = 12.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Square (1:1)", "Banner (4:1)", "Compact (16:9)").forEach { ratio ->
                                FilterChip(
                                    selected = logoAspectRatio == ratio,
                                    onClick = { logoAspectRatio = ratio },
                                    label = { Text(ratio, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepNavyBlue,
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Fit", "Cover").forEach { mode ->
                                FilterChip(
                                    selected = logoScaleMode == mode,
                                    onClick = { logoScaleMode = mode },
                                    label = { Text("Scale: $mode", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SaffronOrange,
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }

                        val previewModifier = when (logoAspectRatio) {
                            "Banner (4:1)" -> Modifier.fillMaxWidth().height(70.dp)
                            "Compact (16:9)" -> Modifier.fillMaxWidth().height(100.dp)
                            else -> Modifier.size(100.dp).align(Alignment.CenterHorizontally)
                        }

                        Box(
                            modifier = previewModifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = logoPath,
                                contentDescription = "Company Logo Preview",
                                contentScale = if (logoScaleMode == "Cover") ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        TextButton(
                            onClick = { logoPath = "" },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Remove Logo", fontSize = 11.sp, color = ExpenseRed)
                        }
                    }
                }
            }

            // Theme & Appearance Customization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Theme & Appearance Look",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SaffronOrange)
                        )
                    }

                    Text("Accent Theme Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Saffron" to "Saffron Orange", "Emerald" to "Emerald Green", "Navy" to "Deep Navy").forEach { (accent, label) ->
                            FilterChip(
                                selected = themeAccent == accent,
                                onClick = { themeAccent = accent },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronOrange,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }
                }
            }

            // CORE REQUIREMENT: GST vs Non-GST Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGstRegistered) DeepNavyLight.copy(alpha = 0.5f) else SaffronLight.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGstRegistered) stringResource(R.string.gst_registered) else stringResource(R.string.non_gst_shop),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGstRegistered) DeepNavyBlue else SaffronOrange
                            )
                        }
                        Switch(
                            checked = isGstRegistered,
                            onCheckedChange = { isGstRegistered = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = DeepNavyBlue,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = SaffronOrange
                            ),
                            modifier = Modifier.testTag("gst_registered_toggle")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    if (isGstRegistered) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = gstin,
                                onValueChange = { input ->
                                    val cleaned = input.uppercase().trim().take(15)
                                    gstin = cleaned
                                    if (cleaned.length >= 10 && pan.isBlank()) {
                                        pan = cleaned.substring(2, kotlin.math.min(12, cleaned.length))
                                    }
                                },
                                label = { Text(stringResource(R.string.gstin_number)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_gstin"),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters
                                )
                            )

                            OutlinedTextField(
                                value = pan,
                                onValueChange = { pan = it.uppercase().take(10) },
                                label = { Text(stringResource(R.string.pan_number)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                            )
                        }
                    }
                }
            }

            // Business Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.business_setup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(stringResource(R.string.shop_name)) },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_shop_name")
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(stringResource(R.string.owner_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.phone_number)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_phone")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email_address)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Location Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.address),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = isStateExpanded,
                        onExpandedChange = { isStateExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedStateWithCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.state)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStateExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isStateExpanded,
                            onDismissRequest = { isStateExpanded = false }
                        ) {
                            indianStates.forEach { stateItem ->
                                DropdownMenuItem(
                                    text = { Text(stateItem) },
                                    onClick = {
                                        selectedStateWithCode = stateItem
                                        isStateExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text(stringResource(R.string.city)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it.take(6) },
                            label = { Text(stringResource(R.string.pincode)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(stringResource(R.string.address)) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bank & UPI Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text(stringResource(R.string.upi_id)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text(stringResource(R.string.bank_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text(stringResource(R.string.account_number)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it },
                            label = { Text(stringResource(R.string.ifsc_code)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
