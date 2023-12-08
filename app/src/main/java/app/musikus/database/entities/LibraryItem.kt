/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.musikus.database.Nullable
import java.util.UUID

private interface ILibraryItemCreationAttributes : ISoftDeleteModelCreationAttributes {
    val name: String
    val colorIndex: Int
    val libraryFolderId: Nullable<UUID>?
}

private interface ILibraryItemUpdateAttributes : ISoftDeleteModelUpdateAttributes {
    val name: String?
    val colorIndex: Int?
    val libraryFolderId: Nullable<UUID>?
    val customOrder: Nullable<Int>?
}

data class LibraryItemCreationAttributes(
    override val name: String,
    override val colorIndex: Int,
    override val libraryFolderId: Nullable<UUID>? = Nullable(null), // default value because attr is optional
) : SoftDeleteModelCreationAttributes(), ILibraryItemCreationAttributes

data class LibraryItemUpdateAttributes(
    override val name: String? = null,
    override val colorIndex: Int? = null,
    override val libraryFolderId: Nullable<UUID>? = null,
    override val customOrder: Nullable<Int>? = null,
) : SoftDeleteModelUpdateAttributes(), ILibraryItemUpdateAttributes

@Entity(
    tableName = "library_item",
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolderModel::class,
            parentColumns = ["id"],
            childColumns = ["library_folder_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ]
)
data class LibraryItemModel(
    @ColumnInfo(name="name") override var name: String,
    @ColumnInfo(name="color_index") override var colorIndex: Int,
    @ColumnInfo(name="library_folder_id", index = true, defaultValue = "null")
    override var libraryFolderId: Nullable<UUID>?,
//    @ColumnInfo(name="profile_id", index = true) override var profileId: UUID? = null,
    @ColumnInfo(name="custom_order", defaultValue = "null") override var customOrder: Nullable<Int>? = null,
) : SoftDeleteModel(), ILibraryItemCreationAttributes, ILibraryItemUpdateAttributes {

    override fun toString(): String {
        return super.toString() +
            "\tname: \t\t\t\t${this.name}\n" +
            "\tcolor index: \t\t${this.colorIndex}\n" +
            "\tlibrary folder id: \t${this.libraryFolderId}\n" +
//            "\tprofile_id: \t\t${this.profileId}\n" +
//            "\tarchived: \t\t\t${this.archived}\n" +
            "\tcustom order: \t\t\t\t${this.customOrder}\n"
    }
}

class InvalidLibraryItemException(message: String): Exception(message)