/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.domain.usecase

data class UserPreferencesUseCases(
    val getTheme: GetThemeUseCase,
    val getColorScheme: GetColorSchemeUseCase,
    val getFolderSortInfo: GetFolderSortInfoUseCase,
    val getItemSortInfo: GetItemSortInfoUseCase,
    val getGoalSortInfo: GetGoalSortInfoUseCase,
    val selectTheme: SelectThemeUseCase,
    val selectColorScheme: SelectColorSchemeUseCase,
    val selectFolderSortMode: SelectFolderSortModeUseCase,
    val selectItemSortMode: SelectItemSortModeUseCase,
    val selectGoalSortMode: SelectGoalsSortModeUseCase,
    val getMetronomeSettings: GetMetronomeSettingsUseCase,
    val changeMetronomeSettings: ChangeMetronomeSettingsUseCase
)
