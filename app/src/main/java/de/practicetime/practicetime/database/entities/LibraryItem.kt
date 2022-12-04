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

package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import de.practicetime.practicetime.database.ModelWithTimestamps
import java.util.*

@Entity(
    tableName = "library_item",
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolder::class,
            parentColumns = ["id"],
            childColumns = ["library_folder_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class LibraryItem (
    @ColumnInfo(name="name") var name: String,
    @ColumnInfo(name="color_index") var colorIndex: Int,
    @ColumnInfo(name="library_folder_id", index = true, defaultValue = "null")
    var libraryFolderId: UUID? = null,
//    @ColumnInfo(name="profile_id", index = true) val profileId: UUID? = null,
    @ColumnInfo(name="archived") var archived: Boolean = false,
    @ColumnInfo(name="order", defaultValue = "0") var order: Int? = null,
) : ModelWithTimestamps() {
    override fun toString(): String {
        return super.toString() +
            "\tname: \t\t\t\t${this.name}\n" +
            "\tcolor_index: \t\t${this.colorIndex}\n" +
            "\tlibrary_folder_id: \t${this.libraryFolderId}\n" +
//            "\tprofile_id: \t\t${this.profileId}\n" +
            "\tarchived: \t\t\t${this.archived}\n" +
            "\torder: \t\t\t\t${this.order}\n"
    }
}
