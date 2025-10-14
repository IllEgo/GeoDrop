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
    primary = CanopyGreen80,
    onPrimary = Color(0xFF00391F),
    primaryContainer = CanopyContainerDark,
    onPrimaryContainer = CanopyContainerLight,
    inversePrimary = CanopyGreen40,
    secondary = FernGreen80,
    onSecondary = Color(0xFF00382B),
    secondaryContainer = FernContainerDark,
    onSecondaryContainer = FernContainerLight,
    tertiary = GoldenLime80,
    onTertiary = Color(0xFF2D2300),
    tertiaryContainer = GoldenContainerDark,
    onTertiaryContainer = GoldenContainerLight,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = CanopyGreen80,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Color(0xFF000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = CanopyGreen40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CanopyContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = CanopyGreen80,
    secondary = FernGreen40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = FernContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = GoldenLime40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = GoldenContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = Color(0xFFFFFFFF),
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = CanopyGreen40,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
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