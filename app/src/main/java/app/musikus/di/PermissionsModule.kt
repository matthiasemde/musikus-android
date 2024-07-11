/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.di

import android.app.Application
import app.musikus.core.data.PermissionRepositoryImpl
import app.musikus.core.domain.usecase.permissions.PermissionRepository
import app.musikus.core.domain.usecase.permissions.PermissionsUseCases
import app.musikus.core.domain.usecase.permissions.RequestPermissionsUseCase
import app.musikus.utils.PermissionChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionsModule {

    @Provides
    @Singleton
    fun providePermissionChecker(
        application: Application,
        @ApplicationScope applicationScope: CoroutineScope
    ): PermissionChecker {
        return PermissionChecker(
            context = application,
            applicationScope = applicationScope
        )
    }

    @Provides
    @Singleton
    fun providePermissionRepository(
        permissionChecker: PermissionChecker
    ): PermissionRepository {
        return PermissionRepositoryImpl(permissionChecker)
    }

    @Provides
    @Singleton
    fun providePermissionsUseCases(
        permissionRepository: PermissionRepository
    ): PermissionsUseCases {
        return PermissionsUseCases(
            request = RequestPermissionsUseCase(permissionRepository)
        )
    }
}