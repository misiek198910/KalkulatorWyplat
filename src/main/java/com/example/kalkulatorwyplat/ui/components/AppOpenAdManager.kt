package com.example.kalkulatorwyplat.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.kalkulatorwyplat.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false

    fun loadAd(context: Context, onLoaded: (Boolean) -> Unit) {
        if (appOpenAd != null) {
            onLoaded(true)
            return
        }

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            BuildConfig.AD_ADSTART_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d("AppOpenAdManager", "Reklama na start załadowana")
                    appOpenAd = ad
                    onLoaded(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AppOpenAdManager", "Błąd ładowania: ${error.message}")
                    onLoaded(false)
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity, onShowComplete: () -> Unit) {
        if (isShowingAd) return

        if (appOpenAd == null) {
            onShowComplete()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                onShowComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                onShowComplete()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        appOpenAd?.show(activity)
    }
}