package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.musikus.database.ITimestampModelCreationAttributes
import app.musikus.database.ITimestampModelUpdateAttributes
import app.musikus.database.Nullable
import app.musikus.database.TimestampModel
import app.musikus.database.TimestampModelCreationAttributes
import app.musikus.database.TimestampModelUpdateAttributes
import java.util.UUID

private interface IGoalInstanceCreationAttributes : ITimestampModelCreationAttributes {
    val goalDescriptionId: Nullable<UUID>
    val startTimestamp: Long
    val periodInSeconds: Int
    val target: Int
}

private interface IGoalInstanceUpdateAttributes : ITimestampModelUpdateAttributes {
    val target: Int?
    val progress: Int?
    val renewed: Boolean?
}

data class GoalInstanceCreationAttributes(
    override val goalDescriptionId: Nullable<UUID>,
    override val startTimestamp: Long,
    override val periodInSeconds: Int,
    override val target: Int,
) : TimestampModelCreationAttributes(), IGoalInstanceCreationAttributes

data class GoalInstanceUpdateAttributes(
    override val target: Int? = null,
    override val progress: Int? = null,
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
    @ColumnInfo(name="start_timestamp") override val startTimestamp: Long,
    @ColumnInfo(name="period_in_seconds") override val periodInSeconds: Int,
    @ColumnInfo(name="target") override var target: Int,
    @ColumnInfo(name="progress") override var progress: Int = 0,
    @ColumnInfo(name="renewed") override var renewed: Boolean = false,
) : TimestampModel(), IGoalInstanceCreationAttributes, IGoalInstanceUpdateAttributes

