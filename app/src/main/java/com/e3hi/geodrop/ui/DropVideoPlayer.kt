package com.e3hi.geodrop.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.e3hi.geodrop.R

@Composable
fun DropVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    showController: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current

    val mediaItem = remember(videoUri) { MediaItem.fromUri(videoUri) }
    val exoPlayer = remember(mediaItem) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            playWhenReady = autoPlay
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var embeddedPlayerView by remember { mutableStateOf<PlayerView?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = shape
    ) {
        Box {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = showController
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setShutterBackgroundColor(Color.Black.toArgb())
                    }.also { playerView ->
                        embeddedPlayerView = playerView
                    }
                },
                update = { playerView ->
                    embeddedPlayerView = playerView
                    if (isFullScreen) {
                        if (playerView.player != null) {
                            playerView.player = null
                        }
                    } else if (playerView.player !== exoPlayer) {
                        playerView.player = exoPlayer
                    }
                }
            )

            if (!isFullScreen) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.6f)),
                    onClick = { isFullScreen = true }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = stringResource(R.string.content_description_expand_video),
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (isFullScreen) {
        FullScreenVideoDialog(
            exoPlayer = exoPlayer,
            onDismissRequest = {
                isFullScreen = false
                embeddedPlayerView?.player = exoPlayer
            }
        )
    }
}

@Composable
private fun FullScreenVideoDialog(
    exoPlayer: ExoPlayer,
    onDismissRequest: () -> Unit
) {
    var fullScreenPlayerView by remember { mutableStateOf<PlayerView?>(null) }

    Dialog(
        onDismissRequest = {
            fullScreenPlayerView?.player = null
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setShutterBackgroundColor(Color.Black.toArgb())
                    }.also { playerView ->
                        fullScreenPlayerView = playerView
                    }
                },
                update = { playerView ->
                    fullScreenPlayerView = playerView
                    if (playerView.player !== exoPlayer) {
                        playerView.player = exoPlayer
                    }
                }
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f)),
                onClick = {
                    fullScreenPlayerView?.player = null
                    onDismissRequest()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.FullscreenExit,
                    contentDescription = stringResource(R.string.content_description_collapse_video),
                    tint = Color.White
                )
            }
        }
    }
}