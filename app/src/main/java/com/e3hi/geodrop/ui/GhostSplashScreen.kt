package com.e3hi.geodrop.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.R
import kotlinx.coroutines.delay

private const val INTRO_FRAMES     = 90
private const val INTRO_FPS        = 15L
private const val FADE_OUT_MS      = 400

@Composable
fun GhostSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current

    val spriteSheet: ImageBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.intro_ghost_spritesheet)
            .asImageBitmap()
    }
    val frameWidth:  Int = remember(spriteSheet) { spriteSheet.width  / INTRO_FRAMES }
    val frameHeight: Int = remember(spriteSheet) { spriteSheet.height }

    var frame       by remember { mutableStateOf(0) }
    var fadingOut   by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (fadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = FADE_OUT_MS),
        label = "SplashFade"
    )

    // Play all frames once, then fade out and call onFinished
    LaunchedEffect(Unit) {
        for (f in 0 until INTRO_FRAMES) {
            frame = f
            delay(1000L / INTRO_FPS)
        }
        fadingOut = true
        delay(FADE_OUT_MS.toLong())
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            drawImage(
                image     = spriteSheet,
                srcOffset = IntOffset(frame * frameWidth, 0),
                srcSize   = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset(0, 0),
                dstSize   = IntSize(size.width.toInt(), size.height.toInt()),
            )
        }
    }
}
