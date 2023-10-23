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

package app.musikus.database

import androidx.room.ColumnInfo
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import app.musikus.Musikus
import app.musikus.utils.getCurrTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
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

/**
 * @Dao Base dao
 */

abstract class BaseDao<
    T : BaseModel,
    U : BaseModelUpdateAttributes,
    D
//    D : BaseModelDisplayAttributes
>(
    private val tableName: String,
    private val database: PTDatabase,
    displayAttributes: List<String>
) {

    protected val displayAttributesString = "id, " + displayAttributes.joinToString(separator = ", ")


    /**
     * @Insert queries
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directInsert(rows: List<T>)

    open suspend fun insert(row: T) {
        directInsert(listOf(row))
    }

    open suspend fun insert(rows: List<T>) {
        directInsert(rows)
    }


    /**
     * @Delete queries
     */

    @Delete
    protected abstract suspend fun directDelete(rows: List<T>)

    open suspend fun delete(id: UUID) {
        delete(listOf(id))
    }

    open suspend fun delete(ids: List<UUID>) {
        directDelete(getModels(ids))
    }


    /**
     * @Update queries
     */
    @Update
    protected abstract suspend fun directUpdate(rows: List<T>)

    open suspend fun update(id: UUID, updateAttributes: U) {
        update(listOf(Pair(id, updateAttributes)))
    }

    open suspend fun update(rows: List<Pair<UUID, U>>) {
        rows.unzip().let { (ids, updateAttributes) ->
            directUpdate(
                getModels(ids).zip(updateAttributes).map { (old, updateAttributes) ->
                    applyUpdateAttributes(old, updateAttributes)
                }
            )
        }
    }

    protected open fun applyUpdateAttributes(old: T, updateAttributes: U): T = old


    /**
     * @RawQueries of all properties for update function
     */

    @RawQuery
    protected abstract suspend fun getModels(query: SupportSQLiteQuery): List<T>

    protected suspend fun getModel(id: UUID) = getModels(listOf(id)).firstOrNull()

    protected open suspend fun getModels(ids: List<UUID>) = getModels(
        SimpleSQLiteQuery(
            query = "SELECT * FROM $tableName WHERE " +
                "id IN (${ids.joinToString(separator = ",") { "?" }});",
            ids.map { UUIDConverter().toByte(it) }.toTypedArray()
        )
    )


    /**
     * @RawQueries for standard getters
     */

    @RawQuery
    protected abstract suspend fun get(query: SupportSQLiteQuery): List<D>

    protected suspend fun get(id: UUID) = get(listOf(id)).firstOrNull()

    protected open suspend fun get(ids: List<UUID>) = get(
        SimpleSQLiteQuery(
            query = "SELECT $displayAttributesString FROM $tableName WHERE " +
                "id IN (${ids.joinToString(separator = ",") { "?" }});",
            ids.map { UUIDConverter().toByte(it) }.toTypedArray()
        )
    )

    protected open suspend fun getAll() = get(
        SimpleSQLiteQuery("SELECT $displayAttributesString FROM $tableName;")
    )

    /**
     * Flow getters
     */

    fun getAsFlow(id: UUID): Flow<D?> = getAsFlow(listOf(id)).map { it.firstOrNull() }
    fun getAsFlow(ids: List<UUID>): Flow<List<D>> = subscribeTo { get(ids) }
    open fun getAllAsFlow(): Flow<List<D>> = subscribeTo { getAll() }

    private fun subscribeTo(query: suspend () -> List<D>): Flow<List<D>> {
        val notify = MutableSharedFlow<String>()

        var observer: InvalidationTracker.Observer? = null

        return flow {
            observer = observer ?: (object : InvalidationTracker.Observer(tableName) {
                override fun onInvalidated(tables: Set<String>) {
                    Musikus.ioThread { runBlocking {
                        notify.emit("invalidated")
                    }}
                }
            }.also {
                database.invalidationTracker.addObserver(it)
            })

            // emit the initial value
            emit(query())

            notify.collect {
                emit(query())
            }
        }.onCompletion {
            observer?.let {
                database.invalidationTracker.removeObserver(it)
            }
        }
    }
}

abstract class TimestampDao<
    T : TimestampModel,
    U : TimestampModelUpdateAttributes,
    D
//    D : TimestampModelDisplayAttributes
>(
    tableName: String,
    database: PTDatabase,
    displayAttributes: List<String>
) : BaseDao<T, U, D>(
    tableName = tableName,
    database = database,
    displayAttributes = listOf("created_at", "modified_at") + displayAttributes
) {
    override suspend fun insert(row: T) {
        insert(listOf(row))
    }

    override suspend fun insert(rows: List<T>) {
        val now = getCurrTimestamp()
        super.insert(rows.onEach {
            it.createdAt = now
            it.modifiedAt = now
        })
    }

    override fun applyUpdateAttributes(
        old: T,
        updateAttributes: U
    ): T = super.applyUpdateAttributes(old, updateAttributes).apply{
        modifiedAt = getCurrTimestamp()
    }
}

abstract class SoftDeleteDao<
    T : SoftDeleteModel,
    U : SoftDeleteModelUpdateAttributes,
    D
//    D : SoftDeleteModelDisplayAttributes
>(
    private val tableName: String,
    database: PTDatabase,
    displayAttributes: List<String>
) : TimestampDao<T, U, D>(
    tableName = tableName,
    database = database,
    displayAttributes = displayAttributes
) {
    @Update
    abstract override suspend fun directUpdate(rows: List<T>)

    override suspend fun delete(id: UUID) {
        delete(listOf(id))
    }

    override suspend fun delete(ids: List<UUID>) {
        directUpdate(getModels(ids).onEach { it.deleted = true })
    }

    suspend fun restore(id: UUID) {
        restore(listOf(id))
    }

    suspend fun restore(ids: List<UUID>) {
        directUpdate(getModels(ids).onEach { it.deleted = false })
    }

    override suspend fun get(ids: List<UUID>) = get(
        SimpleSQLiteQuery(
            query = "SELECT ${super.displayAttributesString} FROM $tableName WHERE " +
                "id IN (${ids.joinToString(separator = ",") { "?" }}) AND deleted=0;",
            ids.map { UUIDConverter().toByte(it) }.toTypedArray()
        )
    )

    override suspend fun getAll() = get(
        SimpleSQLiteQuery("SELECT * FROM $tableName WHERE deleted=0;")
    )

    @RawQuery
    abstract suspend fun clean(
        query: SimpleSQLiteQuery = SimpleSQLiteQuery(
            query = "DELETE FROM $tableName WHERE " +
                "deleted=1 " +
                "AND (modified_at+5<${getCurrTimestamp()});"
//                    "AND (modified_at+2592000<${getCurrTimestamp()});"
        )
    ) : Int
}
