/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Michael Prommersberger
 */

package app.musikus.activesession.presentation

import app.musikus.R
import app.musikus.core.presentation.utils.UiText

val PLACEHOLDER = UiText.DynamicString("PLACEHOLDER")


enum class ActiveSessionIntroElement(
    val headline: UiText,
    val message: UiText,
    val version: Int,
) {
    FAB_START_PRACTICING(
        headline = UiText.StringResource(R.string.active_session_app_intro_fab_start_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_fab_start),
        version = 1,
    ),
    CURRENT_SECTION(
        headline = UiText.StringResource(R.string.active_session_app_intro_current_section_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_current_section),
        version = 1,
    ),
    PRACTICE_TIMER(
        headline = UiText.StringResource(R.string.active_session_app_intro_practice_timer_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_practice_timer),
        version = 1,
    ),
    MINIMIZE_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_minimize_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_minimize_button),
        version = 1,
    ),
    PAUSE_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_pause_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_pause_button),
        version = 1,
    ),
    DISCARD_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_discard_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_discard_button),
        version = 1,
    ),
    FAB_NEXT_SECTION(
        headline = UiText.StringResource(R.string.active_session_app_intro_fab_next_section_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_fab_next_section),
        version = 1,
    ),
    PAST_SECTIONS(
        headline = UiText.StringResource(R.string.active_session_app_intro_past_sections_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_past_sections),
        version = 1,
    ),
    FINISH_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_finish_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_finish_button),
        version = 1,
    ),
    RESUME_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_resume_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_resume_button),
        version = 1,
    ),
    TOOLS_BOTTOM_BAR(
        headline = UiText.StringResource(R.string.active_session_app_intro_tools_bottom_bar_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_tools_bottom_bar),
        version = 1,
    ),
    TOOLS_METRONOME_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_tools_metronome_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_tools_metronome_button),
        version = 1,
    ),
    TOOLS_RECORDER_BUTTON(
        headline = UiText.StringResource(R.string.active_session_app_intro_tools_recorder_button_hl),
        message = UiText.StringResource(R.string.active_session_app_intro_tools_recorder_button),
        version = 1,
    ),
    TOOLS_METRONOME(
        headline = PLACEHOLDER,
        message = PLACEHOLDER,
        version = 1,
    ),
    TOOLS_RECORDER(
        headline = PLACEHOLDER,
        message = PLACEHOLDER,
        version = 1,
    ),
}
