/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.permissions.data

import app.musikus.permissions.domain.PermissionChecker
import app.musikus.permissions.domain.PermissionRepository

class PermissionRepositoryImpl(
    private val permissionChecker: PermissionChecker
) : PermissionRepository {
    override suspend fun requestPermissions(permissions: List<String>): Result<Unit> {
        return permissionChecker.requestPermission(*permissions.toTypedArray())
    }
}
