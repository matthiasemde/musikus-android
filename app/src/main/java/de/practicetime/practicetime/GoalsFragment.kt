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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.Goal
import de.practicetime.practicetime.entities.GoalWithCategories
import kotlinx.coroutines.launch

private var dao: PTDao? = null

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
        )

        requireActivity().findViewById<RecyclerView>(R.id.goalList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = goalAdapter
        }

        // load all active goals from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveGoalsWithCategories()?.let {
                Log.d("TAG","$it")
                activeGoalsWithCategories.addAll(it.reversed())
            }
            goalAdapter?.notifyItemRangeInserted(0, activeGoalsWithCategories.size)
        }
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private class GoalAdapter(
        private val goals: ArrayList<GoalWithCategories>,
        private val context: Activity,
    ) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

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
                        false
                    ),
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
                private val progressBar: ProgressBar = view.findViewById(R.id.goalProgressBar)

                fun bind(goalWithCategories: GoalWithCategories) {
                    val (goal, categories) = goalWithCategories

                    progressBar.max = goal.target
                    progressBar.progress = goal.progress

                    val categoryColors =  context.resources.getIntArray(R.array.category_colors)
                    progressBar.backgroundTintList = ColorStateList.valueOf(
                        categoryColors[categories.first().colorIndex]
                    )
                }
            }

            class AddNewGoalViewHolder(
                view: View,
            ) : GoalAdapter.ViewHolder(view) {

                private val button: ImageButton = view.findViewById(R.id.addNewCategory)

                init {
                    button.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                }

                fun bind() {
                    button.setOnClickListener {

    //                  addGoalDialog?.show()
                    }
                }
            }
        }
    }
}

