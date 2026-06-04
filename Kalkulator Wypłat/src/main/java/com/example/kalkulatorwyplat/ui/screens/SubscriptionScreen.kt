package com.example.kalkulatorwyplat.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.kalkulatorwyplat.billing.BillingManager
import com.example.kalkulatorwyplat.billing.SubscriptionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    subscriptionManager: SubscriptionManager // --- NOWY PARAMETR ---
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val theme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    val goldColor = Color(0xFFFFD700)

    // --- OBSERWACJA DANYCH Z GOOGLE PLAY ---
    val isPremium by subscriptionManager.isPremium.observeAsState(initial = false)
    val productDetails by subscriptionManager.productDetails.observeAsState()

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Wersja Premium",
                            color = goldColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = theme.surface
                ),
                border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Odblokuj pełen potencjał",
                        color = theme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Zyskaj dostęp do zaawansowanych funkcji i wspieraj rozwój aplikacji.",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = theme.onSurfaceVariant,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        BenefitRow("Brak reklam w całej aplikacji", theme)
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 24.dp),
                        thickness = 1.dp,
                        color = theme.outlineVariant
                    )

                    Text(
                        text = "STATUS SUBSKRYPCJI",
                        color = theme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isPremium) "AKTYWNA" else "NIEAKTYWNA",
                        color = if (isPremium) Color(0xFF4CAF50) else theme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- LOGIKA PRZYCISKÓW ZAKUPU ---
                    if (isPremium) {
                        SubscriptionButton(
                            text = "Zarządzaj subskrypcją",
                            theme = theme,
                            onClick = {
                                val url = "https://play.google.com/store/account/subscriptions?package=${context.packageName}"
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Nie znaleziono sklepu Play", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        // Sprawdzamy, czy dane z Google Play zostały już pobrane
                        if (productDetails != null && activity != null) {
                            // Przycisk dla planu miesięcznego
                            SubscriptionButton(
                                text = subscriptionManager.billingManager.getPlanOfferInfo(context, productDetails, BillingManager.PLAN_MONTHLY),
                                theme = theme,
                                onClick = {
                                    subscriptionManager.billingManager.launchPurchaseFlow(activity, productDetails!!, BillingManager.PLAN_MONTHLY)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Przycisk dla planu rocznego
                            SubscriptionButton(
                                text = subscriptionManager.billingManager.getPlanOfferInfo(context, productDetails, BillingManager.PLAN_YEARLY),
                                theme = theme,
                                isFeatured = true,
                                onClick = {
                                    subscriptionManager.billingManager.launchPurchaseFlow(activity, productDetails!!, BillingManager.PLAN_YEARLY)
                                }
                            )
                        } else {
                            // Widok ładowania cen
                            CircularProgressIndicator(color = theme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pobieranie cen...", color = theme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = {
                        // Ręczne wymuszenie sprawdzenia zakupów
                        subscriptionManager.billingManager.queryPurchasesAsync()
                        Toast.makeText(context, "Sprawdzam zakupy...", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Przywróć zakup", color = theme.primary)
                    }

                    Text(
                        text = "Subskrypcja odnawia się automatycznie. Możesz zrezygnować w dowolnym momencie w ustawieniach Google Play.",
                        modifier = Modifier.padding(top = 24.dp),
                        color = theme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitRow(text: String, theme: ColorScheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = theme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = theme.onSurface, fontSize = 15.sp)
    }
}

@Composable
fun SubscriptionButton(
    text: String,
    onClick: () -> Unit,
    theme: ColorScheme,
    isFeatured: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFeatured) theme.primary else theme.surfaceVariant,
            contentColor = if (isFeatured) theme.onPrimary else theme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isFeatured) null else BorderStroke(1.dp, theme.primary.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}