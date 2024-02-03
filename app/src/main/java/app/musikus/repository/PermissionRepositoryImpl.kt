/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.repository

import app.musikus.usecase.permissions.PermissionRepository
import app.musikus.utils.PermissionChecker

class PermissionRepositoryImpl(
    private val permissionChecker: PermissionChecker
) : PermissionRepository {
    override suspend fun requestPermissions(permissions: List<String>): Boolean {
        val result = permissionChecker.requestPermission(*permissions.toTypedArray())
        return result.isSuccess
    }
}