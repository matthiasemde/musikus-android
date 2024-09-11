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
import androidx.room.ForeignKey
import androidx.room.Ignore
import app.musikus.core.data.Nullable
import app.musikus.core.data.UUIDConverter
import app.musikus.core.data.entities.ITimestampModelCreationAttributes
import app.musikus.core.data.entities.ITimestampModelUpdateAttributes
import app.musikus.core.data.entities.TimestampModel
import app.musikus.core.data.entities.TimestampModelCreationAttributes
import app.musikus.core.data.entities.TimestampModelUpdateAttributes
import app.musikus.core.domain.TimeProvider
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private interface IGoalInstanceCreationAttributes : ITimestampModelCreationAttributes {
    var descriptionId: UUID
    var previousInstanceId: UUID?
    var startTimestamp: ZonedDateTime
    var target: Duration
}

private interface IGoalInstanceUpdateAttributes : ITimestampModelUpdateAttributes {
    val endTimestamp: Nullable<ZonedDateTime>?
    val target: Duration?
}

data class GoalInstanceCreationAttributes(
    override var descriptionId: UUID = UUIDConverter.deadBeef,
    override var previousInstanceId: UUID? = null,
    override var startTimestamp: ZonedDateTime = TimeProvider.uninitializedDateTime,
    override var target: Duration,
) : TimestampModelCreationAttributes(), IGoalInstanceCreationAttributes

data class GoalInstanceUpdateAttributes(
    override val endTimestamp: Nullable<ZonedDateTime>? = null,
    override val target: Duration? = null,
) : TimestampModelUpdateAttributes(), IGoalInstanceUpdateAttributes

@Entity(
    tableName = "goal_instance",
    foreignKeys = [
        ForeignKey(
            entity = GoalDescriptionModel::class,
            parentColumns = ["id"],
            childColumns = ["goal_description_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GoalInstanceModel(
    @ColumnInfo(name = "goal_description_id", index = true) override var descriptionId: UUID,
    @ColumnInfo(name = "previous_goal_instance_id", index = true) override var previousInstanceId: UUID?,
    @ColumnInfo(name = "start_timestamp") override var startTimestamp: ZonedDateTime,
    @ColumnInfo(name = "end_timestamp") override var endTimestamp: Nullable<ZonedDateTime>? = null,
    @ColumnInfo(name = "target_seconds") var targetSeconds: Long,
) : TimestampModel(), IGoalInstanceCreationAttributes, IGoalInstanceUpdateAttributes {

    @get:Ignore
    override var target: Duration
        get() = targetSeconds.seconds
        set(value) { targetSeconds = value.inWholeSeconds }

    @Ignore
    constructor(
        descriptionId: UUID,
        previousInstanceId: UUID?,
        startTimestamp: ZonedDateTime,
        target: Duration,
    ) : this(
        descriptionId = descriptionId,
        previousInstanceId = previousInstanceId,
        startTimestamp = startTimestamp,
        targetSeconds = target.inWholeSeconds,
    )
}

class InvalidGoalInstanceException(message: String) : Exception(message)
