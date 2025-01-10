/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.menu.domain.ThemeSelections

/** Library item colors */

@Suppress("MagicNumber")
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
    val useDarkTheme = when (theme) {
        ThemeSelections.SYSTEM -> isSystemInDarkTheme()
        ThemeSelections.DAY -> false
        ThemeSelections.NIGHT -> true
    }

    val colorSchemeDarkOrLight = when (colorScheme) {
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
                statusBarStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(
                        scrim = statusBarColorDarkTheme
                    )
                } else {
                    SystemBarStyle.light(
                        scrim = statusBarColorLightTheme,
                        darkScrim = statusBarColorLightThemeOldDevices
                    )
                },
                navigationBarStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(
                        scrim = navBarColorDarkTheme
                    )
                } else {
                    SystemBarStyle.light(
                        scrim = navBarColorLightTheme,
                        darkScrim = navBarColorLightThemeOldDevices
                    )
                },
            )
        }
    }

    MaterialTheme(
        colorScheme = colorSchemeDarkOrLight,
        content = content
    )
}

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
    val cardHandleHeight: Dp = 19.dp, // ~3dp handle height + 2*small spacing
    val bottomButtonsPagerHeight: Dp = 50.dp,
    val cardPeekContentHeight: Dp = cardPeekHeight - cardHandleHeight,
    val cardNormalContentHeight: Dp = cardNormalHeight - cardHandleHeight,
    val toolsHeaderHeight: Dp = 95.dp,
    val toolsBodyHeight: Dp = 205.dp,
    val toolsSheetPeekHeight: Dp = toolsHeaderHeight + 3.dp + 8.dp, // 3.dp DragHandle + spacing
    val fabHeight: Dp = 56.dp // TODO: remove and try to get it via intrisic defaults
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
