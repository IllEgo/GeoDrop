package com.e3hi.geodrop.ui

import android.app.Activity
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.io.IOException

class AudioRecorderActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null
    private var recordedFile: File? = null
    private var deliveredResult: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        setContent {
            GeoDropTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AudioRecorderRoute(
                        onStartRecording = ::startRecording,
                        onStopRecording = ::stopRecordingAndKeep,
                        onDiscardRecording = ::discardRecording,
                        onFinish = ::finishWithRecording,
                        onCancel = ::cancelAndFinish
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // If the activity goes to the background while recording, stop and discard the capture.
        if (!isFinishing && recorder != null) {
            discardRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!deliveredResult) {
            discardRecording()
        } else {
            recorder?.let { activeRecorder ->
                runCatching { activeRecorder.stop() }
                runCatching { activeRecorder.release() }
                recorder = null
            }
        }
    }

    private fun startRecording(onError: (String) -> Unit): Boolean {
        discardRecording()

        val audioDir = File(cacheDir, "audio").apply { if (!exists()) mkdirs() }
        val file = try {
            File.createTempFile("geodrop_audio_", ".m4a", audioDir)
        } catch (e: IOException) {
            onError("Couldn't prepare space for the recording. Try again.")
            null
        } ?: return false

        val recorder = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
        } catch (e: Exception) {
            file.delete()
            onError("Recording unavailable on this device.")
            return false
        }

        return try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            this.recorder = recorder
            recordedFile = file
            true
        } catch (e: Exception) {
            runCatching { recorder.release() }
            file.delete()
            onError("Couldn't start recording. Try again.")
            false
        }
    }

    private fun stopRecordingAndKeep(onError: (String) -> Unit): Boolean {
        val recorder = recorder ?: return recordedFile?.exists() == true

        var success = true
        try {
            recorder.stop()
        } catch (e: RuntimeException) {
            success = false
        } catch (e: Exception) {
            success = false
        } finally {
            recorder.release()
            this.recorder = null
        }

        if (!success) {
            recordedFile?.delete()
            recordedFile = null
            onError("Recording failed. Try again.")
            return false
        }

        val file = recordedFile
        if (file == null || !file.exists()) {
            onError("Recording unavailable. Try again.")
            recordedFile = null
            return false
        }

        return true
    }

    private fun discardRecording() {
        val recorder = recorder
        if (recorder != null) {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
            this.recorder = null
        }
        recordedFile?.let { file ->
            runCatching { file.delete() }
        }
        recordedFile = null
    }

    private fun cancelAndFinish() {
        discardRecording()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun finishWithRecording() {
        val file = recordedFile ?: run {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val data = Intent().apply {
            setDataAndType(uri, "audio/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setResult(Activity.RESULT_OK, data)
        deliveredResult = true
        recordedFile = null
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioRecorderRoute(
    onStartRecording: ((onError: (String) -> Unit) -> Boolean),
    onStopRecording: ((onError: (String) -> Unit) -> Boolean),
    onDiscardRecording: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var hasRecording by rememberSaveable { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var lastKnownDuration by rememberSaveable { mutableStateOf(0L) }
    var recordingStartTime by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isRecording, recordingStartTime) {
        if (isRecording) {
            val start = recordingStartTime ?: SystemClock.elapsedRealtime().also { recordingStartTime = it }
            while (isActive) {
                lastKnownDuration = SystemClock.elapsedRealtime() - start
                delay(200)
            }
        }
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            errorMessage = null
        }
    }

    val onPrimaryClick = {
        if (isRecording) {
            val stopped = onStopRecording { message -> errorMessage = message }
            if (stopped) {
                isRecording = false
                hasRecording = true
                statusMessage = "Recording saved."
                recordingStartTime = null
            } else {
                isRecording = false
                hasRecording = false
                recordingStartTime = null
                lastKnownDuration = 0L
            }
        } else {
            if (hasRecording) {
                onDiscardRecording()
                hasRecording = false
                lastKnownDuration = 0L
                statusMessage = null
            }
            val started = onStartRecording { message -> errorMessage = message }
            if (started) {
                isRecording = true
                hasRecording = false
                statusMessage = "Recording in progress..."
                recordingStartTime = SystemClock.elapsedRealtime()
                lastKnownDuration = 0L
            }
        }
    }

    val displayDurationText = when {
        isRecording -> formatDuration(lastKnownDuration) + " (recording)"
        hasRecording && lastKnownDuration > 0L -> "Recorded length: ${formatDuration(lastKnownDuration)}"
        else -> "Tap start to capture a short voice note."
    }

    BackHandler { onCancel() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Record audio") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel recording")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Capture a quick audio message for your drop.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = displayDurationText,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                val label = when {
                    isRecording -> "Stop recording"
                    hasRecording -> "Start new recording"
                    else -> "Start recording"
                }
                Text(label)
            }

            if (hasRecording) {
                OutlinedButton(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use this recording")
                }

                TextButton(onClick = {
                    onDiscardRecording()
                    hasRecording = false
                    lastKnownDuration = 0L
                    statusMessage = null
                }) {
                    Text("Discard recording")
                }
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Make sure you're in a quiet spot so others can hear your message clearly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0L) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}