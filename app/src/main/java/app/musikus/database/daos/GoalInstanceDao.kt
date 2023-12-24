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

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalInstanceModel
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.TimestampModelDisplayAttributes
import app.musikus.utils.getStartOfDay
import app.musikus.utils.getStartOfMonth
import app.musikus.utils.getStartOfWeek
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class GoalInstance(
    @ColumnInfo(name="goal_description_id") val goalDescriptionId: UUID,
    @ColumnInfo(name="start_timestamp") val startTimestamp: ZonedDateTime,
    @ColumnInfo(name="end_timestamp") val endTimestamp: ZonedDateTime?,
    @ColumnInfo(name="target") val target: Int,
    @ColumnInfo(name="renewed") val renewed: Boolean,
) : TimestampModelDisplayAttributes() {

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is GoalInstance) &&
                (other.endTimestamp == endTimestamp) &&
                (other.target == target) &&
                (other.renewed == renewed)

    override fun hashCode() =
        ((super.hashCode() *
                HASH_FACTOR + endTimestamp.hashCode()) *
                HASH_FACTOR + target.hashCode()) *
                HASH_FACTOR + renewed.hashCode()

}

@Dao
abstract class GoalInstanceDao(
    database : MusikusDatabase,
) : TimestampDao<GoalInstanceModel, GoalInstanceUpdateAttributes, GoalInstance>(
    tableName = "goal_instance",
    database = database,
    displayAttributes = listOf(
        "goal_description_id",
        "start_timestamp",
        "end_timestamp",
        "target",
        "renewed",
    )
) {

    /**
     * @Insert
     */

    suspend fun insert(
        goalDescription: GoalDescription,
        timeframe: ZonedDateTime,
        target: Int,
    ) {
        insert(
            goalDescriptionId = goalDescription.id,
            periodUnit = goalDescription.periodUnit,
            timeframe = timeframe,
            target = target
        )
    }

    suspend fun insert(
        goalDescription: GoalDescriptionModel,
        timeframe: ZonedDateTime,
        target: Int,
    ) {
        insert(
            goalDescriptionId = goalDescription.id,
            periodUnit = goalDescription.periodUnit,
            timeframe = timeframe,
            target = target
        )
    }

    // create a new instance of this goal, storing the target and progress during a single period
    private suspend fun insert(
        goalDescriptionId: UUID,
        periodUnit: GoalPeriodUnit,
        timeframe: ZonedDateTime,
        target: Int,
    ) {

        // to find the correct starting point and period for the goal, we execute these steps:
        // 1. reset the time to the beginning of the current day
        // 2. set the time frame to the beginning of the day, week or month
        // 3. save the time in seconds as startTimestamp
        // 4. then set the day to the end of the period according to the periodInPeriodUnits
        // 5. calculate the period in seconds from the difference of the two timestamps
        val start = when(periodUnit) {
            GoalPeriodUnit.DAY -> getStartOfDay(dateTime = timeframe)
            GoalPeriodUnit.WEEK -> getStartOfWeek(dateTime = timeframe)
            GoalPeriodUnit.MONTH -> getStartOfMonth(dateTime = timeframe)
        }

        super.insert(
            GoalInstanceModel(
                goalDescriptionId = Nullable(goalDescriptionId),
                startTimestamp = start,
                target = target
            )
        )
    }

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: GoalInstanceModel,
        updateAttributes: GoalInstanceUpdateAttributes
    ): GoalInstanceModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        endTimestamp = updateAttributes.endTimestamp ?: old.endTimestamp
        target = updateAttributes.target ?: old.target
        renewed = updateAttributes.renewed ?: old.renewed
    }

    @Transaction
    open suspend fun renewGoalInstance(id: UUID) {
        update(id, GoalInstanceUpdateAttributes(renewed = true))
    }

    /**
     * @Queries
     */

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
            "WHERE end_timestamp IS NULL " +
            "AND datetime(start_timestamp) < datetime('now') " +
            "AND goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE archived=0 " +
                "AND deleted=0" +
            ")"
    )
    abstract suspend fun getActiveInstancesWithDescription(
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
            "WHERE end_timestamp IS NULL " +
            "AND datetime(start_timestamp) < datetime('now') " +
            "AND goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE archived=0 " +
                "AND deleted=0" +
            ")"
    )
    abstract fun getActiveInstancesWithDescriptionWithLibraryItems(
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "Select * FROM goal_instance " +
                "WHERE goal_instance.start_timestamp=(" +
                "SELECT MAX(datetime(start_timestamp)) FROM goal_instance WHERE " +
                "goal_description_id = :goalDescriptionId" +
                ") " +
                "AND EXISTS (" +
                "SELECT id FROM goal_description " +
                "WHERE id = :goalDescriptionId " +
                "AND archived=0 " +
                "AND deleted=0" +
                ")"
    )
    abstract suspend fun getLatest(
        goalDescriptionId: UUID
    ): GoalInstance?

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE deleted=0" +
                ")" +
                "AND end_timestamp IS NOT NULL " +
                "ORDER BY datetime(end_timestamp) DESC " +
                "LIMIT :n"
    )
    abstract fun getLastNCompletedWithDescriptionsWithLibraryItems(
        n: Int,
    ): Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                    "WHERE deleted=0" +
                ")" +
                "ORDER BY datetime(start_timestamp) DESC"
    )
    abstract fun getAllWithDescriptionWithLibraryItems()
        : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>


}