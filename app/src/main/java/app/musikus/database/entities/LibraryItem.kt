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

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.musikus.database.SoftDeleteModel
import app.musikus.database.SoftDeleteModelCreationAttributes
import app.musikus.database.SoftDeleteModelUpdateAttributes
import java.util.UUID

class LibraryItemCreationAttributes(
    val name: String,
    val colorIndex: Int,
    val libraryFolderId: UUID? = null,
) : SoftDeleteModelCreationAttributes()

class LibraryItemUpdateAttributes(
    id: UUID,
    val name: String? = null,
    val colorIndex: Int? = null,
    val libraryFolderId: UUID? = null,
    val order: Int? = null,
) : SoftDeleteModelUpdateAttributes(
    id = id,
)

@Entity(
    tableName = "library_item",
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolder::class,
            parentColumns = ["id"],
            childColumns = ["library_folder_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ]
)
class LibraryItem : SoftDeleteModel {
    @ColumnInfo(name="name") var name: String?
    @ColumnInfo(name="color_index") var colorIndex: Int?
    @ColumnInfo(name="library_folder_id", index = true, defaultValue = "null")
    var libraryFolderId: UUID?
//    @ColumnInfo(name="profile_id", index = true) lateinit var profileId: UUID? = null
    @ColumnInfo(name="custom_order", defaultValue = "null") var order: Int? = null

    constructor(
        name: String,
        colorIndex: Int,
        libraryFolderId: UUID? = null,
        order: Int? = null,
    ) : super() {
        this.name = name
        this.colorIndex = colorIndex
        this.libraryFolderId = libraryFolderId
        this.order = order

        Log.e("LibraryItem","Use LibraryItemCreationAttributes instead")
//        throw NotImplementedError("Use LibraryItemCreationAttributes instead")
    }

    // Creation Constructor
    constructor(creationAttributes: LibraryItemCreationAttributes) : super() {
        name = creationAttributes.name
        colorIndex = creationAttributes.colorIndex
        libraryFolderId = creationAttributes.libraryFolderId
    }

    // Update Constructor
    constructor(updateAttributes: LibraryItemUpdateAttributes) : super(
        updateAttributes
    ) {
        name = updateAttributes.name
        colorIndex = updateAttributes.colorIndex
        libraryFolderId = updateAttributes.libraryFolderId
        order = updateAttributes.order
    }


    override fun toString(): String {
        return super.toString() +
            "\tname: \t\t\t\t${this.name}\n" +
            "\tcolor_index: \t\t${this.colorIndex}\n" +
            "\tlibrary_folder_id: \t${this.libraryFolderId}\n" +
//            "\tprofile_id: \t\t${this.profileId}\n" +
//            "\tarchived: \t\t\t${this.archived}\n" +
            "\torder: \t\t\t\t${this.order}\n"
    }
}
