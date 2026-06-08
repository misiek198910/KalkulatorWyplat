package com.example.kalkulatorwyplat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kalkulatorwyplat.data.SalaryResult

@Composable
fun ResultPanel(
    result: SalaryResult,
    highlightNetto: Boolean, // True = domyślny tryb (wpisano brutto), False = tryb "od netto"
    onMoreClick: (SalaryResult) -> Unit
) {
    val theme = MaterialTheme.colorScheme
    // Kolor akcentu (limonka) dla głównej wartości, szary dla drugorzędnej
    val nettoColor = if (highlightNetto) theme.primary else theme.onSurfaceVariant
    val bruttoColor = if (!highlightNetto) theme.primary else theme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onMoreClick(result) },
        colors = CardDefaults.cardColors(
            containerColor = theme.surface, // Używamy koloru powierzchni z motywu
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Typ umowy
                Text(
                    text = result.type,
                    style = MaterialTheme.typography.titleSmall,
                    color = theme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sekcja Brutto i Netto obok siebie
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Kolumna Brutto
                    Column {
                        Text("BRUTTO", color = bruttoColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.2f zł", result.gross),
                            color = bruttoColor,
                            fontSize = if (!highlightNetto) 18.sp else 14.sp, // Większe jeśli jest głównym wynikiem
                            fontWeight = if (!highlightNetto) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Kolumna Netto
                    Column(horizontalAlignment = Alignment.End) {
                        Text("NETTO (DO RĘKI)", color = nettoColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.2f zł", result.netto),
                            color = nettoColor,
                            fontSize = if (highlightNetto) 22.sp else 18.sp, // Jeszcze większe dla Netto
                            fontWeight = if (highlightNetto) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }

            // Strzałka w prawo
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = theme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}