package com.e3hi.geodrop.ui.theme

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
    primary = OceanBlue80,
    onPrimary = Color(0xFF0B1B49),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDCE7FF),
    inversePrimary = OceanBlue40,
    secondary = Amber80,
    onSecondary = Color(0xFF2F1500),
    secondaryContainer = Color(0xFF5A3100),
    onSecondaryContainer = Color(0xFFFFE2B8),
    tertiary = Emerald80,
    onTertiary = Color(0xFF003322),
    tertiaryContainer = Color(0xFF005C3A),
    onTertiaryContainer = Color(0xFFC4F8E7),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFDCE7FF),
    surface = Color(0xFF111C3B),
    onSurface = Color(0xFFDCE7FF),
    surfaceVariant = Color(0xFF2D3A60),
    onSurfaceVariant = Color(0xFFC5CEFF),
    surfaceTint = OceanBlue80,
    inverseSurface = Color(0xFFE6ECFF),
    inverseOnSurface = Color(0xFF111C3B),
    outline = Midnight80,
    outlineVariant = Color(0xFF424F7A),
    scrim = Color(0xFF000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0B1B49),
    inversePrimary = OceanBlue80,
    secondary = Amber40,
    onSecondary = Color(0xFF2F1500),
    secondaryContainer = Color(0xFFFFE2B8),
    onSecondaryContainer = Color(0xFF2F1500),
    tertiary = Emerald40,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = Color(0xFFC4F8E7),
    onTertiaryContainer = Color(0xFF003322),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF111C3B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111C3B),
    surfaceVariant = Color(0xFFE0E7FF),
    onSurfaceVariant = Color(0xFF303D66),
    surfaceTint = OceanBlue40,
    inverseSurface = Color(0xFF2A3658),
    inverseOnSurface = Color(0xFFF4F6FB),
    outline = Midnight40,
    outlineVariant = Color(0xFFC5CEFF),
    scrim = Color(0xFF000000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun GeoDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}