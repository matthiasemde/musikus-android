package de.practicetime.practicetime.database

import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import de.practicetime.practicetime.utils.getCurrTimestamp

abstract class BaseModel (
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
) {
    override fun toString(): String {
        return "Pretty print of ${this.javaClass.simpleName} entity:\n" +
                "id: ${this.id}:\n" +
            (if (this is ModelWithTimestamps)
                "\tcreated at: \t${this.createdAt}\n" +
                "\tmodified_at: \t${this.modifiedAt}\n"
            else "") +
            "\tentity: \t\t$this"
    }
}

/**
 * @Model Model with timestamps
 */

abstract class ModelWithTimestamps (
    @ColumnInfo(name="created_at", defaultValue = "0") var createdAt: Long = getCurrTimestamp(),
    @ColumnInfo(name="modified_at", defaultValue = "0") var modifiedAt: Long = getCurrTimestamp(),
) : BaseModel()

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
    abstract suspend fun insert(row: T) : Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(rows: List<T>) : List<Long>

    @Transaction
    open suspend fun insertAndGet(row: T) : T? {
        val newId = insert(row)
        return get(newId)
    }

    @Transaction
    open suspend fun insertAndGet(rows: List<T>) : List<T> {
        val newIds = insert(rows)
        return get(newIds)
    }


    /**
     * @Delete queries
     */

    @Delete
    abstract suspend fun delete(row: T)

    @Delete
    abstract suspend fun delete(rows: List<T>)

    suspend fun getAndDelete(id: Long) {
        get(id)?.let { delete(it) }
            ?: Log.e("BASE_DAO", "id: $id not found while trying to delete")
    }

    suspend fun getAndDelete(ids: List<Long>) {
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

    open suspend fun get(id: Long): T? {
        return getSingle(
            SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id=$id")
        )
    }

    @RawQuery
    protected abstract suspend fun getMultiple(query: SupportSQLiteQuery): List<T>

    open suspend fun get(ids: List<Long>): List<T> {
        return getMultiple(
            SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id IN (${ids.joinToString(",")})")
        )
    }

    @RawQuery
    protected abstract suspend fun getAll(query: SupportSQLiteQuery): List<T>

    open suspend fun getAll(): List<T> {
        return getAll(SimpleSQLiteQuery("SELECT * FROM $tableName"))
    }
}