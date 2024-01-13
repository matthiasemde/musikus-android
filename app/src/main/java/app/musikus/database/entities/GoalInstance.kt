package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private interface IGoalInstanceCreationAttributes : ITimestampModelCreationAttributes {
    var goalDescriptionId: UUID
    var startTimestamp: ZonedDateTime
    var target: Duration
}

private interface IGoalInstanceUpdateAttributes : ITimestampModelUpdateAttributes {
    val endTimestamp: Nullable<ZonedDateTime>?
    val target: Duration?
}

data class GoalInstanceCreationAttributes(
    override var goalDescriptionId: UUID = UUIDConverter.deadBeef,
    override var startTimestamp: ZonedDateTime,
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
    @ColumnInfo(name="goal_description_id", index = true) override var goalDescriptionId: UUID,
    @ColumnInfo(name="start_timestamp") override var startTimestamp: ZonedDateTime,
    @ColumnInfo(name="end_timestamp") override var endTimestamp: Nullable<ZonedDateTime>? = null,
    @ColumnInfo(name="target_seconds") var targetSeconds: Long,
) : TimestampModel(), IGoalInstanceCreationAttributes, IGoalInstanceUpdateAttributes {

    @get:Ignore
    override var target: Duration
        get() = targetSeconds.seconds
        set(value) { targetSeconds = value.inWholeSeconds }

    @Ignore
    constructor(
        goalDescriptionId: UUID,
        startTimestamp: ZonedDateTime,
        target: Duration,
    ) : this(
        goalDescriptionId = goalDescriptionId,
        startTimestamp = startTimestamp,
        targetSeconds = target.inWholeSeconds,
    )
}

