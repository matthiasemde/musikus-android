/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.ui.activesession.recorder

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import app.musikus.datastore.ColorSchemeSelections
import app.musikus.ui.components.DialogActions
import app.musikus.ui.components.ExceptionHandler
import app.musikus.ui.components.Waveform
import app.musikus.ui.theme.MusikusColorSchemeProvider
import app.musikus.ui.theme.MusikusPreviewElement1
import app.musikus.ui.theme.MusikusPreviewElement2
import app.musikus.ui.theme.MusikusThemedPreview
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import app.musikus.utils.RecorderState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


@Composable
fun RecorderUi(
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent
    val exceptionChannel = viewModel.exceptionChannel

    val context = LocalContext.current

    /**
     * Exception handling
     */
    ExceptionHandler<RecorderException>(
        exceptionChannel,
        exceptionHandler = { exception ->
            Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
        },
        onUnhandledException = { throw (it) }
    )


    /**
     * MediaController, managed via Compose State
     */
    // TODO maybe remove by because of recomopositions??
    val mediaController by rememberManagedMediaController()

    // Remember the player state
    // TODO why dedicated playerstate and not always derive from mediacontroller?
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

    // show possible Media Player error
    LaunchedEffect(key1 = playerState?.playerError) {
        playerState?.playerError?.let { exception ->
            Log.e("Recorder", "Player error: $exception")
        }
    }

    // in milliseconds
    var currentPlaybackPosition by remember { mutableLongStateOf(0) }

    // TODO what is this why necessary???
    LaunchedEffect(key1 = playerState?.currentMediaItem) {
        while (playerState?.currentMediaItem != null && isActive) {
            currentPlaybackPosition = playerState?.player?.currentPosition ?: 0
            delay(100)
        }
    }

    RecorderLayout(
        uiState = uiState,
        eventHandler = eventHandler,
        mediaController = mediaController,
        playerState = playerState,
    )
}

@Composable
fun RecorderLayout(
    modifier: Modifier = Modifier,
    uiState: RecorderUiState,
    eventHandler: RecorderUiEventHandler,
    mediaController: MediaController?,
    playerState: PlayerState?,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RecorderToolbar(
            modifier = Modifier.height(MaterialTheme.dimensions.toolsHeaderHeight),
            uiState = uiState,
            eventHandler = eventHandler
        )

        HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.medium))
        Spacer(Modifier.height(MaterialTheme.spacing.medium))

        RecordingsList(
            recordingsList = uiState.recordings.toImmutableList(),
            mediaController = mediaController,
            playerState = playerState,
            onNewMediaSelected = { contentUri ->
                // hook to notify the ViewModel which mediaItem was newly selected
                eventHandler(RecorderUiEvent.LoadRecording(contentUri))
            },
        )
    }

    /** Dialogs */

    val dialogUiState = uiState.dialogUiState

    if (dialogUiState.showDeleteRecordingDialog) {
        DialogDeleteRecording(
            onDismiss = { eventHandler(RecorderUiEvent.DeleteRecordingDialogDismissed) },
            onConfirm = { eventHandler(RecorderUiEvent.DeleteRecordingDialogConfirmed) }
        )
    }
    val saveDialogUiState = dialogUiState.saveRecordingDialogUiState
    saveDialogUiState?.let {
        DialogSaveRecording(
            uiState = saveDialogUiState,
            onDismiss = { eventHandler(RecorderUiEvent.SaveRecordingDialogDismissed) },
            onConfirm = { eventHandler(RecorderUiEvent.SaveRecordingDialogConfirmed) },
        )
    }
}


