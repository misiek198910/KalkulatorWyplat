package com.example.kalkulatorwyplat.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kalkulatorwyplat.BuildConfig
import com.example.kalkulatorwyplat.R
import com.example.kalkulatorwyplat.billing.SubscriptionManager
import com.example.kalkulatorwyplat.ui.components.AppOpenAdManager
import com.example.kalkulatorwyplat.ui.screens.CalculatorScreen
import com.example.kalkulatorwyplat.ui.screens.PrivacyPolicyScreen
import com.example.kalkulatorwyplat.ui.screens.SettingsScreen
import com.example.kalkulatorwyplat.ui.screens.SubscriptionScreen
import com.example.kalkulatorwyplat.ui.theme.KalkulatorWyplatTheme
import com.example.kalkulatorwyplat.viewmodel.CalculatorViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics

// --- Nowe importy dla In-App Updates ---
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {
    private lateinit var consentInformation: ConsentInformation
    private val appOpenAdManager = AppOpenAdManager()
    private var isMobileAdsInitializeCalled = false
    private var isAdFlowCompleted = false
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.w("InAppUpdate", "Aktualizacja anulowana lub nie powiodła się. Kod: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkForUpdate()

        setContent {

            KalkulatorWyplatTheme {
                val theme = MaterialTheme.colorScheme
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen()

                    LaunchedEffect(Unit) {
                        gatherConsentAndShowAd {
                            showSplash = false
                        }
                    }
                } else {
                    val navController = rememberNavController()
                    val viewModel: CalculatorViewModel = viewModel()
                    val subscriptionManager = SubscriptionManager.getInstance(this@MainActivity)
                    val isPremium by subscriptionManager.isPremium.observeAsState(initial = false)

                    Surface(modifier = Modifier.fillMaxSize(), color = theme.background) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                NavHost(navController = navController, startDestination = "calculator") {
                                    composable("calculator") {
                                        CalculatorScreen(
                                            viewModel = viewModel,
                                            onNavigateToSettings = { navController.navigate("settings") }
                                        )
                                    }
                                    composable("settings") {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            onNavigateToSubscription = { navController.navigate("subscription") },
                                            onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                                            onBackClick = { navController.popBackStack() }
                                        )
                                    }
                                    composable("subscription") {
                                        SubscriptionScreen(navController = navController, subscriptionManager = subscriptionManager)
                                    }
                                    composable("privacy_policy") {
                                        PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
                                    }
                                }
                            }

                            if (!isPremium) {
                                Surface(modifier = Modifier.fillMaxWidth(), color = theme.background, tonalElevation = 2.dp) {
                                    Column {
                                        Divider(color = theme.outlineVariant)
                                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                            GlobalAdBanner()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun checkForUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
        }
    }

    private fun gatherConsentAndShowAd(onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation.requestConsentInfoUpdate(this, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ ->
                if (consentInformation.canRequestAds()) {
                    initializeMobileAds(onComplete)
                } else {
                    finishFlow(onComplete)
                }
            }
        }, { _ ->
            if (consentInformation.canRequestAds()) {
                initializeMobileAds(onComplete)
            } else {
                finishFlow(onComplete)
            }
        })

        if (consentInformation.canRequestAds()) {
            initializeMobileAds(onComplete)
        }
    }

    private fun initializeMobileAds(onComplete: () -> Unit) {
        if (isMobileAdsInitializeCalled) return
        isMobileAdsInitializeCalled = true

        MobileAds.initialize(this) {
            appOpenAdManager.loadAd(this) {
                runOnUiThread {
                    appOpenAdManager.showAdIfAvailable(this) {
                        finishFlow(onComplete)
                    }
                }
            }
        }
    }

    private fun finishFlow(onComplete: () -> Unit) {
        if (!isAdFlowCompleted) {
            isAdFlowCompleted = true
            onComplete()
        }
    }
}

@Composable
fun SplashScreen() {
    val theme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = theme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kalkulator Wypłat",
                color = theme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GlobalAdBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}