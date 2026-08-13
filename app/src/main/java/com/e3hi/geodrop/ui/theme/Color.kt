package com.e3hi.geodrop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Approved R3 palette from docs/design-system-v1.md. State is never conveyed
// by these colors alone; shared components pair every state with icon + text.
val BrandPrimaryLight = Color(0xFF0B5D5D)
val BrandPrimaryDark = Color(0xFF4FD1C5)
val StateLocked = Color(0xFF5B6470)
val StateNear = Color(0xFFE07B24)
val FeedbackErrorLight = Color(0xFFB3261E)
val FeedbackErrorDark = Color(0xFFFFB4AB)

val SurfaceLight = Color(0xFFFDFCFA)
val SurfaceDark = Color(0xFF14171A)
val SurfaceElevatedLight = Color(0xFFF4F6F5)
val SurfaceElevatedDark = Color(0xFF1D2024)
val SurfaceHighDark = Color(0xFF262A2F)

val InkPrimaryLight = Color(0xFF14171A)
val InkPrimaryDark = Color(0xFFF2F4F5)
val InkSecondaryLight = Color(0xFF4A525C)
val InkSecondaryDark = Color(0xFFA8B2BC)

val PrimaryContainerLight = Color(0xFFC7E9E6)
val OnPrimaryContainerLight = Color(0xFF063C3C)
val PrimaryContainerDark = Color(0xFF174B4B)
val OnPrimaryContainerDark = Color(0xFFB8F2EC)
val LockedContainerLight = Color(0xFFE5E8EC)
val LockedContainerDark = Color(0xFF343A42)
val NearContainerLight = Color(0xFFFFE1C8)
val NearContainerDark = Color(0xFF573115)
val OutlineLight = Color(0xFF68727D)
val OutlineDark = Color(0xFF939DA7)
val OutlineVariantLight = Color(0xFFC5CBD1)
val OutlineVariantDark = Color(0xFF414850)

@Immutable
data class GeoDropStateColors(
    val locked: Color,
    val lockedContainer: Color,
    val onLockedContainer: Color,
    val near: Color,
    val nearContainer: Color,
    val onNearContainer: Color,
    val found: Color,
    val foundContainer: Color,
    val onFoundContainer: Color
)

internal val LightStateColors = GeoDropStateColors(
    locked = StateLocked,
    lockedContainer = LockedContainerLight,
    onLockedContainer = InkPrimaryLight,
    near = StateNear,
    nearContainer = NearContainerLight,
    onNearContainer = Color(0xFF3E1C00),
    found = BrandPrimaryLight,
    foundContainer = PrimaryContainerLight,
    onFoundContainer = OnPrimaryContainerLight
)

internal val DarkStateColors = GeoDropStateColors(
    locked = Color(0xFFA9B1BA),
    lockedContainer = LockedContainerDark,
    onLockedContainer = InkPrimaryDark,
    near = StateNear,
    nearContainer = NearContainerDark,
    onNearContainer = Color(0xFFFFE1C8),
    found = BrandPrimaryDark,
    foundContainer = PrimaryContainerDark,
    onFoundContainer = OnPrimaryContainerDark
)
