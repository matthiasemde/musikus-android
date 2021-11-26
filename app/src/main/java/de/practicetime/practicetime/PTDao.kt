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
    suspend fun insertGoal(goal: Goal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalCategoryCrossRef(crossRef: GoalCategoryCrossRef): Long

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
    suspend fun insertGoalWithCategories(goalWithCategories: GoalWithCategories) {
        val newGoalId = insertGoal(goalWithCategories.goal).toInt()

        // for every category linked with the goal...
        for (category in goalWithCategories.categories) {
            // insert a row in the cross reference table
            insertGoalCategoryCrossRef(
                GoalCategoryCrossRef(
                    newGoalId,
                    category.id,
                )
            )
        }
    }

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Delete
    suspend fun deleteSection(section: PracticeSection)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM Category WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)

    @Update
    suspend fun updateCategory(category: Category)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Transaction
    suspend fun archiveGoal(goalId: Int) {
        getGoal(goalId).also { g ->
            g.archived = true
            updateGoal(g)
        }
    }

    @Transaction
    suspend fun archiveGoals(goalIds: List<Int>) {
        goalIds.forEach { id ->
            getGoal(id).also { g ->
                g.archived = true
                updateGoal(g)
            }
        }
    }

    @Transaction
    suspend fun archiveCategory(categoryId: Int) {
        // to archive a category, fetch it from the database along with associated goals
        getCategoryWithGoalsWithCategories(categoryId).also {
            val (category, goalsWithCategories) = it
            // check if the goals are only associated with the selected category
            // or other categories which are already archived
            for ((goal, categories) in goalsWithCategories) {
                if ((categories.filter { c -> !c.archived }).size == 1) {
                    // in this case, archive the goal as well
                    goal.archived = true
                    updateGoal(goal)
                }
            }
            category.archived = true
            updateCategory(category)
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
    suspend fun getCategoryWithGoalsWithCategories(id: Int)
        : CategoryWithGoalsWithCategories

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM Category WHERE NOT archived")
    suspend fun getActiveCategories(): List<Category>

    @Query("SELECT * FROM Goal WHERE id=:goalId")
    suspend fun getGoal(goalId: Int): Goal

    @Query("SELECT MAX(groupId) FROM Goal")
    suspend fun getMaxGoalGroupId() : Long

    @Query("SELECT * FROM Goal WHERE type = :type  AND startTimestamp < :now AND startTimestamp + period > :now")
    suspend fun getActiveTotalTimeGoals(
        type: GoalType = GoalType.TOTAL_TIME,
        now : Long = Date().time / 1000L
    ) : List<Goal>

//    @Transaction
//    @Query("SELECT * FROM Goal WHERE startTimestamp < :now AND startTimestamp + period > :now")
//    suspend fun getActiveGoalsWithCategories(now : Long = Date().time / 1000L) : List<GoalWithCategories>

    @Transaction
    @Query("SELECT * FROM Goal WHERE archived = 0")
    suspend fun getActiveGoalsWithCategories() : List<GoalWithCategories>

    @Transaction
    @Query("SELECT * FROM Goal WHERE id In (:ids) AND startTimestamp < :now AND startTimestamp + period > :now")
    suspend fun getSelectedActiveGoalsWithCategories(
        ids : List<Int>,
        now : Long = Date().time / 1000L
    ) : List<GoalWithCategories>

    @Transaction
    @Query("SELECT * FROM PracticeSession")
    suspend fun getSessionsWithSections(): List<SessionWithSections>

    @Transaction
    @Query("SELECT * FROM PracticeSession")
    suspend fun getSessionsWithSectionsWithCategories(): List<SessionWithSectionsWithCategories>

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE id = (SELECT MAX(id) FROM PracticeSession)")
    suspend fun getLatestSessionWithSectionsWithCategoriesWithGoals()
        : SessionWithSectionsWithCategoriesWithGoals
}
