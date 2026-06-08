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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kalkulatorwyplat.BuildConfig
import com.example.kalkulatorwyplat.R
import com.example.kalkulatorwyplat.data.SalaryResult
import com.example.kalkulatorwyplat.ui.components.AppInfoDialog
import com.example.kalkulatorwyplat.ui.components.ResultPanel
import com.example.kalkulatorwyplat.ui.components.YearlyResultPanel
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

    val options = listOf(
        stringResource(id = R.string.kup_basic),
        stringResource(id = R.string.kup_elevated)
    )

    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<SalaryResult?>(null) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val analytics = remember { FirebaseAnalytics.getInstance(context) }

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

    val infoPitTitle = stringResource(R.string.info_pit_title)
    val infoPitMsg = stringResource(R.string.info_pit_msg)
    val infoSickTitle = stringResource(R.string.info_sick_title)
    val infoSickMsg = stringResource(R.string.info_sick_msg)
    val infoTaxFreeTitle = stringResource(R.string.info_tax_free_title)
    val infoTaxFreeMsg = stringResource(R.string.info_tax_free_msg)
    val infoPpkTitle = stringResource(R.string.info_ppk_title)
    val infoPpkMsg = stringResource(R.string.info_ppk_msg)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_title),
                        color = theme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.background),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings), tint = theme.primary)
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
                        "PIT" -> showInfo(infoPitTitle, infoPitMsg)
                        "CHOROBOWE" -> showInfo(infoSickTitle, infoSickMsg)
                        "KWOTA_WOLNA" -> showInfo(infoTaxFreeTitle, infoTaxFreeMsg)
                        "PPK" -> showInfo(infoPpkTitle, infoPpkMsg)
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
                    stringResource(R.string.label_pit_exempt),
                    viewModel.isPitExempt,
                    { viewModel.isPitExempt = it; viewModel.calculate() },
                    { onInfoClick("PIT") },
                    theme
                )
                Divider(color = theme.background, thickness = 1.dp)
                ConfigRow(
                    stringResource(R.string.label_sick_leave),
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
                        stringResource(R.string.label_advanced_options),
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
                            stringResource(R.string.label_tax_free_amount),
                            viewModel.isTaxFreeAmountEnabled,
                            { viewModel.isTaxFreeAmountEnabled = it; viewModel.calculate() },
                            { onInfoClick("KWOTA_WOLNA") },
                            theme
                        )
                        Divider(color = theme.background, thickness = 1.dp)
                        ConfigRow(
                            stringResource(R.string.label_ppk),
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
                                label = { Text(stringResource(R.string.label_kup)) },
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
                label = { Text(if (viewModel.isCalculatingFromNetto) stringResource(R.string.input_net_amount) else stringResource(R.string.input_gross_amount)) },
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
                    stringResource(R.string.label_calculate_from_net),
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

        Button(
            onClick = { viewModel.calculateYearly() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = theme.onPrimary)
        ) {
            Text(
                text = stringResource(id = R.string.btn_yearly_simulation),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        viewModel.yearlyCalculationResult?.let { yearlyResult ->
            YearlyResultPanel(result = yearlyResult)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (viewModel.calculationResults.isNotEmpty()) {
            Text(
                stringResource(R.string.label_results),
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
                contentDescription = stringResource(R.string.desc_info_icon),
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
        Text(stringResource(R.string.summary_components), color = theme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        DetailItem(stringResource(R.string.gross_amount), result.gross, theme, isHeader = true)
        Divider(
            color = theme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        DetailItem(stringResource(R.string.pension_contribution), result.sEmery, theme)
        DetailItem(stringResource(R.string.disability_contribution), result.sRent, theme)
        DetailItem(stringResource(R.string.sick_contribution), result.sChor, theme)
        DetailItem(stringResource(R.string.health_insurance), result.uZdro, theme)
        DetailItem(stringResource(R.string.income_costs), result.kUzyPrz, theme)
        DetailItem(stringResource(R.string.tax_base), result.poDoch, theme)
        DetailItem(stringResource(R.string.tax_advance), result.zalPoddoch, theme)
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
                stringResource(R.string.net_amount_to_hand),
                style = MaterialTheme.typography.titleMedium,
                color = theme.onSurface
            )
            Text(
                stringResource(R.string.pln_format, result.netto),
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
            stringResource(R.string.pln_format, value),
            color = if (isHeader) theme.onSurface else theme.onSurfaceVariant,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        )
    }
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