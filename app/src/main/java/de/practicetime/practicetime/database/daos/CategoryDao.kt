/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.daos

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.database.entities.CategoryWithGoalDescriptions

@Dao
abstract class CategoryDao : BaseDao<Category>(tableName = "category") {

    /**
     * @Delete / archive
     */

    @Transaction
    open suspend fun archive(categoryId: Long) : Boolean {
        // to archive a category, fetch it from the database along with associated goals
        getWithGoalDescriptions(categoryId)?.also {
            val (category, goalDescriptions) = it
            // check if there are non-archived goals associated with the selected category
            return if (goalDescriptions.any { d -> !d.archived }) {
                // in this case, we don't allow deletion and return false
                false
            } else {
                category.archived = true
                update(category)
                true
            }
        }
        Log.e("CATEGORY_DAO", "Tried to delete category with invalid id")
        return false
    }


    /**
    *  @Queries
    */

    @Query("SELECT * FROM category WHERE archived=0 OR NOT :activeOnly")
    abstract suspend fun get(activeOnly: Boolean = false): List<Category>

    @Transaction
    @Query("SELECT * FROM Category WHERE id=:id")
    abstract suspend fun getWithGoalDescriptions(id: Long): CategoryWithGoalDescriptions?
}
