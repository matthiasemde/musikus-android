/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.settings.di

import app.musikus.settings.domain.UserPreferencesRepository
import app.musikus.settings.domain.usecase.ChangeMetronomeSettingsUseCase
import app.musikus.settings.domain.usecase.GetColorSchemeUseCase
import app.musikus.settings.domain.usecase.GetFolderSortInfoUseCase
import app.musikus.settings.domain.usecase.GetGoalSortInfoUseCase
import app.musikus.settings.domain.usecase.GetItemSortInfoUseCase
import app.musikus.settings.domain.usecase.GetMetronomeSettingsUseCase
import app.musikus.settings.domain.usecase.GetThemeUseCase
import app.musikus.settings.domain.usecase.SelectColorSchemeUseCase
import app.musikus.settings.domain.usecase.SelectFolderSortModeUseCase
import app.musikus.settings.domain.usecase.SelectGoalsSortModeUseCase
import app.musikus.settings.domain.usecase.SelectItemSortModeUseCase
import app.musikus.settings.domain.usecase.SelectThemeUseCase
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserPreferencesUseCasesModule {

    @Provides
    @Singleton
    fun provideUserPreferencesUseCases(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesUseCases {
        return UserPreferencesUseCases(
            getTheme = GetThemeUseCase(userPreferencesRepository),
            getColorScheme = GetColorSchemeUseCase(userPreferencesRepository),
            getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository),
            getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository),
            getGoalSortInfo = GetGoalSortInfoUseCase(userPreferencesRepository),
            selectTheme = SelectThemeUseCase(userPreferencesRepository),
            selectColorScheme = SelectColorSchemeUseCase(userPreferencesRepository),
            selectItemSortMode = SelectItemSortModeUseCase(userPreferencesRepository),
            selectFolderSortMode = SelectFolderSortModeUseCase(userPreferencesRepository),
            selectGoalSortMode = SelectGoalsSortModeUseCase(userPreferencesRepository),
            getMetronomeSettings = GetMetronomeSettingsUseCase(userPreferencesRepository),
            changeMetronomeSettings = ChangeMetronomeSettingsUseCase(userPreferencesRepository),
        )
    }
}
