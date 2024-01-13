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
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import app.musikus.Musikus.Companion.ioThread
import app.musikus.database.MusikusDatabase
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.BaseModel
import app.musikus.database.entities.BaseModelCreationAttributes
import app.musikus.database.entities.BaseModelDisplayAttributes
import app.musikus.database.entities.BaseModelUpdateAttributes
import app.musikus.database.entities.SoftDeleteModel
import app.musikus.database.entities.SoftDeleteModelCreationAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import app.musikus.database.entities.SoftDeleteModelUpdateAttributes
import app.musikus.database.entities.TimestampModel
import app.musikus.database.entities.TimestampModelCreationAttributes
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
    M : BaseModel,
    C : BaseModelCreationAttributes,
    U : BaseModelUpdateAttributes,
    D : BaseModelDisplayAttributes
>(
    private val tableName: String,
    private val database: MusikusDatabase,
    displayAttributes: List<String>,
    private val dependencies: List<String>? = null // queries results will be filtered if rows in these tables are deleted
) {

    protected val displayAttributesString = (
        listOf("id") +
        displayAttributes
    ).joinToString(separator = ", ")


    /**
     * @Insert queries
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directInsert(models: List<M>)

    open suspend fun insert(creationAttributes: C): UUID {
        return insert(listOf(creationAttributes)).single() // returns the id of the inserted row
    }

    open suspend fun insert(creationAttributes: List<C>): List<UUID> {
        val models = creationAttributes
            .map { createModel(it) }
            .onEach { it.id = database.idProvider.generateId() }

        directInsert(models)

        return models.map { it.id }
    }

    protected abstract fun createModel(creationAttributes: C): M

    /**
     * @Delete queries
     */

    @Delete
    protected abstract suspend fun directDelete(rows: List<M>)

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
    protected abstract suspend fun directUpdate(rows: List<M>)

    open suspend fun update(id: UUID, updateAttributes: U) {
        update(listOf(Pair(id, updateAttributes)))
    }

    open suspend fun update(rows: List<Pair<UUID, U>>) {
        rows.unzip().let { (ids, updateAttributes) ->
            directUpdate(
                getModels(ids).zip(updateAttributes).map { (old, updateAttributes) ->
                    modelWithAppliedUpdateAttributes(old, updateAttributes)
                }
            )
        }
    }

    protected open fun modelWithAppliedUpdateAttributes(oldModel: M, updateAttributes: U): M = oldModel


    /**
     * @RawQueries of all properties for update function
     */

    @RawQuery
    protected abstract suspend fun getModels(query: SupportSQLiteQuery): List<M>

    protected suspend fun getModel(id: UUID) = getModels(listOf(id)).first()

    protected open suspend fun getModels(ids: List<UUID>): List<M> {
        val uniqueIds = ids.toSet() // TODO possibly make ids Set instead of List in the first place
        val models = getModels(
            SimpleSQLiteQuery(
                query = "SELECT * FROM $tableName WHERE " +
                    "id IN (${uniqueIds.joinToString(separator = ",") { "?" }}) " +
                    if (dependencies == null) ";"
                    else dependencies.joinToString(separator = "") { dependentTableName ->
                        "AND ${dependentTableName}_id IN (SELECT id FROM $dependentTableName WHERE deleted = 0) "
                    } + ";",
                uniqueIds.map { UUIDConverter().toByte(it) }.toTypedArray()
            )
        )

        // make sure all models were found
        if(models.size != uniqueIds.size) {
            throw IllegalArgumentException(
                "Could not find ${tableName}(s) with the following id(s): ${uniqueIds - models.map { it.id }.toSet()}"
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
        val uniqueIds = ids.toSet() // TODO possibly make ids Set instead of List in the first place
        val rows = get(
            SimpleSQLiteQuery(
                query = "SELECT $displayAttributesString FROM $tableName WHERE " +
                        "id IN (${uniqueIds.joinToString(separator = ",") { "?" }}) " +
                        if (dependencies == null) ";"
                        else dependencies.joinToString(separator = "") { dependentTableName ->
                            "AND ${dependentTableName}_id IN (SELECT id FROM $dependentTableName WHERE deleted = 0) "
                        } + ";",
                uniqueIds.map { UUIDConverter().toByte(it) }.toTypedArray()
            )
        )

        // make sure all rows were found
        if(rows.size != uniqueIds.size) {
            throw IllegalArgumentException(
                "Could not find ${tableName}(s) with the following id(s): ${uniqueIds - rows.map { it.id }.toSet()}"
            )
        }

        return rows
    }

    protected open suspend fun getAll() = get(
        SimpleSQLiteQuery(
            "SELECT $displayAttributesString FROM $tableName " +
            if (dependencies == null) ";"
            else "WHERE " + dependencies.joinToString(separator = "AND ") { dependentTableName ->
                "${dependentTableName}_id IN (SELECT id FROM $dependentTableName WHERE deleted = 0) "
            } + ";"
        )
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
                    ioThread { runBlocking {
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

    /**
     * Transactions
     */
    suspend fun <R> transaction(block: suspend () -> R): R {
        return database.withTransaction(block)
    }
}

abstract class TimestampDao<
    M : TimestampModel,
    C : TimestampModelCreationAttributes,
    U : TimestampModelUpdateAttributes,
    D : TimestampModelDisplayAttributes
>(
    tableName: String,
    private val database: MusikusDatabase,
    displayAttributes: List<String>,
    dependencies: List<String>? = null
) : BaseDao<M, C, U, D>(
    tableName = tableName,
    database = database,
    displayAttributes =
        listOf("created_at", "modified_at") +
        displayAttributes,
    dependencies = dependencies
) {

    // no need to override insert (single) because it calls insert (list)
    override suspend fun insert(creationAttributes: List<C>): List<UUID> {
        val now = database.timeProvider.now()

        val models = creationAttributes
            .map { createModel(it) }
            .onEach {
                it.id = database.idProvider.generateId()
                it.createdAt = now
                it.modifiedAt = now
            }

        directInsert(models)

        return models.map { it.id }
    }

    override fun modelWithAppliedUpdateAttributes(
        oldModel: M,
        updateAttributes: U
    ): M = super.modelWithAppliedUpdateAttributes(oldModel, updateAttributes).apply {
        modifiedAt = database.timeProvider.now()
    }
}

abstract class SoftDeleteDao<
    M : SoftDeleteModel,
    C : SoftDeleteModelCreationAttributes,
    U : SoftDeleteModelUpdateAttributes,
    D : SoftDeleteModelDisplayAttributes
>(
    private val tableName: String,
    private val database: MusikusDatabase,
    displayAttributes: List<String>
) : TimestampDao<M, C, U, D>(
    tableName = tableName,
    database = database,
    displayAttributes = displayAttributes
) {
    @Update
    abstract override suspend fun directUpdate(rows: List<M>)

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
        val uniqueIds = ids.toSet() // TODO possibly make ids Set instead of List in the first place
        val rows = get(
            SimpleSQLiteQuery(
                query = "SELECT ${super.displayAttributesString} FROM $tableName WHERE " +
                        "id IN (${uniqueIds.joinToString(separator = ",") { "?" }}) " +
                        "AND deleted=0;",
                uniqueIds.map { UUIDConverter().toByte(it) }.toTypedArray()
            )
        )

        // make sure all rows were found
        if(rows.size != uniqueIds.size) {
            throw IllegalArgumentException(
                "Could not find ${tableName}(s) with the following id(s): ${uniqueIds - rows.map { it.id }.toSet()}"
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
