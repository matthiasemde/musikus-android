/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class LibraryItem(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name="modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name="name") val name: String,
    @ColumnInfo(name="color_index") val colorIndex: Int,
    @ColumnInfo(name="library_folder_id") val libraryFolderId: UUID?,
    @ColumnInfo(name="custom_order") val customOrder: Int?,
) : SoftDeleteModelDisplayAttributes()  {

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is LibraryItem) &&
                (other.name == name) &&
                (other.colorIndex == colorIndex) &&
                (other.libraryFolderId == libraryFolderId) &&
                (other.customOrder == customOrder)

    override fun hashCode() =
        (((super.hashCode() *
                HASH_FACTOR + name.hashCode()) *
                HASH_FACTOR + colorIndex.hashCode()) *
                HASH_FACTOR + libraryFolderId.hashCode()) *
                HASH_FACTOR + customOrder.hashCode()

    override fun toString(): String {
        return super.toString() +
                "\tname:\t\t\t\t$name\n" +
                "\tcolorIndex:\t\t\t$colorIndex\n" +
                "\tlibraryFolderId:\t$libraryFolderId\n" +
                "\tcustomOrder:\t\t$customOrder\n"
    }
}

@Dao
abstract class LibraryItemDao(
    private val database: MusikusDatabase
) : SoftDeleteDao<LibraryItemModel, LibraryItemUpdateAttributes, LibraryItem>(
    tableName = "library_item",
    database = database,
    displayAttributes = listOf("name", "color_index", "library_folder_id", "custom_order")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: LibraryItemModel,
        updateAttributes: LibraryItemUpdateAttributes
    ): LibraryItemModel = super.applyUpdateAttributes(old, updateAttributes).apply{
        name = updateAttributes.name ?: old.name
        colorIndex = updateAttributes.colorIndex ?: old.colorIndex
        libraryFolderId = updateAttributes.libraryFolderId ?: old.libraryFolderId
        customOrder = updateAttributes.customOrder ?: old.customOrder
    }

    /**
    *  @Queries
    */

    @Transaction
    @Query(
        "SELECT " +
                "library_item.id, " +
                "library_item.name, " +
                "library_item.color_index, " +
                "library_item.custom_order, " +
                "library_item.created_at, " +
                "library_item.modified_at, " +
            "CASE WHEN library_folder.deleted=1 THEN NULL ELSE library_folder.id END AS library_folder_id" +
            " FROM library_item LEFT JOIN library_folder ON library_item.library_folder_id = library_folder.id WHERE " +
            "library_item.deleted=0"
    )
    abstract override fun getAllAsFlow(): Flow<List<LibraryItem>>

    override suspend fun clean(query: SimpleSQLiteQuery): Int {
        return cleanItems()
    }
    @Transaction
    @Query(
        "DELETE FROM library_item WHERE " +
            "deleted=1 " +
            "AND (NOT EXISTS (SELECT id FROM section WHERE library_item_id = library_item.id)) " +
            "AND (NOT EXISTS (SELECT id FROM goal_description_library_item_cross_ref WHERE library_item_id = library_item.id)) " +
            "AND (datetime(modified_at) < datetime(:now, '-1 month'));"
    )
    protected abstract suspend fun cleanItems(
        now : ZonedDateTime = database.timeProvider.now()
    ) : Int

//    @Transaction
//    @Query("SELECT * FROM library_item WHERE id=:id")
//    abstract suspend fun getWithGoalDescriptions(id: UUID): LibraryItemWithGoalDescriptions?
}