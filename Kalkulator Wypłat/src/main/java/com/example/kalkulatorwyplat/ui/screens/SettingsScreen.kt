package com.example.kalkulatorwyplat.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val theme = MaterialTheme.colorScheme

    // Stany dla dialogów
    var showInfoDialog by remember { mutableStateOf(false) }

    // Stan rozwijania sekcji podatkowej
    var isRatesExpanded by remember { mutableStateOf(false) }

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
            // --- SEKCJA PODATKOWA (Rozwijana Karta) ---
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Wersja 1.0.0",
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