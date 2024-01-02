package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private interface IGoalInstanceCreationAttributes : ITimestampModelCreationAttributes {
    val startTimestamp: ZonedDateTime
    val target: Duration
}

private interface IGoalInstanceUpdateAttributes : ITimestampModelUpdateAttributes {
    val endTimestamp: ZonedDateTime?
    val target: Duration?
    val renewed: Boolean?
}

data class GoalInstanceCreationAttributes(
    override val startTimestamp: ZonedDateTime,
    override val target: Duration,
) : TimestampModelCreationAttributes(), IGoalInstanceCreationAttributes

data class GoalInstanceUpdateAttributes(
    override val endTimestamp: ZonedDateTime? = null,
    override val target: Duration? = null,
    override val renewed: Boolean? = null,
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
    @ColumnInfo(name="goal_description_id", index = true) val goalDescriptionId: UUID,
    @ColumnInfo(name="start_timestamp") override val startTimestamp: ZonedDateTime,
    @ColumnInfo(name="end_timestamp") override var endTimestamp: ZonedDateTime? = null,
    @ColumnInfo(name="target_seconds") var targetSeconds: Long,
    @ColumnInfo(name="renewed") override var renewed: Boolean = false,
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

