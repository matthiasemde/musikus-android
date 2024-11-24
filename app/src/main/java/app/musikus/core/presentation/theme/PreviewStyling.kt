/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.core.presentation.theme

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.menu.domain.ThemeSelections

@Composable
fun MusikusThemedPreview(
    theme: ColorSchemeSelections = ColorSchemeSelections.DEFAULT,
    content: @Composable () -> Unit,
) {
    MusikusTheme(
        theme = ThemeSelections.SYSTEM,
        colorScheme = theme
    ) {
        Surface {
            content()
        }
    }
}

class MusikusColorSchemeProvider : PreviewParameterProvider<ColorSchemeSelections> {
    override val values = sequenceOf(
        ColorSchemeSelections.MUSIKUS,
        ColorSchemeSelections.DYNAMIC,
        ColorSchemeSelections.LEGACY,
    )
}

/** Convenience Templates for predefined Compose Preview groups **/

private const val groupNameAllLight: String = "All Elements - Light"
private const val groupNameAllDark: String = "All Elements - Dark"
private const val groupNameElem1: String = "Element 1"
private const val groupNameElem2: String = "Element 2"
private const val groupNameElem3: String = "Element 3"
private const val groupNameElem4: String = "Element 4"
private const val groupNameElem5: String = "Element 5"
private const val groupNameElem6: String = "Element 6"
private const val groupNameElem7: String = "Element 7"
private const val groupNameElem8: String = "Element 8"

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
@Preview(name = "Light (redundant)", group = groupNameAllLight)
@Preview(
    name = "Dark (redundant)",
    group = groupNameAllDark,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
private annotation class MusikusPreviewCommonAnnotations

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = "Whole Screen")
@Preview(
    name = "Dark",
    group = "Whole Screen",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "OldDevice",
    group = "Whole Screen",
    device = "id:Nexus 5",
    showSystemUi = true
)
@Preview(
    name = "Rotated",
    group = "Whole Screen",
    device = "spec:parent=Nexus 6,orientation=landscape",
    showSystemUi = true
)
annotation class MusikusPreviewWholeScreen

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem1)
@Preview(
    name = "Dark",
    group = groupNameElem1,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement1

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem2)
@Preview(
    name = "Dark",
    group = groupNameElem2,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement2

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem3)
@Preview(
    name = "Dark",
    group = groupNameElem3,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement3

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem4)
@Preview(
    name = "Dark",
    group = groupNameElem4,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement4

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem5)
@Preview(
    name = "Dark",
    group = groupNameElem5,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement5

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem6)
@Preview(
    name = "Dark",
    group = groupNameElem6,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement6

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem7)
@Preview(
    name = "Dark",
    group = groupNameElem7,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement7

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem8)
@Preview(
    name = "Dark",
    group = groupNameElem8,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MusikusPreviewElement8
