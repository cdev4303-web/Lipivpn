package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = ShieldSurfaceVariantDark,
    onPrimaryContainer = ElectricCyan,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    tertiary = EmeraldProtected,
    background = ShieldDarkBackground,
    onBackground = TextPrimary,
    surface = ShieldSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = ShieldSurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = ShieldSurfaceBorder,
    error = CrimsonAlert,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = ShieldSurfaceVariantLight,
    onPrimaryContainer = DeepCyan,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    tertiary = EmeraldProtected,
    background = ShieldLightBackground,
    onBackground = TextPrimaryLight,
    surface = ShieldSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ShieldSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = ShieldSurfaceBorderLight,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature cyber-shield palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // VPN looks most natural in crisp dark cyber security mode
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
