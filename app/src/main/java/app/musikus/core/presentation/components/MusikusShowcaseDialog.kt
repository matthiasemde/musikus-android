/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Michael Prommersberger
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiText
import app.musikus.menu.domain.ColorSchemeSelections
import com.joco.dialog.arrow.ArrowDialog
import com.joco.showcase.sequence.SequenceShowcase
import com.joco.showcase.sequence.rememberSequenceShowcaseState

@Composable
fun MusikusShowcaseDialog(
    targetRect: Rect,
    text: UiText,
    dialogWidth: Dp = MusikusShowcaseDialogDefaults.dialogWidth,
    onClick: () -> Unit
) {

    ArrowDialog(
        targetRect = targetRect,
        pointerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = { pointerColor ->
            Surface (
                modifier = Modifier
                    .width(dialogWidth),
                shape = MaterialTheme.shapes.large,
                color = pointerColor,
            ) {
                Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                    Text(text = text.asString())
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Button(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
                        Text(MusikusShowcaseDialogDefaults.buttonText.asString())
                    }
                }
            }
        }
    )
}

object MusikusShowcaseDialogDefaults {
    val dialogWidth: Dp = 300.dp
    val buttonText: UiText = UiText.StringResource(R.string.components_showcase_dialog_button)
}

@PreviewLightDark
@Composable
private fun MusikusShowcaseDialogPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        Box(Modifier.padding(20.dp)){
            MusikusShowcaseDialog(
                targetRect = Rect(
                    left = 750f,
                    top = 800f,
                    right = 0f,
                    bottom = 0f
                ),
                text = UiText.DynamicString("This is a showcase dialog. It can be used to demonstrate features or provide information."),
                onClick = { /* Handle click */ }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MusikusShowcaseDialogPreviewInteractive(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        val showcaseState = rememberSequenceShowcaseState()
        showcaseState.start()
        SequenceShowcase(state = showcaseState) {
            Button(
                modifier = Modifier.sequenceShowcaseTarget(
                    index = 0,
                    content = {
                        MusikusShowcaseDialog(
                            targetRect = it,
                            text = UiText.DynamicString(
                                "This is a showcase dialog. It can be used to demonstrate features or provide information."),
                            onClick = { /* Handle click */ }
                        )
                    }
                ),
                onClick = {}
            ) {
                Text("Enable interactive Preview to see Showcase dialog!")
            }
        }
    }
}