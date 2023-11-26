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

package app.musikus.database.daos

import androidx.room.*
import app.musikus.R
import app.musikus.database.*
import app.musikus.database.entities.*
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.UiText
import app.musikus.utils.getDurationString
import kotlinx.coroutines.flow.Flow
import java.util.*

data class GoalDescription(
    @ColumnInfo(name="type") val type: GoalType,
    @ColumnInfo(name="repeat") val repeat: Boolean,
    @ColumnInfo(name="period_in_period_units") val periodInPeriodUnits: Int,
    @ColumnInfo(name="period_unit") val periodUnit: GoalPeriodUnit,
    @ColumnInfo(name="progress_type") val progressType: GoalProgressType,
    @ColumnInfo(name="paused") val paused: Boolean,
    @ColumnInfo(name="archived") val archived: Boolean,
//    @ColumnInfo(name="profile_id") val profileId: UUID?,
    @ColumnInfo(name="custom_order") val customOrder: Int?,
) : SoftDeleteModelDisplayAttributes() {

    fun title(item: LibraryItem? = null) =
        item?.let {
            UiText.DynamicString(item.name)
        } ?: UiText.StringResource(R.string.goal_name_non_specific)

    fun subtitle(instance: GoalInstance) = listOf(
        UiText.DynamicString(
            getDurationString(instance.target, TIME_FORMAT_HUMAN_PRETTY).toString()
        ),
        UiText.PluralResource(
            resId = when(periodUnit) {
                GoalPeriodUnit.DAY ->R.plurals.time_period_day
                GoalPeriodUnit.WEEK -> R.plurals.time_period_week
                GoalPeriodUnit.MONTH -> R.plurals.time_period_month
            },
            quantity = periodInPeriodUnits,
            periodInPeriodUnits // argument used in the format string
        )
    )
}

@Dao
abstract class GoalDescriptionDao(
    private val database : MusikusDatabase
) : SoftDeleteDao<
        GoalDescriptionModel,
        GoalDescriptionUpdateAttributes,
        GoalDescription
        >(
    tableName = "goal_description",
    database = database,
    displayAttributes = GoalDescription::class.java.fields.map { it.name }
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: GoalDescriptionModel,
        updateAttributes: GoalDescriptionUpdateAttributes
    ): GoalDescriptionModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        paused = updateAttributes.paused ?: old.paused
        archived = updateAttributes.archived ?: old.archived
        customOrder = updateAttributes.customOrder ?: old.customOrder
    }


    /**
     * @Insert
     */

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertGoalDescriptionLibraryItemCrossRef(
        crossRef: GoalDescriptionLibraryItemCrossRefModel
    ): Long

    @Transaction
    open suspend fun insert(
        goalDescription: GoalDescriptionModel,
        startingTimeframe: Calendar = Calendar.getInstance(),
        libraryItemIds: List<UUID>?,
        target: Int,
    ) {

        insert(goalDescription)

        // Create the first instance of the newly created goal description
        database.goalInstanceDao.insert(
            goalDescription,
            startingTimeframe,
            target
        )

        if(goalDescription.type == GoalType.NON_SPECIFIC) {
            return  // don't add cross ref for non specific goals
        }

        libraryItemIds?.forEach { libraryItemId ->
            insertGoalDescriptionLibraryItemCrossRef(
                GoalDescriptionLibraryItemCrossRefModel(
                    goalDescriptionId = goalDescription.id,
                    libraryItemId = libraryItemId
                )
            )
        }
    }

    /**
     * @Queries
     */

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_description_library_item_cross_ref " +
        "WHERE goal_description_id=:goalDescriptionId"
    )
    abstract suspend fun getGoalDescriptionLibraryItemCrossRefs(
        goalDescriptionId: UUID
    ) : List<GoalDescriptionLibraryItemCrossRefModel>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM goal_description " +
            "WHERE id=:goalDescriptionId AND deleted=0")
    abstract suspend fun getWithLibraryItems(goalDescriptionId: UUID)
        : GoalDescriptionWithLibraryItems?

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM goal_description WHERE deleted=0")
    abstract suspend fun getAllWithLibraryItems(): List<GoalDescriptionWithLibraryItems>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_description WHERE " +
        "(archived=0 OR archived=:checkArchived) " +
        "AND type=:type " +
        "AND deleted=0"
    )
    abstract suspend fun getGoalDescriptions(
        checkArchived : Boolean = false,
        type : GoalType
    ) : List<GoalDescription>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_description " +
        "WHERE goal_description.deleted=0 " + ""
    )
    abstract fun getAllWithInstancesAndLibraryItems()
    : Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
}

