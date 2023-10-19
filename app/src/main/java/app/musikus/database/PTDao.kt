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

import android.util.Log
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

abstract class BaseModel (
    @PrimaryKey var id: UUID = UUID.randomUUID(),
) {
    override fun toString(): String {
        return "\nPretty print of ${this.javaClass.simpleName} entity:\n" +
            "\tid: \t\t\t\t${this.id}\n"
    }
}

/**
 * @Model Model with timestamps
 */

abstract class ModelWithTimestamps (
    @ColumnInfo(name="created_at", defaultValue = "0") var createdAt: Long = 0,
    @ColumnInfo(name="modified_at", defaultValue = "0") var modifiedAt: Long = 0,
) : BaseModel() {
    override fun toString(): String {
        return super.toString() +
            "\tcreated at: \t\t${this.createdAt}\n" +
            "\tmodified_at: \t\t${this.modifiedAt}\n"
    }
}

/**
 * @Model Soft delete model
 */

abstract class SoftDeleteModel (
    @ColumnInfo(name="deleted", defaultValue = "false") var deleted: Boolean = false,
) : ModelWithTimestamps() {
    override fun toString(): String {
        return super.toString() +
            "\tdeleted: ${deleted}\n"
    }
}

/**
 * @Dao Base dao
 */

abstract class BaseDao<T : BaseModel>(
    private val tableName: String,
    private val database: PTDatabase
) {

    /**
     * @Insert queries
     */

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directInsert(row: T)

    open suspend fun insert(row: T) {
        directInsert(row)
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directInsert(rows: List<T>)

    open suspend fun insert(rows: List<T>) {
        directInsert(rows)
    }

    /**
     * @Delete queries
     */

    @Delete
    abstract suspend fun delete(row: T)

    @Delete
    abstract suspend fun delete(rows: List<T>)

    suspend fun getAndDelete(id: UUID) {
        get(id)?.let { delete(it) }
            ?: Log.e("BASE_DAO", "id: $id not found while trying to delete")
    }

    suspend fun getAndDelete(ids: List<UUID>) {
        delete(get(ids))
    }


    /**
     * @Update queries
     */

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directUpdate(row: T)

    open suspend fun update(row: T) {
        directUpdate(row)
    }

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directUpdate(rows: List<T>)

    open suspend fun update(rows: List<T>) {
        directUpdate(rows)
    }


    /**
     * @RawQueries for standard getters
     */

    @RawQuery
    protected abstract suspend fun get(query: SupportSQLiteQuery): List<T>

    suspend fun get(id: UUID) = get(listOf(id)).firstOrNull()

    open suspend fun get(ids: List<UUID>) = get(
        SimpleSQLiteQuery(
            "SELECT * FROM $tableName WHERE id IN (${ids.joinToString(separator = ",") { "?" }});",
            ids.map { UUIDConverter().toByte(it) }.toTypedArray()
        )
    )

    open suspend fun getAll() = get(
        SimpleSQLiteQuery("SELECT * FROM $tableName;")
    )

    /**
     * Flow getters
     */

    fun getAsFlow(id: UUID): Flow<T?> = getAsFlow(listOf(id)).map { it.firstOrNull() }
    fun getAsFlow(ids: List<UUID>): Flow<List<T>> = subscribeTo { get(ids) }
    fun getAllAsFlow(): Flow<List<T>> = subscribeTo { getAll() }

    private fun subscribeTo(query: suspend () -> List<T>): Flow<List<T>> {
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

abstract class TimestampDao<T : ModelWithTimestamps>(
    tableName: String,
    database: PTDatabase
) : BaseDao<T>(
    tableName = tableName,
    database = database
) {
    override suspend fun insert(row: T) {
        row.createdAt = getCurrTimestamp()
        row.modifiedAt = getCurrTimestamp()
        super.insert(row)
    }

    override suspend fun insert(rows: List<T>) {
        rows.forEach {
            it.createdAt = getCurrTimestamp()
            it.modifiedAt = getCurrTimestamp()
        }
        super.insert(rows)
    }

    override suspend fun update(row: T) {
        row.modifiedAt = getCurrTimestamp()
        super.update(row)
    }

    override suspend fun update(rows: List<T>) {
        rows.forEach {
            it.modifiedAt = getCurrTimestamp()
        }
        super.update(rows)
    }
}

abstract class SoftDeleteDao<T : SoftDeleteModel>(
    private val tableName: String,
    private val database: PTDatabase
) : TimestampDao<T>(
    tableName = tableName,
    database = database
) {
    override suspend fun delete(row: T) {
        row.deleted = true
        super.update(row)
    }

    override suspend fun delete(rows: List<T>) {
        rows.forEach {
            it.deleted = true
        }
        super.update(rows)
    }

    suspend fun restore(row: T) {
        row.deleted = false
        super.update(row)
    }

    suspend fun restore(rows: List<T>) {
        rows.forEach {
            it.deleted = false
        }
        super.update(rows)
    }

    override suspend fun get(ids: List<UUID>) = get(
        SimpleSQLiteQuery(
            query = "SELECT * FROM $tableName WHERE " +
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