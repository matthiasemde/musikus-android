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
import app.musikus.database.daos.HASH_FACTOR
import java.time.ZonedDateTime
import java.util.UUID

interface IBaseModelCreationAttributes
interface IBaseModelUpdateAttributes

abstract class BaseModelCreationAttributes : IBaseModelCreationAttributes

abstract class BaseModelUpdateAttributes : IBaseModelUpdateAttributes

abstract class BaseModelDisplayAttributes {
    abstract val id: UUID

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        (other is BaseModelDisplayAttributes) && (other.id == id)

    override fun hashCode() = id.hashCode()

    override fun toString(): String {
        return "\nPretty print of ${this.javaClass.simpleName} entity:\n" +
                "\tid:\t\t\t\t\t\t$id\n"
    }
}

abstract class BaseModel(
    // Can't be use null, but this id will be overwritten when inserted
    @PrimaryKey
    var id: UUID = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF")
) : IBaseModelCreationAttributes, IBaseModelUpdateAttributes {

    override fun toString(): String {
        return "\nPretty print of ${this.javaClass.simpleName} entity:\n" +
                "\tid:\t\t\t\t\t$id\n"
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

abstract class TimestampModelDisplayAttributes : BaseModelDisplayAttributes() {
    abstract val createdAt: ZonedDateTime
    abstract val modifiedAt: ZonedDateTime

    override fun equals(other: Any?) =
        super.equals(other) &&
        (other is TimestampModelDisplayAttributes) &&
        (other.modifiedAt == modifiedAt)

    override fun hashCode() =
        super.hashCode() *
        HASH_FACTOR + modifiedAt.hashCode()

    override fun toString(): String {
        return super.toString() +
                "\tcreated at:\t\t\t\t${createdAt}\n" +
                "\tmodified_at:\t\t\t${modifiedAt}\n"
    }
}
abstract class TimestampModel(
    @ColumnInfo(name="created_at") var createdAt: ZonedDateTime = ZonedDateTime.parse("1970-01-01T00:00:00.000Z"),
    @ColumnInfo(name="modified_at") var modifiedAt: ZonedDateTime = ZonedDateTime.parse("1970-01-01T00:00:00.000Z")
) : BaseModel(), ITimestampModelCreationAttributes, ITimestampModelUpdateAttributes {

    override fun toString(): String {
        return super.toString() +
                "\tcreated at:\t\t\t$createdAt\n" +
                "\tmodified_at:\t\t$modifiedAt\n"
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
    : TimestampModelDisplayAttributes() {
}

abstract class SoftDeleteModel(
    @ColumnInfo(name="deleted", defaultValue = "false") var deleted: Boolean = false
) : TimestampModel(), ISoftDeleteModelCreationAttributes, ISoftDeleteModelUpdateAttributes {

    override fun toString(): String {
        return super.toString() +
                "\tdeleted: ${deleted}\n"
    }
}