/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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

    // source: https://gist.github.com/Khazbs/1f1f1b5c05f45dbfa465f249b1e20506
    // and https://stackoverflow.com/questions/65610216/how-to-change-statusbar-color-in-jetpack-compose
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)

                // background extends behind status and nav bar (even 3 button nav bar)
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, false)

                // status bar icons can adapt to light/dark theme,
                // so we can make the status bar transparent
                window.statusBarColor = Color.Transparent.toArgb()

                // navigation bar icons can only adapt to light theme for API >= 26 (O),
                // otherwise we need to manually set the nav bar color to something dark
                window.navigationBarColor = if(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || useDarkTheme
                ) {
                    Color.Transparent.toArgb()
                } else {
                    LightColorScheme.inverseSurface.toArgb()
                }
            }

            // make sure there is no black translucent overlay on the nav bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !useDarkTheme
            insets.isAppearanceLightNavigationBars = !useDarkTheme // has no effect on API < 26
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
    val cardHandleHeight: Dp = 19.dp,   // ~3dp handle height + 2*small spacing
    val bottomButtonsPagerHeight: Dp = 50.dp,
    val cardPeekContentHeight: Dp = cardPeekHeight - cardHandleHeight,
    val cardNormalContentHeight: Dp = cardNormalHeight - cardHandleHeight,
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