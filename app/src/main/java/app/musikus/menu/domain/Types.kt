/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.menu.domain

import app.musikus.R
import app.musikus.core.data.EnumWithDescription
import app.musikus.core.data.EnumWithLabel
import app.musikus.core.presentation.utils.UiText
import app.musikus.menu.domain.ColorSchemeSelections.valueOf
import app.musikus.menu.domain.ThemeSelections.valueOf

enum class ThemeSelections : EnumWithLabel {
    SYSTEM {
        override val label = UiText.StringResource(R.string.settings_appearance_theme_options_system)
    },
    DAY {
        override val label = UiText.StringResource(R.string.settings_appearance_theme_options_day)
    },
    NIGHT {
        override val label = UiText.StringResource(R.string.settings_appearance_theme_options_night)
    };

    companion object {
        val DEFAULT = SYSTEM

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

enum class ColorSchemeSelections : EnumWithLabel, EnumWithDescription {
    MUSIKUS {
        override val label = UiText.StringResource(R.string.settings_appearance_color_scheme_options_musikus_title)
        override val description = UiText.StringResource(R.string.settings_appearance_color_scheme_options_musikus_text)
    },
    LEGACY {
        override val label = UiText.StringResource(R.string.settings_appearance_color_scheme_options_legacy_title)
        override val description = UiText.StringResource(R.string.settings_appearance_color_scheme_options_legacy_text)
    },
    DYNAMIC {
        override val label = UiText.StringResource(R.string.settings_appearance_color_scheme_options_dynamic_title)
        override val description = UiText.StringResource(R.string.settings_appearance_color_scheme_options_dynamic_text)
    };

    companion object {
        val DEFAULT = MUSIKUS

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}
