// ui/DropDetailActivity.kt
package com.e3hi.geodrop.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.e3hi.geodrop.MainActivity
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.DropVoteType
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.data.applyUserVote
import com.e3hi.geodrop.geo.DropDecisionReceiver
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import com.e3hi.geodrop.util.formatTimestamp
import com.e3hi.geodrop.util.EXTRA_SHOW_DECISION_OPTIONS
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.launch

class DropDetailActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dropId = intent.getStringExtra("dropId").orEmpty()
        val initialText = intent.getStringExtra("dropText")
        val initialLat = if (intent.hasExtra("dropLat")) intent.getDoubleExtra("dropLat", 0.0) else null
        val initialLng = if (intent.hasExtra("dropLng")) intent.getDoubleExtra("dropLng", 0.0) else null
        val initialCreatedAt = intent.getLongExtra("dropCreatedAt", -1L).takeIf { it > 0L }
        val initialGroupCode = intent.getStringExtra("dropGroupCode")?.takeIf { it.isNotBlank() }
        val initialContentType = DropContentType.fromRaw(intent.getStringExtra("dropContentType"))
        val initialMediaUrl = intent.getStringExtra("dropMediaUrl")?.takeIf { it.isNotBlank() }
        val initialMediaMimeType = intent.getStringExtra("dropMediaMimeType")?.takeIf { it.isNotBlank() }
        val initialMediaData = intent.getStringExtra("dropMediaData")?.takeIf { it.isNotBlank() }
        val showDecisionOptions = intent.getBooleanExtra(EXTRA_SHOW_DECISION_OPTIONS, false)

        setContent {
            GeoDropTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current

                    val initialState = if (dropId.isBlank()) {
                        if (
                            initialText != null ||
                            initialLat != null ||
                            initialLng != null ||
                            initialCreatedAt != null ||
                            initialGroupCode != null ||
                            initialMediaUrl != null ||
                            initialMediaData != null
                        ) {
                            DropDetailUiState.Loaded(
                                text = initialText,
                                lat = initialLat,
                                lng = initialLng,
                                createdAt = initialCreatedAt,
                                groupCode = initialGroupCode,
                                contentType = initialContentType,
                                mediaUrl = initialMediaUrl,
                                mediaMimeType = initialMediaMimeType,
                                mediaData = initialMediaData,
                                upvoteCount = 0,
                                downvoteCount = 0,
                                voteMap = emptyMap()
                            )
                        } else {
                            DropDetailUiState.NotFound
                        }
                    } else if (
                        initialText != null ||
                        initialLat != null ||
                        initialLng != null ||
                        initialCreatedAt != null ||
                        initialGroupCode != null ||
                        initialMediaUrl != null ||
                        initialMediaData != null
                    ) {
                        DropDetailUiState.Loaded(
                            text = initialText,
                            lat = initialLat,
                            lng = initialLng,
                            createdAt = initialCreatedAt,
                            groupCode = initialGroupCode,
                            contentType = initialContentType,
                            mediaUrl = initialMediaUrl,
                            mediaMimeType = initialMediaMimeType,
                            mediaData = initialMediaData,
                            upvoteCount = 0,
                            downvoteCount = 0,
                            voteMap = emptyMap()
                        )
                    } else {
                        DropDetailUiState.Loading
                    }

                    var state by remember { mutableStateOf(initialState) }

                    LaunchedEffect(dropId) {
                        if (dropId.isBlank()) return@LaunchedEffect
                        val previousLoaded = state as? DropDetailUiState.Loaded
                        if (previousLoaded == null) {
                            state = DropDetailUiState.Loading
                        }
                        val doc = Firebase.firestore.collection("drops").document(dropId).get().awaitOrNull()

                        if (doc == null) {
                            previousLoaded?.let {
                                Toast.makeText(
                                    context,
                                    "Couldn't refresh drop details. Showing saved info.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                state = it
                            } ?: run {
                                state = DropDetailUiState.NotFound
                            }
                            return@LaunchedEffect
                        }

                        val isDeleted = doc.getBoolean("isDeleted") == true
                        if (isDeleted) {
                            state = DropDetailUiState.Deleted
                            return@LaunchedEffect
                        }

                        val initialLoaded = initialState as? DropDetailUiState.Loaded
                        val sanitizedVoteMap = parseVoteMap(doc.get("voteMap"))
                        state = DropDetailUiState.Loaded(
                            text = doc.getString("text")?.takeIf { it.isNotBlank() }
                                ?: previousLoaded?.text
                                ?: initialText,
                            lat = doc.getDouble("lat") ?: previousLoaded?.lat ?: initialLat,
                            lng = doc.getDouble("lng") ?: previousLoaded?.lng ?: initialLng,
                            createdAt = doc.getLong("createdAt")?.takeIf { it > 0L }
                                ?: previousLoaded?.createdAt
                                ?: initialCreatedAt,
                            groupCode = doc.getString("groupCode")?.takeIf { it.isNotBlank() }
                                ?: previousLoaded?.groupCode
                                ?: initialGroupCode,
                            contentType = doc.getString("contentType")?.let { DropContentType.fromRaw(it) }
                                ?: previousLoaded?.contentType
                                ?: initialContentType,
                            mediaUrl = doc.getString("mediaUrl")?.takeIf { it.isNotBlank() }
                                ?: previousLoaded?.mediaUrl
                                ?: initialMediaUrl,
                            mediaMimeType = doc.getString("mediaMimeType")?.takeIf { it.isNotBlank() }
                                ?: previousLoaded?.mediaMimeType
                                ?: initialMediaMimeType,
                            mediaData = (
                                    doc.getString("mediaData")?.takeIf { it.isNotBlank() }
                                        ?: doc.getString("audioFile")?.takeIf { it.isNotBlank() }
                                        ?: doc.getBlob("mediaData")?.toBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                                        ?: doc.getBlob("audioFile")?.toBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                                    )
                                ?: previousLoaded?.mediaData
                                ?: initialMediaData,
                            upvoteCount = doc.getLong("upvoteCount")
                                ?: previousLoaded?.upvoteCount
                                ?: initialLoaded?.upvoteCount
                                ?: 0L,
                            downvoteCount = doc.getLong("downvoteCount")
                                ?: previousLoaded?.downvoteCount
                                ?: initialLoaded?.downvoteCount
                                ?: 0L,
                            voteMap = sanitizedVoteMap
                                ?: previousLoaded?.voteMap
                                ?: initialLoaded?.voteMap
                                ?: emptyMap()
                        )
                    }
                    val appContext = context.applicationContext
                    val noteInventory = remember(appContext) { NoteInventory(appContext) }
                    val auth = remember { FirebaseAuth.getInstance() }
                    var currentUser by remember { mutableStateOf(auth.currentUser) }
                    DisposableEffect(auth) {
                        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                            currentUser = firebaseAuth.currentUser
                        }
                        auth.addAuthStateListener(listener)
                        onDispose { auth.removeAuthStateListener(listener) }
                    }
                    val currentUserId = currentUser?.uid
                    val repo = remember { FirestoreRepo() }
                    val scope = rememberCoroutineScope()
                    var decisionHandled by remember(dropId) { mutableStateOf(false) }
                    var decisionStatusMessage by remember(dropId) { mutableStateOf<String?>(null) }
                    var decisionProcessing by remember(dropId) { mutableStateOf(false) }
                    var isVoting by remember(dropId) { mutableStateOf(false) }
                    val isAlreadyCollected = remember(dropId) {
                        dropId.isNotBlank() && noteInventory.isCollected(dropId)
                }

                val isAlreadyIgnored = remember(dropId) {
                    dropId.isNotBlank() && noteInventory.isIgnored(dropId)
                }
                val shouldShowDecisionPrompt =
                    showDecisionOptions && dropId.isNotBlank() && !isAlreadyCollected && !isAlreadyIgnored && !decisionHandled
                val defaultDecisionMessage = when {
                    showDecisionOptions && isAlreadyCollected -> "You've already picked up this drop."
                    showDecisionOptions && isAlreadyIgnored -> "You've already ignored this drop."
                    else -> null
                }
                val decisionMessage = decisionStatusMessage ?: defaultDecisionMessage

                Scaffold(
                    topBar = {
                        val activity = context as? ComponentActivity
                        CenterAlignedTopAppBar(
                            title = { Text("Drop details") },
                            navigationIcon = {
                                IconButton(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) { paddingValues ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val loadedState = state as? DropDetailUiState.Loaded
                        val message = when (val current = state) {
                            is DropDetailUiState.Loaded -> {
                                val fallback = when (current.contentType) {
                                    DropContentType.TEXT -> "Not available"
                                    DropContentType.PHOTO -> "Photo drop"
                                    DropContentType.AUDIO -> "Audio drop"
                                    }
                                current.text?.takeIf { it.isNotBlank() } ?: fallback
                            }
                            DropDetailUiState.Loading -> "Loading…"
                            DropDetailUiState.Deleted -> "Not available"
                            DropDetailUiState.NotFound -> "Not available"
                        }

                        val typeText = when (val current = state) {
                            is DropDetailUiState.Loaded -> when (current.contentType) {
                                DropContentType.TEXT -> "Text note"
                                DropContentType.PHOTO -> "Photo drop"
                                DropContentType.AUDIO -> "Audio drop"
                            }
                            DropDetailUiState.Loading -> "Loading…"
                            DropDetailUiState.Deleted -> "Not available"
                            DropDetailUiState.NotFound -> "Not available"
                        }
                        val typeIcon: ImageVector = when (loadedState?.contentType) {
                            DropContentType.TEXT -> Icons.Rounded.Article
                            DropContentType.PHOTO -> Icons.Rounded.PhotoCamera
                            DropContentType.AUDIO -> Icons.Rounded.GraphicEq
                            null -> Icons.Rounded.Info
                        }

                        val visibilityText = when (val current = state) {
                            is DropDetailUiState.Loaded -> current.groupCode?.let { "Group-only · $it" } ?: "Public drop"
                            DropDetailUiState.Loading -> "Loading…"
                            DropDetailUiState.Deleted -> "Not available"
                            DropDetailUiState.NotFound -> "Not available"
                        }
                        val visibilityIcon: ImageVector = if (loadedState?.groupCode != null) {
                            Icons.Rounded.Groups
                        } else {
                            Icons.Rounded.Public
                        }

                        val statusTag: DropDetailTagData? = when (state) {
                            DropDetailUiState.Deleted -> DropDetailTagData(
                                label = "Deleted drop",
                                icon = Icons.Rounded.DeleteForever,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            DropDetailUiState.NotFound -> DropDetailTagData(
                                label = "Not available",
                                icon = Icons.Rounded.Block,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DropDetailUiState.Loading -> DropDetailTagData(
                                label = "Loading",
                                icon = Icons.Rounded.HourglassEmpty,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> null
                        }

                        val mediaAttachment = when (loadedState) {
                            null -> null
                            else -> remember(
                                loadedState.mediaUrl,
                                loadedState.mediaData,
                                loadedState.mediaMimeType,
                                loadedState.contentType
                            ) {
                                resolveMediaAttachment(context, loadedState)
                            }
                        }

                        val createdAtText = when (val current = state) {
                            is DropDetailUiState.Loaded -> current.createdAt?.let { formatTimestamp(it) }
                            DropDetailUiState.Loading -> "Loading…"
                            DropDetailUiState.Deleted -> null
                            DropDetailUiState.NotFound -> null
                        }

                        val latText = when (val current = state) {
                            is DropDetailUiState.Loaded -> current.lat?.let { formatCoordinate(it) } ?: "-"
                            DropDetailUiState.Loading -> "Loading…"
                            DropDetailUiState.Deleted -> "-"
                            DropDetailUiState.NotFound -> "-"
                        }
                        val lngText = when (val current = state) {
                            is DropDetailUiState.Loaded -> current.lng?.let { formatCoordinate(it) } ?: "-"
                            DropDetailUiState.Loading -> "Loading…"
                            DropDetailUiState.Deleted -> "-"
                            DropDetailUiState.NotFound -> "-"
                        }
                        val locationText = when {
                            latText == "Loading…" || lngText == "Loading…" -> "Loading…"
                            latText == "-" && lngText == "-" -> "Not available"
                            else -> "$latText, $lngText"
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DropDetailTag(text = typeText, icon = typeIcon)
                                    DropDetailTag(text = visibilityText, icon = visibilityIcon)
                                    statusTag?.let {
                                        DropDetailTag(
                                            text = it.label,
                                            icon = it.icon,
                                            containerColor = it.containerColor,
                                            contentColor = it.contentColor
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Message", style = MaterialTheme.typography.titleMedium)
                                    Text(message, style = MaterialTheme.typography.bodyLarge)
                                }

                                if (state is DropDetailUiState.Deleted) {
                                    Text(
                                        text = "This drop has been deleted.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                loadedState?.let {
                                    DropVoteSection(
                                        state = it,
                                        currentUserId = currentUserId,
                                        isVoting = isVoting,
                                        onVote = { desiredVote ->
                                            val currentLoaded = state as? DropDetailUiState.Loaded
                                                ?: return@DropVoteSection
                                            val userId = currentUserId
                                            if (userId.isNullOrBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Sign in to vote on drops.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@DropVoteSection
                                            }
                                            if (dropId.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Unable to vote on this drop.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@DropVoteSection
                                            }

                                            val previous = currentLoaded
                                            val updated = currentLoaded.applyUserVoteLocal(userId, desiredVote)
                                            if (updated == previous) return@DropVoteSection

                                            state = updated
                                            isVoting = true
                                            scope.launch {
                                                try {
                                                    repo.voteOnDrop(dropId, userId, desiredVote)
                                                } catch (e: Exception) {
                                                    state = previous
                                                    val message = e.localizedMessage?.takeIf { it.isNotBlank() }
                                                        ?: "Couldn't update your vote. Try again."
                                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isVoting = false
                                                }
                                            }
                                        }
                                    )
                                }

                                if (loadedState?.contentType == DropContentType.PHOTO) {
                                    val link = loadedState.mediaUrl
                                    if (!link.isNullOrBlank()) {
                                        val imageRequest = remember(link) {
                                            ImageRequest.Builder(context)
                                                .data(link)
                                                .crossfade(true)
                                                .build()
                                        }

                                        AsyncImage(
                                            model = imageRequest,
                                            contentDescription = loadedState.text ?: "Photo drop",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 180.dp, max = 360.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                if (loadedState != null && mediaAttachment != null) {
                                    val buttonLabel = when (loadedState.contentType) {
                                        DropContentType.TEXT -> "Open attachment"
                                        DropContentType.PHOTO -> "View photo"
                                        DropContentType.AUDIO -> "Play audio"
                                    }
                                    Button(
                                        onClick = {
                                            when (mediaAttachment) {
                                                is MediaAttachment.Link -> {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mediaAttachment.url))
                                                    runCatching { context.startActivity(intent) }
                                                        .onFailure {
                                                            Toast.makeText(
                                                                context,
                                                                "No app found to open this media.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }

                                                is MediaAttachment.Local -> {
                                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(mediaAttachment.uri, mediaAttachment.mimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.grantUriPermission(
                                                        context.packageName,
                                                        mediaAttachment.uri,
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    )
                                                    runCatching { context.startActivity(viewIntent) }
                                                        .onFailure {
                                                            Toast.makeText(
                                                                context,
                                                                "No app found to open this media.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(buttonLabel)
                                    }

                                    if (mediaAttachment is MediaAttachment.Local) {
                                        loadedState.mediaMimeType?.let { mime ->
                                            Text(
                                                text = mime,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Drop info", style = MaterialTheme.typography.titleMedium)
                                DropDetailInfoRow(
                                    icon = typeIcon,
                                    label = "Type",
                                    value = typeText
                                )
                                DropDetailInfoRow(
                                    icon = visibilityIcon,
                                    label = "Visibility",
                                    value = visibilityText
                                )
                                DropDetailInfoRow(
                                    icon = Icons.Rounded.Event,
                                    label = "Dropped",
                                    value = createdAtText ?: "Not available"
                                )
                                DropDetailInfoRow(
                                    icon = Icons.Rounded.Place,
                                    label = "Location",
                                    value = locationText
                                )
                            }
                        }

                        if (shouldShowDecisionPrompt) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val decisionPrompt = when (loadedState?.contentType) {
                                        DropContentType.TEXT -> "Would you like to pick up this note?"
                                        DropContentType.PHOTO -> "Would you like to pick up this photo?"
                                        DropContentType.AUDIO -> "Would you like to pick up this audio drop?"
                                        null -> "Would you like to pick up this drop?"
                                    }
                                    Text(decisionPrompt, style = MaterialTheme.typography.titleMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val current = loadedState ?: return@Button
                                                decisionProcessing = true
                                                val pickupIntent = Intent(appContext, DropDecisionReceiver::class.java).apply {
                                                    action = DropDecisionReceiver.ACTION_PICK_UP
                                                    putExtra(DropDecisionReceiver.EXTRA_DROP_ID, dropId)
                                                    current.text?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_TEXT, it) }
                                                    current.lat?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_LAT, it) }
                                                    current.lng?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_LNG, it) }
                                                    current.createdAt?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_CREATED_AT, it) }
                                                    current.groupCode?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_GROUP, it) }
                                                    putExtra(DropDecisionReceiver.EXTRA_DROP_CONTENT_TYPE, current.contentType.name)
                                                    current.mediaUrl?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_URL, it) }
                                                    current.mediaMimeType?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_MIME_TYPE, it) }
                                                    current.mediaData?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_DATA, it) }
                                                }
                                                val result = runCatching {
                                                    appContext.sendBroadcast(pickupIntent)
                                                    NotificationManagerCompat.from(appContext).cancel(dropId.hashCode())
                                                }
                                                decisionProcessing = false
                                                if (result.isSuccess) {
                                                    decisionHandled = true
                                                    decisionStatusMessage = "Drop added to your collection."
                                                    Toast.makeText(context, "Drop added to your collection.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Couldn't pick up this drop.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = !decisionProcessing && loadedState != null,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Pick up")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                decisionProcessing = true
                                                val ignoreIntent = Intent(appContext, DropDecisionReceiver::class.java).apply {
                                                    action = DropDecisionReceiver.ACTION_IGNORE
                                                    putExtra(DropDecisionReceiver.EXTRA_DROP_ID, dropId)
                                                }
                                                val result = runCatching {
                                                    appContext.sendBroadcast(ignoreIntent)
                                                    NotificationManagerCompat.from(appContext).cancel(dropId.hashCode())
                                                }
                                                decisionProcessing = false
                                                if (result.isSuccess) {
                                                    decisionHandled = true
                                                    decisionStatusMessage = "Drop ignored. You won't be notified about it again."
                                                    Toast.makeText(context, "Drop ignored.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Couldn't ignore this drop.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = !decisionProcessing,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Ignore")
                                        }
                                    }
                                }
                            }
                        }

                        decisionMessage?.let { messageText ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = messageText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                                }
                            }

                        FilledTonalButton(
                            onClick = {
                                val activity = context as? ComponentActivity
                                activity?.let {
                                    if (it.isTaskRoot) {
                                        val backIntent = Intent(it, MainActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    }
                                    it.finish()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to GeoDrop")
                        }
                }
            }
        }
    }
}

        @Composable
        private fun DropVoteSection(
            state: DropDetailUiState.Loaded,
            currentUserId: String?,
            isVoting: Boolean,
            onVote: (DropVoteType) -> Unit
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Community votes", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val userVote = state.userVote(currentUserId)

                    VoteToggleButton(
                        icon = Icons.Rounded.ThumbUp,
                        label = state.upvoteCount.toString(),
                        selected = userVote == DropVoteType.UPVOTE,
                        enabled = !isVoting,
                        onClick = {
                            val nextVote = if (userVote == DropVoteType.UPVOTE) {
                                DropVoteType.NONE
                            } else {
                                DropVoteType.UPVOTE
                            }
                            onVote(nextVote)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    VoteToggleButton(
                        icon = Icons.Rounded.ThumbDown,
                        label = state.downvoteCount.toString(),
                        selected = userVote == DropVoteType.DOWNVOTE,
                        enabled = !isVoting,
                        onClick = {
                            val nextVote = if (userVote == DropVoteType.DOWNVOTE) {
                                DropVoteType.NONE
                            } else {
                                DropVoteType.DOWNVOTE
                            }
                            onVote(nextVote)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (isVoting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Text(
                    text = "Score: ${formatVoteScore(state.voteScore())} (↑${state.upvoteCount} / ↓${state.downvoteCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (currentUserId.isNullOrBlank()) {
                    Text(
                        text = "Sign in to vote on drops.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        @Composable
        private fun VoteToggleButton(
            icon: ImageVector,
            label: String,
            selected: Boolean,
            enabled: Boolean,
            onClick: () -> Unit,
            modifier: Modifier = Modifier
        ) {
            if (selected) {
                FilledTonalButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier.heightIn(min = 40.dp)
                ) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(label)
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier.heightIn(min = 40.dp)
                ) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(label)
                }
            }
        }

        private fun DropDetailUiState.Loaded.userVote(userId: String?): DropVoteType {
            if (userId.isNullOrBlank()) return DropVoteType.NONE
            return DropVoteType.fromRaw(voteMap[userId])
        }

        private fun DropDetailUiState.Loaded.applyUserVoteLocal(
            userId: String,
            vote: DropVoteType
        ): DropDetailUiState.Loaded {
            val updated = toDropForVoting().applyUserVote(userId, vote)
            return copy(
                upvoteCount = updated.upvoteCount,
                downvoteCount = updated.downvoteCount,
                voteMap = updated.voteMap
            )
        }

        private fun DropDetailUiState.Loaded.toDropForVoting(): Drop {
            return Drop(
                text = text.orEmpty(),
                lat = lat ?: 0.0,
                lng = lng ?: 0.0,
                createdAt = createdAt ?: 0L,
                groupCode = groupCode,
                contentType = contentType,
                mediaUrl = mediaUrl,
                mediaMimeType = mediaMimeType,
                mediaData = mediaData,
                upvoteCount = upvoteCount,
                downvoteCount = downvoteCount,
                voteMap = voteMap
            )
        }

        private fun DropDetailUiState.Loaded.voteScore(): Long = upvoteCount - downvoteCount

        private fun formatVoteScore(score: Long): String {
            return when {
                score > 0 -> "+$score"
                score < 0 -> score.toString()
                else -> "0"
            }
        }

        private fun parseVoteMap(raw: Any?): Map<String, Long>? {
            if (raw !is Map<*, *>) return null
            if (raw.isEmpty()) return emptyMap()

            val result = mutableMapOf<String, Long>()
            raw.forEach { (key, value) ->
                val keyString = key as? String ?: return@forEach
                val longValue = when (value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull()
                    else -> null
                }
                if (longValue != null) {
                    result[keyString] = longValue
                }
            }
            return result
        }

private data class DropDetailTagData(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
private fun DropDetailTag(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DropDetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .padding(6.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
    try {
        com.google.android.gms.tasks.Tasks.await(this)
    } catch (_: Exception) {
        null
    }

private fun formatCoordinate(value: Double): String = "%.5f".format(value)

private fun resolveMediaAttachment(
    context: Context,
    state: DropDetailUiState.Loaded
): MediaAttachment? {
    if (state.contentType == DropContentType.AUDIO) {
        val data = state.mediaData?.takeIf { it.isNotBlank() }
        if (data != null) {
            val preferredMime = state.mediaMimeType?.takeIf { it.isNotBlank() }
            val decoded = decodeBase64ToTempFile(
                context = context,
                base64Data = data,
                mimeType = preferredMime,
                subDir = "audio",
                defaultMime = preferredMime ?: "audio/mpeg",
                defaultExtension = "m4a"
            )
            if (decoded != null) {
                return MediaAttachment.Local(decoded.uri, decoded.mimeType)
            }
        }
    }

    val url = state.mediaUrl?.takeIf { it.isNotBlank() }
    return url?.let { MediaAttachment.Link(it) }
}

private data class DecodedMedia(val uri: Uri, val mimeType: String)

private fun decodeBase64ToTempFile(
    context: Context,
    base64Data: String,
    mimeType: String?,
    subDir: String,
    defaultMime: String,
    defaultExtension: String
): DecodedMedia? {
    return try {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: defaultMime
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMime)?.takeIf { it.isNotBlank() }
            ?: defaultExtension
        val dir = File(context.cacheDir, subDir).apply { if (!exists()) mkdirs() }
        val file = File.createTempFile("geodrop_media_", ".${extension}", dir)
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        DecodedMedia(uri, resolvedMime)
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IOException) {
        null
    }
}

private sealed class MediaAttachment {
    data class Link(val url: String) : MediaAttachment()
    data class Local(val uri: Uri, val mimeType: String) : MediaAttachment()
}

private sealed interface DropDetailUiState {
    data class Loaded(
        val text: String?,
        val lat: Double?,
        val lng: Double?,
        val createdAt: Long?,
        val groupCode: String?,
        val contentType: DropContentType = DropContentType.TEXT,
        val mediaUrl: String? = null,
        val mediaMimeType: String? = null,
        val mediaData: String? = null,
        val upvoteCount: Long = 0,
        val downvoteCount: Long = 0,
        val voteMap: Map<String, Long> = emptyMap()
    ) : DropDetailUiState

    object Loading : DropDetailUiState
    object NotFound : DropDetailUiState
    object Deleted : DropDetailUiState
}