@Composable
fun RecorderToolbar(
    modifier: Modifier = Modifier,
    uiState: RecorderUiState,
    eventHandler: RecorderUiEventHandler
) {
    val showDeleteAndSave =
        uiState.recorderState !in listOf(RecorderState.IDLE, RecorderState.UNINITIALIZED)

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Duration
        Text(
            text = uiState.recordingDuration,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Delete button
        AnimatedVisibility(showDeleteAndSave) {
            Row {
                TextButton(onClick = { eventHandler(RecorderUiEvent.DeleteRecording) }) {
                    Text(text = "Delete")
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            }
        }

        // Play / Pause / Stop button
        FilledIconButton(
            modifier = Modifier.size(48.dp),
            onClick = {
                when (uiState.recorderState) {
                    RecorderState.UNINITIALIZED -> {}
                    RecorderState.IDLE -> eventHandler(RecorderUiEvent.StartRecording)
                    RecorderState.RECORDING -> eventHandler(RecorderUiEvent.PauseRecording)
                    RecorderState.PAUSED -> eventHandler(RecorderUiEvent.ResumeRecording)
                }
            },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
        ) {
            Box(Modifier.padding(MaterialTheme.spacing.small)) {
                when (uiState.recorderState) {
                    RecorderState.UNINITIALIZED -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Microphone not available"
                        )
                    }

                    RecorderState.IDLE -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start recording"
                        )
                    }

                    RecorderState.RECORDING -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause recording"
                        )
                    }

                    RecorderState.PAUSED -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume recording"
                        )
                    }
                }
            }
        }

        // Save button
        AnimatedVisibility(showDeleteAndSave) {
            Row {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                TextButton(onClick = { eventHandler(RecorderUiEvent.SaveRecording) }) {
                    Text(text = "Save")
                }
            }
        }
    }
}


@Composable
private fun RecordingsList(
    modifier: Modifier = Modifier,
    recordingsList: ImmutableList<RecordingListItemUiState>,
    mediaController: MediaController?,
    playerState: PlayerState?,
    onNewMediaSelected: (Uri) -> Unit
) {
    // TODO LazyList?
    Column(modifier = modifier.fillMaxSize()) {
        for (recording in recordingsList) {
            RecordingListItem(
                Modifier.height(56.dp),
                uiState = recording,
                isPlaying = ( // TODO lambda better for recompositions???
                        mediaController != null &&
                        playerState?.isPlaying == true &&
                        mediaController.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM) &&
                        mediaController.currentMediaItem == recording.mediaItem),
                onStartPlayingPressed = {
                    if (mediaController == null) return@RecordingListItem
                    loadAndPlayNewMediaItem(
                        mediaController = mediaController,
                        mediaItem = recording.mediaItem,
                        onSetMediaItem = { onNewMediaSelected(recording.contentUri) }
                    )
                },
                onPausePressed = { mediaController?.pause() },
                onResumePressed = { mediaController?.play() },
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
}


@Composable
private fun RecordingListItem(
    modifier: Modifier = Modifier,
    uiState: RecordingListItemUiState,
    isPlaying: Boolean,
    onStartPlayingPressed: () -> Unit,
    onPausePressed: () -> Unit,
    onResumePressed: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(vertical = MaterialTheme.spacing.small)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

        // Play / Pause Button
        OutlinedIconButton(
            onClick = {
                if (isPlaying) onPausePressed()
                else if (uiState.showPlayerUi) onResumePressed()
                else onStartPlayingPressed()
            },
            colors = IconButtonDefaults.iconButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            // rather annoyingly, mediaController.seekTo() causes the player to pause
            // for a split second which toggles the icon back and forth
            // TODO fix with animation
            if (isPlaying) {
                Icon(Icons.Default.Pause, contentDescription = null)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

        AnimatedContent(
            uiState.showPlayerUi,
            modifier = Modifier.weight(1f),
            label = "recorder-header-content-animation"
        ) { isPlaying ->

            if (isPlaying) {
                // Player
                Text("...Media Player...")
//                WaveformMediaPlayer(
//                    playerState = playerState,
//                    rawRecording = null,
//                    mediaController = mediaController,
//                    onSetCurrentPosition = {
////                        currentPlaybackPosition = it
//                    },
//                    currentPosition = currentPlaybackPosition
//                )
            } else {
                // Item description
                RecordingItemDescription(
                    title = uiState.title,
                    date = uiState.date,
                    duration = uiState.duration
                )
            }
        }
        Spacer(Modifier.width(MaterialTheme.spacing.medium))
    }
}


private fun loadAndPlayNewMediaItem(
    mediaController: MediaController,
    mediaItem: MediaItem,
    onSetMediaItem: () -> Unit,
) {
    // routine for stopping, clearing old item, loading and playing new item
    mediaController.run {
        if (isCommandAvailable(Player.COMMAND_STOP)) stop()
        if (isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) clearMediaItems()
        if (isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)) {
            setMediaItem(mediaItem)
            onSetMediaItem()
        }
        if (isCommandAvailable(Player.COMMAND_PREPARE)) prepare()
        if (isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) play()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordingItemDescription(
    modifier: Modifier = Modifier,
    title: String,
    date: String,
    duration: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                modifier = Modifier.basicMarquee(),
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                modifier = Modifier.basicMarquee(),
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = duration,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
private fun WaveformMediaPlayer(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    rawRecording: ShortArray?,
    currentPosition: Long,
    mediaController: MediaController,
    onSetCurrentPosition: (Long) -> Unit = {}
) {

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Box(Modifier.weight(1f)) {
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
            Text(
                text = "TODO time!!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = {
                mediaController.seekTo(mediaController.currentPosition - 5000)
            }
        ) {
            Icon(Icons.Default.Replay5, contentDescription = null)
        }

        IconButton(onClick = { mediaController.clearMediaItems() }) {
            Icon(Icons.Default.Close, contentDescription = "Close player")
        }
    }
}


@Composable
private fun DialogSaveRecording(
    uiState: SaveRecordingDialogUiState,
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {},
    recordingNameChanged: (String) -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                Text(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.large)
                        .padding(vertical = MaterialTheme.spacing.medium),
                    text = "Save recording as:",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.medium),
                    value = uiState.recordingName,
                    label = { Text(text = "Recording name") },
                    onValueChange = { recordingNameChanged(it) },
                )
                DialogActions(
                    confirmButtonText = "Save",
                    onDismissHandler = onDismiss,
                    onConfirmHandler = onConfirm,
                    confirmButtonEnabled = uiState.recordingName.isNotEmpty()
                )
            }
        }

    }
}

