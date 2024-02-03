/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.permissions

import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    suspend fun requestPermission(permission: String): Boolean

    val permissions: Flow<List<String>>

}