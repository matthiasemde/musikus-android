package app.musikus.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.activesession.presentation.ActiveSessionIntroElement
import app.musikus.core.presentation.theme.dimensions
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiText


/**
 * Usage:
 *  val registry = rememberAppIntroElementBounds()
 *  Button(modifier = Modifier.registerAppIntroElement("my_button", registry)) { ... }
 *  if (showOverlay) OnboardingOverlay(registry, activeTargetId = "my_button") { ... dialog content ... }
 */

/** remember a registry you pass to targets and the overlay */
@Composable
fun rememberAppIntroElementBounds(): SnapshotStateMap<ActiveSessionIntroElement, Rect> = remember { mutableStateMapOf() }

/** mark a composable as a target to be cut out */
fun Modifier.registerAppIntroElement(id: ActiveSessionIntroElement, registry: SnapshotStateMap<ActiveSessionIntroElement, Rect>): Modifier =
    this.then(
        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
            // store bounds in window coordinates so overlay can use them directly
            registry[id] = coordinates.boundsInWindow()
        })


/**
 * Full-screen onboarding overlay that cuts out a target rect using BlendMode.Clear.
 * - `registry` is the map filled via `Modifier.onShowcaseTarget`
 * - `activeTargetId` the id to cut out (null = full scrim, no cutout)
 * - `scrimColor` overlay color
 * - `cutoutCornerRadius` radius in Dp
 * - `dialog` content will be positioned near the target (below) or centered if no target
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppIntroDialog(
    cutout: Rect,
    headline: UiText,
    message: UiText,
    onSkipIntro: () -> Unit,
    onConfirm: () -> Unit,
) {
    val density = LocalDensity.current
    val margin = MaterialTheme.dimensions.introElementCutoutMarginPx

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Offscreen compositing required for BlendMode.Clear to punch holes
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(Color.Transparent)
    ) {
        // Draw scrim and cutout(s)
        Canvas(
            modifier = Modifier
                // consume all pointer events so taps/presses don't pass through the scrim
                .pointerInteropFilter() { event ->
                    val position = Offset(event.x, event.y)
                    val isInsideCutout = cutout.contains(position)
                    if (isInsideCutout) {
                        false // Don't consume - let event pass through
                    } else {
                        true // Consume - block taps outside cutout
                    }
                }
                .matchParentSize()) {
            // full scrim
            drawRect(Color.Black.copy(alpha = 0.7f))

            drawIntoCanvas { canvas ->
                val paint = Paint().apply { blendMode = BlendMode.Clear }
                // convert window rect (already in px) to DrawScope coordinates
                val left = cutout.left - margin/2
                val top = cutout.top - margin/2
                val size = Size(cutout.width + margin, cutout.height + margin)

                // draw rounded rect cleared
                val cornerRadius = 12.dp
                canvas.drawRoundRect(
                    left = left,
                    top = top,
                    right = left + size.width,
                    bottom = top + size.height,
                    radiusX = with(density) { cornerRadius.toPx() },
                    radiusY = with(density) { cornerRadius.toPx() },
                    paint = paint
                )
            }
        }

        // calculate if dialog would overlap element. If yes, align dialog on Top instead
        val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val introDialogTotalHeightDp = MaterialTheme.dimensions.introDialogHeight + MaterialTheme.spacing.extraLarge * 2
        val thresholdPx = with(density) { (introDialogTotalHeightDp).toPx() }
        // distance from the cutout's bottom to the screen bottom
        val distanceFromBottomPx = screenHeightPx - cutout.bottom
        // alignTop = true when the cutout's bottom is less than intro dialog height (plus spacing) away from the screen bottom
        val alignTop = distanceFromBottomPx < thresholdPx

        IntroDialogContent(headline, message, alignTop, onConfirm, onSkipIntro)
    }
}


@Composable
private fun BoxScope.IntroDialogContent(
    headline: UiText,
    message: UiText,
    alignTop: Boolean,
    onConfirm: () -> Unit,
    onSkipIntro: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraLarge)
            .fillMaxWidth()
            .then(
                if (alignTop) {
                    Modifier.align(Alignment.TopCenter)
                } else {
                    Modifier.align(Alignment.BottomCenter)
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.height(MaterialTheme.dimensions.introDialogHeight)) {
            Column(
                Modifier
                    .padding(
                        start = MaterialTheme.spacing.large, end = MaterialTheme.spacing.large, top = MaterialTheme.spacing.large
                    )
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                Text(
                    text = headline.asString(), style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = message.asString(), style = MaterialTheme.typography.bodyMedium
                )
            }
            DialogActions(
                onConfirmHandler = onConfirm,
                confirmButtonText = stringResource(R.string.components_intro_dialog_action_ok),
                onDismissHandler = onSkipIntro,
                dismissButtonText = stringResource(R.string.components_intro_dialog_action_skip_intro),
            )
        }
    }
}

