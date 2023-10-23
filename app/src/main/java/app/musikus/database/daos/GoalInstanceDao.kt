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

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.PTDatabase
import app.musikus.database.TimestampDao
import app.musikus.database.entities.GoalInstance
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.utils.getCurrTimestamp
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class GoalInstanceDao(
    database : PTDatabase
) : TimestampDao<
    GoalInstance,
    GoalInstanceUpdateAttributes,
    GoalInstance
>(
    tableName = "goal_instance",
    database = database,
    displayAttributes = listOf(
        "goal_description_id",
        "start_timestamp",
        "period_in_seconds",
        "target",
        "progress",
        "renewed"
    )
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: GoalInstance,
        updateAttributes: GoalInstanceUpdateAttributes
    ): GoalInstance = super.applyUpdateAttributes(old, updateAttributes).apply {
        target = updateAttributes.target ?: old.target
        progress = updateAttributes.progress ?: old.progress
        renewed = updateAttributes.renewed ?: old.renewed
    }

    @Transaction
    open suspend fun renewGoalInstance(id: UUID) { // TODO: make prettier
        get(id)?.also { g ->
            g.renewed = true
//            update(g)
        } ?: Log.e("GOAL_INSTANCE_DAO", "Trying to renew goal instance with id: $id failed")
    }

    /**
     * @Queries
     */

    /**
     * Get all [GoalInstance] entities matching a specific pattern
     * @param goalDescriptionIds
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp] / 1000L
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id IN (:goalDescriptionIds)" +
                "AND (" +
                "start_timestamp>:from AND NOT :inclusiveFrom OR " +
                "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
                ")" +
                "AND (" +
                "start_timestamp<:to AND :inclusiveTo OR " +
                "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
                ")"
    )
    abstract suspend fun get(
        goalDescriptionIds: List<UUID>,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance>

    suspend fun get(
        goalDescriptionId: UUID,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance> {
        return get(
            goalDescriptionIds = listOf(goalDescriptionId),
            from = from,
            to = to,
            inclusiveFrom = inclusiveFrom,
            inclusiveTo = inclusiveTo
        )
    }

    /**
     * Get all [GoalInstanceWithDescription] entities matching a specific pattern
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp]
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE (" +
                "start_timestamp>:from AND NOT :inclusiveFrom OR " +
                "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
                ")" +
                "AND (" +
                "start_timestamp<:to AND :inclusiveTo OR " +
                "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
                ")"
    )
    abstract suspend fun getWithDescription(
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ) : List<GoalInstanceWithDescription>

    /**
     * Get all [GoalInstanceWithDescriptionWithLibraryItems] entities matching a specific pattern
     * @param goalDescriptionId
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp]
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE " +
                "goal_description_id=:goalDescriptionId " +
                "AND (" +
                "start_timestamp>:from AND NOT :inclusiveFrom OR " +
                "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
                ")" +
                "AND (" +
                "start_timestamp<:to AND :inclusiveTo OR " +
                "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
                ")"
    )
    abstract suspend fun getWithDescriptionWithLibraryItems(
        goalDescriptionId: UUID,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstanceWithDescriptionWithLibraryItems>

    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE " +
                "renewed=0 " +
                "AND start_timestamp + period_in_seconds < :to"
    )
    abstract suspend fun getOutdatedWithDescriptions(
        to : Long = getCurrTimestamp()
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE " +
                "start_timestamp < :now " +
                "AND start_timestamp+period_in_seconds > :now " +
                "AND goal_description_id IN (" +
                "SELECT id FROM goal_description WHERE " +
                "archived=0 OR :checkArchived" +
                ")"
    )
    abstract fun getWithDescriptionsWithLibraryItems(
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE " +
                "goal_description_id IN (:goalDescriptionIds) " +
                "AND start_timestamp < :now " +
                "AND start_timestamp+period_in_seconds > :now " +
                "AND goal_description_id IN (" +
                "SELECT id FROM goal_description WHERE " +
                "archived=0 OR :checkArchived" +
                ")"
    )
    abstract suspend fun getWithDescriptionsWithLibraryItems(
        goalDescriptionIds: List<UUID>,
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : List<GoalInstanceWithDescriptionWithLibraryItems>

    @Query(
        "Select * FROM goal_instance WHERE " +
                "goal_description_id=:goalDescriptionId AND " +
                "start_timestamp=(" +
                "SELECT MAX(start_timestamp) FROM goal_instance WHERE " +
                "goal_description_id = :goalDescriptionId" +
                ")"
    )
    abstract suspend fun getLatest(
        goalDescriptionId: UUID
    ): GoalInstance
}