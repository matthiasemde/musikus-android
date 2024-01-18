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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.R
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionLibraryItemCrossRefModel
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.UiText
import app.musikus.utils.getDurationString
import app.musikus.utils.inLocalTimezone
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class GoalDescription(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name="modified_at") override val modifiedAt: ZonedDateTime,
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

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
        (other is GoalDescription) &&
        (other.paused == paused) &&
        (other.archived == archived) &&
        (other.customOrder == customOrder)

    override fun hashCode() =
        ((super.hashCode() *
        HASH_FACTOR + paused.hashCode()) *
        HASH_FACTOR + archived.hashCode()) *
        HASH_FACTOR + customOrder.hashCode()

    override fun toString(): String {
        return super.toString() +
            "\ttype:\t\t\t\t\t$type\n" +
            "\trepeat:\t\t\t\t\t$repeat\n" +
            "\tperiodInPeriodUnits:\t$periodInPeriodUnits\n" +
            "\tperiodUnit:\t\t\t\t$periodUnit\n" +
            "\tprogressType:\t\t\t$progressType\n" +
            "\tpaused:\t\t\t\t\t$paused\n" +
            "\tarchived:\t\t\t\t$archived\n" +
            "\tcustomOrder:\t\t\t$customOrder\n"
    }

    fun title(item: LibraryItem? = null) =
        item?.let {
            UiText.DynamicString(item.name)
        } ?: UiText.StringResource(R.string.goal_name_non_specific)

    fun subtitle(instance: GoalInstance) = listOf(
        UiText.DynamicString(
            getDurationString(instance.target, DurationFormat.HUMAN_PRETTY).toString()
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

    fun endOfInstanceInLocalTimezone(
        instance: GoalInstance,
        timeProvider: TimeProvider
    ): ZonedDateTime =
        when(periodUnit) {
            GoalPeriodUnit.DAY -> instance.startTimestamp.plusDays(periodInPeriodUnits.toLong())
            GoalPeriodUnit.WEEK -> instance.startTimestamp.plusWeeks(periodInPeriodUnits.toLong())
            GoalPeriodUnit.MONTH -> instance.startTimestamp.plusMonths(periodInPeriodUnits.toLong())
        }
        // removes timezone information since the end timestamp is always in the local timezone
        .inLocalTimezone(timeProvider)
}

@Dao
abstract class GoalDescriptionDao(
    private val database : MusikusDatabase
) : SoftDeleteDao<
        GoalDescriptionModel,
        GoalDescriptionCreationAttributes,
        GoalDescriptionUpdateAttributes,
        GoalDescription
        >(
    tableName = "goal_description",
    database = database,
    displayAttributes = listOf(
        "type",
        "repeat",
        "period_in_period_units",
        "period_unit",
        "progress_type",
        "paused",
        "archived",
        "custom_order"
    )
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        oldModel: GoalDescriptionModel,
        updateAttributes: GoalDescriptionUpdateAttributes
    ): GoalDescriptionModel = super.applyUpdateAttributes(oldModel, updateAttributes).apply {
        paused = updateAttributes.paused ?: oldModel.paused
        archived = updateAttributes.archived ?: oldModel.archived
        customOrder = updateAttributes.customOrder ?: oldModel.customOrder
    }


    /**
     * @Insert
     */

    override fun createModel(creationAttributes: GoalDescriptionCreationAttributes): GoalDescriptionModel {
        return GoalDescriptionModel(
            type = creationAttributes.type,
            repeat = creationAttributes.repeat,
            periodInPeriodUnits = creationAttributes.periodInPeriodUnits,
            periodUnit = creationAttributes.periodUnit,
            progressType = creationAttributes.progressType,
        )
    }

    override suspend fun insert(creationAttributes: GoalDescriptionCreationAttributes): UUID {
        throw NotImplementedError(
            "Use overload insert(description, instanceCreationAttributes, libraryItemIds) instead"
        )
    }

    override suspend fun insert(creationAttributes: List<GoalDescriptionCreationAttributes>): List<UUID> {
        throw NotImplementedError(
            "Use overload insert(description, instanceCreationAttributes, libraryItemIds) instead"
        )
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertGoalDescriptionLibraryItemCrossRef(
        crossRef: GoalDescriptionLibraryItemCrossRefModel
    ): Long

    @Transaction
    open suspend fun insert(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItemIds: List<UUID>? = null,
    ): Pair<UUID, UUID> {

        if(descriptionCreationAttributes.type == GoalType.NON_SPECIFIC && !libraryItemIds.isNullOrEmpty()) {
            throw IllegalArgumentException("Non-specific goals cannot have library items")
        } else if(descriptionCreationAttributes.type != GoalType.NON_SPECIFIC && libraryItemIds.isNullOrEmpty()) {
            throw IllegalArgumentException("Specific goals must have at least one library item")
        }

        val descriptionId = super.insert(listOf(descriptionCreationAttributes)).single() // insert of description (single) would call the overridden insert method

        // Create the first instance of the newly created goal description
        val firstInstanceId = database.goalInstanceDao.insert(
            creationAttributes = instanceCreationAttributes.apply {
                this.descriptionId = descriptionId
            },
            firstInstance = true
        )

        libraryItemIds?.forEach { libraryItemId ->
            insertGoalDescriptionLibraryItemCrossRef(
                GoalDescriptionLibraryItemCrossRefModel(
                    goalDescriptionId = descriptionId,
                    libraryItemId = libraryItemId
                )
            )
        }

        return Pair(descriptionId, firstInstanceId)
    }

    /**
     * @Queries
     */

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_description " +
            "WHERE id = (SELECT goal_description_id FROM goal_instance WHERE id = :instanceId) " +
            "AND deleted=0 " +
            "AND archived=0"
    )
    abstract fun getDescriptionForInstance(
        instanceId: UUID
    ): GoalDescription?

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_description " +
        "WHERE goal_description.deleted=0 " + ""
    )
    abstract fun getAllWithInstancesAndLibraryItems()
    : Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
}

