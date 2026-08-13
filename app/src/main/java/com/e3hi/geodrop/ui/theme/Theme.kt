package com.e3hi.geodrop.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF003735),
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = BrandPrimaryLight,
    secondary = Color(0xFFB8C5C5),
    onSecondary = Color(0xFF243333),
    secondaryContainer = LockedContainerDark,
    onSecondaryContainer = InkPrimaryDark,
    tertiary = StateNear,
    onTertiary = Color(0xFF431F00),
    tertiaryContainer = NearContainerDark,
    onTertiaryContainer = Color(0xFFFFE1C8),
    background = SurfaceDark,
    onBackground = InkPrimaryDark,
    surface = SurfaceDark,
    onSurface = InkPrimaryDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = InkSecondaryDark,
    surfaceTint = BrandPrimaryDark,
    inverseSurface = InkPrimaryDark,
    inverseOnSurface = SurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Color.Black,
    error = FeedbackErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = BrandPrimaryDark,
    secondary = StateLocked,
    onSecondary = Color.White,
    secondaryContainer = LockedContainerLight,
    onSecondaryContainer = InkPrimaryLight,
    tertiary = StateNear,
    onTertiary = Color(0xFF431F00),
    tertiaryContainer = NearContainerLight,
    onTertiaryContainer = Color(0xFF3E1C00),
    background = SurfaceLight,
    onBackground = InkPrimaryLight,
    surface = SurfaceLight,
    onSurface = InkPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = InkSecondaryLight,
    surfaceTint = BrandPrimaryLight,
    inverseSurface = InkPrimaryLight,
    inverseOnSurface = SurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Color.Black,
    error = FeedbackErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val LocalGeoDropStateColors = compositionLocalOf { LightStateColors }
private val LocalGeoDropMotion = compositionLocalOf { motionTokens(reducedMotion = false) }

object GeoDropThemeTokens {
    val stateColors: GeoDropStateColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGeoDropStateColors.current

    val motion: GeoDropMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalGeoDropMotion.current
}

@Composable
private fun systemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

@Composable
fun GeoDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    reducedMotion: Boolean? = null,
    content: @Composable () -> Unit
) {
    // Dynamic color remains an explicit compatibility parameter, but brand and
    // state tokens intentionally win in R3. The map/state system must not vary
    // by wallpaper or become indistinguishable from terrain colors.
    @Suppress("UNUSED_VARIABLE")
    val dynamicColorCompatibility = dynamicColor
    val resolvedReducedMotion = reducedMotion ?: systemReducedMotion()
    CompositionLocalProvider(
        LocalGeoDropStateColors provides if (darkTheme) DarkStateColors else LightStateColors,
        LocalGeoDropMotion provides motionTokens(resolvedReducedMotion)
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = GeoDropTypography,
            shapes = GeoDropShapes,
            content = content
        )
    }
}
