/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession.metronome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.ui.theme.spacing

@Composable
fun MetronomeCardHeader(
    modifier: Modifier = Modifier,
    viewModel: MetronomeViewModel = hiltViewModel(),
    onTextClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        val smallIncrementButtonSize = 30.dp
        val largeIncrementButtonSize = 35.dp

        // -5 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = -5,
            size = largeIncrementButtonSize,
            eventHandler = eventHandler
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        // -1 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = -1,
            size = smallIncrementButtonSize,
            eventHandler = eventHandler
        )

        // Bpm
        Text(
            modifier = Modifier
                .width(100.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTextClicked
                ),
            text = uiState.settings.bpm.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Clip
        )

        // +1 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = 1,
            size = smallIncrementButtonSize,
            eventHandler = eventHandler
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        // +5 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = 5,
            size = largeIncrementButtonSize,
            eventHandler = eventHandler
        )


        Spacer(modifier = Modifier.weight(1f))

        // Start/Stop Metronome
        FilledIconButton(
            modifier = Modifier.size(48.dp),
            onClick = { eventHandler(MetronomeUiEvent.ToggleIsPlaying) },
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

@Composable
fun MetronomeCardBody(
    modifier: Modifier = Modifier,
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent
    BoxWithConstraints(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.zIndex(4f).background(Color.White).align(Alignment.TopEnd),
            text = "MetronomeBody min/maxHeight: $minHeight/$maxWidth, min/maxWidth: $maxWidth/$maxWidth",
            style = MaterialTheme.typography.labelSmall
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            BoxWithConstraints {
                Text(
                    modifier = Modifier.zIndex(4f).background(Color.White).align(Alignment.BottomEnd),
                    text = "MetronomeInnderBody min/maxHeight: $minHeight/$maxWidth, min/maxWidth: $maxWidth/$maxWidth",
                    style = MaterialTheme.typography.labelSmall
                )

                Column {
                    /** Tempo Slider */
                    /** Tempo Slider */
                    Text(
                        modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
                        text = uiState.tempoDescription,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = uiState.sliderValue,
                        valueRange =
                        MetronomeSettings.BPM_RANGE.first.toFloat()..
                                MetronomeSettings.BPM_RANGE.last.toFloat(),
                        onValueChange = { eventHandler(MetronomeUiEvent.UpdateSliderValue(it)) },
                    )
                }

                /** Beats per bar, Click per beat and Tab tempo */

                /** Beats per bar, Click per beat and Tab tempo */
                MetronomeExtraSettingsRow(
                    uiState = uiState,
                    eventHandler = eventHandler
                )
            }
        }
    }
}


@Composable
fun MetronomeIncrementBpmButton(
    bpmIncrement: Int,
    size: Dp,
    eventHandler: (MetronomeUiEvent) -> Unit
) {
    OutlinedButton(
        onClick = { eventHandler(MetronomeUiEvent.IncrementBpm(bpmIncrement)) },
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(MaterialTheme.spacing.extraSmall)
    ) {
        Text(
            text = (if(bpmIncrement > 0) "+" else "") + bpmIncrement.toString(),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun MetronomeExtraSettingsRow(
    uiState: MetronomeUiState,
    eventHandler: (MetronomeUiEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        MetronomeExtraSettingsColumn(label = "Beats/bar") {
            MetronomeIncrementer(
                value = uiState.settings.beatsPerBar,
                onIncrement = { eventHandler(MetronomeUiEvent.IncrementBeatsPerBar) },
                onDecrement = { eventHandler(MetronomeUiEvent.DecrementBeatsPerBar) },
            )
        }

        MetronomeExtraSettingsColumn(label = "Clicks/beat") {
            MetronomeIncrementer(
                value = uiState.settings.clicksPerBeat,
                onIncrement = { eventHandler(MetronomeUiEvent.IncrementClicksPerBeat) },
                onDecrement = { eventHandler(MetronomeUiEvent.DecrementClicksPerBeat) },
            )
        }

        MetronomeExtraSettingsColumn(label = "Tab tempo") {
            IconButton(
                onClick = { eventHandler(MetronomeUiEvent.TabTempo) },
                modifier = Modifier.size(25.dp),
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Box(
            modifier = Modifier.heightIn(max = 50.dp)
        ) {
            content()
        }
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