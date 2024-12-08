/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.permissions.di

import android.app.Application
import app.musikus.core.di.ApplicationScope
import app.musikus.permissions.data.TestPermissionRepository
import app.musikus.permissions.domain.PermissionChecker
import app.musikus.permissions.domain.PermissionRepository
import app.musikus.permissions.domain.usecase.PermissionsUseCases
import app.musikus.permissions.domain.usecase.RequestPermissionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PermissionsModule::class]
)
object TestPermissionsModule {

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
    fun providePermissionRepository(): PermissionRepository {
        return TestPermissionRepository()
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
