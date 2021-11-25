package de.practicetime.practicetime

import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.entities.GoalPeriodUnit
import de.practicetime.practicetime.entities.GoalType
import de.practicetime.practicetime.entities.GoalWithCategories
import java.util.*
import kotlin.collections.ArrayList

class GoalAdapter(
    private val goalsWithCategories: ArrayList<GoalWithCategories>,
    private val context: Activity,
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    override fun getItemCount() = goalsWithCategories.size

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.view_goal_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val (goal, categories) = goalsWithCategories[position]

        viewHolder.progressBarView
        val now = Date().time / 1000L

        viewHolder.progressBarView.max = goal.target
        viewHolder.progressBarView.progress = goal.progress

        val targetHours = goal.target / 3600
        val targetMinutes = goal.target % 3600 / 60

        val targetFormatted = if(targetHours > 0) "$targetHours hours" else "" +
                if (targetHours > 0 && targetMinutes > 0) " and " else "" +
                        if(targetMinutes > 0) "$targetMinutes mins" else ""

        val periodFormatted = when(goal.periodUnit) {
            GoalPeriodUnit.DAY -> "${goal.period / SECONDS_PER_DAY} days"
            GoalPeriodUnit.WEEK -> "${goal.period / SECONDS_PER_WEEK} weeks"
            GoalPeriodUnit.MONTH -> "${goal.period / SECONDS_PER_MONTH} months"
        }

        // if the goal tracks the total time, leave it as the primary color for now
        if(goal.type == GoalType.TOTAL_TIME) {
            viewHolder.goalNameView.text = "Practice for $targetFormatted in $periodFormatted"
        } else {
            val categoryColors = context.resources.getIntArray(R.array.category_colors)
            viewHolder.progressBarView.progressTintList = ColorStateList.valueOf(
                categoryColors[categories.first().colorIndex]
            )
            viewHolder.goalNameView.text = "Practice ${categories.first().name} " +
                    "for $targetFormatted in $periodFormatted"
        }

        // set the percent text to the progress capped at 100 %
        viewHolder.progressPercentView.text = "${minOf(goal.progress * 100 / goal.target, 100)}%"

        val remainingTime = (goal.startTimestamp + goal.period) - now
        // if time left is larger than a day, show the number of days
        when {
            remainingTime > SECONDS_PER_DAY -> {
                viewHolder.remainingTimeView.text = "${remainingTime / SECONDS_PER_DAY + 1} days to go"
                // otherwise, if time left is larger than an hour, show the number of hours
            }
            remainingTime > SECONDS_PER_HOUR -> {
                viewHolder.remainingTimeView.text = "${remainingTime % SECONDS_PER_DAY / SECONDS_PER_HOUR + 1} hours to go"
                // otherwise, show the number of minutes
            }
            else -> {
                viewHolder.remainingTimeView.text = "${remainingTime % SECONDS_PER_HOUR / 60 + 1} minutes to go"
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val progressBarView: ProgressBar = view.findViewById(R.id.goalProgressBar)
        val progressPercentView: TextView = view.findViewById(R.id.goalProgressPercent)
        val goalNameView: TextView = view.findViewById(R.id.goalName)
        val remainingTimeView: TextView = view.findViewById(R.id.goalRemainingTime)
    }
}
