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

package de.practicetime.practicetime.database

import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import de.practicetime.practicetime.PracticeTime.Companion.ioThread
import de.practicetime.practicetime.utils.getCurrTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Semaphore
import java.util.*

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
    protected abstract fun directInsert(row: T)

    fun insert(row: T) {
        ioThread {
            if(row is ModelWithTimestamps) {
                row.createdAt = getCurrTimestamp()
                row.modifiedAt = getCurrTimestamp()
            }
            directInsert(row)
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun directInsert(rows: List<T>)

    fun insert(rows: List<T>) {
        ioThread {
            rows.forEach {
                if (it is ModelWithTimestamps) {
                    it.createdAt = getCurrTimestamp()
                    it.modifiedAt = getCurrTimestamp()
                }
            }
            directInsert(rows)
        }
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
    protected abstract fun directUpdate(row: T)

    fun update(row: T) {
        ioThread {
            if(row is ModelWithTimestamps) {
                row.modifiedAt = getCurrTimestamp()
            }
            directUpdate(row)
        }
    }

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun directUpdate(rows: List<T>)

    fun update(rows: List<T>) {
        ioThread {
            rows.forEach {
                if (it is ModelWithTimestamps) {
                    it.modifiedAt = getCurrTimestamp()
                }
            }
            directUpdate(rows)
        }
    }


    /**
     * @RawQueries for standard getters
     */

    @RawQuery
    protected abstract suspend fun get(query: SupportSQLiteQuery): List<T>

    open suspend fun get(id: UUID) = get(listOf(id)).firstOrNull()

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

    fun getAsFlow(from: String, id: UUID): Flow<T?> = getAsFlow(from, listOf(id)).map { it.firstOrNull() }
    fun getAsFlow(from: String, ids: List<UUID>): Flow<List<T>> = subscribeTo(from) { get(ids) }
    fun getAllAsFlow(from: String): Flow<List<T>> = subscribeTo(from) { getAll() }

    private fun subscribeTo(from: String, query: suspend () -> List<T>): Flow<List<T>> {
        val invalidations = Semaphore(permits = 1, acquiredPermits = 0)

        val id = UUID.randomUUID().toString()

        var observer: InvalidationTracker.Observer? = null

        Log.d("BASE_DAO", "$tableName, $id from $from: subscribedTo")
        return flow {
            observer = object : InvalidationTracker.Observer(tableName) {
                override fun onInvalidated(tables: Set<String>) {
                    Log.d("BASE_DAO", "$tableName, $id from $from: invalidated")
                    try {
                        invalidations.release()
                        Log.d("BASE_DAO", "$tableName, $id from $from: permit released")
                    } catch (e: Exception) {
                        Log.w("BASE_DAO", "Semaphore already released. Is your query taking a long time?")
                    }
                }
            }.also {
                database.invalidationTracker.addObserver(it)
                Log.d("BASE_DAO", "$tableName, $id from $from: observer added")
            }

            while (true) {
                invalidations.acquire()
                Log.d("BASE_DAO", "$tableName, $id from $from: acquired permit")
                emit(query())
            }
        }.onCompletion {
            observer?.let {
                database.invalidationTracker.removeObserver(it)
            }
            Log.d("BASE_DAO", "$tableName, $id from $from: observer removed")
        }
    }
}
