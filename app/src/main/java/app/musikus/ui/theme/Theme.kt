/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.datastore.ColorSchemeSelections
import app.musikus.datastore.ThemeSelections


/** Library item colors */

val libraryItemColors = listOf(
    Color(0xFFf7a397),
    Color(0xFFE03F54),
    Color(0xFFFF8F33),
    Color(0xFFFFC233),
    Color(0xFF997099),
    Color(0xFF5474BF),
    Color(0xFF80995C),
    Color(0xFF29CCAE),
    Color(0xFF748EA7),
    Color(0xFFA8684C)
)

@Composable
fun MusikusTheme(
    theme: ThemeSelections,
    colorScheme: ColorSchemeSelections,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when(theme) {
        ThemeSelections.SYSTEM -> isSystemInDarkTheme()
        ThemeSelections.DAY -> false
        ThemeSelections.NIGHT -> true
    }

    val colorSchemeDarkOrLight = when(colorScheme) {
        ColorSchemeSelections.MUSIKUS -> {
            if (useDarkTheme) DarkColorScheme else LightColorScheme
        }
        ColorSchemeSelections.LEGACY -> {
            if (useDarkTheme) LegacyDarkColorScheme else LegacyLightColorScheme
        }
        ColorSchemeSelections.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (useDarkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    // enable Edge-to-Edge drawing with new API
    // skip this block when in LocalInspectionMode (Compose Preview) so that we can use
    // MusikusTheme{} wrapper also in Compose Previews
    if (!LocalInspectionMode.current) {
        val context = LocalContext.current as ComponentActivity
        LaunchedEffect(key1 = theme) {

            val statusBarColorDarkTheme = android.graphics.Color.TRANSPARENT
            val statusBarColorLightTheme = android.graphics.Color.TRANSPARENT
            val statusBarColorLightThemeOldDevices = DarkColorScheme.surface.toArgb()

            val navBarColorDarkTheme = android.graphics.Color.TRANSPARENT
            val navBarColorLightTheme = android.graphics.Color.TRANSPARENT
            val navBarColorLightThemeOldDevices = DarkColorScheme.surface.toArgb()

            context.enableEdgeToEdge(
                statusBarStyle = if (useDarkTheme)
                    SystemBarStyle.dark(
                        scrim = statusBarColorDarkTheme
                    )
                else SystemBarStyle.light(
                    scrim = statusBarColorLightTheme,
                    darkScrim = statusBarColorLightThemeOldDevices
                ),
                navigationBarStyle = if (useDarkTheme)
                    SystemBarStyle.dark(
                        scrim = navBarColorDarkTheme
                    )
                else SystemBarStyle.light(
                    scrim = navBarColorLightTheme,
                    darkScrim = navBarColorLightThemeOldDevices
                ),
            )
        }
    }

    MaterialTheme(
        colorScheme = colorSchemeDarkOrLight,
        content = content
    )
}

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
@Preview(name = "Dark (redundant)", group = groupNameAllDark, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
private annotation class MusikusPreviewCommonAnnotations

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = "Whole Screen")
@Preview(name = "Dark", group = "Whole Screen", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "OldDevice", group = "Whole Screen",
    device = "id:Nexus 5",
    showSystemUi = true
)
@Preview(name = "Rotated", group = "Whole Screen",
    device = "spec:parent=Nexus 6,orientation=landscape", showSystemUi = true)
annotation class MusikusPreviewWholeScreen

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem1)
@Preview(name = "Dark", group = groupNameElem1, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement1

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem2)
@Preview(name = "Dark", group = groupNameElem2, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement2

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem3)
@Preview(name = "Dark", group = groupNameElem3, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement3

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem4)
@Preview(name = "Dark", group = groupNameElem4, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement4

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem5)
@Preview(name = "Dark", group = groupNameElem5, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement5

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem6)
@Preview(name = "Dark", group = groupNameElem6, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement6

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem7)
@Preview(name = "Dark", group = groupNameElem7, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement7

@MusikusPreviewCommonAnnotations
@Preview(name = "Light", group = groupNameElem8)
@Preview(name = "Dark", group = groupNameElem8, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class MusikusPreviewElement8


data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)


data class Dimensions(
    val cardPeekHeight: Dp = 105.dp,
    val cardNormalHeight: Dp = 300.dp,
    val cardHandleHeight: Dp = 19.dp,   // ~3dp handle height + 2*small spacing
    val bottomButtonsPagerHeight: Dp = 50.dp,
    val cardPeekContentHeight: Dp = cardPeekHeight - cardHandleHeight,
    val cardNormalContentHeight: Dp = cardNormalHeight - cardHandleHeight,
    /** new feature card properties **/
    val featureCardHeight: Dp = 95.dp,
    val featureCardExtendedHeight: Dp = 300.dp,
    val featureCardMargin: Dp = 16.dp,
    val featureCardCornerRadius: Dp = 16.dp,
    val featureCardElevation: Dp = 3.dp,
    val bottomToolbarHeight: Dp = 150.dp,
    /** new simple tools concept properties */
    val toolsHeaderHeight: Dp = 95.dp,
    val toolsBodyHeight: Dp = 205.dp,
    /** finally the new final shit */
    val toolsCardHeaderHeight: Dp = 95.dp,
    val toolsSheetTabRowHeight: Dp = 48.dp, // TODO: intrinsic size from Android, may be set explicitly in future?

    val toolsSheetPeekHeight: Dp = toolsHeaderHeight + 3.dp + 8.dp,    // 3.dp DragHandle + spacing

    val fabHeight: Dp = 56.dp   // TODO: remove and try to get it via intrisic defaults
)

val LocalSpacing = compositionLocalOf { Spacing() }
val LocalDimensions = compositionLocalOf { Dimensions() }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

@Suppress("UnusedReceiverParameter")
val MaterialTheme.dimensions: Dimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current