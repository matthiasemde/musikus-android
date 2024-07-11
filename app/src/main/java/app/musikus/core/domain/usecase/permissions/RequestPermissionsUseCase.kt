/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.domain.usecase.permissions

class RequestPermissionsUseCase(
    private val permissionRepository: PermissionRepository
) {
    suspend operator fun invoke(permissions: List<String>) : Result<Unit> {
        return permissionRepository.requestPermissions(permissions)
    }
}