@Composable
private fun DialogDeleteRecording(
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                Text(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.large)
                        .padding(top = MaterialTheme.spacing.medium),
                    style = MaterialTheme.typography.titleLarge,
                    text = "Delete recording?",
                )
                DialogActions(
                    confirmButtonText = "Delete",
                    onDismissHandler = onDismiss,
                    onConfirmHandler = onConfirm
                )
            }
        }
    }
}

/*

@Composable
fun RecorderCardHeaderOld(
    modifier: Modifier = Modifier,
) {

    Box(modifier = modifier.padding(vertical = MaterialTheme.spacing.small)) {

        AnimatedContent(
            playerState?.currentMediaItem,
            label = "recorder-header-content-animation"
        ) { currentMediaItem ->
            if (currentMediaItem != null) {
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

    val dialogUiState = uiState.dialogUiState

    if (dialogUiState.showDeleteRecordingDialog) {
        Dialog(onDismissRequest = { eventHandler(RecorderUiEvent.DeleteRecordingDialogDismissed) }) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.spacing.large)
                            .padding(top = MaterialTheme.spacing.medium),
                        style = MaterialTheme.typography.titleLarge,
                        text = "Delete recording?",
                    )
                    DialogActions(
                        confirmButtonText = "Delete",
                        onDismissHandler = { eventHandler(RecorderUiEvent.DeleteRecordingDialogDismissed) },
                        onConfirmHandler = { eventHandler(RecorderUiEvent.DeleteRecordingDialogConfirmed) }
                    )
                }
            }
        }
    }

    dialogUiState.saveRecordingDialogUiState?.let { saveRecordingDialogUiState ->
        Dialog(onDismissRequest = { eventHandler(RecorderUiEvent.SaveRecordingDialogDismissed) }) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.spacing.large)
                            .padding(vertical = MaterialTheme.spacing.medium),
                        text = "Save recording as:",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.medium),
                        value = saveRecordingDialogUiState.recordingName,
                        label = { Text(text = "Recording name") },
                        onValueChange = { eventHandler(RecorderUiEvent.RecordingNameChanged(it)) },
                    )
                    DialogActions(
                        confirmButtonText = "Save",
                        onDismissHandler = { eventHandler(RecorderUiEvent.SaveRecordingDialogDismissed) },
                        onConfirmHandler = { eventHandler(RecorderUiEvent.SaveRecordingDialogConfirmed) },
                        confirmButtonEnabled = saveRecordingDialogUiState.recordingName.isNotEmpty()
                    )
                }
            }
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
                withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic)
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
                    isRecording = uiState.recorderState != RecorderState.IDLE,
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
                        if (isCommandAvailable(Player.COMMAND_STOP)) stop()
                        if (isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) clearMediaItems()
                        if (isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)) setMediaItem(
                            recording.mediaItem
                        )
                        eventHandler(RecorderUiEvent.LoadRecording(recording.contentUri))
                        if (isCommandAvailable(Player.COMMAND_PREPARE)) prepare()
                        if (isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) play()
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
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiState.recordingDuration,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.weight(1f))

        val showDeleteAndSave =
            uiState.recorderState != RecorderState.IDLE &&
            uiState.recorderState != RecorderState.UNINITIALIZED

        AnimatedVisibility (showDeleteAndSave) {
            Row {
                TextButton(onClick = { eventHandler(RecorderUiEvent.DeleteRecording) }) {
                    Text(text = "Delete")
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            }
        }

        FilledIconButton(
            modifier = Modifier.size(48.dp),
            onClick = {
                when (uiState.recorderState) {
                    RecorderState.UNINITIALIZED -> { */
