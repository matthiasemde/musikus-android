package de.practicetime.practicetime.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ToggleButtonOption(
    val id: Int,
    val name: String
)

@Composable
fun MyToggleButton(
    modifier: Modifier = Modifier,
    options: List<ToggleButtonOption>,
    selected: ToggleButtonOption,
    onSelectedChanged: (ToggleButtonOption) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            Button(
                modifier = Modifier.
                    background(if(selected == option)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                    ),
                onClick = { onSelectedChanged(option) }
            ) {
                Text(text = option.name)
            }
        }
    }
}