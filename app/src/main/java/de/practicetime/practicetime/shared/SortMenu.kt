package de.practicetime.practicetime.shared

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.practicetime.practicetime.ui.SortDirection

@Composable
fun <T> SortMenu(
    offset: DpOffset,
    show: Boolean,
    sortModes: List<T>,
    label: (T) -> String,
    onDismissHandler: () -> Unit,
    currentSortMode: T,
    currentSortDirection: SortDirection,
    onSelectionHandler: (T) -> Unit
) {
    DropdownMenu(
        offset = offset,
        expanded = show,
        onDismissRequest = onDismissHandler,
    ) {
        // Menu Header
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Sort by",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // Menu Body
        val directionIcon: @Composable () -> Unit = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = when (currentSortDirection) {
                    SortDirection.ASCENDING -> Icons.Default.ArrowUpward
                    SortDirection.DESCENDING -> Icons.Default.ArrowDownward
                },
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        }
        sortModes.forEach { sortMode ->
            val selected = sortMode == currentSortMode
            DropdownMenuItem(
                text = { Text(
                    text = label(sortMode),
                    color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Unspecified
                ) },
                onClick = { onSelectionHandler(sortMode) },
                trailingIcon = if (selected) directionIcon else null
            )
        }
    }
}
