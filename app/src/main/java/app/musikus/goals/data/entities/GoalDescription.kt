/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.musikus.core.data.entities.ISoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.ISoftDeleteModelUpdateAttributes
import app.musikus.core.data.entities.SoftDeleteModel
import app.musikus.core.data.entities.SoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.SoftDeleteModelUpdateAttributes
import app.musikus.core.data.Nullable

// shows, whether a goal will count all sections
// or only the one from specific libraryItems
enum class GoalType {
    NON_SPECIFIC, ITEM_SPECIFIC;

    companion object {
        val DEFAULT = NON_SPECIFIC
    }
    override fun toString() = when (this) {
        NON_SPECIFIC -> "All items"
        ITEM_SPECIFIC -> "Specific item"
    }
}

// shows, whether a goal will track practice time
// or number of sessions
enum class GoalProgressType {
    TIME, SESSION_COUNT;

    companion object {
        val DEFAULT = TIME
    }
    override fun toString() = when (this) {
        TIME -> "Time"
        SESSION_COUNT -> "Sessions"
    }
}

enum class GoalPeriodUnit {
    DAY, WEEK, MONTH;

    companion object {
        val DEFAULT = DAY
    }

    override fun toString() = when (this) {
        DAY -> "Day"
        WEEK -> "Week"
        MONTH -> "Month"
    }
}

private interface IGoalDescriptionCreationAttributes : ISoftDeleteModelCreationAttributes {
    val type: GoalType
    val repeat: Boolean
    val periodInPeriodUnits: Int
    val periodUnit: GoalPeriodUnit
    val progressType: GoalProgressType
}

private interface IGoalDescriptionUpdateAttributes : ISoftDeleteModelUpdateAttributes {
    val paused: Boolean?
    val archived: Boolean?
    val customOrder: Nullable<Int>?
}

data class GoalDescriptionCreationAttributes(
    override val type: GoalType,
    override val repeat: Boolean,
    override val periodInPeriodUnits: Int,
    override val periodUnit: GoalPeriodUnit,
    override val progressType: GoalProgressType = GoalProgressType.DEFAULT,
) : SoftDeleteModelCreationAttributes(), IGoalDescriptionCreationAttributes

data class GoalDescriptionUpdateAttributes(
    override val paused: Boolean? = null,
    override val archived: Boolean? = null,
    override val customOrder: Nullable<Int>? = null,
) : SoftDeleteModelUpdateAttributes(), IGoalDescriptionUpdateAttributes



@Entity(tableName = "goal_description")
data class GoalDescriptionModel (
    @ColumnInfo(name="type") override val type: GoalType,
    @ColumnInfo(name="repeat") override val repeat: Boolean,
    @ColumnInfo(name="period_in_period_units") override val periodInPeriodUnits: Int,
    @ColumnInfo(name="period_unit") override val periodUnit: GoalPeriodUnit,
    @ColumnInfo(name="progress_type")
    override val progressType: GoalProgressType = GoalProgressType.TIME,
    @ColumnInfo(name="paused", defaultValue = "false") override var paused: Boolean = false,
    @ColumnInfo(name="archived") override var archived: Boolean = false,
    @ColumnInfo(name="custom_order", defaultValue = "null") override var customOrder: Nullable<Int>? = null,
) : SoftDeleteModel(), IGoalDescriptionCreationAttributes, IGoalDescriptionUpdateAttributes

class InvalidGoalDescriptionException(message: String) : Exception(message)