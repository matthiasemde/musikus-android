/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession.recorder

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import app.musikus.shared.conditional
import app.musikus.ui.components.PlayerState
import app.musikus.ui.components.Waveform
import app.musikus.ui.components.rememberManagedMediaController
import app.musikus.ui.components.seekToRelativePosition
import app.musikus.ui.components.state
import app.musikus.ui.theme.spacing
import app.musikus.usecase.recordings.Recording
import app.musikus.utils.DateFormat
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import app.musikus.utils.musikusFormat

@Composable
fun RecorderCardHeader(
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exception by viewModel.exception.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    val context = LocalContext.current

    /**
     * Exception handling
     */

    LaunchedEffect(key1 = exception) {
        exception?.let { e ->
            when(e) {
                is RecorderException ->
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                else -> throw(e)
            }
        }
    }

    /**
     * MediaController
     */

    val mediaController by rememberManagedMediaController()

    // Remember the player state
    var playerState: PlayerState? by remember {
        mutableStateOf(mediaController?.state())
    }

    // Update the player state when the MediaController changes
    DisposableEffect(key1 = mediaController) {
        mediaController?.run {
            playerState = state()
        }
        onDispose {
            playerState?.dispose()
        }
    }

    LaunchedEffect(key1 = playerState?.playerError) {
        playerState?.playerError?.let { exception ->
            Log.e("Recorder", "Player error: $exception")
        }
    }

    var currentPosition by remember { mutableLongStateOf(0) }

    val playerPositionTracker = object : Runnable {
        override fun run() {
            playerState?.player?.let {
                currentPosition = it.currentPosition
                if(it.isPlaying) {
                    Handler(Looper.getMainLooper()).postDelayed(this, 100)
                }
            }
        }
    }

    LaunchedEffect(key1 = playerState?.isPlaying) {
        if(playerState?.isPlaying == true) {
            Handler(Looper.getMainLooper()).postDelayed(playerPositionTracker, 100)
        }
    }



    Box(modifier = modifier.padding(vertical = MaterialTheme.spacing.small)) {
        if(playerState?.currentMediaItem != null) {
            MediaPlayerBar(
                uiState = uiState,
                playerState = playerState,
                currentPosition = currentPosition,
                mediaController = mediaController,
                onSetCurrentPosition = { position ->
                    currentPosition = position
                }
            )
        } else {
            RecorderBar(
                uiState = uiState,
                playerState = playerState,
                eventHandler = eventHandler
            )
        }
    }
}

@Composable
fun RecorderCardBody(
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    val mediaController by rememberManagedMediaController()

    // Remember the player state
    var playerState: PlayerState? by remember {
        mutableStateOf(mediaController?.state())
    }

    // Update the player state when the MediaController changes
    DisposableEffect(key1 = mediaController) {
        mediaController?.run {
            playerState = state()
        }
        onDispose {
            playerState?.dispose()
        }
    }

    LaunchedEffect(key1 = playerState?.playerError) {
        playerState?.playerError?.let { exception ->
            Log.e("Recorder", "Player error: $exception")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        Text(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.small
            ),
            text = buildAnnotatedString {
                append("Location: ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)
                ) {
                    append("Music/Musikus")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Column {
            for (recording in  uiState.recordings) {
                Recording(
                    recording = recording,
                    isRecording = uiState.isRecording,
                    mediaController = mediaController,
                    eventHandler = eventHandler
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
}

@Composable
fun Recording (
    recording: Recording,
    isRecording: Boolean,
    mediaController: MediaController?,
    eventHandler: RecorderUiEventHandler
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .conditional(!isRecording) {
                clickable {
                    mediaController?.run {
                        setMediaItem(recording.mediaItem)
                        eventHandler(RecorderUiEvent.LoadRecording(recording.contentUri))
                        if (mediaController.isCommandAvailable(Player.COMMAND_PREPARE)) prepare()
                        if (mediaController.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) play()
                    }
                }
            }
            .padding(
                horizontal = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = recording.title,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "recorded at: ${recording.date.musikusFormat(DateFormat.DAY_MONTH_YEAR)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )

        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = getDurationString(recording.duration, DurationFormat.HMS_DIGITAL),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun RecorderBar(
    modifier: Modifier = Modifier,
    uiState: RecorderUiState,
    playerState: PlayerState?,
    eventHandler: RecorderUiEventHandler
) {
    Row(
        modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.extraLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiState.recordingDuration,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.weight(1f))

        FilledIconButton(
            modifier = Modifier.size(48.dp),
            onClick = { eventHandler(RecorderUiEvent.ToggleRecording) },
            shape = CircleShape,
            enabled = playerState?.isPlaying != true,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
        ) {
            Box(Modifier.padding(MaterialTheme.spacing.small)) {
                if (uiState.isRecording) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Stop recording"
                    )
                } else {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start recording"
                    )
                }
            }
        }
    }
}

@Composable
fun MediaPlayerBar(
    modifier: Modifier = Modifier,
    uiState: RecorderUiState,
    playerState: PlayerState?,
    currentPosition: Long,
    mediaController: MediaController?,
    onSetCurrentPosition: (Long) -> Unit
) {
    if(playerState == null || mediaController == null) return

    Row(
        modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { mediaController.clearMediaItems() }) {
            Icon(Icons.Default.Close, contentDescription = "Close player")
        }

        uiState.currentRawRecording?.let { rawRecording ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.spacing.small)
            ) {

                var wasPlayerPlayingPreDrag = remember { false }

                Waveform(
                    rawRecording = rawRecording,
                    playBackMarker = currentPosition.toFloat() / playerState.player.duration,
                    onDragStart = {
                        wasPlayerPlayingPreDrag = playerState.isPlaying
                        mediaController.pause()
                    },
                    onDragEnd = {
                        if (wasPlayerPlayingPreDrag) mediaController.play()
                    },
                    onDrag = { position ->
                        onSetCurrentPosition((position * playerState.player.duration).toLong())
                        mediaController.seekToRelativePosition(position)
                    },
                    onClick = { position ->
                        onSetCurrentPosition((position * playerState.player.duration).toLong())
                        mediaController.seekToRelativePosition(position)
                    }
                )
            }
        } ?: Spacer(Modifier.weight(1f))

        OutlinedIconButton(
            onClick = { mediaController.seekBack() }
        ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = null)
        }

        OutlinedIconButton(
            onClick = {
                if (playerState.isPlaying) {
                    mediaController.pause()
                } else {
                    mediaController.play()
                }
            }
        ) {
            // rather annoyingly, mediaController.seekTo() causes the player to pause
            // for a split second which toggles the icon back and forth
            // TODO fix with animation
            if (playerState.isPlaying) {
                Icon(Icons.Default.Pause, contentDescription = null)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
        }

        OutlinedIconButton(
            onClick = { mediaController.seekForward() }
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null)
        }
    }
}
