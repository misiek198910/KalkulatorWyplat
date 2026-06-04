package com.example.kalkulatorwyplat.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definiujemy schemat ciemny używając naszych nowych kolorów
private val DarkColorScheme = darkColorScheme(
    primary = AccentLime,        // Główny kolor akcentu (przyciski, aktywne elementy)
    onPrimary = BackgroundDarkGreen, // Kolor tekstu na głównym akcencie
    secondary = PrimaryTeal,
    tertiary = AccentLime,
    background = BackgroundDarkGreen, // Główne tło
    onBackground = TextWhite,         // Tekst na tle
    surface = SurfaceDarkGreen,       // Tło kart/paneli
    onSurface = TextWhite,            // Tekst na kartach
    surfaceVariant = SurfaceDarkGreen, // Wariant dla pól tekstowych
    onSurfaceVariant = TextGray,       // Tekst/ikony w nieaktywnych polach
    error = ErrorRed
)

@Composable
fun KalkulatorWyplatTheme(
    // Wymuszamy ciemny motyw dla tej aplikacji
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Ustawiamy kolor paska statusu na nasz kolor tła
            window.statusBarColor = colorScheme.background.toArgb()
            // Ikony na pasku statusu mają być jasne
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Jeśli masz plik Type.kt, dodaj go tu: typography = Typography,
        content = content
    )
}