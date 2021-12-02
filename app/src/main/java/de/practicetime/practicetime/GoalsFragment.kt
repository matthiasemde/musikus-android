package de.practicetime.practicetime

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null
private var savedCardElevation = 0f

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val activeGoalInstancesWithDescriptionWithCategories =
        ArrayList<GoalInstanceWithDescriptionWithCategories>()
    private var goalAdapter : GoalAdapter? = null

    private var addGoalDialog: GoalDialog? = null
    private var editGoalDialog: GoalDialog? = null
    private var deleteGoalDialog: AlertDialog? = null

    private var goalsToolbar: androidx.appcompat.widget.Toolbar? = null
    private var goalsCollapsingToolbarLayout: CollapsingToolbarLayout? = null

    private val selectedGoals = ArrayList<Pair<Int, View>>()

    // catch the back press for the case where the selection should be reverted
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(selectedGoals.isNotEmpty()){
                    resetToolbar()
                }else{
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        updateGoals(dao!!, lifecycleScope)

        // wait for goals to be updated TODO find better solution
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                initGoalList()
            } catch (e: Exception) {
                return@postDelayed
            }
        }, 500)

        lifecycleScope.launch {
            // create a new goal dialog for adding new goals
            addGoalDialog = GoalDialog(
                requireActivity(),
                dao?.getActiveCategories()?: listOf(),
                ::addGoalHandler
            )
        }

        view.findViewById<FloatingActionButton>(R.id.goalsFab).setOnClickListener {
            resetToolbar()
            addGoalDialog?.show()
        }

        // create the category dialog for editing categories
        editGoalDialog = GoalDialog(requireActivity(), listOf(), ::editGoalHandler)

        // create the dialog for deleting goals
        initDeleteGoalDialog()

        goalsToolbar = view.findViewById(R.id.goalsToolbar)
        goalsCollapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar_layout)
    }

    private fun initGoalList() {
        goalAdapter = GoalAdapter(
            activeGoalInstancesWithDescriptionWithCategories,
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
            dao?.getActiveGoalInstancesWithDescriptionsWithCategories()?.let {
                activeGoalInstancesWithDescriptionWithCategories.addAll(it)
                goalAdapter?.notifyItemRangeInserted(0, it.size)
            }
        }
    }

    // initialize the goal delete dialog
    private fun initDeleteGoalDialog() {

        deleteGoalDialog = requireActivity().let { context ->
            val view = context.layoutInflater.inflate(R.layout.dialog_view_delete_goal, null)
            val builder = AlertDialog.Builder(context)

            val checkBox = view.findViewById<CheckBox>(R.id.deleteGoalDialogCheckBox)

            builder.apply {
                setView(view)
                setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                    if(checkBox.isChecked) deleteGoalHandler(selectedGoals.map { p -> p.first })
                    else archiveGoalHandler(selectedGoals.map { p -> p.first })
                    checkBox.isChecked = false
                    dialog.dismiss()
                }
                setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                    checkBox.isChecked = false
                    dialog.cancel()
                }
            }
            builder.create()
        }
    }

    // the handler for dealing with short clicks on goals
    private fun shortClickOnGoalHandler(
        goalInstanceWithDescription: GoalInstanceWithDescription,
        goalView: View
    ) {
        if(selectedGoals.isNotEmpty()) {
            if(selectedGoals.remove(Pair(goalInstanceWithDescription.description.id, goalView))) {
                setCardSelected(false, goalView)

                if(selectedGoals.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnGoalHandler(goalInstanceWithDescription.description.id, goalView)
            }
        } else {
            editGoalDialog?.show(goalInstanceWithDescription)
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
                        R.id.topToolbarSelectionDelete -> {
                            deleteGoalDialog?.show()
                            deleteGoalDialog?.also { dialog ->
                                dialog.findViewById<TextView>(R.id.deleteGoalDialogMessage).setText(
                                    if(selectedGoals.size > 1) R.string.deleteGoalsDialogMessage
                                    else R.string.deleteGoalDialogMessage
                                )
                            }
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            // change the background color of the App Bar
            val typedValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
            val color = typedValue.data
            goalsCollapsingToolbarLayout?.setBackgroundColor(color)
        }

        // now add the newly selected goal to the list...
        selectedGoals.add(Pair(goalId, goalView))

        setCardSelected(true, goalView)

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
        goalsCollapsingToolbarLayout?.apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        for ((_, view) in selectedGoals) {
            setCardSelected(false, view)
        }
        selectedGoals.clear()
    }

    private fun setCardSelected(selected: Boolean, view: View) {
        (view as CardView).apply {
            isSelected = selected // set selected so that background changes
            if (!selected) {
                // restore card Elevation
                cardElevation = savedCardElevation
                // re-enable ripple effect
                foreground = with(TypedValue()) {
                    context.theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
                    ContextCompat.getDrawable(context, resourceId)
                }
            } else {
                // remove Card Elevation because in Light theme it would look ugly
                savedCardElevation = cardElevation
                cardElevation = 0f
                // remove ripple effect
                foreground = null
            }
        }
    }

    // the handler for creating new goals
    private fun addGoalHandler(
        newGoalDescriptionWithCategories: GoalDescriptionWithCategories,
        firstTarget: Int,
    ) {
        lifecycleScope.launch {
            val newGoalDescriptionId = dao?.insertGoalDescriptionWithCategories(
                newGoalDescriptionWithCategories
            )
            if(newGoalDescriptionId != null) {
                // we need to fetch the newly created goal to get the correct id
                dao?.getGoalWithCategories(newGoalDescriptionId)?.let { d ->
                    val newGoalInstanceId = dao?.insertGoalInstance(
                        d.description.createInstance(Calendar.getInstance(), firstTarget)
                    )?.toInt()
                    if(newGoalInstanceId != null) {
                        dao?.getGoalInstance(newGoalInstanceId)?.let { i ->
                            activeGoalInstancesWithDescriptionWithCategories.add(
                                GoalInstanceWithDescriptionWithCategories(
                                    instance = i,
                                    description = d,
                                )
                            )
                            goalAdapter?.notifyItemInserted(
                                activeGoalInstancesWithDescriptionWithCategories.size
                            )
                        }
                    }
                }
            }
        }
    }

    // the handler for editing goals
    private fun editGoalHandler(
        newGoalDescriptionWithCategories: GoalDescriptionWithCategories,
        newTarget: Int,
    ) {
        lifecycleScope.launch {
            dao?.updateGoalTarget(newGoalDescriptionWithCategories.description.id, newTarget)
            activeGoalInstancesWithDescriptionWithCategories.indexOfFirst {
                it.description.description.id == newGoalDescriptionWithCategories.description.id
            }.also { i ->
                activeGoalInstancesWithDescriptionWithCategories[i].instance.target = newTarget
                goalAdapter?.notifyItemChanged(i)
            }
        }
    }

    // the handler for archiving goals
    private fun archiveGoalHandler(goalDescriptionIds: List<Int>) {
        lifecycleScope.launch {
            dao?.archiveGoals(goalDescriptionIds)
        }
        goalDescriptionIds.forEach { goalDescriptionId ->
            activeGoalInstancesWithDescriptionWithCategories.indexOfFirst{ (_, d) ->
                d.description.id == goalDescriptionId
            }.also { index ->
                activeGoalInstancesWithDescriptionWithCategories.removeAt(index)
                goalAdapter?.notifyItemRemoved(index)
            }
        }
        if(goalDescriptionIds.size > 1) {
            Toast.makeText(context, R.string.deleteGoalsToast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.deleteGoalToast, Toast.LENGTH_SHORT).show()
        }
        resetToolbar()
    }

    // the handler for deleting goals
    private fun deleteGoalHandler(goalDescriptionIds: List<Int>) {
        lifecycleScope.launch {
            dao?.deleteGoals(goalDescriptionIds)
        }
        goalDescriptionIds.forEach { goalDescriptionId ->
            activeGoalInstancesWithDescriptionWithCategories.indexOfFirst{ (_, d) ->
                d.description.id == goalDescriptionId
            }.also { index ->
                activeGoalInstancesWithDescriptionWithCategories.removeAt(index)
                goalAdapter?.notifyItemRemoved(index)
            }
        }
        if(goalDescriptionIds.size > 1) {
            Toast.makeText(context, R.string.deleteGoalsToast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.deleteGoalToast, Toast.LENGTH_SHORT).show()
        }
        resetToolbar()
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}
