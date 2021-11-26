package de.practicetime.practicetime

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

private var dao: PTDao? = null

const val SECONDS_PER_MONTH = 60 * 60 * 24 * 28 // don't judge me :(
const val SECONDS_PER_WEEK = 60 * 60 * 24 * 7
const val SECONDS_PER_DAY = 60 * 60 * 24
const val SECONDS_PER_HOUR = 60 * 60

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val activeGoalsWithCategories = ArrayList<GoalWithCategories>()
    private var goalAdapter : GoalAdapter? = null

    private var addGoalDialog: GoalDialog? = null
    private var deleteGoalDialog: AlertDialog? = null

    private var goalsToolbar: androidx.appcompat.widget.Toolbar? = null

    private val selectedGoals = ArrayList<Pair<Int, View>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        initGoalList()

        // create a new category dialog for adding new goals
        addGoalDialog = GoalDialog(
            requireActivity(),
            dao!!,
            lifecycleScope,
            ::addGoalHandler
        )

        view.findViewById<FloatingActionButton>(R.id.goalsFab).setOnClickListener {
            addGoalDialog?.show()
        }

        initArchiveGoalDialog()

        goalsToolbar = view.findViewById(R.id.goalsToolbar)

        goalsToolbar?.inflateMenu(R.menu.goals_toolbar_menu_base)

    }

    private fun initGoalList() {
        goalAdapter = GoalAdapter(
            activeGoalsWithCategories,
            context = requireActivity(),
            ::shortClickOnGoalHandler,
            ::longClickOnGoalHandler,
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

    // initialize the goal delete dialog
    private fun initArchiveGoalDialog() {
        deleteGoalDialog = requireActivity().let { it ->
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(R.string.deleteGoalDialogTitle)
                setPositiveButton(R.string.deleteGoalDialogConfirm) { dialog, _ ->
                    lifecycleScope.launch {
                        Log.d("selectedGoals", "$selectedGoals")
                        selectedGoals.forEach { (goalId, _) ->
                            dao?.archiveGoal(goalId)
                            Log.d("GoalID", "$goalId")
                            activeGoalsWithCategories.indexOfFirst { goalWithCategory ->
                                goalWithCategory.goal.id == goalId
                            }.also { index ->
                                activeGoalsWithCategories.removeAt(index)
                                goalAdapter?.notifyItemRemoved(index)
                            }
                        }
                        Toast.makeText(context, "Goal(s) deleted", Toast.LENGTH_SHORT).show()
                        resetToolbar()
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.deleteGoalDialogCancel) { dialog, _ ->
                    dialog.cancel()
                }
            }
            builder.create()
        }
    }

    private fun shortClickOnGoalHandler(goalId: Int, goalView: View) {
        if(selectedGoals.isNotEmpty()) {
            if(selectedGoals.remove(Pair(goalId, goalView))) {
                goalView.foregroundTintList = null
                if(selectedGoals.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnGoalHandler(goalId, goalView)
            }
        }
    }

    // the handler for dealing with long clicks on goals
    private fun longClickOnGoalHandler(goalId: Int, goalView: View): Boolean {
        // if there is not goal selected already, change the toolbar
        if(selectedGoals.isEmpty()) {
            goalsToolbar?.apply {
                // clear the base menu from the toolbar and inflate the new menu
                menu?.clear()
                inflateMenu(R.menu.goals_toolbar_menu_for_selection)

                // set the back button and its click listener
                setNavigationIcon(R.drawable.ic_nav_back)
                setNavigationOnClickListener {
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.topToolbarSelectionDelete -> deleteGoalDialog?.show()
                    }
                    return@setOnMenuItemClickListener true
                }
            }
        }

        // now add the newly selected goal to the list...
        selectedGoals.add(Pair(goalId, goalView))

        // and tint its foreground to mark it as selected
        goalView.foregroundTintList = ColorStateList.valueOf(
            requireActivity().resources.getColor(R.color.redTransparent, requireActivity().theme)
        )

        // we consumed the event so we return true
        return true
    }

    // reset the toolbar and associated data
    private fun resetToolbar() {
        goalsToolbar?.apply {
            menu?.clear()
            inflateMenu(R.menu.goals_toolbar_menu_base)
            navigationIcon = null
        }
        for ((_, view) in selectedGoals) {
            view.foregroundTintList = null
        }
        selectedGoals.clear()
    }

    // the handler for creating new goals
    private fun addGoalHandler(newGoalWithCategories: GoalWithCategories) {
        lifecycleScope.launch {
            dao?.insertGoalWithCategories(newGoalWithCategories)
            activeGoalsWithCategories.add(newGoalWithCategories)
            goalAdapter?.notifyItemInserted(activeGoalsWithCategories.size)
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
