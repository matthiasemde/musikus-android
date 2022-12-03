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

abstract class BaseDao<T>(
    private val tableName: String
) where T : BaseModel {

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
    protected  abstract suspend fun directInsert(rows: List<T>)

    suspend fun insert(rows: List<T>) {
        rows.forEach {
            if(it is ModelWithTimestamps) {
                it.createdAt = getCurrTimestamp()
                it.modifiedAt = getCurrTimestamp()
            }
        }
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

    suspend fun update(row: T) {
        if(row is ModelWithTimestamps) {
            row.modifiedAt = getCurrTimestamp()
        }
        directUpdate(row)
    }

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun directUpdate(rows: List<T>)

    suspend fun update(rows: List<T>) {
        if(rows.firstOrNull() is ModelWithTimestamps) {
            rows.forEach { (it as ModelWithTimestamps).modifiedAt = getCurrTimestamp() }
        }
        directUpdate(rows)
    }


    /**
     * @RawQueries for standard getters
     */

    @RawQuery
    protected abstract suspend fun getSingle(query: SupportSQLiteQuery): T?

    open suspend fun get(id: UUID): T? {
        return getSingle(
            SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id=x'${uuidConverter.toDBString(id)}'")
        )
    }

    @RawQuery
    protected abstract suspend fun getMultiple(query: SupportSQLiteQuery): List<T>

    open suspend fun get(ids: List<UUID>): List<T> {
        return getMultiple(
            SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id IN (${ids.joinToString(",") { id -> uuidConverter.toDBString(id) }})")
        )
    }

    @RawQuery
    protected abstract suspend fun getAll(query: SupportSQLiteQuery): List<T>

    open suspend fun getAll(): List<T> {
        return getAll(SimpleSQLiteQuery("SELECT * FROM $tableName"))
    }

    companion object {
        val uuidConverter = UUIDConverter()
    }
}
