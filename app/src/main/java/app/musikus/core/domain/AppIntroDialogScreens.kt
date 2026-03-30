/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Michael Prommersberger
 */

package app.musikus.core.domain

/**
 * Enum representing the screens for which app intro dialogs should be shown.
 * Every screen has its own set of intro dialogs with a specific order.
 */
enum class AppIntroDialogScreens {
    ACTIVESESSION,
    METRONOME,
    RECORDER,
    SESSIONS,
    LIBRARY,
    GOALS,
    HAMBURGER_MENU
}