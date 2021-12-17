package de.practicetime.practicetime

import androidx.room.*
import de.practicetime.practicetime.entities.*
import java.util.*

@Dao
interface PTDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: PracticeSection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalDescription(goalDescription: GoalDescription): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalInstance(goalInstance: GoalInstance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
    ): Int {
        val newGoalDescriptionId = insertGoalDescription(
            goalDescriptionWithCategories.description
        ).toInt()

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
    suspend fun deleteCategory(category: Category)

    @Delete
    suspend fun deleteGoalDescription(goalDescription: GoalDescription)

    @Delete
    suspend fun deleteGoalDescriptionCategoryCrossRef(crossRef: GoalDescriptionCategoryCrossRef)

    @Delete
    suspend fun deleteGoalInstance(goalInstance: GoalInstance)

    @Update
    suspend fun updateCategory(category: Category)

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
        getGoalInstancesFromNowOnWhereDescriptionId(goalDescriptionId).forEach {
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
    suspend fun deleteCategory(categoryId: Int) : Boolean {
        // to archive a category, fetch it from the database along with associated goals
        getCategoryWithGoalDescriptions(categoryId).also {
            val (category, goalDescriptions) = it
            // check if there are non-archived goals associated with the selected category
            return if (goalDescriptions.any { d -> !d.archived }) {
                // in this case, we don't allow deletion and return false
                false
            } else {
                category.archived = true
                updateCategory(category)
                true
            }
        }
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
    suspend fun getSectionsWithCateories(beginTimeStamp: Long, endTimeStamp: Long): List<SectionWithCategory>

    @Query("SELECT * FROM Category WHERE id=:id")
    suspend fun getCategory(id: Int): Category

    @Transaction
    @Query("SELECT * FROM Category WHERE id=:id")
    suspend fun getCategoryWithGoalDescriptions(id: Int)
        : CategoryWithGoalDescriptions

    @Transaction
    @Query("SELECT * FROM GoalDescription WHERE id=:id")
    suspend fun getGoalDescriptionWithCategories(id: Int)
        : GoalDescriptionWithCategories

    @Transaction
    @Query("SELECT * FROM GoalDescriptionCategoryCrossRef WHERE goalDescriptionId=:id")
    suspend fun getGoalDescriptionCategoryCrossRefsWhereDescriptionId(id: Int)
        : List<GoalDescriptionCategoryCrossRef>

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM Category WHERE NOT archived")
    suspend fun getActiveCategories(): List<Category>

    @Query("SELECT * FROM GoalDescription WHERE id=:id")
    suspend fun getGoalDescription(id: Int): GoalDescription

    @Query("SELECT * FROM GoalDescription WHERE id IN (:ids)")
    suspend fun getGoalDescriptions(ids: List<Int>): List<GoalDescription>

    @Query("SELECT * FROM GoalInstance WHERE id=:id")
    suspend fun getGoalInstance(id: Int): GoalInstance

    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND goalDescriptionId=(SELECT id FROM GoalDescription WHERE id=:descriptionId)")
    suspend fun getGoalInstancesWhereDescriptionId(
        descriptionId: Int,
        now : Long = Date().time / 1000L
    ): List<GoalInstance>

    @Query("SELECT * FROM GoalInstance WHERE goalDescriptionId=(SELECT id FROM GoalDescription WHERE id=:descriptionId)")
    suspend fun getGoalInstancesFromNowOnWhereDescriptionId(
        descriptionId: Int,
    ): List<GoalInstance>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE renewed=0 AND startTimestamp + periodInSeconds < :now")
    suspend fun getOutdatedGoalInstancesWithDescriptions(
        now : Long = Date().time / 1000L
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @Query("SELECT * FROM GoalDescription WHERE id=:goalId")
    suspend fun getGoalWithCategories(goalId: Int) : GoalDescriptionWithCategories

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

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND startTimestamp+periodInSeconds > :now AND goalDescriptionId IN (:descriptionIds)")
    suspend fun getGoalInstances(
        descriptionIds: List<Int>,
        now : Long = Date().time / 1000L,
    ) : List<GoalInstance>

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
    ) : Map<Int, Int>{
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
