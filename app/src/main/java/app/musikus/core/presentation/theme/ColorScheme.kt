/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde, Michael Prommersberger
 */

@file:Suppress("MagicNumber")

package app.musikus.core.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(

    // Primary
    primary = Color(0xFF7C5846),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB4),
    onPrimaryContainer = Color(0xFF291800),
    inversePrimary = Color(0xFFFFB955),

    // Secondary
    secondary = Color(0xFF705B40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDEBC),
    onSecondaryContainer = Color(0xFF271905),

    // Tertiary
    tertiary = Color(0xFF6F4E37),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7BBA9),
    onTertiaryContainer = Color(0xFF2C1F15),

    // Error
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Background
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF1F1B16),

    // Surface Colors
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF1F1B16),

    surfaceDim = Color(0xFFEBE7EB),
    surfaceBright = Color(0xFFFFFBFF),
    surfaceTint = Color(0xFF795548),

    surfaceVariant = Color(0xFFF0E0D0),
    onSurfaceVariant = Color(0xFF4F4539),

    inverseSurface = Color(0xFF34302A),
    inverseOnSurface = Color(0xFFF9EFE7),

    surfaceContainerLowest = Color(0xFFF8F0EB),
    surfaceContainerLow = Color(0xFFF4E8E1),
    surfaceContainer = Color(0xFFF2E3D9),
    surfaceContainerHigh = Color(0xFFEFDDD2),
    surfaceContainerHighest = Color(0xFFEBD4C6),

    // Outline
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4),

    // Shadow & Scrim
    scrim = Color(0xFF000000),
)

val DarkColorScheme = darkColorScheme(

    // Primary
    primary = Color(0xFFEEB666),
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFFD3B386),
    onPrimaryContainer = Color(0xFF251B0E),
    inversePrimary = Color(0xFF795548),

    // Secondary
    secondary = Color(0xFFDEC2A2),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF56432B),
    onSecondaryContainer = Color(0xFFFBDEBC),

    // Tertiary
    tertiary = Color(0xFF8C6038),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C3D21),
    onTertiaryContainer = Color(0xFFD7BBA9),

    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Background
    background = Color(0xFF12100D),
    onBackground = Color(0xFFEAE1D9),

    // Surface Colors
    surface = Color(0xFF12100D),
    onSurface = Color(0xFFEAE1D9),

    surfaceDim = Color(0xFF15110C),
    surfaceBright = Color(0xFF292520),
    surfaceTint = Color(0xFFFFB955),

    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),

    inverseSurface = Color(0xFFEAE1D9),
    inverseOnSurface = Color(0xFF1F1B16),

    surfaceContainerLowest = Color(0xFF12100D),
    surfaceContainerLow = Color(0xFF1E1A15),
    surfaceContainer = Color(0xFF241F19),
    surfaceContainerHigh = Color(0xFF2A241E),
    surfaceContainerHighest = Color(0xFF362F26),

    // Outline
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),

    // Shadow & Scrim
    scrim = Color(0xFF000000),
)
