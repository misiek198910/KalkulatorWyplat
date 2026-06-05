package com.example.kalkulatorwyplat.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kalkulatorwyplat.R
import com.example.kalkulatorwyplat.ui.components.AppInfoDialog
import com.example.kalkulatorwyplat.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CalculatorViewModel, onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val theme = MaterialTheme.colorScheme
    var showInfoDialog by remember { mutableStateOf(false) }
    var isRatesExpanded by remember { mutableStateOf(false) }
    val versionName = getAppVersionName(context)

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ustawienia",
                        color = theme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Wróć",
                            tint = theme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "PODATKI",
                style = MaterialTheme.typography.labelSmall,
                color = theme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = theme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, theme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRatesExpanded = !isRatesExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Konfiguracja stawek (%)",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isRatesExpanded) {
                                Text(
                                    text = "Kliknij, aby edytować stawki ZUS/PIT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isRatesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = theme.primary
                        )
                    }

                    AnimatedVisibility(
                        visible = isRatesExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            Text(
                                "Zmień wartości ręcznie, jeśli rząd wprowadzi zmiany w prawie podatkowym.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            RateInput("Składka Emerytalna (%)", viewModel.currentTaxRates.emerytalna.toString()) { viewModel.updateRate("emerytalna", it) }
                            RateInput("Składka Rentowa (%)", viewModel.currentTaxRates.rentowa.toString()) { viewModel.updateRate("rentowa", it) }
                            RateInput("Składka Chorobowa (%)", viewModel.currentTaxRates.chorobowa.toString()) { viewModel.updateRate("chorobowa", it) }
                            RateInput("Składka Zdrowotna (%)", viewModel.currentTaxRates.zdrowotna.toString()) { viewModel.updateRate("zdrowotna", it) }
                            RateInput("Podatek PIT (%)", viewModel.currentTaxRates.pit.toString()) { viewModel.updateRate("pit", it) }

                            // --- NOWY PRZYCISK: Przywróć domyślne ---
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.resetTaxRates() },
                                modifier = Modifier.align(Alignment.End) // Wyrównanie do prawej strony
                            ) {
                                Text("Przywróć domyślne", color = theme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SEKCJA APLIKACJI ---
            Text(
                text = "APLIKACJA",
                style = MaterialTheme.typography.labelSmall,
                color = theme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = theme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, theme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsButton(
                        text = "Subskrypcja Premium",
                        icon = Icons.Default.WorkspacePremium,
                        theme = theme,
                        highlight = true,
                        onClick = { onNavigateToSubscription() }
                    )
                    Divider(color = theme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsButton(
                        text = "Informacje o aplikacji",
                        icon = Icons.Default.Info,
                        theme = theme,
                        onClick = { showInfoDialog = true }
                    )
                    Divider(color = theme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsButton(
                        text = "Polityka prywatności",
                        icon = Icons.Default.PrivacyTip,
                        theme = theme,
                        onClick = {
                            onNavigateToPrivacyPolicy()
                        }
                    )
                    Divider(color = theme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsButton(
                        text = "Postaw kawę",
                        icon = Icons.Default.Coffee,
                        theme = theme,
                        onClick = { uriHandler.openUri("https://ko-fi.com/michals") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.app_version, versionName),
                style = MaterialTheme.typography.labelSmall,
                color = theme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showInfoDialog) {
        AppInfoDialog(
            title = "O aplikacji",
            message = "Kalkulator Wypłat v1.0\n\nProfesjonalne narzędzie do wyliczania wynagrodzeń w oparciu o aktualne przepisy polskiego ładu podatkowego.\n\nAutor: Michał Schmude",
            onDismiss = { showInfoDialog = false },
            primaryColor = theme.primary
        )
    }
}

@Composable
private fun RateInput(label: String, value: String, onValueChange: (String) -> Unit) {
    val theme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.primary,
            unfocusedBorderColor = theme.outlineVariant,
            focusedLabelColor = theme.primary,
            unfocusedLabelColor = theme.onSurfaceVariant,
            cursorColor = theme.primary,
            focusedTextColor = theme.onSurface,
            unfocusedTextColor = theme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SettingsButton(text: String, icon: ImageVector, theme: ColorScheme, highlight: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (highlight) Color(0xFFFFD700) else theme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, color = theme.onSurface, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = theme.onSurfaceVariant)
    }
}

fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0" // Fallback w razie błędu
    }
}