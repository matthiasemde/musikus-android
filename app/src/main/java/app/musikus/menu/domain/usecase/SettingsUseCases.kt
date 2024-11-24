/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.menu.domain.usecase

data class SettingsUseCases(
    val getTheme: GetThemeUseCase,
    val getColorScheme: GetColorSchemeUseCase,
    val selectTheme: SelectThemeUseCase,
    val selectColorScheme: SelectColorSchemeUseCase,
)
