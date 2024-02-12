package app.musikus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Selectable(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
    shape: CornerBasedShape = MaterialTheme.shapes.medium,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.clip(shape)) {
        content()
        Box(
            modifier = Modifier
                .matchParentSize()
                .conditional(enabled) {
                    combinedClickable(
                        onClick = onShortClick,
                        onLongClick = onLongClick
                    )
                }
                .background(
                    if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    else Color.Transparent,
                )
        )
    }
}