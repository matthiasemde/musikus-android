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

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import app.musikus.Musikus
import app.musikus.database.MusikusDatabase
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.BaseModel
import app.musikus.database.entities.BaseModelDisplayAttributes
import app.musikus.database.entities.BaseModelUpdateAttributes
import app.musikus.database.entities.SoftDeleteModel
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import app.musikus.database.entities.SoftDeleteModelUpdateAttributes
import app.musikus.database.entities.TimestampModel
import app.musikus.database.entities.TimestampModelDisplayAttributes
import app.musikus.database.entities.TimestampModelUpdateAttributes
import app.musikus.database.toDatabaseString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import java.util.UUID

const val HASH_FACTOR = 524287


/**
 * @Dao Base dao
 */

abstract class BaseDao<
    T : BaseModel,
    U : BaseModelUpdateAttributes,
    D : BaseModelDisplayAttributes
>(
    private val tableName: String,
    private val database: MusikusDatabase,
    displayAttributes: List<String>
) {

    protected val displayAttributesString = (
        listOf("id") +
        displayAttributes
    ).joinToString(separator = ", ")


    /**
     * @Insert queries
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directInsert(rows: List<T>)

    open suspend fun insert(row: T) {
        directInsert(listOf(row))
    }

    open suspend fun insert(rows: List<T>) {
        rows.onEach {
            it.id = database.idProvider.generateId()
        }
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

    protected suspend fun getModel(id: UUID) = getModels(listOf(id)).first()

    protected open suspend fun getModels(ids: List<UUID>): List<T> {
        val models = getModels(
            SimpleSQLiteQuery(
                query = "SELECT * FROM $tableName WHERE " +
                    "id IN (${ids.joinToString(separator = ",") { "?" }});",
                ids.map { UUIDConverter().toByte(it) }.toTypedArray()
            )
        )

        // make sure all models were found
        if(models.size != ids.size) {
            throw IllegalArgumentException(
                "Could not find the following id(s): ${ids - models.map { it.id }.toSet()}"
            )
        }

        return models
    }


    /**
     * @RawQueries for standard getters
     */

    @RawQuery
    protected abstract suspend fun get(query: SupportSQLiteQuery): List<D>

    protected suspend fun get(id: UUID) = get(listOf(id)).first()

    protected open suspend fun get(ids: List<UUID>): List<D>  {
        val entities = get(
            SimpleSQLiteQuery(
                query = "SELECT $displayAttributesString FROM $tableName WHERE " +
                        "id IN (${ids.joinToString(separator = ",") { "?" }});",
                ids.map { UUIDConverter().toByte(it) }.toTypedArray()
            )
        )

        // make sure all entities were found
        if(entities.size != ids.size) {
            throw IllegalArgumentException(
                "Could not find the following id(s): ${ids - entities.map { it.id }.toSet()}"
            )
        }

        return entities
    }

    protected open suspend fun getAll() = get(
        SimpleSQLiteQuery("SELECT $displayAttributesString FROM $tableName;")
    )

    /**
     * Flow getters
     */

    fun getAsFlow(id: UUID): Flow<D> = getAsFlow(listOf(id)).map { it.first() }
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

    /**
     * Exists Check
     */
    suspend fun exists(id: UUID): Boolean {
        return try {
            get(id)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

abstract class TimestampDao<
    T : TimestampModel,
    U : TimestampModelUpdateAttributes,
    D : TimestampModelDisplayAttributes
>(
    tableName: String,
    private val database: MusikusDatabase,
    displayAttributes: List<String>
) : BaseDao<T, U, D>(
    tableName = tableName,
    database = database,
    displayAttributes =
        listOf("created_at", "modified_at") +
        displayAttributes
) {

    override suspend fun insert(row: T) {
        insert(listOf(row))
    }

    override suspend fun insert(rows: List<T>) {
        val now = database.timeProvider.now()
        super.insert(rows.onEach {
            it.createdAt = now
            it.modifiedAt = now
        })
    }

    override fun applyUpdateAttributes(
        old: T,
        updateAttributes: U
    ): T = super.applyUpdateAttributes(old, updateAttributes).apply{
        modifiedAt = database.timeProvider.now()
    }
}

abstract class SoftDeleteDao<
    T : SoftDeleteModel,
    U : SoftDeleteModelUpdateAttributes,
    D : SoftDeleteModelDisplayAttributes
>(
    private val tableName: String,
    private val database: MusikusDatabase,
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
        directUpdate(getModels(ids).onEach {
            it.deleted = true
            it.modifiedAt = database.timeProvider.now()
        })
    }

    suspend fun restore(id: UUID) {
        restore(listOf(id))
    }

    suspend fun restore(ids: List<UUID>) {
        directUpdate(getModels(ids).onEach {
            it.deleted = false
            it.modifiedAt = database.timeProvider.now()
        })
    }

    override suspend fun get(ids: List<UUID>) : List<D> {
        val rows = get(
            SimpleSQLiteQuery(
                query = "SELECT ${super.displayAttributesString} FROM $tableName WHERE " +
                        "id IN (${ids.joinToString(separator = ",") { "?" }}) AND deleted=0;",
                ids.map { UUIDConverter().toByte(it) }.toTypedArray()
            )
        )

        // make sure all rows were found
        if(rows.size != ids.size) {
            throw IllegalArgumentException(
                "Could not find the following id(s): ${ids - rows.map { it.id }.toSet()}"
            )
        }

        return rows
    }

    override suspend fun getAll() = get(
        SimpleSQLiteQuery("SELECT * FROM $tableName WHERE deleted=0;")
    )

    @RawQuery
    abstract suspend fun clean(
        query: SimpleSQLiteQuery = SimpleSQLiteQuery(
            query = "DELETE FROM $tableName WHERE " +
                "deleted=1 " +
                "AND (datetime(modified_at) < " +
                    "datetime('${database.timeProvider.now().toDatabaseString()}', '-1 month')" +
                    ");"
        )
    ) : Int
}
