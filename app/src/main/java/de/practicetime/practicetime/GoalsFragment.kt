package de.practicetime.practicetime

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null

const val SECONDS_PER_MONTH = 60 * 60 * 24 * 28 // don't judge me :(
const val SECONDS_PER_WEEK = 60 * 60 * 24 * 7
const val SECONDS_PER_DAY = 60 * 60 * 24
const val SECONDS_PER_HOUR = 60 * 60

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val activeGoalsWithCategories = ArrayList<GoalWithCategories>()

    private var goalAdapter : GoalAdapter? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        initGoalList()
    }

    private fun initGoalList() {
        goalAdapter = GoalAdapter(
            activeGoalsWithCategories,
            context = requireActivity(),
            dao!!,
            lifecycleScope,
            ::addGoalHandler,
        )

        requireActivity().findViewById<RecyclerView>(R.id.goalList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = goalAdapter
        }

        // load all active goals from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveGoalsWithCategories()?.let {
                activeGoalsWithCategories.addAll(it)
            }
            goalAdapter?.notifyItemRangeInserted(0, activeGoalsWithCategories.size)
        }
    }



    // the handler for creating new categories
    fun addGoalHandler(newGoalWithCategories: GoalWithCategories) {
        val dateFormat: SimpleDateFormat = SimpleDateFormat("HH:mm - dd.MM.yyyy")
        Log.d("addNewGoal", "$newGoalWithCategories\nstartTimestamp: ${dateFormat.format(Date((newGoalWithCategories.goal.startTimestamp) * 1000L))}")

        lifecycleScope.launch {
            dao?.insertGoalWithCategories(newGoalWithCategories)
            activeGoalsWithCategories.add(newGoalWithCategories)
            goalAdapter?.notifyItemInserted(activeGoalsWithCategories.size)
        }
    }

    private class GoalAdapter(
        private val goals: ArrayList<GoalWithCategories>,
        private val context: Activity,
        dao: PTDao,
        lifecycleScope: LifecycleCoroutineScope,
        addGoalHandler: (newGoal: GoalWithCategories) -> Unit,
    ) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

        private var addGoalDialog: GoalDialog? = null

        init {
            // create a new category dialog for adding new categories
            addGoalDialog = GoalDialog(
                context,
                dao,
                lifecycleScope,
                addGoalHandler
            )
        }

        companion object {
            private const val VIEW_TYPE_GOAL = 1
            private const val VIEW_TYPE_ADD_NEW = 2
        }

        // returns the view type (ADD_NEW button on first position)
        override fun getItemViewType(position: Int): Int {
            return if (position > 0)
                VIEW_TYPE_GOAL
            else
                VIEW_TYPE_ADD_NEW
        }

        // return the amount of categories + 1 for the add new button
        override fun getItemCount() = goals.size + 1

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(viewGroup.context)
            return when (viewType) {
                VIEW_TYPE_GOAL -> ViewHolder.GoalViewHolder(
                    inflater.inflate(
                        R.layout.view_goal_item,
                        viewGroup,
                        false
                    ),
                    context
                )
                else -> ViewHolder.AddNewGoalViewHolder(
                    inflater.inflate(
                        R.layout.view_add_new_category,
                        viewGroup,
                        false,
                    ),
                    addGoalDialog
                )
            }
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            when (viewHolder) {
                is ViewHolder.GoalViewHolder -> viewHolder.bind(
                    goals[position - 1]
                )
                is ViewHolder.AddNewGoalViewHolder -> viewHolder.bind()
            }
        }

        sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            class GoalViewHolder(
                view: View,
                val context: Activity,
            ) : ViewHolder(view) {
                private val progressBarView: ProgressBar = view.findViewById(R.id.goalProgressBar)
                private val progressPercentView: TextView = view.findViewById(R.id.goalProgressPercent)
                private val goalNameView: TextView = view.findViewById(R.id.goalName)
                private val remainingTimeView: TextView = view.findViewById(R.id.goalRemainingTime)

                fun bind(goalWithCategories: GoalWithCategories) {
                    val now = Date().time / 1000L
                    val (goal, categories) = goalWithCategories

                    progressBarView.max = goal.target
                    progressBarView.progress = goal.progress

                    val targetHours = goal.target / 3600
                    val targetMinutes = goal.target % 3600 / 60

                    val targetFormatted = (if(targetHours > 0) "$targetHours hours" else "") +
                            (if(targetHours > 0 && targetMinutes > 0) " and " else "") +
                            if(targetMinutes > 0) "$targetMinutes mins" else ""

                    val periodFormatted = when(goal.periodUnit) {
                        GoalPeriodUnit.DAY -> "${goal.period / SECONDS_PER_DAY} days"
                        GoalPeriodUnit.WEEK -> "${goal.period / SECONDS_PER_WEEK} weeks"
                        GoalPeriodUnit.MONTH -> "${goal.period / SECONDS_PER_MONTH} months"
                    }

                    // if the goal tracks the total time, leave it as the primary color for now
                    if(goal.type == GoalType.TOTAL_TIME) {
                        goalNameView.text = "Practice for $targetFormatted in $periodFormatted"
                    } else {
                        val categoryColors = context.resources.getIntArray(R.array.category_colors)
                        progressBarView.progressTintList = ColorStateList.valueOf(
                            categoryColors[categories.first().colorIndex]
                        )
                        goalNameView.text = "Practice ${categories.first().name} for $targetFormatted in $periodFormatted"
                    }

                    progressPercentView.text = "${minOf((goal.progress * 100 / goal.target), 100)}%"

                    val remainingTime = (goal.startTimestamp + goal.period) - now
                    // if time left is larger than a day, show the number of days
                    when {
                        remainingTime > SECONDS_PER_DAY -> {
                            remainingTimeView.text = "${remainingTime / SECONDS_PER_DAY + 1} days to go"
                            // otherwise, if time left is larger than an hour, show the number of hours
                        }
                        remainingTime > SECONDS_PER_HOUR -> {
                            remainingTimeView.text = "${remainingTime % SECONDS_PER_DAY / SECONDS_PER_HOUR + 1} hours to go"
                            // otherwise, show the number of minutes
                        }
                        else -> {
                            remainingTimeView.text = "${remainingTime % SECONDS_PER_HOUR / 60 + 1} minutes to go"
                        }
                    }
                }
            }

            class AddNewGoalViewHolder(
                view: View,
                private val addGoalDialog: GoalDialog?,
            ) : GoalAdapter.ViewHolder(view) {

                private val button: ImageButton = view.findViewById(R.id.addNewCategory)

                init {
                    button.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                }

                fun bind() {
                    button.setOnClickListener {
                        addGoalDialog?.show()
                    }
                }
            }
        }
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}
