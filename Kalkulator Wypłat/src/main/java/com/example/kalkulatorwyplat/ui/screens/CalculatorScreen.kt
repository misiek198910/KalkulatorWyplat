@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.kalkulatorwyplat.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kalkulatorwyplat.BuildConfig
import com.example.kalkulatorwyplat.data.SalaryResult
import com.example.kalkulatorwyplat.ui.components.AppInfoDialog
import com.example.kalkulatorwyplat.ui.components.ResultPanel
import com.example.kalkulatorwyplat.viewmodel.CalculatorViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Podstawowe (250 zł)", "Podwyższone (300 zł)")
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<SalaryResult?>(null) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val analytics = remember { FirebaseAnalytics.getInstance(context) }

    // USUNIĘTO: adShowCounter

    LaunchedEffect(Unit) {
        loadInterstitialAd(context) { ad ->
            mInterstitialAd = ad
        }
    }

    fun showInfo(title: String, msg: String) {
        dialogTitle = title
        dialogMessage = msg
        showDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kalkulator Wypłat",
                        color = theme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.background),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Ustawienia", tint = theme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CalculatorContent(
                viewModel = viewModel,
                theme = theme,
                options = options,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                isAdvancedExpanded = isAdvancedExpanded,
                onAdvancedExpandedChange = { isAdvancedExpanded = it },
                onResultClick = { result ->

                    analytics.logEvent("view_salary_details") {
                        param("contract_type", result.type)
                        param("gross_amount", result.gross.toDouble())
                        param("is_netto_calculation", viewModel.isCalculatingFromNetto.toString())
                    }

                    selectedResult = result
                    showSheet = true
                },
                onInfoClick = { type ->
                    when (type) {
                        "PIT" -> showInfo(
                            "Zwolnienie z PIT",
                            "Dla osób poniżej 26. roku życia przychody (do kwoty 85 528 zł rocznie) są zwolnione z podatku dochodowego."
                        )
                        "CHOROBOWE" -> showInfo(
                            "Składka chorobowa",
                            "Dobrowolna przy zleceniu, obowiązkowa przy etacie. Daje prawo do L4."
                        )
                        "KWOTA_WOLNA" -> showInfo(
                            "Kwota wolna od podatku",
                            "Ulga podatkowa (PIT-2), zazwyczaj 300 zł miesięcznie."
                        )
                        "PPK" -> showInfo(
                            "PPK",
                            "Pracownicze Plany Kapitałowe. Wpływa na potrącenie z netto i podatek."
                        )
                    }
                }
            )

            if (showDialog) {
                AppInfoDialog(
                    title = dialogTitle,
                    message = dialogMessage,
                    onDismiss = { showDialog = false },
                    primaryColor = theme.primary
                )
            }
        }
    }

    if (showSheet && selectedResult != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false

                // ZMIANA: Pokazujemy reklamę za KAŻDYM razem, bez sprawdzania licznika
                mInterstitialAd?.let { ad ->
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            mInterstitialAd = null
                            loadInterstitialAd(context) { newAd -> mInterstitialAd = newAd }
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            mInterstitialAd = null
                        }
                    }
                    ad.show(context as Activity)
                }
            },
            sheetState = sheetState,
            containerColor = theme.surface
        ) {
            SalaryDetailsContent(selectedResult!!, theme)
        }
    }
}

