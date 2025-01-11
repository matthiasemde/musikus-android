/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.goals.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.musikus.R
import app.musikus.core.data.Nullable
import app.musikus.core.data.entities.ISoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.ISoftDeleteModelUpdateAttributes
import app.musikus.core.data.entities.SoftDeleteModel
import app.musikus.core.data.entities.SoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.SoftDeleteModelUpdateAttributes
import app.musikus.core.presentation.utils.UiText

// shows, whether a goal will count all sections
// or only the one from specific libraryItems
enum class GoalType {
    NON_SPECIFIC, ITEM_SPECIFIC;

    companion object {
        val DEFAULT = NON_SPECIFIC
    }
    fun toUiText() = when (this) {
        NON_SPECIFIC -> UiText.StringResource(R.string.goals_goal_type_non_specific)
        ITEM_SPECIFIC -> UiText.StringResource(R.string.goals_goal_type_item_specific)
    }
}

// shows, whether a goal will track practice time
// or number of sessions
enum class GoalProgressType {
    TIME, SESSION_COUNT;

    companion object {
        val DEFAULT = TIME
    }
    fun toUiText() = when (this) {
        TIME -> UiText.StringResource(R.string.goals_goal_progress_type_time)
        SESSION_COUNT -> UiText.StringResource(R.string.goals_goal_progress_type_session_count)
    }
}

enum class GoalPeriodUnit {
    DAY, WEEK, MONTH;

    companion object {
        val DEFAULT = DAY
    }

    fun toUiText(quantity: Int = 1) = when (this) {
        DAY -> UiText.PluralResource(R.plurals.goals_goal_period_unit_day, quantity)
        WEEK -> UiText.PluralResource(R.plurals.goals_goal_period_unit_week, quantity)
        MONTH -> UiText.PluralResource(R.plurals.goals_goal_period_unit_month, quantity)
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
data class GoalDescriptionModel(
    @ColumnInfo(name = "type") override val type: GoalType,
    @ColumnInfo(name = "repeat") override val repeat: Boolean,
    @ColumnInfo(name = "period_in_period_units") override val periodInPeriodUnits: Int,
    @ColumnInfo(name = "period_unit") override val periodUnit: GoalPeriodUnit,
    @ColumnInfo(name = "progress_type")
    override val progressType: GoalProgressType = GoalProgressType.TIME,
    @ColumnInfo(name = "paused") override var paused: Boolean = false,
    @ColumnInfo(name = "archived") override var archived: Boolean = false,
    @ColumnInfo(name = "custom_order") override var customOrder: Nullable<Int>? = null,
) : SoftDeleteModel(), IGoalDescriptionCreationAttributes, IGoalDescriptionUpdateAttributes

class InvalidGoalDescriptionException(message: String) : Exception(message)
