/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.library.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.musikus.core.data.Nullable
import app.musikus.core.data.entities.ISoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.ISoftDeleteModelUpdateAttributes
import app.musikus.core.data.entities.SoftDeleteModel
import app.musikus.core.data.entities.SoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.SoftDeleteModelUpdateAttributes

private interface ILibraryFolderCreationAttributes : ISoftDeleteModelCreationAttributes {
    val name: String
}

private interface ILibraryFolderUpdateAttributes : ISoftDeleteModelUpdateAttributes {
    val name: String?
    val customOrder: Nullable<Int>?
}

data class LibraryFolderCreationAttributes(
    override val name: String,
) : SoftDeleteModelCreationAttributes(), ILibraryFolderCreationAttributes

data class LibraryFolderUpdateAttributes(
    override val name: String? = null,
    override val customOrder: Nullable<Int>? = null,
) : SoftDeleteModelUpdateAttributes(), ILibraryFolderUpdateAttributes

@Entity(tableName = "library_folder")
data class LibraryFolderModel(
    @ColumnInfo(name = "name") override var name: String,
    @ColumnInfo(name = "custom_order") override var customOrder: Nullable<Int>? = null,
) : SoftDeleteModel(), ILibraryFolderCreationAttributes, ILibraryFolderUpdateAttributes
