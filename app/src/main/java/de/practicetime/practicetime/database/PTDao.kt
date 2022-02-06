package de.practicetime.practicetime.database

import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.utils.getCurrTimestamp
import java.util.*

abstract class BaseModel (
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
) {
    // pretty print entity id, timestamps (if given) and entity data
    fun pprint(): String {
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

@Dao
interface PTDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: PracticeSession): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSection(section: PracticeSection)

    @Transaction
    suspend fun insertSessionAndSectionsInTransaction(
        session: PracticeSession,
        sections: List<PracticeSection>,
    ) {
        val newSessionId = insertSession(session).toInt()

        // add the new sessionId to every section...
        for (section in sections) {
            section.practice_session_id = newSessionId
            // and insert them into the database
            insertSection(section)
        }
    }


    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Delete
    suspend fun deleteSection(section: PracticeSection)

    @Update
    suspend fun updateSession(session: PracticeSession)

    @Update
    suspend fun updateSection(section: PracticeSection)

    @Transaction
    suspend fun deleteSession(sessionId: Int, updatedGoalInstances: List<GoalInstance>) {
        updatedGoalInstances.forEach { PracticeTime.goalInstanceDao.update(it) }
        getSections(sessionId).forEach { deleteSection(it) }
        deleteSession(getSession(sessionId))
    }

    @Query("SELECT * FROM PracticeSession WHERE id=:id")
    suspend fun getSession(id: Int): PracticeSession

    @Query("SELECT * FROM PracticeSection WHERE practice_session_id=:sessionId")
    suspend fun getSections(sessionId: Int): List<PracticeSection>

    @Query("SELECT * FROM PracticeSection")
    suspend fun getAllSections(): List<PracticeSection>

    @Transaction
    @Query("SELECT * FROM PracticeSection WHERE timestamp>=:beginTimeStamp AND timestamp<=:endTimeStamp")
    suspend fun getSectionsWithCategories(beginTimeStamp: Long, endTimeStamp: Long): List<SectionWithCategory>

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE id=:id")
    suspend fun getSessionWithSectionsWithCategoriesWithGoals(id: Int)
        : SessionWithSectionsWithCategoriesWithGoalDescriptions

    @Transaction
    @Query("SELECT * FROM PracticeSession")
    suspend fun getSessionsWithSectionsWithCategories(): List<SessionWithSectionsWithCategories>

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE id=:id")
    suspend fun getSessionWithSections(id: Int): SessionWithSections

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE id=:id")
    suspend fun getSessionWithSectionsWithCategories(id: Int): SessionWithSectionsWithCategories

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE id IN (SELECT practice_session_id FROM PracticeSection WHERE timestamp > :from AND timestamp < :to )")
    suspend fun getSessionIds(
        from: Long,
        to: Long,
    ): List<SessionWithSections>

    @Transaction
    suspend fun updateSession(
        sessionId: Int,
        newRating: Int,
        newSections: List<SectionWithCategory>,
        newComment: String,
    ) {
        // the difference session will save the difference in the section duration
        // between the original session and the edited sections
        val session =
            getSessionWithSectionsWithCategoriesWithGoals(sessionId)

        session.sections.forEach { (section, _) ->
            section.duration = (newSections.find {
                it.section.id == section.id
            }?.section?.duration ?: 0) - (section.duration ?: 0)
        }

        val goalProgress = PracticeTime.goalDescriptionDao.computeGoalProgressForSession(
            session,
            checkArchived = true
        )

        // get all active goal instances at the time of the session
        PracticeTime.goalInstanceDao.apply {
            get(
                goalDescriptionIds = goalProgress.keys.toList(),
                from = session.sections.first().section.timestamp
                // add the progress
            ).onEach { instance ->
                goalProgress[instance.goalDescriptionId].also { progress ->
                    if (progress != null) {
                        // progress should not get lower than 0
                        instance.progress = maxOf(0 , instance.progress + progress)
                    }
                }
                update(instance)
            }
        }

        // update all sections
        newSections.forEach { (section, _) ->
            updateSection(section)
        }

        session.session.apply {
            comment = newComment
            rating = newRating
        }

        updateSession(session.session)
    }
}
