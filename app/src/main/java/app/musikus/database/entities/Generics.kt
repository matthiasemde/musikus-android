/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.UUID

interface IBaseModelCreationAttributes
interface IBaseModelUpdateAttributes

abstract class BaseModelCreationAttributes : IBaseModelCreationAttributes

abstract class BaseModelUpdateAttributes : IBaseModelUpdateAttributes

abstract class BaseModelDisplayAttributes(
    @ColumnInfo(name = "id") var id: UUID = UUID.randomUUID()
)

abstract class BaseModel(
    @PrimaryKey var id: UUID = UUID.randomUUID()
) : IBaseModelCreationAttributes, IBaseModelUpdateAttributes {

    override fun toString(): String {
        return "\nPretty print of ${this.javaClass.simpleName} entity:\n" +
                "\tid: \t\t\t\t${this.id}\n"
    }
}

/**
 * @Model Model with timestamps
 */

interface ITimestampModelCreationAttributes : IBaseModelCreationAttributes
interface ITimestampModelUpdateAttributes : IBaseModelUpdateAttributes

abstract class TimestampModelCreationAttributes
    : BaseModelCreationAttributes(), ITimestampModelCreationAttributes

abstract class TimestampModelUpdateAttributes
    : BaseModelUpdateAttributes(), ITimestampModelUpdateAttributes

abstract class TimestampModelDisplayAttributes(
    @ColumnInfo(name = "created_at") var createdAt: Long = 0,
    @ColumnInfo(name = "modified_at") var modifiedAt: Long = 0
) : BaseModelDisplayAttributes()

abstract class TimestampModel(
    @ColumnInfo(name="created_at", defaultValue = "0") var createdAt: Long? = null,
    @ColumnInfo(name="modified_at", defaultValue = "0") var modifiedAt: Long? = null
) : BaseModel(), ITimestampModelCreationAttributes, ITimestampModelUpdateAttributes {

    override fun toString(): String {
        return super.toString() +
                "\tcreated at: \t\t${this.createdAt}\n" +
                "\tmodified_at: \t\t${this.modifiedAt}\n"
    }
}

/**
 * @Model Soft delete model
 */

interface ISoftDeleteModelCreationAttributes : ITimestampModelCreationAttributes
interface ISoftDeleteModelUpdateAttributes : ITimestampModelUpdateAttributes

abstract class SoftDeleteModelCreationAttributes
    : TimestampModelCreationAttributes(), ISoftDeleteModelCreationAttributes

abstract class SoftDeleteModelUpdateAttributes
    : TimestampModelUpdateAttributes(), ISoftDeleteModelUpdateAttributes

abstract class SoftDeleteModelDisplayAttributes
    : TimestampModelDisplayAttributes()

abstract class SoftDeleteModel(
    @ColumnInfo(name="deleted", defaultValue = "false") var deleted: Boolean = false
) : TimestampModel(), ISoftDeleteModelCreationAttributes, ISoftDeleteModelUpdateAttributes {

    override fun toString(): String {
        return super.toString() +
                "\tdeleted: ${deleted}\n"
    }
}