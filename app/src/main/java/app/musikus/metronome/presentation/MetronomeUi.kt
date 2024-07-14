/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.metronome.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.core.presentation.components.ExceptionHandler
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.dimensions
import app.musikus.core.presentation.theme.spacing


@Composable
fun MetronomeUi(
    modifier: Modifier = Modifier,
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent
    val exceptionChannel = viewModel.exceptionChannel
    val context = LocalContext.current

    /**
     * Exception handling
     */
    ExceptionHandler<MetronomeException>(
        exceptionChannel,
        exceptionHandler = { exception ->
            Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
        },
        onUnhandledException = { throw (it) }
    )

    MetronomeLayout(
        modifier = modifier,
        uiState = uiState,
        eventHandler = eventHandler
    )
}

@Composable
fun MetronomeLayout(
    modifier: Modifier = Modifier,
    uiState: MetronomeUiState,
    eventHandler: (MetronomeUiEvent) -> Unit,
) {
    Column (modifier = modifier
        .padding(horizontal = MaterialTheme.spacing.large)
    ) {
        MetronomeHeader(
            modifier = Modifier.height(MaterialTheme.dimensions.toolsHeaderHeight),
            uiState = uiState,
            onIncrementBpm = { eventHandler(MetronomeUiEvent.IncrementBpm(it)) },
            onTogglePlay = { eventHandler(MetronomeUiEvent.ToggleIsPlaying) }
        )

        HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.medium))

        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimensions.toolsBodyHeight),
        ) {

            Spacer(modifier = Modifier.weight(1f))

            MetronomeSlider(
                modifier = Modifier,
                uiState = uiState,
                onSliderValueChange = { eventHandler(MetronomeUiEvent.UpdateSliderValue(it)) }
            )

            Spacer(modifier = Modifier.weight(1f))

            /** Beats per bar, Click per beat and Tab tempo */
            MetronomeExtraSettingsRow(
                uiState = uiState.settings,
                onIncrementBeatsPerBar = { eventHandler(MetronomeUiEvent.IncrementBeatsPerBar) },
                onDecrementBeatsPerBar = { eventHandler(MetronomeUiEvent.DecrementBeatsPerBar) },
                onIncrementClicksPerBear = { eventHandler(MetronomeUiEvent.IncrementClicksPerBeat) },
                onDecrementClicksPerBear = { eventHandler(MetronomeUiEvent.DecrementClicksPerBeat) },
                onTapTempo = { eventHandler(MetronomeUiEvent.TabTempo) }
            )
        }
        
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}


@PreviewLightDark
@Composable
private fun PreviewMetronome(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview (theme) {
        Surface (color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            MetronomeLayout(
                uiState = MetronomeUiState(
                    settings = MetronomeSettings.DEFAULT.copy(bpm = 40),
                    tempoDescription = "Allegro",
                    isPlaying = false,
                    sliderValue = 120f,
                ),
                eventHandler = {}
            )
        }
    }
}

@Composable
private fun MetronomeHeader(
    modifier: Modifier = Modifier,
    uiState: MetronomeUiState,
    onIncrementBpm: (Int) -> Unit,
    onTogglePlay: () -> Unit
) {
    val smallWidthThreshold = 310.dp

    BoxWithConstraints {
        val boxScope = this
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            val smallIncrementButtonSize = 25.dp
            val largeIncrementButtonSize = 30.dp

            // don't show 5 bpm increment buttons on small screens
            if (boxScope.maxWidth > smallWidthThreshold) {
                // -5 Bpm
                MetronomeIncrementBpmButton(
                    bpmIncrement = -5,
                    size = largeIncrementButtonSize,
                    onClick = {
                        onIncrementBpm(it)
                    }
                )
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))


            // -1 Bpm
            MetronomeIncrementBpmButton(
                bpmIncrement = -1,
                size = smallIncrementButtonSize,
                onClick = {
                    onIncrementBpm(it)
                }
            )

            // Bpm
            Row (
                Modifier
                    .width(130.dp)
                    .padding(MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.Center){
                Text(
                    modifier = Modifier
                        .alignByBaseline(),
                    text = uiState.settings.bpm.toString(),
                    textAlign = TextAlign.Right,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Clip
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "bpm",
                    textAlign = TextAlign.Left,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // +1 Bpm
            MetronomeIncrementBpmButton(
                bpmIncrement = 1,
                size = smallIncrementButtonSize,
                onClick = {
                    onIncrementBpm(it)
                }
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

            if (boxScope.maxWidth > smallWidthThreshold) {
                // +5 Bpm
                MetronomeIncrementBpmButton(
                    bpmIncrement = 5,
                    size = largeIncrementButtonSize,
                    onClick = {
                        onIncrementBpm(it)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start/Stop Metronome
            FilledIconButton(
                modifier = Modifier.size(48.dp),
                onClick = onTogglePlay,
                shape = CircleShape,
            ) {
                Box(Modifier.padding(MaterialTheme.spacing.small)) {
                    if (!uiState.isPlaying) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start metronome"
                        )
                    } else {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop metronome"
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun MetronomeSlider(
    modifier: Modifier = Modifier,
    uiState: MetronomeUiState,
    onSliderValueChange: (Float) -> Unit
) {
    Column {
        /** Tempo Slider */
        Text(
            modifier = Modifier,
            text = uiState.tempoDescription,
            style = MaterialTheme.typography.bodyLarge,
        )


        Slider(
            value = uiState.sliderValue,
            valueRange =
            MetronomeSettings.BPM_RANGE.first.toFloat()..
                    MetronomeSettings.BPM_RANGE.last.toFloat(),
            onValueChange = { onSliderValueChange(it) },
        )
    }
}


@Composable
fun MetronomeIncrementBpmButton(
    bpmIncrement: Int,
    size: Dp,
    onClick: (Int) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(bpmIncrement) },
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(MaterialTheme.spacing.extraSmall)
    ) {
        Text(
            text = (if(bpmIncrement > 0) "+" else "") + bpmIncrement.toString(),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun MetronomeExtraSettingsRow(
    uiState: MetronomeSettings,
    onIncrementBeatsPerBar: () -> Unit,
    onDecrementBeatsPerBar: () -> Unit,
    onIncrementClicksPerBear: () -> Unit,
    onDecrementClicksPerBear: () -> Unit,
    onTapTempo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        MetronomeExtraSettingsColumn(label = "Beats/bar") {
            MetronomeIncrementer(
                value = uiState.beatsPerBar,
                onIncrement = onIncrementBeatsPerBar,
                onDecrement = onDecrementBeatsPerBar,
            )
        }

        MetronomeExtraSettingsColumn(label = "Clicks/beat") {
            MetronomeIncrementer(
                value = uiState.clicksPerBeat,
                onIncrement = onIncrementClicksPerBear,
                onDecrement = onDecrementClicksPerBear,
            )
        }

        MetronomeExtraSettingsColumn(label = "Tab tempo") {
            IconButton(
                onClick = onTapTempo,
//                modifier = Modifier.size(25.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Tab tempo"
                )
            }

        }
    }
}

@Composable
fun MetronomeExtraSettingsColumn(
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.height(75.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.heightIn(max = 50.dp)
        ) {
            content()
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun MetronomeIncrementer(
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .padding(MaterialTheme.spacing.extraSmall)
                .size(25.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrement"
            )
        }
        Text(
            modifier = Modifier.width(20.dp),
            text = value.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .padding(MaterialTheme.spacing.extraSmall)
                .size(25.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Decrement"
            )
        }
    }
}