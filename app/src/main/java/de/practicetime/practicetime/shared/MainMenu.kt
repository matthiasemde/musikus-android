package de.practicetime.practicetime.shared

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.practicetime.practicetime.PracticeTime

enum class ThemeSelections {
    SYSTEM,
    DAY,
    NIGHT,
}

enum class CommonMenuSelections {
    THEME,
    APP_INFO
}

@Composable
fun CommonMenuItems(
    onSelectionHandler: (CommonMenuSelections) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Toast.makeText(context, "Settings Activity Result", Toast.LENGTH_SHORT).show()
    }
    DropdownMenuItem(
        text = { Text(text = "Theme") },
        trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
        onClick = { onSelectionHandler(CommonMenuSelections.THEME) }
    )
    DropdownMenuItem(
        text = { Text(text = "App Info") },
        onClick = { onSelectionHandler(CommonMenuSelections.APP_INFO) }
    )
//    DropdownMenuItem(
//        text = { Text(text="Export") },
//        onClick = { PracticeTime.exportDatabase(context, launcher) }
//    )
//    DropdownMenuItem(
//        text = { Text(text="Import") },
//        onClick = { PracticeTime.importDatabase() }
//    )
}

@Composable
fun MainMenu(
    show: Boolean,
    onDismissHandler: () -> Unit,
    onSelectionHandler: (
        commonSelection: CommonMenuSelections
    ) -> Unit,
    uniqueMenuItems: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismissHandler,
    ) {
        uniqueMenuItems()
        CommonMenuItems(
            onSelectionHandler = onSelectionHandler
        )
    }
}


@Composable
fun ThemeMenu(
    expanded: Boolean,
    currentTheme: ThemeSelections,
    onDismissHandler: () -> Unit,
    onSelectionHandler: (ThemeSelections) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissHandler) {
        // Menu Header
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Theme",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // Menu Items
        DropdownMenuItem(
            text = { Text(
                text = "Automatic",
                color =
                    if (currentTheme == ThemeSelections.SYSTEM) MaterialTheme.colorScheme.primary
                    else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(ThemeSelections.SYSTEM) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.SYSTEM) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        DropdownMenuItem(
            text = { Text(
                text = "Light",
                color =
                    if (currentTheme == ThemeSelections.DAY) MaterialTheme.colorScheme.primary
                    else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(ThemeSelections.DAY) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.DAY) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        DropdownMenuItem(
            text = { Text(
                text = "Dark",
                color =
                    if (currentTheme == ThemeSelections.NIGHT) MaterialTheme.colorScheme.primary
                    else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(ThemeSelections.NIGHT) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.NIGHT) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

