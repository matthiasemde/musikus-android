/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.repository

import android.util.Log
import app.musikus.usecase.permissions.PermissionRepository
import app.musikus.utils.PermissionChecker
import kotlinx.coroutines.flow.Flow

class PermissionRepositoryImpl(
    private val permissionChecker: PermissionChecker
) : PermissionRepository {
    override suspend fun requestPermission(permission: String): Boolean {
        val result = permissionChecker.requestPermission(permission)
        Log.d("PermissionRepositoryImpl", "requestPermission: $permission, result: $result")
        return result.isSuccess
    }

    override val permissions: Flow<List<String>>
        get() = TODO("Not yet implemented")

}