package com.hisabpro.app.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.hisabpro.app.domain.subscription.BillingConstants
import com.hisabpro.app.domain.subscription.SubscriptionManager
import com.hisabpro.app.domain.subscription.SubscriptionPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository handling Google Play Billing 6+ / 7+ Integration for HisabPro Subscriptions.
 *
 * Responsibilities:
 * 1. Querying subscription products and dynamic Play Store pricing.
 * 2. Launching in-app subscription purchase flows.
 * 3. Handling purchase acknowledgements and pending purchases safely.
 * 4. Restoring active purchases and updating [SubscriptionManager] single entitlement layer.
 */
class BillingRepository private constructor(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    companion object {
        private const val TAG = "BillingRepository"

        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(context: Context): BillingRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun startConnection() {
        if (_isServiceConnected.value) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Google Play Billing setup successful.")
                    _isServiceConnected.value = true
                    scope.launch {
                        querySubscriptionProductDetails()
                        restorePurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed with response code: ${billingResult.responseCode}")
                    _isServiceConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Will retry connection on next interaction.")
                _isServiceConnected.value = false
            }
        })
    }

    private suspend fun querySubscriptionProductDetails() = withContext(Dispatchers.IO) {
        if (!_isServiceConnected.value) return@withContext

        val productList = BillingConstants.ALL_SUBSCRIPTION_SKUS.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsResult
                Log.i(TAG, "Queried ${details.size} subscription products from Play Store.")
                _productDetailsList.value = details
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        if (!_isServiceConnected.value) {
            startConnection()
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                .setDebugMessage("Billing service disconnected. Retrying connection.")
                .build()
        }

        val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
        val offerToken = offerDetails?.offerToken ?: ""

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled purchase flow.")
                SubscriptionManager.resetBillingState()
            }
            else -> {
                Log.e(TAG, "Purchase failed with code ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Purchase acknowledged successfully.")
                        activateEntitlementsFromPurchase(purchase)
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            } else {
                activateEntitlementsFromPurchase(purchase)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.i(TAG, "Purchase is pending completion by user.")
        }
    }

    private fun activateEntitlementsFromPurchase(purchase: Purchase) {
        val products = purchase.products
        val targetPlan = when {
            products.contains(BillingConstants.SKU_PREMIUM_MONTHLY) || products.contains(BillingConstants.SKU_PREMIUM_YEARLY) -> SubscriptionPlan.PREMIUM
            products.contains(BillingConstants.SKU_PRO_MONTHLY) || products.contains(BillingConstants.SKU_PRO_YEARLY) -> SubscriptionPlan.PRO
            else -> SubscriptionPlan.FREE
        }
        SubscriptionManager.activatePlan(targetPlan)
    }

    fun restorePurchases() {
        if (!_isServiceConnected.value) {
            startConnection()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (activePurchases.isNotEmpty()) {
                    Log.i(TAG, "Found ${activePurchases.size} active subscriptions on Play Store.")
                    for (purchase in activePurchases) {
                        handlePurchase(purchase)
                    }
                } else {
                    Log.i(TAG, "No active Play Store subscriptions found.")
                }
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }
}
