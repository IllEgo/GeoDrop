package com.kitheapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GeoDropSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val screenGutter = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object GeoDropSize {
    val minimumTouchTarget = 48.dp
    val adjacentTargetSpacing = 8.dp
    val pin = 48.dp
    val icon = 24.dp
}

@Immutable
data class GeoDropElevationTokens(
    val base: Dp = 0.dp,
    val raised: Dp = 0.dp,
    val overlay: Dp = 0.dp
)

val GeoDropElevation = GeoDropElevationTokens()

@Immutable
data class GeoDropMotionTokens(
    val reducedMotion: Boolean,
    val unlockRevealMillis: Int,
    val pinStateChangeMillis: Int,
    val crossFadeMillis: Int
)

internal fun motionTokens(reducedMotion: Boolean) = GeoDropMotionTokens(
    reducedMotion = reducedMotion,
    unlockRevealMillis = if (reducedMotion) 0 else 250,
    pinStateChangeMillis = if (reducedMotion) 0 else 200,
    crossFadeMillis = if (reducedMotion) 80 else 150
)
