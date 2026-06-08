package com.example.kalkulatorwyplat.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AppInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    primaryColor: Color = Color(0xFF86D957) // Nasza limonka
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Text(text = message, color = Color.LightGray, fontSize = 14.sp)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ROZUMIEM", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF132A22), // Ciemnozielone tło (Surface)
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}