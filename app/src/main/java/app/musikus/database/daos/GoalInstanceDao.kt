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
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceModel
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.TimestampModelDisplayAttributes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class GoalInstance(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name="modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name="goal_description_id") val goalDescriptionId: UUID,
    @ColumnInfo(name="start_timestamp") val startTimestamp: ZonedDateTime,
    @ColumnInfo(name="end_timestamp") val endTimestamp: ZonedDateTime?,
    @ColumnInfo(name="target_seconds") val targetSeconds: Long,
) : TimestampModelDisplayAttributes() {

    val target : Duration
        get() = targetSeconds.seconds

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is GoalInstance) &&
                (other.endTimestamp == endTimestamp) &&
                (other.targetSeconds == targetSeconds)

    override fun hashCode() =
        (super.hashCode() *
                HASH_FACTOR + endTimestamp.hashCode()) *
                HASH_FACTOR + targetSeconds.hashCode()

    override fun toString(): String {
        return super.toString() +
            "\tgoalDescriptionId:\t\t$goalDescriptionId\n" +
            "\tstartTimestamp:\t\t\t$startTimestamp\n" +
            "\tendTimestamp:\t\t\t$endTimestamp\n" +
            "\ttargetSeconds:\t\t\t$targetSeconds\n"
    }
}

@Dao
abstract class GoalInstanceDao(
    private val database : MusikusDatabase,
) : TimestampDao<GoalInstanceModel, GoalInstanceUpdateAttributes, GoalInstance>(
    tableName = "goal_instance",
    database = database,
    displayAttributes = listOf(
        "goal_description_id",
        "start_timestamp",
        "end_timestamp",
        "target_seconds",
    ),
    dependencies = listOf("goal_description")
) {

    /**
     * @Insert
     */

    override suspend fun insert(row: GoalInstanceModel) {
        throw NotImplementedError("Use insert(goalDescriptionId, creationAttributes) instead")
    }

    override suspend fun insert(rows: List<GoalInstanceModel>) {
        throw NotImplementedError("Use insert(goalDescriptionId, creationAttributes) instead")
    }

    // create a new instance of this goal, storing the target for a single period
    suspend fun insert(
        descriptionId: UUID,
        creationAttributes: GoalInstanceCreationAttributes,
        firstInstanceOfGoal: Boolean = false
    ) {
        // throws exception if description does not exist
        val description = database.goalDescriptionDao.getAsFlow(descriptionId).first()

        if(description.archived) {
            throw IllegalArgumentException("Cannot insert instance for archived goal: $descriptionId")
        }

        if(!firstInstanceOfGoal) {
            val latestEndTimestamp = getLatest(listOf(descriptionId)).single().instance.endTimestamp ?:
                throw IllegalArgumentException("Cannot insert instance before finalizing previous instance")

            if (creationAttributes.startTimestamp < latestEndTimestamp) {
                throw IllegalArgumentException("Cannot insert instance with startTimestamp before latest endTimestamp: $latestEndTimestamp")
            }
        }

        super.insert(listOf(GoalInstanceModel(
            goalDescriptionId = descriptionId,
            startTimestamp = creationAttributes.startTimestamp,
            target = creationAttributes.target,
        )))
    }

    /**
     * @Update
     */

    // no need to override update(id, updateAttributes) since it calls update(rows) anyways
    override suspend fun update(rows: List<Pair<UUID, GoalInstanceUpdateAttributes>>) {
        // get all instances to be updated
        val instances = getAsFlow(rows.map { it.first }).first()

        // get all descriptions of the instances to be updated
        // throws exception if any description does not exist
        val descriptions = database.goalDescriptionDao.getAsFlow(
            instances.map { it.goalDescriptionId }
        ).first()

        if(descriptions.any { it.archived }) {
            throw IllegalArgumentException("Cannot update instance(s) for archived goal(s): ${descriptions.filter {it.archived}.map { it.id }}")
        }

        super.update(rows)
    }

    override fun applyUpdateAttributes(
        old: GoalInstanceModel,
        updateAttributes: GoalInstanceUpdateAttributes
    ): GoalInstanceModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        endTimestamp = updateAttributes.endTimestamp ?: old.endTimestamp
        target = updateAttributes.target ?: old.target
    }

    /**
     * @Delete
     */

    override suspend fun delete(id: UUID) {
        throw NotImplementedError(
            "Instances are deleted automatically when their description is deleted or a paused goal is renewed"
        )
    }

    override suspend fun delete(ids: List<UUID>) {
        throw NotImplementedError(
            "Instances are deleted automatically when their description is deleted or a paused goal is renewed"
        )
    }

    suspend fun deletePausedGoalInstance(id: UUID) {
        // try to get the description of the instance
        // if null is returned, it must be because the instance does not exist
        val description = database.goalDescriptionDao.getDescriptionForInstance(id) ?:
            throw IllegalArgumentException("Could not find goal_instance with the following id: $id")

        if (!description.paused) {
            throw IllegalArgumentException("Can only delete instances of paused goals")
        }

        super.delete(listOf(id)) // need to call listOf(id) because super.delete(id) would call overridden delete(listOf(id))
    }

    suspend fun deleteFutureGoalInstances(ids: List<UUID>) {
        val uniqueIds = ids.distinct()

        val instances = get(uniqueIds)

        if (instances.any { it.endTimestamp != null || it.startTimestamp < database.timeProvider.now() }) {
            throw IllegalArgumentException("Can only delete instances in the future")
        }

        super.delete(uniqueIds)
    }

    /**
     * @Queries
     */

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
            "WHERE end_timestamp IS NULL " +
            "AND datetime(start_timestamp) < datetime(:now) " +
            "AND goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE deleted=0 " +
            ")"
    )
    abstract suspend fun directGetUpdateCandidates(
        now: ZonedDateTime = database.timeProvider.now()
    ) : List<GoalInstanceWithDescription>

    suspend fun getUpdateCandidates() = directGetUpdateCandidates()

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
            "WHERE datetime(start_timestamp) <= datetime(:now) " +
            "AND (" +
                "end_timestamp IS NULL " +
                "OR datetime(:now) < datetime(end_timestamp)" +
            ") " +
            "AND goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE archived=0 " +
                "AND deleted=0" +
            ")"
    )
    abstract fun directGetCurrent(
        now: ZonedDateTime = database.timeProvider.now()
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    fun getCurrent() = directGetCurrent()


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "Select * FROM goal_instance " +
        "WHERE datetime(start_timestamp)=(" +
            "SELECT MAX(datetime(start_timestamp)) FROM goal_instance " +
            "WHERE goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE id IN (:descriptionIds) " +
                "AND deleted=0" +
            ")"+
        ") GROUP BY goal_description_id"
    )
    abstract suspend fun directGetLatest(
        descriptionIds: List<UUID>
    ): List<GoalInstanceWithDescription>

    suspend fun getLatest(
        descriptionIds: List<UUID>
    ) : List<GoalInstanceWithDescription> {
        val uniqueIds = descriptionIds.distinct()

        database.goalDescriptionDao.getAsFlow(uniqueIds).first()

        val instances = directGetLatest(uniqueIds)

        // make sure all descriptions exist
        if(instances.size != uniqueIds.size) {
            throw IllegalArgumentException(
                "Could not find goal_instance(s) for goal_description(s) with the following id(s): ${descriptionIds - instances.map { it.description.id }.toSet()}"
            )
        }

        return instances
    }

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
    abstract fun getLastNCompleted(
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