/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val primary_light = Color(0xFF007792)
val secondary_light = primary_light
val secondary_container_light = Color(0xFF6EC5D8)
val primary_dark = Color(0xFF009FBF)
val secondary_dark = primary_dark
val md_grey_100 = Color(0xFFF5F5F5)

val grey = Color(0xFF9E9E9E)
val dark_grey_lighter = Color(0xFF292929)
val dark_grey = Color(0xFF121212)
val dark_grey_darker = Color(0xFF080808)

val md_grey_300 = Color(0xFFE0E0E0)
val md_grey_400 = Color(0xFFBDBDBD)
val md_grey_500 = Color(0xFF9E9E9E)
val md_grey_600 = Color(0xFF757575)
val md_grey_700 = Color(0xFF616161)

private val md_theme_light_primary = primary_light
private val md_theme_light_onPrimary = Color.White
private val md_theme_light_primaryContainer = primary_light
private val md_theme_light_onPrimaryContainer = Color.White
private val md_theme_light_secondary = secondary_light
private val md_theme_light_onSecondary = Color.White
private val md_theme_light_secondaryContainer = secondary_container_light
private val md_theme_light_onSecondaryContainer = Color.Black
private val md_theme_light_tertiary = Color.Green
private val md_theme_light_onTertiary = Color.White
private val md_theme_light_tertiaryContainer = md_theme_light_tertiary
private val md_theme_light_onTertiaryContainer = md_theme_light_onTertiary
private val md_theme_light_error = Color.Red
private val md_theme_light_errorContainer = md_theme_light_error
private val md_theme_light_onError = Color.White
private val md_theme_light_onErrorContainer = md_theme_light_errorContainer
private val md_theme_light_background = md_grey_100
private val md_theme_light_onBackground = Color.Black
private val md_theme_light_surface = md_grey_100
private val md_theme_light_onSurface = Color.Black
private val md_theme_light_surfaceVariant = md_theme_light_surface
private val md_theme_light_onSurfaceVariant = Color.Black
private val md_theme_light_outline = grey
private val md_theme_light_inverseOnSurface = Color(0xFFD6F6FF)
private val md_theme_light_inverseSurface = Color(0xFF00363F)
private val md_theme_light_inversePrimary = Color(0xFF5CD5FB)
private val md_theme_light_surfaceTint = md_grey_700
private val md_theme_light_outlineVariant = grey
private val md_theme_light_scrim = Color.Black

private val md_theme_dark_primary = primary_dark
private val md_theme_dark_onPrimary = Color.White
private val md_theme_dark_primaryContainer = primary_dark
private val md_theme_dark_onPrimaryContainer = Color.White
private val md_theme_dark_secondary = secondary_dark
private val md_theme_dark_onSecondary = Color.White
private val md_theme_dark_secondaryContainer = secondary_dark
private val md_theme_dark_onSecondaryContainer = Color.White
private val md_theme_dark_tertiary = Color.Green
private val md_theme_dark_onTertiary = Color.White
private val md_theme_dark_tertiaryContainer = Color.Green
private val md_theme_dark_onTertiaryContainer = Color.White
private val md_theme_dark_error = Color.Red
private val md_theme_dark_errorContainer = Color.Red
private val md_theme_dark_onError = Color.White
private val md_theme_dark_onErrorContainer = Color.White
private val md_theme_dark_background = dark_grey_darker
private val md_theme_dark_onBackground = Color.White
private val md_theme_dark_surface = dark_grey_darker
private val md_theme_dark_onSurface = Color.White
private val md_theme_dark_surfaceVariant = dark_grey_lighter
private val md_theme_dark_onSurfaceVariant = Color.White
private val md_theme_dark_outline = grey
private val md_theme_dark_inverseOnSurface = Color(0xFF001F25)
private val md_theme_dark_inverseSurface = Color(0xFFA6EEFF)
private val md_theme_dark_inversePrimary = Color(0xFF00677F)
private val md_theme_dark_surfaceTint = Color.White
private val md_theme_dark_outlineVariant = grey
private val md_theme_dark_scrim = Color(0xFF000000)

val LegacyLightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

val LegacyDarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)