/* do nothing *//*
 }
                    RecorderState.IDLE -> eventHandler(RecorderUiEvent.StartRecording)
                    RecorderState.RECORDING -> eventHandler(RecorderUiEvent.PauseRecording)
                    RecorderState.PAUSED -> eventHandler(RecorderUiEvent.ResumeRecording)
                }
            },
            shape = CircleShape,
            enabled = playerState?.isPlaying != true,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
        ) {
            Box(Modifier.padding(MaterialTheme.spacing.small)) {
                when (uiState.recorderState) {
                    RecorderState.UNINITIALIZED -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Microphone not available"
                        )
                    }
                    RecorderState.IDLE -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start recording"
                        )
                    }
                    RecorderState.RECORDING -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause recording"
                        )
                    }
                    RecorderState.PAUSED -> {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume recording"
                        )
                    }
                }
            }
        }

        AnimatedVisibility (showDeleteAndSave) {
            Row {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                TextButton(onClick = { eventHandler(RecorderUiEvent.SaveRecording) }) {
                    Text(text = "Save")
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

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = MaterialTheme.spacing.small)
        ) {

            var wasPlayerPlayingPreDrag = remember { false }

            Waveform(
                rawRecording = uiState.currentRawRecording,
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

        IconButton(
            onClick = {
                mediaController.seekTo(mediaController.currentPosition - 5000)
            }
        ) {
            Icon(Icons.Default.Replay5, contentDescription = null)
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
    }
}*/


//@MusikusPreviewWholeScreen
//@Composable
//private fun PreviewRecorder(
//    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
//) {
//
//    MusikusThemedPreview(theme) {
//        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
//            RecorderLayout(
//                uiState = RecorderUiState(
//                    recorderState = RecorderState.RECORDING,
//                    recordingDuration = AnnotatedString("00:00"),
//                    recordings = dummyRecordings.toList(),
//                    currentPlaybackRawMedia = null,
//                    dialogUiState = RecorderDialogUiState(
//                        showDeleteRecordingDialog = false,
//                        saveRecordingDialogUiState = null
//                    )
//                ),
//                eventHandler = { },
//                mediaController = rememberManagedMediaController(),
//                playerState = remember { remember }
//            )
//        }
//    }
//}

@MusikusPreviewElement1
@Composable
private fun PreviewDialogDismiss(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme) {
        DialogDeleteRecording()
    }
}

@MusikusPreviewElement2
@Composable
private fun PreviewDialogSave(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme) {
        DialogSaveRecording(
            uiState = SaveRecordingDialogUiState(
                recordingName = "My Recording",
            )
        )
    }
}

//private val dummyRecordings = (0..10).asSequence().map {
//    RecordingListItemUiState(
//        title = LoremIpsum(Random.nextInt(1, 5)).values.first(),
//        date = "23.12.2024",
//        duration = "02:34",
//        mediaItem = null,
//        contentUri = null,
//        showPlayerUi = false,
//    )
//}