@Composable
fun CalculatorContent(viewModel: CalculatorViewModel, theme: ColorScheme, options: List<String>, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, isAdvancedExpanded: Boolean, onAdvancedExpandedChange: (Boolean) -> Unit, onResultClick: (SalaryResult) -> Unit, onInfoClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = theme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ConfigRow(
                    "Zwolnienie z PIT (do 26 lat)",
                    viewModel.isPitExempt,
                    { viewModel.isPitExempt = it; viewModel.calculate() },
                    { onInfoClick("PIT") },
                    theme
                )
                Divider(color = theme.background, thickness = 1.dp)
                ConfigRow(
                    "Dobrowolna składka chorobowa",
                    viewModel.isSickLeaveEnabled,
                    { viewModel.isSickLeaveEnabled = it; viewModel.calculate() },
                    { onInfoClick("CHOROBOWE") },
                    theme
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAdvancedExpandedChange(!isAdvancedExpanded) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Opcje zaawansowane (PPK, Kwota wolna)",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = theme.primary
                    )
                }

                AnimatedVisibility(
                    visible = isAdvancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        ConfigRow(
                            "Uwzględnij kwotę wolną (300 zł)",
                            viewModel.isTaxFreeAmountEnabled,
                            { viewModel.isTaxFreeAmountEnabled = it; viewModel.calculate() },
                            { onInfoClick("KWOTA_WOLNA") },
                            theme
                        )
                        Divider(color = theme.background, thickness = 1.dp)
                        ConfigRow(
                            "Uczestnictwo w PPK (2%)",
                            viewModel.isPpkEnabled,
                            { viewModel.isPpkEnabled = it; viewModel.calculate() },
                            { onInfoClick("PPK") },
                            theme
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = onExpandedChange,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.selectedOption,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Koszty uzyskania przychodu") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = theme.primary,
                                    unfocusedBorderColor = theme.onSurfaceVariant,
                                    focusedLabelColor = theme.primary,
                                    unfocusedLabelColor = theme.onSurfaceVariant
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { onExpandedChange(false) }) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            viewModel.selectedOption =
                                                option; onExpandedChange(false); viewModel.calculate()
                                        })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.amount = it; viewModel.calculate() },
                label = { Text(if (viewModel.isCalculatingFromNetto) "Wpisz kwotę Netto" else "Wpisz kwotę Brutto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.onSurfaceVariant,
                    focusedLabelColor = theme.primary,
                    unfocusedLabelColor = theme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Licz od\nNetto",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
                Switch(
                    checked = viewModel.isCalculatingFromNetto,
                    onCheckedChange = {
                        viewModel.isCalculatingFromNetto = it; viewModel.calculate()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = theme.primary,
                        checkedTrackColor = theme.primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = theme.onSurfaceVariant,
                        uncheckedTrackColor = theme.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.calculationResults.isNotEmpty()) {
            Text(
                "Wyniki:",
                style = MaterialTheme.typography.titleMedium,
                color = theme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            viewModel.calculationResults.forEach { result ->
                ResultPanel(
                    result = result,
                    highlightNetto = !viewModel.isCalculatingFromNetto,
                    onMoreClick = onResultClick
                )
            }
        }
    }
}

@Composable
fun ConfigRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onInfoClick: () -> Unit, theme: ColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = theme.primary,
                    uncheckedColor = theme.onSurfaceVariant
                )
            )
            Text(label, color = theme.onSurface, fontSize = 13.sp, lineHeight = 16.sp)
        }
        IconButton(onClick = onInfoClick) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = theme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SalaryDetailsContent(result: SalaryResult, theme: ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(result.type, style = MaterialTheme.typography.headlineSmall, color = theme.onSurface)
        Text("Podsumowanie składników", color = theme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        DetailItem("Kwota Brutto", result.gross, theme, isHeader = true)
        Divider(
            color = theme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        DetailItem("Składka emerytalna", result.sEmery, theme)
        DetailItem("Składka rentowa", result.sRent, theme)
        DetailItem("Składka chorobowa", result.sChor, theme)
        DetailItem("Ubezpieczenie zdrowotne", result.uZdro, theme)
        DetailItem("Koszty uzyskania przychodu", result.kUzyPrz, theme)
        DetailItem("Podstawa opodatkowania", result.poDoch, theme)
        DetailItem("Zaliczka na podatek (PIT)", result.zalPoddoch, theme)
        Divider(
            color = theme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DO RĘKI (NETTO)",
                style = MaterialTheme.typography.titleMedium,
                color = theme.onSurface
            )
            Text(
                String.format("%.2f PLN", result.netto),
                style = MaterialTheme.typography.headlineSmall,
                color = theme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: Float, theme: ColorScheme, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (isHeader) theme.onSurface else theme.onSurfaceVariant,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            String.format("%.2f PLN", value),
            color = if (isHeader) theme.onSurface else theme.onSurfaceVariant,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AdBanner() {
    AndroidView(modifier = Modifier.fillMaxWidth(), factory = { context ->
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
            loadAd(AdRequest.Builder().build())
        }
    })
}

fun loadInterstitialAd(context: Context, onAdLoaded: (InterstitialAd?) -> Unit) {
    val adRequest = AdRequest.Builder().build()

    InterstitialAd.load(
        context,
        BuildConfig.AD_INTERSTITIAL_ID,
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("AdMob", "Reklama załadowana")
                onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d("AdMob", "Błąd ładowania reklamy: ${error.message}")
                onAdLoaded(null)
            }
        }
    )
}