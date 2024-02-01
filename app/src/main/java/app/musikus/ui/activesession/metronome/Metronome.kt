package app.musikus.ui.activesession.metronome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.ui.theme.spacing

@Composable
fun Metronome(
    viewModel: MetronomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent


    Column(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        MetronomeTopBar(
            uiState = uiState,
            eventHandler = eventHandler
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium))

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

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        /** Beats per bar, Click per beat and Tab tempo */

        MetronomeExtraSettingsRow(
            uiState = uiState,
            eventHandler = eventHandler
        )



        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }

}


@Composable
fun MetronomeTopBar(
    uiState: MetronomeUiState,
    eventHandler: (MetronomeUiEvent) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        // -5 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = -5,
            size = 30.dp,
            eventHandler = eventHandler
        )
        // -1 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = -1,
            size = 25.dp,
            eventHandler = eventHandler
        )

        // Bpm
        Text(
            modifier = Modifier
                .padding(MaterialTheme.spacing.small)
                .width(100.dp),
            text = uiState.settings.bpm.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        // +1 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = 1,
            size = 25.dp,
            eventHandler = eventHandler
        )
        // +5 Bpm
        MetronomeIncrementBpmButton(
            bpmIncrement = 5,
            size = 30.dp,
            eventHandler = eventHandler
        )

        Spacer(modifier = Modifier.weight(1f))

        // Start/Stop Metronome
        FilledIconButton(
            onClick = { eventHandler(MetronomeUiEvent.ToggleIsPlaying) },
            modifier = Modifier,
            shape = CircleShape,
        ) {
            if(!uiState.isPlaying) {
                Icon(
                    modifier = Modifier
                        .size(25.dp),
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start metronome"
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(25.dp),
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop metronome"
                )
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
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
        modifier = Modifier
            .padding(MaterialTheme.spacing.extraSmall)
            .size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
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
        verticalAlignment = Alignment.CenterVertically,
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
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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