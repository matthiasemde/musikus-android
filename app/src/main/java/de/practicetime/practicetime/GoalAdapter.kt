package de.practicetime.practicetime

import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import de.practicetime.practicetime.entities.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

const val SECONDS_PER_HOUR = 60 * 60
const val SECONDS_PER_DAY = 60 * 60 * 24

class GoalAdapter(
    private val goals: List<GoalInstanceWithDescriptionWithCategories>,
    private val selectedGoals: List<Int> = listOf(),
    private val context: Activity,
    private val shortClickHandler: (index: Int) -> Unit = {},
    private val longClickHandler: (index: Int) -> Boolean = { false },
    private val showEmptyHeader: Boolean = false
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    private val defaultCardElevation = 11F // default value

    companion object {
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_GOAL = 2
    }

    override fun getItemCount() =
        goals.size + if (showEmptyHeader) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (!showEmptyHeader) VIEW_TYPE_GOAL else {
            when (position) {
                0 -> VIEW_TYPE_HEADER
                else -> VIEW_TYPE_GOAL
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.view_goal_item, viewGroup, false)
        if (viewType == VIEW_TYPE_HEADER) {
            view.layoutParams.height = 0
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        var goalPosition = position

        if(showEmptyHeader) {
            if(position == 0) return
            else goalPosition -= 1
        }

        viewHolder.goalCardView.apply {
            if (selectedGoals.contains(viewHolder.layoutPosition)) {
                isSelected = true // set selected so that background changes
                // remove Card Elevation because in Light theme it would look ugly
                cardElevation = 0f
            } else {
                isSelected = false // set selected so that background changes
                cardElevation = defaultCardElevation
            }
        }

        val (instance, descriptionWithCategories) = goals[goalPosition]
        val (description, categories) = descriptionWithCategories

        // get the category color for later use in different UI elements
        var categoryColor: ColorStateList? = null
        if(description.type != GoalType.NON_SPECIFIC) {
            categoryColor = ColorStateList.valueOf(
                context.resources.getIntArray(R.array.category_colors)[categories.firstOrNull()?.colorIndex ?: 0]
            )
        }

        /** set Click listener */
        viewHolder.itemView.setOnClickListener {
            shortClickHandler(viewHolder.layoutPosition)
        }
        viewHolder.itemView.setOnLongClickListener {
            // tell the event handler we consumed the event
            return@setOnLongClickListener longClickHandler(viewHolder.layoutPosition)
        }

        /** Goal Title */
        if(description.type == GoalType.NON_SPECIFIC) {
            viewHolder.goalNameView.text = context.getString(R.string.goal_name_non_specific)
        } else {
            viewHolder.goalNameView.apply {
                text = categories.firstOrNull()?.name ?: "Delete me!"
            }
        }

        /** Goal Description */
        val targetHours = instance.target / 3600
        val targetMinutes = instance.target % 3600 / 60
        var targetHoursString = ""
        var targetMinutesString = ""
        if (targetHours > 0) targetHoursString = "${targetHours}h "
        if (targetMinutes > 0) targetMinutesString = "${targetMinutes}min "

        val periodFormatted =
            if (description.periodInPeriodUnits > 1) {  // plural
                when (description.periodUnit) {
                    GoalPeriodUnit.DAY -> context.getString(R.string.goal_description_days, description.periodInPeriodUnits)
                    GoalPeriodUnit.WEEK -> context.getString(R.string.goal_description_weeks, description.periodInPeriodUnits)
                    GoalPeriodUnit.MONTH -> context.getString(R.string.goal_description_months, description.periodInPeriodUnits)
                }
            } else {    // singular
                when (description.periodUnit) {
                    GoalPeriodUnit.DAY -> context.getString(R.string.goal_description_day)
                    GoalPeriodUnit.WEEK -> context.getString(R.string.goal_description_week)
                    GoalPeriodUnit.MONTH -> context.getString(R.string.goal_description_month)
                }
            }

        viewHolder.goalDescriptionView.text = context.getString(
            R.string.goal_description_complete,
            targetHoursString,
            targetMinutesString,
            periodFormatted
        )

        /** ProgressBar */
        viewHolder.progressBarView.max = instance.target
        viewHolder.progressBarView.progress = instance.progress

        // tint progressbar
        if(description.type != GoalType.NON_SPECIFIC) {
            viewHolder.sectionColorView.visibility = View.VISIBLE
            viewHolder.progressBarView.progressTintList = categoryColor
            viewHolder.sectionColorView.backgroundTintList = categoryColor
        } else {
            viewHolder.sectionColorView.visibility = View.GONE
            viewHolder.progressBarView.progressTintList = null
        }

        /** progress Indicator Text */
        val progressLeft = maxOf(0, instance.target - instance.progress)
        if(progressLeft > 0) {
            val progressDoneHours = instance.progress / 3600
            val progressDoneMinutes = instance.progress % 3600 / 60
            val progressLeftHours = progressLeft / 3600
            val progressLeftMinutes = ceil(progressLeft % 3600 / 60F).toInt()
            when {
                progressDoneHours > 0 ->
                    viewHolder.goalProgressDoneIndicatorView.text =
                        String.format("%02d:%02d", progressDoneHours, progressDoneMinutes)
                progressDoneMinutes > 0 ->
                    viewHolder.goalProgressDoneIndicatorView.text =
                        String.format("%d min", progressDoneMinutes)
                else -> viewHolder.goalProgressDoneIndicatorView.text = "< 1 min"
            }
            when {
                progressLeftHours > 0 ->
                    viewHolder.goalProgressLeftIndicatorView.text =
                        String.format("%02d:%02d", progressLeftHours, progressLeftMinutes)
                else ->
                    viewHolder.goalProgressLeftIndicatorView.text =
                        String.format("%d min", progressLeftMinutes)
            }

            viewHolder.goalProgressAchievedView.visibility = View.INVISIBLE
            viewHolder.goalProgressDoneIndicatorView.visibility = View.VISIBLE
            viewHolder.goalProgressLeftIndicatorView.visibility = View.VISIBLE
        } else {
            viewHolder.goalProgressAchievedView.visibility = View.VISIBLE
            viewHolder.goalProgressDoneIndicatorView.visibility = View.GONE
            viewHolder.goalProgressLeftIndicatorView.visibility = View.GONE
        }

        // remaining time
        val now = Date().time / 1000L
        val remainingTime = (instance.startTimestamp + instance.periodInSeconds) - now
        // if time left is larger than a day, show the number of days
        when {
            remainingTime > SECONDS_PER_DAY -> {
                viewHolder.remainingTimeView.text =
                    context.getString(
                        R.string.days_left,
                        remainingTime / SECONDS_PER_DAY + 1
                    )
                // otherwise, if time left is larger than an hour, show the number of hours
            }
            remainingTime > SECONDS_PER_HOUR -> {
                viewHolder.remainingTimeView.text =
                    context.getString(
                        R.string.hours_left,
                        remainingTime % SECONDS_PER_DAY / SECONDS_PER_HOUR + 1
                    )
            }
            else -> {   // otherwise, show the number of minutes
                viewHolder.remainingTimeView.text =
                    context.getString(
                        R.string.min_left,
                        remainingTime % SECONDS_PER_HOUR / 60 + 1
                    )
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val goalCardView: CardView = view.findViewById(R.id.cardView_goal_item)
        val progressBarView: ProgressBar = view.findViewById(R.id.goalProgressBar)
        val goalNameView: TextView = view.findViewById(R.id.goalName)
        val goalDescriptionView: TextView = view.findViewById(R.id.goalDescription)
        val remainingTimeView: Chip = view.findViewById(R.id.goalRemainingTime)
        val goalProgressDoneIndicatorView: TextView = view.findViewById(R.id.goalProgressDoneIndicator)
        val goalProgressLeftIndicatorView: TextView = view.findViewById(R.id.goalProgressLeftIndicator)
        val goalProgressAchievedView: TextView = view.findViewById(R.id.goalProgressAchieved)
        val sectionColorView: ImageView = view.findViewById(R.id.sectionColor)
    }
}
