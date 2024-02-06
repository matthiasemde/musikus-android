/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession.recorder

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import app.musikus.ui.components.PlayerState
import app.musikus.ui.components.rememberManagedMediaController
import app.musikus.ui.components.state
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import app.musikus.usecase.recordings.Recording
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString

@Composable
fun Recorder(
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

    Column(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimensions.cardPeekContentHeight)
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.recordingDuration,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.weight(1f))

            FilledIconButton(
                modifier = Modifier.size(50.dp),
                onClick = { eventHandler(RecorderUiEvent.ToggleRecording) },
                shape = CircleShape,
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

        HorizontalDivider()


        Text(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
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
                    mediaController = mediaController
                )
            }
        }

        
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
}

@Composable
fun Recording (
    recording: Recording,
    mediaController: MediaController?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = recording.title,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = getDurationString(recording.duration, DurationFormat.HMS_DIGITAL),
            style = MaterialTheme.typography.bodySmall
        )

        FilledIconButton(
            onClick = {
                Log.d("Recorder", "Trying to play recording: ${recording.title}")
                mediaController?.run {
                    Log.d("Recorder", "Playing recording: ${recording.title}")
                    setMediaItem(recording.mediaItem)
                    prepare()
                    play()
                }
            }
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play recording")
        }
    }
}
