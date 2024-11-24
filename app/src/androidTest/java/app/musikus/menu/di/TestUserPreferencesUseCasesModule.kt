/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.menu.di

import app.musikus.core.domain.UserPreferencesRepository
import app.musikus.menu.domain.usecase.GetColorSchemeUseCase
import app.musikus.menu.domain.usecase.GetThemeUseCase
import app.musikus.menu.domain.usecase.SelectColorSchemeUseCase
import app.musikus.menu.domain.usecase.SelectThemeUseCase
import app.musikus.menu.domain.usecase.SettingsUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SettingsUseCasesModule::class]
)
object TestUserPreferencesUseCasesModule {

    @Provides
    fun provideUserPreferencesUseCases(
        userPreferencesRepository: UserPreferencesRepository
    ): SettingsUseCases {
        return SettingsUseCases(
            getTheme = GetThemeUseCase(userPreferencesRepository),
            getColorScheme = GetColorSchemeUseCase(userPreferencesRepository),
            selectTheme = SelectThemeUseCase(userPreferencesRepository),
            selectColorScheme = SelectColorSchemeUseCase(userPreferencesRepository),
        )
    }
}