/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.library.di

import app.musikus.core.data.MusikusDatabase
import app.musikus.core.di.IoScope
import app.musikus.library.data.LibraryRepositoryImpl
import app.musikus.library.domain.LibraryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LibraryRepositoryModule {

    @Provides
    @Singleton
    fun provideLibraryRepository(
        database: MusikusDatabase,
        @IoScope ioScope: CoroutineScope
    ): LibraryRepository {
        return LibraryRepositoryImpl(
            itemDao = database.libraryItemDao,
            folderDao = database.libraryFolderDao,
        ).apply {
            ioScope.launch {
                clean()
            }
        }
    }
}
