/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.recorder.presentation

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.ExceptionHandler
import app.musikus.core.presentation.components.Waveform
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusPreviewElement2
import app.musikus.core.presentation.theme.MusikusPreviewElement3
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.dimensions
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.getDurationString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Composable
fun RecorderUi(
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
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
    // TODO maybe remove by because of recompositions??
    val mediaController by rememberManagedMediaController()

    // Remember the player state
    // TODO why dedicated playerState and not always derive from mediaController?
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
        currentPlaybackPosition = currentPlaybackPosition,
        onSetCurrentPosition = {
            currentPlaybackPosition = it
        },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun RecorderLayout(
    modifier: Modifier = Modifier,
    uiState: RecorderUiState,
    eventHandler: RecorderUiEventHandler,
    mediaController: MediaController?,
    currentPlaybackPosition: Long,
    onSetCurrentPosition: (Long) -> Unit,
    playerState: PlayerState?,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large)
    ) {
        RecorderToolbar(
            modifier = Modifier.height(MaterialTheme.dimensions.toolsHeaderHeight),
            uiState = uiState,
            eventHandler = eventHandler
        )

        HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.medium))

        RecordingsList(
            modifier = Modifier.defaultMinSize(minHeight = MaterialTheme.dimensions.toolsBodyHeight),
            recordingsList = uiState.recordings.toImmutableList(),
            mediaController = mediaController,
            playerState = playerState,
            onNewMediaSelected = { contentUri ->
                // hook to notify the ViewModel which mediaItem was newly selected
                eventHandler(RecorderUiEvent.LoadRecording(contentUri))
            },
            onSetCurrentPosition = onSetCurrentPosition,
            currentPosition = currentPlaybackPosition,
            currentRawRecording = uiState.currentPlaybackRawMedia,
            snackbarHostState = snackbarHostState
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
    val smallWidthThreshold = 310.dp
    val showDeleteAndSave =
        uiState.recorderState !in listOf(RecorderState.IDLE, RecorderState.UNINITIALIZED)

    BoxWithConstraints {
        val boxScope = this

        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duration
            Text(
                text = uiState.recordingDuration,
                style = if (boxScope.maxWidth > smallWidthThreshold) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineLarge
                },
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            // Delete button
            AnimatedVisibility(showDeleteAndSave) {
                Row {
                    TextButton(onClick = { eventHandler(RecorderUiEvent.DeleteRecording) }) {
                        Text(text = "Delete")
                    }
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
                    TextButton(onClick = { eventHandler(RecorderUiEvent.SaveRecording) }) {
                        Text(text = "Save")
                    }
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
    currentPosition: Long,
    currentRawRecording: ShortArray?,
    onSetCurrentPosition: (Long) -> Unit,
    playerState: PlayerState?,
    onNewMediaSelected: (Uri?) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    if (recordingsList.isEmpty()) {
        Column(modifier = modifier) {
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Text(
                "No Recordings",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.medium)
    ) {
        items(recordingsList) { recording ->
            RecordingListItem(
                modifier = Modifier.height(56.dp),
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
                onRecordingDeleted = { /* TODO not implemented */ },
                playerState = playerState,
                mediaController = mediaController,
                currentPlaybackPosition = currentPosition,
                onSetCurrentPosition = onSetCurrentPosition,
                onClearPlayback = { onNewMediaSelected(null) },
                currentRawRecording = currentRawRecording,
                snackbarHostState = snackbarHostState
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingListItem(
    modifier: Modifier = Modifier,
    uiState: RecordingListItemUiState,
    currentRawRecording: ShortArray?,
    isPlaying: Boolean,
    playerState: PlayerState?,  // TODO remove?
    mediaController: MediaController?, // TODO remove?
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    currentPlaybackPosition: Long,
    onSetCurrentPosition: (Long) -> Unit,
    onStartPlayingPressed: () -> Unit,
    onPausePressed: () -> Unit,
    onResumePressed: () -> Unit,
    onClearPlayback: () -> Unit,
    onRecordingDeleted: (Uri) -> Unit,
) {
    /*
    ~~~ This implements Swipe-to-delete for recordings in the UI ~~~
        However, the actual deletion of the recording is not implemented yet, so this is commented out
        TODO uncomment when deleting recordings is implemented

    val scope = rememberCoroutineScope()
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            deleted = targetValue == SwipeToDismissBoxValue.EndToStart
            true// don't set to deleted or item will not be dismissable again after restore
        },
        positionalThreshold = with(LocalDensity.current) {
            { 100.dp.toPx() }
        }
    )
    SwipeToDeleteContainer(
        state = dismissState,
        deleted = deleted,
        onDeleted = {
            scope.launch {
                // TODO handle deletion when user leaves screen before timeout
                val result = snackbarHostState.showSnackbar(
                    message = "Recording deleted",
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        deleted = false
                        dismissState.reset()
                    }

                    SnackbarResult.Dismissed -> {
                        onRecordingDeleted(uiState.contentUri)
                    }
                }
            }
        }
    ) {
    */
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row( // Don't use vertical padding here it will cut the Waveform
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                    if (playerState == null || mediaController == null) {
                        return@AnimatedContent
                    }
                    // Player
                    WaveformMediaPlayer(
                        rawMediaData = currentRawRecording,
                        currentPositionMs = currentPlaybackPosition,
                        totalDurationMs = playerState.player.duration,
                        isPlaying = playerState.isPlaying,
                        onSetCurrentRelativePosition = { position ->
                            onSetCurrentPosition((position * playerState.player.duration).toLong())
                            mediaController.seekToRelativePosition(position)
                        },
                        onSeekToPositionMs = mediaController::seekTo,
                        onPause = mediaController::pause,
                        onPlay = mediaController::play,
                        onClear = {
                            mediaController.clearMediaItems()
                            onClearPlayback()
                        }
                    )
                } else {
                    // Item description
                    RecordingItemDescription(
                        title = uiState.title,
                        date = uiState.date,
                        duration = uiState.duration
                    )
                }
            }
        }
//        } swipe-to-delete
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Spacer(Modifier.width(MaterialTheme.spacing.small))
    }
}


@Composable
private fun WaveformMediaPlayer(
    modifier: Modifier = Modifier,
    rawMediaData: ShortArray?,
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSetCurrentRelativePosition: (Float) -> Unit,
    onSeekToPositionMs: (Long) -> Unit,
    onClear: () -> Unit,
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
                    rawMediaData = rawMediaData,
                    playBackMarker = currentPositionMs.toFloat() / totalDurationMs.toFloat(),
                    onDragStart = {
                        wasPlayerPlayingPreDrag = isPlaying
                        onPause()
                    },
                    onDragEnd = {
                        if (wasPlayerPlayingPreDrag) onPlay()
                        // TODO auto-resume does not work currently
                    },
                    onDrag = onSetCurrentRelativePosition,
                    onClick = onSetCurrentRelativePosition,
                )
            }
            Text(
                text = getDurationString(currentPositionMs.milliseconds, DurationFormat.MS_DIGITAL),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = {
                onSeekToPositionMs(currentPositionMs - 5000)
            }
        ) {
            Icon(Icons.Default.Replay5, contentDescription = null)
        }

        IconButton(onClick = onClear) {
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

@Preview(group = "TEST", widthDp = 320)
@MusikusPreviewElement1
@Composable
private fun PreviewRecorderUi(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme) {
        RecorderLayout(
            uiState = RecorderUiState(
                recorderState = RecorderState.RECORDING,
                recordingDuration = getDurationString(
                    1032.seconds,
                    DurationFormat.MSC_DIGITAL
                ),
                recordings = emptyList(),
                dialogUiState = RecorderDialogUiState(
                    showDeleteRecordingDialog = false,
                    saveRecordingDialogUiState = null
                ),
                currentPlaybackRawMedia = null
            ),
            eventHandler = {},
            mediaController = null,
            playerState = null,
            currentPlaybackPosition = 0,
            onSetCurrentPosition = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@MusikusPreviewElement2
@Composable
private fun PreviewDialogDismiss(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme) {
        DialogDeleteRecording()
    }
}

@MusikusPreviewElement3
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