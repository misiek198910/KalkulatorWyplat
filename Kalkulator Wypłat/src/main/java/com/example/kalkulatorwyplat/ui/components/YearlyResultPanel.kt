package com.example.kalkulatorwyplat.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kalkulatorwyplat.R
import com.example.kalkulatorwyplat.data.YearlySalaryResult

@SuppressLint("DefaultLocale")
@Composable
fun YearlyResultPanel(
    result: YearlySalaryResult,
    modifier: Modifier = Modifier
) {
    val theme = MaterialTheme.colorScheme
    val alertColor = Color(0xFFCF6679)
    val safeColor = theme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.yearly_summary_title),
                style = MaterialTheme.typography.titleMedium,
                color = theme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.background, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.yearly_average_netto),
                    color = theme.onBackground,
                    fontSize = 12.sp
                )
                Text(
                    text = String.format("%.2f zł", result.averageNetto),
                    color = theme.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            YearlyDetailRow(label = stringResource(id = R.string.yearly_total_gross), value = result.totalGross, color = theme.onSurfaceVariant)
            YearlyDetailRow(label = stringResource(id = R.string.yearly_total_netto), value = result.totalNetto, color = theme.primary, isBold = true)

            Divider(color = theme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

            YearlyDetailRow(label = stringResource(id = R.string.yearly_total_zus), value = result.totalZus, color = theme.onSurfaceVariant)
            YearlyDetailRow(label = stringResource(id = R.string.yearly_total_health), value = result.totalHealthInsurance, color = theme.onSurfaceVariant)
            YearlyDetailRow(label = stringResource(id = R.string.yearly_total_tax), value = result.totalIncomeTax, color = theme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // Komunikat o progu podatkowym
            val thresholdMsg = if (result.crossedTaxThresholdMonth != null) {
                stringResource(id = R.string.tax_threshold_alert, result.crossedTaxThresholdMonth)
            } else {
                stringResource(id = R.string.tax_threshold_safe)
            }

            val thresholdColor = if (result.crossedTaxThresholdMonth != null) alertColor else safeColor

            Text(
                text = thresholdMsg,
                color = thresholdColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun YearlyDetailRow(label: String, value: Float, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = String.format("%.2f zł", value),
            color = color,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}