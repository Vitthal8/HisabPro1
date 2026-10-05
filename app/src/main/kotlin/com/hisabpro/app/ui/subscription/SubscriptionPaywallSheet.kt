package com.hisabpro.app.ui.subscription

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPaywallSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    userEmail: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activePlan by SubscriptionManager.activePlanFlow.collectAsStateWithLifecycle()
    var selectedPlan by remember { mutableStateOf(activePlan) }

    val isAdminEmail = remember(userEmail) {
        val clean = userEmail?.trim()?.lowercase() ?: ""
        clean == "vittalmali3@gmail.com" || clean == "vittalmli3@gmail.com" || clean.contains("vittalmali") || clean.contains("vittalmli")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("sheet_subscription_paywall")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SaffronOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = SaffronOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "HisabPro Subscriptions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select plan to unlock cloud sync, multi-business & GST",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_paywall")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin VIP Lifetime Badge if applicable
            if (isAdminEmail) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald700.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Emerald700),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Emerald700)
                        Column {
                            Text(
                                text = "VIP Owner Account Granted Lifetime Premium",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Emerald700
                            )
                            Text(
                                text = "Logged in as $userEmail • Unlimited Cloud Sync, 5 Businesses & GSTR-1 active.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Plan Options Cards
            SubscriptionPlanCard(
                plan = SubscriptionPlan.FREE,
                isActive = activePlan == SubscriptionPlan.FREE,
                isSelected = selectedPlan == SubscriptionPlan.FREE,
                onSelect = { selectedPlan = SubscriptionPlan.FREE },
                title = "Free Starter",
                priceText = "₹0 / Month",
                subtitle = "1 Shop • 50 Bills/Month • Local Backup"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SubscriptionPlanCard(
                plan = SubscriptionPlan.PRO,
                isActive = activePlan == SubscriptionPlan.PRO,
                isSelected = selectedPlan == SubscriptionPlan.PRO,
                onSelect = { selectedPlan = SubscriptionPlan.PRO },
                title = "Pro Business",
                priceText = "₹99 / Month (or ₹799/yr)",
                subtitle = "1 Shop • Unlimited Bills • Custom Logo • WhatsApp Share • Excel Export • Ad-Free"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SubscriptionPlanCard(
                plan = SubscriptionPlan.PREMIUM,
                isActive = activePlan == SubscriptionPlan.PREMIUM,
                isSelected = selectedPlan == SubscriptionPlan.PREMIUM,
                onSelect = { selectedPlan = SubscriptionPlan.PREMIUM },
                title = "Premium Enterprise (Pro Max)",
                priceText = "₹199 / Month (or ₹1499/yr)",
                subtitle = "Up to 5 Shops • Supabase Cloud Sync • Auto Backup • GSTR-1 & 3B • Priority Support"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Upgrade Button
            Button(
                onClick = {
                    SubscriptionManager.activatePlan(selectedPlan)
                    Toast.makeText(
                        context,
                        "Plan updated: ${selectedPlan.title} activated successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_activate_subscription")
            ) {
                Text(
                    text = if (selectedPlan == activePlan) "Current Active Plan (${selectedPlan.title})" else "Activate ${selectedPlan.title}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feature Comparison Table
            Text(
                text = "Feature Matrix Comparison",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val features = SubscriptionManager.getComparisonFeatures()
                    features.forEach { feat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feat.title,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                MatrixIndicator(label = "FREE", isIncluded = feat.isIncludedInFree)
                                MatrixIndicator(label = "PRO", isIncluded = feat.isIncludedInPro)
                                MatrixIndicator(label = "PREMIUM", isIncluded = feat.isIncludedInPremium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isActive: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    title: String,
    priceText: String,
    subtitle: String
) {
    val borderColor = when {
        isSelected -> SaffronOrange
        isActive -> Emerald700
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                border = BorderStroke(if (isSelected || isActive) 2.dp else 1.dp, borderColor),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SaffronOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Emerald700
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = priceText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = SaffronOrange,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isSelected) SaffronOrange else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixIndicator(label: String, isIncluded: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isIncluded) Emerald700.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isIncluded) Emerald700 else Slate700,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
