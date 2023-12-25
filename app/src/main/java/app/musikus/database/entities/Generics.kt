/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.entities

import androidx.annotation.NonNull
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
    @ColumnInfo(name = "id")
    private lateinit var _id: UUID

    fun setId(id: UUID) {
        assert(!this::_id.isInitialized)
        _id = id
    }

    val id: UUID
        get() = _id

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        (other is BaseModelDisplayAttributes) && (other._id == _id)

    override fun hashCode() = _id.hashCode()

    override fun toString(): String {
        return "\nPretty print of ${this.javaClass.simpleName} entity:\n" +
                "\tid:\t\t\t\t$id\n"
    }
}

abstract class BaseModel(
    // Can't be use null, but this id will be overwritten when inserted
    @PrimaryKey
    var id: UUID = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF")
) : IBaseModelCreationAttributes, IBaseModelUpdateAttributes {

    override fun toString(): String {
        return "\nPretty print of ${this.javaClass.simpleName} entity:\n" +
                "\tid:\t\t\t\t$id\n"
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
    @ColumnInfo(name = "created_at")
    private lateinit var _createdAt: ZonedDateTime
    @ColumnInfo(name = "modified_at")
    private lateinit var _modifiedAt: ZonedDateTime

    fun setCreatedAt(createdAt: ZonedDateTime) {
        assert(!this::_createdAt.isInitialized)
        _createdAt = createdAt
    }

    fun setModifiedAt(modifiedAt: ZonedDateTime) {
        assert(!this::_modifiedAt.isInitialized)
        _modifiedAt = modifiedAt
    }

    val createdAt: ZonedDateTime
        get() = _createdAt

    val modifiedAt: ZonedDateTime
        get() = _modifiedAt
    override fun equals(other: Any?) =
        super.equals(other) &&
        (other is TimestampModelDisplayAttributes) &&
        (other.modifiedAt == modifiedAt)

    override fun hashCode() =
        super.hashCode() *
        HASH_FACTOR + modifiedAt.hashCode()

    override fun toString(): String {
        return super.toString() +
                "\tcreated at:\t\t${createdAt}\n" +
                "\tmodified_at:\t${modifiedAt}\n"
    }
}
abstract class TimestampModel(
    @ColumnInfo(name="created_at") var createdAt: ZonedDateTime? = null,
    @ColumnInfo(name="modified_at") var modifiedAt: ZonedDateTime? = null
) : BaseModel(), ITimestampModelCreationAttributes, ITimestampModelUpdateAttributes {

    override fun toString(): String {
        return super.toString() +
                "\tcreated at:\t\t$createdAt\n" +
                "\tmodified_at:\t$modifiedAt\n"
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