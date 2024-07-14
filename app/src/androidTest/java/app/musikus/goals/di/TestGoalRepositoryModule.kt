/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.goals.di

import app.musikus.core.data.MusikusDatabase
import app.musikus.goals.data.GoalRepositoryImpl
import app.musikus.goals.domain.GoalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [GoalRepositoryModule::class]
)
object TestGoalRepositoryModule {
    @Provides
    fun provideGoalRepository(
        @Named("test_db") database: MusikusDatabase
    ): GoalRepository {
        return GoalRepositoryImpl(database)
    }

}