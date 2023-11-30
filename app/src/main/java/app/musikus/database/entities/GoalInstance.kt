package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.musikus.database.Nullable
import java.time.ZonedDateTime
import java.util.UUID

private interface IGoalInstanceCreationAttributes : ITimestampModelCreationAttributes {
    val goalDescriptionId: Nullable<UUID>
    val startTimestamp: ZonedDateTime
    val target: Int
}

private interface IGoalInstanceUpdateAttributes : ITimestampModelUpdateAttributes {
    val endTimestamp: ZonedDateTime?
    val target: Int?
    val renewed: Boolean?
}

data class GoalInstanceCreationAttributes(
    override val goalDescriptionId: Nullable<UUID>,
    override val startTimestamp: ZonedDateTime,
    override val target: Int,
) : TimestampModelCreationAttributes(), IGoalInstanceCreationAttributes

data class GoalInstanceUpdateAttributes(
    override val endTimestamp: ZonedDateTime? = null,
    override val target: Int? = null,
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
    @ColumnInfo(name="goal_description_id", index = true) override val goalDescriptionId: Nullable<UUID>,
    @ColumnInfo(name="start_timestamp") override val startTimestamp: ZonedDateTime,
    @ColumnInfo(name="end_timestamp") override var endTimestamp: ZonedDateTime? = null,
    @ColumnInfo(name="target") override var target: Int,
    @ColumnInfo(name="renewed") override var renewed: Boolean = false,
) : TimestampModel(), IGoalInstanceCreationAttributes, IGoalInstanceUpdateAttributes

