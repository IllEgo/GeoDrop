package com.e3hi.geodrop.ui

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.e3hi.geodrop.R
import kotlinx.coroutines.delay

// Default single-row sheet frame count and FPS
private const val GHOST_FRAMES = 10
private const val GHOST_FPS    = 10L

// Distance thresholds in metres for proximity-based states
private const val THRESHOLD_DETECTING     = 300.0
private const val THRESHOLD_TRACKING      = 150.0
private const val THRESHOLD_CLOSE         = 75.0
private const val THRESHOLD_ATTACK        = 30.0  // pickup range

/**
 * Each state pairs light-mode and dark-mode sprite sheet resources.
 * [loops] — true means the animation repeats; false plays once then returns to proximity.
 * [frameCount], [cols], [rows] describe the sheet layout (single-row sheets have rows = 1).
 */
enum class GhostState(
    @DrawableRes val lightRes: Int,
    @DrawableRes val darkRes: Int,
    val loops: Boolean,
    val frameCount: Int = GHOST_FRAMES,
    val cols: Int = GHOST_FRAMES,
    val rows: Int = 1,
) {
    IDLE(
        R.drawable.ghost_idle,
        R.drawable.ghost_idle,
        loops = true,
        frameCount = 130,
        cols = 13,
        rows = 10,
    ),
    DETECTING(
        R.drawable.detecting_white_spritesheet,
        R.drawable.detecting_blue_spritesheet,
        loops = true,
    ),
    TRACKING(
        R.drawable.tracking_white_spritesheet,
        R.drawable.tracking_blue_spritesheet,
        loops = true,
    ),
    CLOSE_PROXIMITY(
        R.drawable.ghost_close_proximity,
        R.drawable.ghost_close_proximity,
        loops = true,
        frameCount = 96,
        cols = 12,
        rows = 8,
    ),
    /** Looping when within pickup range. */
    ATTACK(
        R.drawable.ghost_close_proximity,
        R.drawable.ghost_close_proximity,
        loops = true,
        frameCount = 96,
        cols = 12,
        rows = 8,
    ),
    /** One-shot played when the user taps the pick-up button. */
    PICKUP(
        R.drawable.ghost_pickup_drop,
        R.drawable.ghost_pickup_drop,
        loops = false,
        frameCount = 15,
        cols = 5,
        rows = 3,
    ),
    DROPPING(
        R.drawable.dropping_white_spritesheet,
        R.drawable.dropping_blue_spritesheet,
        loops = false,
    ),
    /** One-shot played when the user adds a drop. */
    GHOST_DROP(
        R.drawable.ghost_drop,
        R.drawable.ghost_drop,
        loops = false,
        frameCount = 96,
        cols = 12,
        rows = 8,
    );
}

/** Returns the looping proximity state matching the given distance. */
private fun proximityState(closestDropMeters: Double?): GhostState = when {
    closestDropMeters == null                        -> GhostState.IDLE
    closestDropMeters > THRESHOLD_DETECTING          -> GhostState.DETECTING
    closestDropMeters > THRESHOLD_TRACKING           -> GhostState.TRACKING
    closestDropMeters > THRESHOLD_CLOSE              -> GhostState.CLOSE_PROXIMITY
    else                                             -> GhostState.ATTACK
}

/**
 * Animated ghost mascot.
 *
 * Proximity states loop continuously. [triggerFound] and [triggerDropping] trigger
 * one-shot animations that play once and then return to the current proximity state.
 *
 * Automatically switches between white (light mode) and blue (dark mode) sheets.
 *
 * @param closestDropMeters Distance to the nearest community drop in metres, or null if none.
 * @param triggerFound      Becomes true when a drop is picked up (rising edge triggers animation).
 * @param triggerDropping   Becomes true while a drop submission is in progress.
 */
@Composable
fun GhostMascot(
    closestDropMeters: Double?,
    triggerFound: Boolean,
    triggerDropping: Boolean,
    modifier: Modifier = Modifier,
) {
    val context   = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    // Keep the latest proximity state accessible inside coroutines without stale closures
    val latestProximity by rememberUpdatedState(proximityState(closestDropMeters))

    var currentState  by remember { mutableStateOf(GhostState.IDLE) }
    var oneShotActive by remember { mutableStateOf(false) }
    var frame         by remember { mutableStateOf(0) }

    // Load the correct sheet whenever the state or theme changes
    val spriteSheet: ImageBitmap = remember(currentState, darkTheme) {
        val resId = if (darkTheme) currentState.darkRes else currentState.lightRes
        BitmapFactory.decodeResource(context.resources, resId).asImageBitmap()
    }
    val frameWidth: Int  = remember(spriteSheet, currentState) { spriteSheet.width  / currentState.cols }
    val frameHeight: Int = remember(spriteSheet, currentState) { spriteSheet.height / currentState.rows }

    // ── One-shot: drop picked up ──────────────────────────────────────────────
    LaunchedEffect(triggerFound) {
        if (triggerFound) {
            oneShotActive = true
            currentState  = GhostState.PICKUP
        }
    }

    // ── One-shot: drop being submitted ────────────────────────────────────────
    LaunchedEffect(triggerDropping) {
        if (triggerDropping) {
            oneShotActive = true
            currentState  = GhostState.GHOST_DROP
        }
    }

    // ── Proximity updates (skipped while a one-shot is playing) ───────────────
    LaunchedEffect(closestDropMeters) {
        if (!oneShotActive) {
            currentState = latestProximity
        }
    }

    // ── Frame ticker ──────────────────────────────────────────────────────────
    LaunchedEffect(currentState) {
        val state     = currentState    // capture for this animation cycle
        val isOneShot = oneShotActive
        val totalFrames = state.frameCount
        frame = 0
        while (true) {
            delay(1000L / GHOST_FPS)
            val next = frame + 1
            if (next >= totalFrames) {
                if (state.loops && !isOneShot) {
                    frame = 0
                } else {
                    // One-shot complete — hold last frame briefly then return to proximity
                    frame = totalFrames - 1
                    delay(80L)
                    oneShotActive = false
                    currentState  = latestProximity
                    break
                }
            } else {
                frame = next
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    Canvas(modifier = modifier) {
        val col = frame % currentState.cols
        val row = frame / currentState.cols
        drawImage(
            image     = spriteSheet,
            srcOffset = IntOffset(col * frameWidth, row * frameHeight),
            srcSize   = IntSize(frameWidth, frameHeight),
            dstOffset = IntOffset(0, 0),
            dstSize   = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}
