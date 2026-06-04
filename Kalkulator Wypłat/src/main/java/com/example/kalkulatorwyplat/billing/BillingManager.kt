package com.example.kalkulatorwyplat.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.example.kalkulatorwyplat.data.db.AppDatabase
import com.example.kalkulatorwyplat.data.entity.SubscriptionEntity
import kotlinx.coroutines.*

class BillingManager private constructor(context: Context) {
    private val billingClient: BillingClient
    private val database = AppDatabase.getDatabase(context.applicationContext)
    private val dao = database.subscriptionDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium

    private val _subscriptionStatus = MutableLiveData(SubscriptionStatus.CHECKING)
    val subscriptionStatus: LiveData<SubscriptionStatus> = _subscriptionStatus

    private val _productDetails = MutableLiveData<ProductDetails?>()
    val productDetails: LiveData<ProductDetails?> = _productDetails

    interface BillingManagerListener {
        fun onPurchaseAcknowledged()
        fun onPurchaseError(error: String?)
    }

    private var listener: BillingManagerListener? = null
    fun setListener(listener: BillingManagerListener?) { this.listener = listener }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) { handlePurchase(purchase) }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            listener?.onPurchaseError("Anulowano zakup.")
        } else {
            listener?.onPurchaseError("Błąd zakupu. Kod: ${billingResult.responseCode}")
        }
    }

    init {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans() // DODANE Z MOJEJ PARAFII
            .build()

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .enableAutoServiceReconnection() // DODANE Z MOJEJ PARAFII (Nowość w 8.x)
            .build()

        connectToGooglePlay()

        scope.launch {
            val status = dao.getStatus()
            val isFull = status?.isPremium ?: false
            _isPremium.postValue(isFull)
            _subscriptionStatus.postValue(if (isFull) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d("BillingManager", "Połączono z Google Play (v8.x)")
                    queryPurchasesAsync()
                    queryProductDetails()
                } else {
                    Log.e("BillingManager", "Błąd połączenia: ${billingResult.responseCode}")
                    _subscriptionStatus.postValue(SubscriptionStatus.NON_PREMIUM)
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d("BillingManager", "Rozłączono z Google Play")
            }
        })
    }

    fun queryPurchasesAsync() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                var hasPremium = false
                var token: String? = null

                if (purchases != null) {
                    purchases.forEach { purchase ->
                        if (purchase.products.contains(SKU_PREMIUM_PRODUCT) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            hasPremium = true
                            token = purchase.purchaseToken
                            if (!purchase.isAcknowledged) handlePurchase(purchase)
                        }
                    }
                }
                updateLocalStatus(hasPremium, token)
            }
        }
    }

    private fun updateLocalStatus(hasPremium: Boolean, token: String?) {
        scope.launch {
            dao.insert(SubscriptionEntity(isPremium = hasPremium, purchaseToken = token))
            _isPremium.postValue(hasPremium)
            _subscriptionStatus.postValue(if (hasPremium) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_PREMIUM_PRODUCT)
                .setProductType(ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val code = billingResult.responseCode

            // DOKŁADNA SKŁADNIA Z MOJEJ PARAFII (Rozwiązuje problemy wersji 8.3.0)
            val list = queryProductDetailsResult.productDetailsList

            if (code == BillingResponseCode.OK && !list.isNullOrEmpty()) {
                val product = list.find { it.productId == SKU_PREMIUM_PRODUCT }
                if (product != null) {
                    _productDetails.postValue(product)
                    Log.d("BillingManager", "Pobrano szczegóły dla Kalkulatora: ${product.productId}")
                }
            } else {
                // TO TUTAJ JEST POWÓD KRĘCENIA SIĘ KÓŁKA!
                Log.e("BillingManager", "BŁĄD: Pusta lista produktów od Google. API zwróciło: ${list?.size ?: 0} produktów. Upewnij się, że opublikowałeś wersję testową AAB, logujesz się z konta testera, lub odczekaj kilka godzin na propagację.")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetailsToPurchase: ProductDetails, basePlanId: String) {
        val offersForBasePlan = productDetailsToPurchase.subscriptionOfferDetails?.filter {
            it.basePlanId == basePlanId
        }

        if (offersForBasePlan.isNullOrEmpty()) {
            listener?.onPurchaseError("Nie znaleziono wybranej oferty ($basePlanId).")
            return
        }

        // PRIORYTET DLA TRIALU (gwarantuje, że podłączy darmowe 7 dni)
        val targetOffer = offersForBasePlan.find { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: offersForBasePlan.first()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetailsToPurchase)
                    .setOfferToken(targetOffer.offerToken)
                    .build()
            )).build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingResponseCode.OK) {
                    updateLocalStatus(true, purchase.purchaseToken)
                    listener?.onPurchaseAcknowledged()
                }
            }
        } else if (purchase.isAcknowledged) {
            updateLocalStatus(true, purchase.purchaseToken)
        }
    }

    fun getPlanOfferInfo(context: Context, productDetails: ProductDetails?, basePlanId: String): String {
        val offers = productDetails?.subscriptionOfferDetails?.filter { it.basePlanId == basePlanId }
        val offer = offers?.find { o ->
            o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: offers?.firstOrNull()

        val trialPhase = offer?.pricingPhases?.pricingPhaseList?.find { it.priceAmountMicros == 0L }
        val basePhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()

        return when {
            basePlanId == PLAN_YEARLY && trialPhase != null && basePhase != null -> {
                "Wypróbuj za darmo przez 7 dni, potem ${basePhase.formattedPrice} / rok"
            }
            basePlanId == PLAN_MONTHLY && basePhase != null -> {
                "Plan Miesięczny - ${basePhase.formattedPrice} / mc"
            }
            basePlanId == PLAN_YEARLY && basePhase != null -> {
                "Plan Roczny - ${basePhase.formattedPrice} / rok"
            }
            else -> "Pobieranie ceny..."
        }
    }

    companion object {
        @Volatile private var INSTANCE: BillingManager? = null
        fun getInstance(context: Context): BillingManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: BillingManager(context).also { INSTANCE = it }
        }

        const val SKU_PREMIUM_PRODUCT = "kalkulator_premium"
        const val PLAN_MONTHLY = "premium-monthly"
        const val PLAN_YEARLY = "premium-yearly"
    }
}