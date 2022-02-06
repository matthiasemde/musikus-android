package de.practicetime.practicetime.database

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
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

abstract class ModelWithTimestamps (
    @ColumnInfo(name="created_at", defaultValue = "0") var createdAt: Long = getCurrTimestamp(),
    @ColumnInfo(name="modified_at", defaultValue = "0") var modifiedAt: Long = getCurrTimestamp(),
) : BaseModel()

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalDescription(goalDescription: GoalDescription): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalInstance(goalInstance: GoalInstance): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalDescriptionCategoryCrossRef(crossRef: GoalDescriptionCategoryCrossRef): Long

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

    @Transaction
    suspend fun insertGoalDescriptionWithCategories(
        goalDescriptionWithCategories: GoalDescriptionWithCategories
    ): Long {
        val newGoalDescriptionId = insertGoalDescription(
            goalDescriptionWithCategories.description
        )

        // for every category linked with the goal...
        for (category in goalDescriptionWithCategories.categories) {
            // insert a row in the cross reference table
            insertGoalDescriptionCategoryCrossRef(
                GoalDescriptionCategoryCrossRef(
                    newGoalDescriptionId,
                    category.id,
                )
            )
        }
        return newGoalDescriptionId
    }

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Delete
    suspend fun deleteSection(section: PracticeSection)

    @Delete
    suspend fun deleteGoalDescription(goalDescription: GoalDescription)

    @Delete
    suspend fun deleteGoalDescriptionCategoryCrossRef(crossRef: GoalDescriptionCategoryCrossRef)

    @Delete
    suspend fun deleteGoalInstance(goalInstance: GoalInstance)

    @Update
    suspend fun updateSession(session: PracticeSession)

    @Update
    suspend fun updateSection(section: PracticeSection)

    @Update
    suspend fun updateGoalDescription(goalDescription: GoalDescription)

    @Update
    suspend fun updateGoalInstance(goalInstance: GoalInstance)

    @Transaction
    suspend fun updateGoalTarget(goalDescriptionId: Int, newTarget: Int) {
        // TODO check if correct because only future targets should be updated
        getGoalInstances(goalDescriptionId).forEach {
            it.target = newTarget
            updateGoalInstance(it)
        }
    }

    @Transaction
    suspend fun renewGoalInstance(id: Int) {
        getGoalInstance(id).also { g ->
            g.renewed = true
            updateGoalInstance(g)
        }
    }

    @Transaction
    suspend fun archiveGoal(goalDescription: GoalDescription) {
            goalDescription.archived = true
            updateGoalDescription(goalDescription)
    }

    suspend fun archiveGoals(goalDescriptionIds: List<Int>) {
        getGoalDescriptions(goalDescriptionIds).forEach { archiveGoal(it) }
    }


    @Transaction
    suspend fun deleteGoal(goalDescriptionId: Int) {
        // to delete a goal, first fetch all instances from the database and delete them
        getGoalInstancesWhereDescriptionId(goalDescriptionId).forEach {
            deleteGoalInstance(it)
        }
        // we also need to remove all entries in the cross reference table
        getGoalDescriptionCategoryCrossRefsWhereDescriptionId(goalDescriptionId).forEach {
            deleteGoalDescriptionCategoryCrossRef(it)
        }
        deleteGoalDescription(getGoalDescription(goalDescriptionId))
    }

    suspend fun deleteGoals(goalDescriptionIds: List<Int>) {
        goalDescriptionIds.forEach { deleteGoal(it) }
    }

    @Transaction
    suspend fun deleteSession(sessionId: Int, updatedGoalInstances: List<GoalInstance>) {
        updatedGoalInstances.forEach { updateGoalInstance(it) }
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
    @Query("SELECT * FROM GoalDescription WHERE id=:id")
    suspend fun getGoalDescriptionWithCategories(id: Int)
        : GoalDescriptionWithCategories

    @Transaction
    @Query("SELECT * FROM GoalDescriptionCategoryCrossRef WHERE goalDescriptionId=:id")
    suspend fun getGoalDescriptionCategoryCrossRefsWhereDescriptionId(id: Int)
        : List<GoalDescriptionCategoryCrossRef>

    @Query("SELECT * FROM GoalDescription WHERE id=:id")
    suspend fun getGoalDescription(id: Int): GoalDescription

    @Transaction
    @Query("SELECT * FROM GoalDescription")
    suspend fun getGoalDescriptionsWithCategories(): List<GoalDescriptionWithCategories>

    @Query("SELECT * FROM GoalDescription WHERE id IN (:ids)")
    suspend fun getGoalDescriptions(ids: List<Int>): List<GoalDescription>

    @Query("SELECT * FROM GoalInstance WHERE id=:id")
    suspend fun getGoalInstance(id: Int): GoalInstance

    @Query("SELECT * FROM GoalInstance " +
            "WHERE startTimestamp < :now AND" +
            " goalDescriptionId=(SELECT id FROM GoalDescription WHERE id=:descriptionId)"
    )
    suspend fun getGoalInstancesWhereDescriptionId(
        descriptionId: Int,
        now : Long = Date().time / 1000L
    ): List<GoalInstance>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE renewed=0 AND startTimestamp + periodInSeconds < :now")
    suspend fun getOutdatedGoalInstancesWithDescriptions(
        now : Long = Date().time / 1000L
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @Query("SELECT * FROM GoalDescription WHERE id=:goalId")
    suspend fun getGoalWithCategories(goalId: Long) : GoalDescriptionWithCategories

    @Query("SELECT * FROM GoalDescription WHERE (archived=0 OR archived=:checkArchived) AND type=:type")
    suspend fun getGoalDescriptions(
        checkArchived : Boolean = false,
        type : GoalType
    ) : List<GoalDescription>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND startTimestamp+periodInSeconds > :now AND goalDescriptionId IN (SELECT id FROM GoalDescription WHERE archived=0 OR archived=:checkArchived)")
    suspend fun getGoalInstancesWithDescriptionsWithCategories(
        checkArchived : Boolean = false,
        now : Long = Date().time / 1000L,
    ) : List<GoalInstanceWithDescriptionWithCategories>

    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND startTimestamp+periodInSeconds > :now AND goalDescriptionId IN (:descriptionIds)")
    suspend fun getGoalInstances(
        descriptionIds: List<Int>,
        now : Long = Date().time / 1000L,
    ) : List<GoalInstance>

    // all active goals and all in future
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp+periodInSeconds > :from AND goalDescriptionId=:descriptionId")
    suspend fun getGoalInstances(
        descriptionId: Int,
        from : Long = Date().time / 1000L,
    ) : List<GoalInstance>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp+periodInSeconds < :expiredBefore")
    suspend fun getGoalInstancesWithDescription(
        expiredBefore : Long = Date().time / 1000L,
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND startTimestamp+periodInSeconds > :now AND goalDescriptionId IN (:descriptionIds) AND goalDescriptionId IN (SELECT id FROM GoalDescription WHERE archived=0 OR archived=:checkArchived)")
    suspend fun getGoalInstancesWithDescriptionsWithCategories(
        descriptionIds : List<Int>,
        checkArchived : Boolean = false,
        now : Long = Date().time / 1000L,
    ) : List<GoalInstanceWithDescriptionWithCategories>

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
    suspend fun computeGoalProgressForSession(
        session: SessionWithSectionsWithCategoriesWithGoalDescriptions,
        checkArchived: Boolean = false,
    ) : Map<Int, Int> {
        var totalSessionDuration = 0

        // goalProgress maps the goalDescription-id to its progress
        val goalProgress = mutableMapOf<Int, Int>()

        // go through all the sections in the session...
        session.sections.forEach { (section, categoryWithGoalDescriptions) ->
            // ... using the respective categories, find the goals,
            // to which the sections are contributing to...
            val (_, goalDescriptions) = categoryWithGoalDescriptions

            // ... and loop through those goals, summing up the duration
            goalDescriptions.filter {d -> checkArchived || !d.archived}.forEach { description ->
                when (description.progressType) {
                    GoalProgressType.TIME -> goalProgress[description.id] =
                        goalProgress[description.id] ?: 0 + (section.duration ?: 0)
                    GoalProgressType.SESSION_COUNT -> goalProgress[description.id] = 1
                }
            }

            // simultaneously sum up the total session duration
            totalSessionDuration += section.duration ?: 0
        }

        // query all goal descriptions which have type NON-SPECIFIC
        getGoalDescriptions(
            checkArchived,
            GoalType.NON_SPECIFIC,
        ).forEach { totalTimeGoal ->
            goalProgress[totalTimeGoal.id] = when (totalTimeGoal.progressType) {
                GoalProgressType.TIME -> totalSessionDuration
                GoalProgressType.SESSION_COUNT -> 1
            }
        }
        return goalProgress
    }

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

        val goalProgress = computeGoalProgressForSession(
            session,
            checkArchived = true
        )

        // get all active goal instances at the time of the session
        getGoalInstances(
            descriptionIds = goalProgress.keys.toList(),
            now = session.sections.first().section.timestamp
            // add the progress
        ).onEach { instance ->
            goalProgress[instance.goalDescriptionId].also { progress ->
                if (progress != null) {
                    // progress should not get lower than 0
                    instance.progress = maxOf(0 , instance.progress + progress)
                }
            }
            updateGoalInstance(instance)
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
