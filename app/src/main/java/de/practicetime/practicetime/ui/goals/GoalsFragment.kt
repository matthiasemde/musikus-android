package de.practicetime.practicetime.ui.goals

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalDescriptionWithCategories
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithCategories
import de.practicetime.practicetime.shared.EditTimeDialog
import de.practicetime.practicetime.shared.setCommonToolbar
import de.practicetime.practicetime.ui.intro.AppIntroActivity
import kotlinx.coroutines.launch
import java.util.*


class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val goalAdapterData =
        ArrayList<GoalInstanceWithDescriptionWithCategories>()
    private var goalAdapter : GoalAdapter? = null

    private lateinit var addGoalDialog: GoalDialog
    private lateinit var editGoalDialog: EditTimeDialog
    private lateinit var deleteGoalDialog: AlertDialog

    private lateinit var goalsToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var goalsCollapsingToolbarLayout: CollapsingToolbarLayout

    private val selectedGoals = ArrayList<Int>()
    private var editGoalId : Long? = null

    // catch the back press for the case where the selection should be reverted
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(selectedGoals.isNotEmpty()){
                    clearGoalSelection()
                    resetToolbar()
                }else{
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        lifecycleScope.launch {
            // trigger update routine and set adapter (initGoalList()) when it is ready
            updateGoals()
            initGoalList()
            // create a new goal dialog for adding new goals
            addGoalDialog = GoalDialog(
                requireActivity(),
                PracticeTime.categoryDao.get(activeOnly = true),
                ::addGoalHandler
            )
        }

        // create the category dialog for editing categories
        initEditGoalDialog()

        // create the dialog for deleting goals
        initDeleteGoalDialog()

        goalsToolbar = view.findViewById(R.id.goalsToolbar)
        goalsCollapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar_layout)
        resetToolbar()  // initialize the toolbar with all its listeners

        view.findViewById<FloatingActionButton>(R.id.goalsFab).setOnClickListener {
            clearGoalSelection()
            resetToolbar()
            addGoalDialog.show()
        }
    }

    private suspend fun initGoalList() {
        goalAdapter = GoalAdapter(
            goalAdapterData,
            selectedGoals,
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
        PracticeTime.goalInstanceDao.getWithDescriptionsWithCategories().let {
            goalAdapterData.addAll(it)
            goalAdapter?.notifyItemRangeInserted(0, it.size)
        }
        if (goalAdapterData.isEmpty()) showHint()
    }

    private fun initEditGoalDialog() {
        editGoalDialog = EditTimeDialog(
            requireActivity(),
            getString(R.string.goalDialogTitleEdit)
        ) { newTarget ->
            lifecycleScope.launch {
                editGoalId?.let {
                    PracticeTime.goalDescriptionDao.updateTarget(it, newTarget)
                }
                editGoalId = null
                clearGoalSelection()
                resetToolbar()
            }
        }
    }

    // initialize the goal delete dialog
    private fun initDeleteGoalDialog() {

        deleteGoalDialog = requireActivity().let { context ->
            val view = context.layoutInflater.inflate(R.layout.dialog_delete_goal, null)
            val builder = AlertDialog.Builder(context)

            val checkBox = view.findViewById<CheckBox>(R.id.deleteGoalDialogCheckBox)

            builder.apply {
                setView(view)
                setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                    if(checkBox.isChecked) deleteGoalHandler()
                    else archiveGoalHandler()
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
    private fun shortClickOnGoalHandler(index: Int) {
        if(selectedGoals.isNotEmpty()) {
            if (selectedGoals.remove(index)) {
                goalAdapter?.notifyItemChanged(index)
                if(selectedGoals.size == 1) {
                    goalsToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible = true
                } else if (selectedGoals.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnGoalHandler(index, vibrate = false)
            }
        } else {
            goalAdapterData[index].let {
                editGoalId = it.description.description.id
                editGoalDialog.show(it.instance.target)
            }
        }
    }

    // the handler for dealing with long clicks on goals
    private fun longClickOnGoalHandler(index: Int, vibrate: Boolean = true): Boolean {
        // if there is no goal selected already, change the toolbar
        if(selectedGoals.isEmpty()) {
            goalsToolbar.apply {
                // clear the base menu from the toolbar and inflate the new menu
                menu?.clear()
                inflateMenu(R.menu.goals_toolbar_menu_for_selection)

                // set the back button and its click listener
                setNavigationIcon(R.drawable.ic_nav_back)
                setNavigationOnClickListener {
                    clearGoalSelection()
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener { clickedItem ->
                    when (clickedItem.itemId) {
                        R.id.topToolbarSelectionDelete -> {
                            deleteGoalDialog.show()
                            deleteGoalDialog.also { dialog ->
                                dialog.findViewById<TextView>(R.id.deleteGoalDialogMessage).setText(
                                    if(selectedGoals.size > 1) R.string.deleteGoalsDialogMessage
                                    else R.string.deleteGoalDialogMessage
                                )
                            }
                        }
                        R.id.topToolbarSelectionEdit -> {
                            goalAdapterData[selectedGoals.first()].let {
                                editGoalId = it.description.description.id
                                editGoalDialog.show(it.instance.target)
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
            goalsCollapsingToolbarLayout.setBackgroundColor(color)
        }

        if(!selectedGoals.contains(index)) {
            if (vibrate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (requireContext().getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                ).defaultVibrator.apply {
                        cancel()
                        vibrate(
                            VibrationEffect.createOneShot(100,100)
                        )
                    }
            }
            // now add the newly selected goal to the list...
            selectedGoals.add(index)
            goalAdapter?.notifyItemChanged(index)
        }

        goalsToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible =
            selectedGoals.size == 1

        // we consumed the event so we return true
        return true
    }

    // remove goals from selections list and notify the recycler items
    private fun clearGoalSelection() {
        val tmpCopy = selectedGoals.toList()
        selectedGoals.clear()
        tmpCopy.forEach { goalAdapter?.notifyItemChanged(it) }
    }

    // reset the toolbar and associated data
    private fun resetToolbar() {
        goalsToolbar.apply {
            menu?.clear()
            inflateMenu(R.menu.goals_toolbar_menu_base)
            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.goalsToolbarArchivedGoals -> {
                        val i = Intent(requireActivity(), ArchivedGoalsActivity::class.java)
                        startActivity(i)
                    }
                }
                return@setOnMenuItemClickListener true
            }
            setCommonToolbar(requireActivity(), this)
            navigationIcon = null
        }
        goalsCollapsingToolbarLayout.background = null
    }

    // the handler for creating new goals
    private fun addGoalHandler(
        newGoalDescriptionWithCategories: GoalDescriptionWithCategories,
        firstTarget: Int,
    ) {
        lifecycleScope.launch {
            val newGoalDescriptionId = PracticeTime.goalDescriptionDao.insertGoalDescriptionWithCategories(
                newGoalDescriptionWithCategories
            )
            PracticeTime.goalDescriptionDao.getWithCategories(
                newGoalDescriptionId
            )?.let { newGoalDescriptionWithCategories ->
                val newGoalDescription = newGoalDescriptionWithCategories.description
                // and create the first instance of the newly created goal description
                PracticeTime.goalInstanceDao.insertAndGet(
                    newGoalDescription.createInstance(Calendar.getInstance(), firstTarget)
                )?.let { newGoalInstance ->
                    PracticeTime.sessionDao.getSessionsContainingSectionFromTimeFrame(
                        newGoalInstance.startTimestamp,
                        newGoalInstance.startTimestamp + newGoalInstance.periodInSeconds
                    ).filter { s -> s.sections.first().timestamp > newGoalInstance.startTimestamp}
                    .forEach { s ->
                        PracticeTime.goalDescriptionDao.computeGoalProgressForSession(
                            PracticeTime.sessionDao.getWithSectionsWithCategoriesWithGoals(s.session.id)
                        ).also { progress ->
                            newGoalInstance.progress += progress[newGoalDescription.id] ?: 0
                        }
                    }

                    PracticeTime.goalInstanceDao.update(newGoalInstance)

                    goalAdapterData.add(
                        GoalInstanceWithDescriptionWithCategories(
                            instance = newGoalInstance,
                            description = newGoalDescriptionWithCategories,
                        )
                    )
                    goalAdapter?.notifyItemInserted(
                        goalAdapterData.size
                    )
                }
            }
            if (goalAdapterData.isNotEmpty()) hideHint()
        }
    }

    // the handler for archiving goals
    private fun archiveGoalHandler() {
        val goalDescriptionIds = selectedGoals.sortedDescending().map {
            goalAdapterData[it].description.description.id
        }
        lifecycleScope.launch {
            PracticeTime.goalDescriptionDao.getAndArchive(goalDescriptionIds)
        }
        goalDescriptionIds.forEach { goalDescriptionId ->
            goalAdapterData.indexOfFirst{ (_, d) ->
                d.description.id == goalDescriptionId
            }.also { index ->
                goalAdapterData.removeAt(index)
                goalAdapter?.notifyItemRemoved(index)
            }
        }
        if(goalDescriptionIds.size > 1) {
            Toast.makeText(context, R.string.deleteGoalsToast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.deleteGoalToast, Toast.LENGTH_SHORT).show()
        }
        if (goalAdapterData.isEmpty()) showHint()
        clearGoalSelection()
        resetToolbar()
    }

    // the handler for deleting goals
    private fun deleteGoalHandler() {
        val goalDescriptionIds = selectedGoals.sortedDescending().map {
            goalAdapterData[it].description.description.id
        }
        lifecycleScope.launch {
            PracticeTime.goalDescriptionDao.deleteGoals(goalDescriptionIds)
        }
        goalDescriptionIds.forEach { goalDescriptionId ->
            goalAdapterData.indexOfFirst{ (_, d) ->
                d.description.id == goalDescriptionId
            }.also { index ->
                goalAdapterData.removeAt(index)
                goalAdapter?.notifyItemRemoved(index)
            }
        }
        if(goalDescriptionIds.size > 1) {
            Toast.makeText(context, R.string.deleteGoalsToast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.deleteGoalToast, Toast.LENGTH_SHORT).show()
        }
        if (goalAdapterData.isEmpty()) showHint()
        clearGoalSelection()
        resetToolbar()
    }

    private fun showHint() {
        requireView().apply {
            findViewById<TextView>(R.id.goalsHint).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.goalList).visibility = View.GONE
        }
    }

    private fun hideHint() {
        requireView().apply {
            findViewById<TextView>(R.id.goalsHint).visibility = View.GONE
            findViewById<RecyclerView>(R.id.goalList).visibility = View.VISIBLE
        }
    }
}
