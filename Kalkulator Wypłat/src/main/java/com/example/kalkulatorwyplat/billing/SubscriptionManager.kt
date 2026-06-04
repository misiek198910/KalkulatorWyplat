package com.example.kalkulatorwyplat.billing

import android.content.Context
import androidx.lifecycle.LiveData

class SubscriptionManager private constructor(context: Context) {

    val billingManager: BillingManager = BillingManager.getInstance(context)

    val isPremium: LiveData<Boolean> = billingManager.isPremium

    val subscriptionStatus: LiveData<SubscriptionStatus> = billingManager.subscriptionStatus

    val productDetails = billingManager.productDetails

    val isPremiumValue: Boolean
        get() = billingManager.isPremium.value ?: false

    companion object {
        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SubscriptionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}