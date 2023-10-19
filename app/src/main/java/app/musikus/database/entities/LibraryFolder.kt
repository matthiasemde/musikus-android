/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.musikus.database.SoftDeleteModel

@Entity(tableName = "library_folder")
data class LibraryFolder (
    @ColumnInfo(name="name") var name: String,
//    @ColumnInfo(name="profile_id", index = true) val profileId: UUID? = null,
    @ColumnInfo(name="order", defaultValue = "0") var order: Int? = null,
) : SoftDeleteModel()