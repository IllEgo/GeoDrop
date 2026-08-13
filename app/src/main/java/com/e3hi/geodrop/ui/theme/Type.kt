package com.e3hi.geodrop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Android's platform sans is the offline-safe Roboto family and renders ʻokina
// and kahakō. R3 removes network-loaded display faces from the foundation.
val GeoDropSans = FontFamily.SansSerif
val GeoDropMono = FontFamily.Monospace

// Compatibility aliases for pre-R4 screens. New components use GeoDropSans.
val RoundedMFontFamily = GeoDropSans
val RalewayFontFamily = GeoDropSans

private fun geoDropStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    family: FontFamily = GeoDropSans
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

val GeoDropTypography = Typography(
    displayLarge = geoDropStyle(34, 40, FontWeight.SemiBold),
    displayMedium = geoDropStyle(34, 40, FontWeight.SemiBold),
    displaySmall = geoDropStyle(34, 40, FontWeight.SemiBold),
    headlineLarge = geoDropStyle(26, 32, FontWeight.SemiBold),
    headlineMedium = geoDropStyle(26, 32, FontWeight.SemiBold),
    headlineSmall = geoDropStyle(26, 32, FontWeight.SemiBold),
    titleLarge = geoDropStyle(20, 26, FontWeight.SemiBold),
    titleMedium = geoDropStyle(20, 26, FontWeight.SemiBold),
    titleSmall = geoDropStyle(20, 26, FontWeight.SemiBold),
    bodyLarge = geoDropStyle(17, 26, FontWeight.Normal),
    bodyMedium = geoDropStyle(15, 22, FontWeight.Normal),
    bodySmall = geoDropStyle(13, 18, FontWeight.Normal),
    labelLarge = geoDropStyle(13, 18, FontWeight.Medium),
    labelMedium = geoDropStyle(13, 18, FontWeight.Medium),
    labelSmall = geoDropStyle(13, 18, FontWeight.Medium)
)

val RewardCodeTextStyle = geoDropStyle(
    size = 28,
    lineHeight = 34,
    weight = FontWeight.Medium,
    family = GeoDropMono
)

val Typography = GeoDropTypography
