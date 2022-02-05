package de.practicetime.practicetime.ui.goals

import android.app.Activity
import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithCategories
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.*

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
            .inflate(R.layout.listitem_goal, viewGroup, false)
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
        val count = description.periodInPeriodUnits
        val periodFormatted =
            when (description.periodUnit) {
                GoalPeriodUnit.DAY -> context.resources.getQuantityString(R.plurals.time_period_day, count, count)
                GoalPeriodUnit.WEEK -> context.resources.getQuantityString(R.plurals.time_period_week, count, count)
                GoalPeriodUnit.MONTH -> context.resources.getQuantityString(R.plurals.time_period_month, count, count)
            }

        viewHolder.goalDescriptionView.text = TextUtils.concat(
            getDurationString(instance.target, TIME_FORMAT_HUMAN_PRETTY),
            " ",
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

            viewHolder.goalProgressDoneIndicatorView.text = getDurationString(
                instance.progress,
                TIME_FORMAT_HUMAN_PRETTY,
                SCALE_FACTOR_FOR_SMALL_TEXT
            )

            viewHolder.goalProgressLeftIndicatorView.text = getDurationString(
                progressLeft,
                TIME_FORMAT_HUMAN_PRETTY,
                SCALE_FACTOR_FOR_SMALL_TEXT
            )

            viewHolder.goalProgressAchievedView.visibility = View.INVISIBLE
            viewHolder.goalProgressDoneIndicatorView.visibility = View.VISIBLE
            viewHolder.goalProgressLeftIndicatorView.visibility = View.VISIBLE
        } else {
            viewHolder.goalProgressAchievedView.visibility = View.VISIBLE
            viewHolder.goalProgressDoneIndicatorView.visibility = View.GONE
            viewHolder.goalProgressLeftIndicatorView.visibility = View.GONE
        }

        // remaining time
        val remainingTime = (instance.startTimestamp + instance.periodInSeconds) - getCurrTimestamp()

        viewHolder.remainingTimeView.text = context.getString(
            R.string.time_left,
            getDurationString(remainingTime.toInt(), TIME_FORMAT_PRETTY_APPROX)
        )
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
