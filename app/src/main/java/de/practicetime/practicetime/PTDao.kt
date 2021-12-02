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
    suspend fun deleteGoalInstance(goalInstance: GoalInstance)

    @Query("DELETE FROM Category WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)

    @Update
    suspend fun updateCategory(category: Category)

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
    suspend fun archiveGoals(goalDescriptionIds: List<Int>) {
        getGoalDescriptions(goalDescriptionIds).forEach { g ->
            g.archived = true
            updateGoalDescription(g)
        }
    }

    @Transaction
    suspend fun deleteCategory(categoryId: Int) : Boolean {
        // to archive a category, fetch it from the database along with associated goals
        getCategoryWithGoalDescriptions(categoryId).also {
            val (category, goalDescriptions) = it
            // check if there are goals associated with the selected category
            return if (goalDescriptions.isNotEmpty()) {
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
    suspend fun deleteGoals(goalDescriptionIds: List<Int>) {
        // to delete a goal, first fetch all instances from the database
        for (goalDescriptionId in goalDescriptionIds) {
            getGoalInstancesWhereDescriptionId(goalDescriptionId).forEach {
                // in this case, archive the goal as well
                deleteGoalInstance(it)
            }
            deleteGoalDescription(getGoalDescription(goalDescriptionId))
        }
    }

    @Query("SELECT * FROM PracticeSession")
    suspend fun getAllSessions(): List<PracticeSession>

    @Query("SELECT * FROM PracticeSection")
    suspend fun getAllSections(): List<PracticeSection>

    @Query("SELECT * FROM Category WHERE id=:id")
    suspend fun getCategory(id: Int): Category

    @Transaction
    @Query("SELECT * FROM Category WHERE id=:id")
    suspend fun getCategoryWithGoalDescriptions(id: Int)
        : CategoryWithGoalDescriptions

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

//
//    @Transaction
//    @Query("SELECT * FROM Goal WHERE archived=0 AND startTimestamp + periodInSeconds < :now")
//    suspend fun getOutdatedGoalsWithCategories(now : Long = Date().time / 1000L) : List<GoalWithCategories>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE renewed=0 AND startTimestamp + periodInSeconds < :now")
    suspend fun getOutdatedGoalInstancesWithDescriptions(
        now : Long = Date().time / 1000L
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @Query("SELECT * FROM GoalDescription WHERE id=:goalId")
    suspend fun getGoalWithCategories(goalId: Int) : GoalDescriptionWithCategories

//    @Query("SELECT * FROM Goal WHERE archived=0 AND type = :type")
//    suspend fun getActiveTotalTimeGoals(type: GoalType = GoalType.TOTAL_TIME) : List<Goal>
//
//    @Transaction
//    @Query("SELECT * FROM Goal WHERE archived=0")
//    suspend fun getActiveGoalsWithCategories() : List<GoalWithCategories>
//
//    @Transaction
//    @Query("SELECT * FROM Goal WHERE id In (:ids) AND archived=0")
//    suspend fun getSelectedActiveGoalsWithCategories(ids : List<Int>) : List<GoalWithCategories>
//
    @Query("SELECT * FROM GoalDescription WHERE archived = 0 AND type = :type")
    suspend fun getActiveGoalDescriptionsOfType(
        type: GoalType
    ) : List<GoalDescription>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND startTimestamp+periodInSeconds > :now AND goalDescriptionId IN (SELECT id FROM GoalDescription WHERE archived=0)")
    suspend fun getActiveGoalInstancesWithDescriptionsWithCategories(
        now : Long = Date().time / 1000L
    ) : List<GoalInstanceWithDescriptionWithCategories>

    @Transaction
    @Query("SELECT * FROM GoalInstance WHERE startTimestamp < :now AND startTimestamp+periodInSeconds > :now AND goalDescriptionId IN (:descriptionIds)")
    suspend fun getActiveSelectedGoalInstancesWithDescriptionsWithCategories(
        descriptionIds : List<Int>,
        now : Long = Date().time / 1000L,
    ) : List<GoalInstanceWithDescriptionWithCategories>

    @Transaction
    @Query("SELECT * FROM PracticeSession")
    suspend fun getSessionsWithSections(): List<SessionWithSections>

    @Transaction
    @Query("SELECT * FROM PracticeSession")
    suspend fun getSessionsWithSectionsWithCategories(): List<SessionWithSectionsWithCategories>

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE id = (SELECT MAX(id) FROM PracticeSession)")
    suspend fun getLatestSessionWithSectionsWithCategoriesWithGoals()
        : SessionWithSectionsWithCategoriesWithGoalDescriptions
}
