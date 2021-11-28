package de.practicetime.practicetime

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val activeGoalsWithCategories = ArrayList<GoalWithCategories>()
    private var goalAdapter : GoalAdapter? = null

    private var addGoalDialog: GoalDialog? = null
    private var editGoalDialog: GoalDialog? = null
    private var archiveGoalDialog: AlertDialog? = null

    private var goalsToolbar: androidx.appcompat.widget.Toolbar? = null

    private val selectedGoals = ArrayList<Pair<Int, View>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        updateGoals(dao!!, lifecycleScope)

        // wait for goals to be updated TODO find better solution
        Handler(Looper.getMainLooper()).postDelayed({  initGoalList() }, 500)

        // create a new goal dialog for adding new goals
        addGoalDialog = GoalDialog(
            requireActivity(),
            dao!!,
            lifecycleScope,
            ::addGoalHandler
        )

        view.findViewById<FloatingActionButton>(R.id.goalsFab).setOnClickListener {
            resetToolbar()
            addGoalDialog?.show()
        }

        // create the category dialog for editing categories
//        editGoalDialog = GoalDialog(requireActivity(), ::editGoalHandler)

        // create the dialog for archiving goals
        initArchiveGoalDialog()

        goalsToolbar = view.findViewById(R.id.goalsToolbar)
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
            itemAnimator?.apply {
                addDuration = 500L
                moveDuration = 500L
                removeDuration = 200L
            }
        }

        // load all active goals from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveGoalsWithCategories()?.let {
                activeGoalsWithCategories.addAll(it)
                goalAdapter?.notifyItemRangeInserted(0, it.size)
            }
        }
    }

    // initialize the goal archive dialog
    private fun initArchiveGoalDialog() {
        archiveGoalDialog = requireActivity().let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(R.string.archiveGoalDialogTitle)
                setPositiveButton(R.string.archiveDialogConfirm) { dialog, _ ->
                    lifecycleScope.launch {
                        selectedGoals.forEach { (goalId, _) ->
                            dao?.archiveGoal(goalId)
                            activeGoalsWithCategories.indexOfFirst { goalWithCategory ->
                                goalWithCategory.goal.id == goalId
                            }.also { index ->
                                activeGoalsWithCategories.removeAt(index)
                                goalAdapter?.notifyItemRemoved(index)
                            }
                        }
                        Toast.makeText(context, R.string.archiveGoalToast, Toast.LENGTH_SHORT).show()
                        resetToolbar()
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.archiveDialogCancel) { dialog, _ ->
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
        // if there is no goal selected already, change the toolbar
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
                        R.id.topToolbarSelectionArchive -> archiveGoalDialog?.show()
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
            val newGoalId = dao?.insertGoalWithCategories(newGoalWithCategories)
            if(newGoalId != null) {
                // we need to fetch the newly created goal to get the correct id
                dao?.getGoalWithCategories(newGoalId)?.let { activeGoalsWithCategories.add(it) }
                goalAdapter?.notifyItemInserted(activeGoalsWithCategories.size)
            }
        }
    }

    // the handler for editing goals
    private fun editGoalHandler(goalWithCategories: GoalWithCategories) {
        lifecycleScope.launch {
            dao?.insertGoalWithCategories(goalWithCategories) // TODO check if this creates duplicate entries in cross reference table
            activeGoalsWithCategories.indexOfFirst {
                    (g, _) -> g.id == goalWithCategories.goal.id
            }.also { i ->
                assert(i != -1)
                activeGoalsWithCategories[i] = goalWithCategories
                goalAdapter?.notifyItemChanged(i)
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
