/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.goals

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalDescriptionWithLibraryItems
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.databinding.DialogAddOrEditGoalBinding
import de.practicetime.practicetime.databinding.FragmentContainerGoalsBinding
import de.practicetime.practicetime.shared.EditTimeDialog
import de.practicetime.practicetime.shared.setCommonToolbar
import de.practicetime.practicetime.ui.MainState
import de.practicetime.practicetime.ui.library.LibraryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.Attr

class GoalsState(
    private val coroutineScope: CoroutineScope,
) {
    init {
        coroutineScope.launch {
            updateGoals()
            loadGoals()
        }
    }

    val goals = mutableStateListOf<GoalInstanceWithDescriptionWithLibraryItems>()
    val showGoalDialog = mutableStateOf(false)

    private suspend fun loadGoals() {
        goals.clear()
        PracticeTime.goalInstanceDao.getWithDescriptionsWithLibraryItems()
            .forEach { goal ->
                if (goals.find {
                    it.description.description.id == goal.description.description.id
                } != null) goals.add(goal)
            }
    }
}

@Composable
fun rememberGoalsState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(coroutineScope) { GoalsState(coroutineScope) }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsFragmentHolder(mainState: MainState) {
    val goalsState = rememberGoalsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { goalsState.showGoalDialog.value = true },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        topBar = {
            LargeTopAppBar(
                title = { Text( text="Goals") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AndroidViewBinding(FragmentContainerGoalsBinding::inflate)
        }
        if(goalsState.showGoalDialog.value) {
            Dialog(
                onDismissRequest = { goalsState.showGoalDialog.value = false },
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AndroidView(
                        factory = {
                            GoalDialog(it, mainState.libraryItems.value) { _, _ -> }
                        },
                        update = { goalDialog ->
                            goalDialog.update()
                        }
                    )
                }
            }
        }
    }
}

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val goalAdapterData =
        ArrayList<GoalInstanceWithDescriptionWithLibraryItems>()
    private lateinit var goalListView: RecyclerView
    private lateinit var goalAdapter : GoalAdapter

    private lateinit var addGoalDialog: GoalDialog
    private lateinit var editGoalDialog: EditTimeDialog

    private lateinit var goalsToolbar: MaterialToolbar
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

//    override fun onResume() {
//        super.onResume()
//        lifecycleScope.launch {
//            refreshGoalList()
//            resetToolbar()
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        goalListView = view.findViewById(R.id.goalList)

        initGoalList()

        lifecycleScope.launch {
            // trigger update routine and set adapter (initGoalList()) when it is ready
            updateGoals()
            refreshGoalList()
            // create a new goal dialog for adding new goals
//            addGoalDialog = GoalDialog(
//                requireActivity(),
//                PracticeTime.libraryItemDao.get(activeOnly = true),
//                ::addGoalHandler
//            )
        }

        // create the libraryItem dialog for editing libraryItems
//        initEditGoalDialog()

//        goalsToolbar = view.findViewById(R.id.goalsToolbar)
//        goalsCollapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar_layout)
//        resetToolbar()  // initialize the toolbar with all its listeners
//
//        view.findViewById<FloatingActionButton>(R.id.goalsFab).setOnClickListener {
//            clearGoalSelection()
//            resetToolbar()
//            addGoalDialog.show()
//        }
    }

    private fun initGoalList() {
        goalAdapter = GoalAdapter(
            goalAdapterData,
            selectedGoals,
            context = requireContext(),
            ::shortClickOnGoalHandler,
            ::longClickOnGoalHandler,
        )

        goalListView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
            itemAnimator?.apply {
                addDuration = 1L
                moveDuration = 500L
                removeDuration = 200L
            }
        }
    }

    private suspend fun refreshGoalList() {
        // load all active goals from the database and notify the adapter
        PracticeTime.goalInstanceDao.getWithDescriptionsWithLibraryItems(
        ).forEachIndexed { index, goalInstanceWithDescriptionWithLibraryItems ->
            if(goalAdapterData.none {
                    it.instance.id == goalInstanceWithDescriptionWithLibraryItems.instance.id
            }) {
                goalAdapterData.add(index, goalInstanceWithDescriptionWithLibraryItems)
                goalAdapter.notifyItemInserted(index)
            }
        }
        if (goalAdapterData.isEmpty()) {
//            showHint()
        }
    }

    private fun initEditGoalDialog() {
        editGoalDialog = EditTimeDialog(
            requireActivity(),
            getString(R.string.goalDialogTitleEdit)
        ) { newTarget ->
            lifecycleScope.launch {
                editGoalId?.let { descriptionId ->
                    PracticeTime.goalDescriptionDao.updateTarget(descriptionId, newTarget)
                    goalAdapterData.indexOfFirst {
                        it.description.description.id == descriptionId
                    }.let {
                        goalAdapterData[it].instance.target = newTarget
                        goalAdapter.notifyItemChanged(it)
                    }
                }
                editGoalId = null
                clearGoalSelection()
                resetToolbar()
            }
        }
    }

    // the handler for dealing with short clicks on goals
    private fun shortClickOnGoalHandler(index: Int) {
        if(selectedGoals.isNotEmpty()) {
            if (selectedGoals.remove(index)) {
                goalAdapter.notifyItemChanged(index)
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
//                setNavigationIcon(R.drawable.ic_nav_back)
                setNavigationOnClickListener {
                    clearGoalSelection()
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener { clickedItem ->
                    when (clickedItem.itemId) {
                        R.id.topToolbarSelectionDelete -> deleteGoalHandler()
                        R.id.topToolbarSelectionArchive -> archiveGoalHandler()
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
            goalAdapter.notifyItemChanged(index)
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
        tmpCopy.forEach { goalAdapter.notifyItemChanged(it) }
    }

    // reset the toolbar and associated data
    private fun resetToolbar() {
        goalsToolbar.apply {
            menu?.clear()
            inflateMenu(R.menu.goals_toolbar_menu_base)
            setCommonToolbar(requireActivity(), this) {
                when(it) {
                    R.id.goalsToolbarArchivedGoals -> {
                        val i = Intent(requireActivity(), ArchivedGoalsActivity::class.java)
                        startActivity(i)
                    }
                }
            }
            lifecycleScope.launch {
                menu.findItem(R.id.goalsToolbarArchivedGoals).title = requireActivity().getString(
                    R.string.archivedGoalsToolbar
                ). format(
                    PracticeTime.goalDescriptionDao.getArchivedWithLibraryItems().size
                )
            }
            navigationIcon = null
        }
        goalsCollapsingToolbarLayout.background = null
    }

    // the handler for creating new goals
    private fun addGoalHandler(
        newGoalDescriptionWithLibraryItems: GoalDescriptionWithLibraryItems,
        target: Int,
    ) {
        lifecycleScope.launch {
            PracticeTime.goalDescriptionDao.insertGoal(
                newGoalDescriptionWithLibraryItems,
                target
            )?.let {
                goalAdapterData.add(it)
                goalAdapter.notifyItemInserted(goalAdapterData.size)
//                hideHint()
            }
        }
    }

    // the handler for archiving goals
    private fun archiveGoalHandler() {
        val sortedDescriptionIds = selectedGoals.sortedDescending().map {
            goalAdapterData[it].description.description.id
        }

        AlertDialog.Builder(requireActivity()).apply {
            setMessage(context.resources.getQuantityText(
                R.plurals.archiveGoalDialogMessage, sortedDescriptionIds.size)
            )
            setPositiveButton(R.string.archiveGoalConfirm) { dialog, _ ->
                lifecycleScope.launch {
                    PracticeTime.goalDescriptionDao.getAndArchive(sortedDescriptionIds)
                    sortedDescriptionIds.forEach { goalDescriptionId ->
                        goalAdapterData.indexOfFirst{ (_, d) ->
                            d.description.id == goalDescriptionId
                        }.also { index ->
                            goalAdapterData.removeAt(index)
                            goalAdapter.notifyItemRemoved(index)
                        }
                    }
                    Toast.makeText(context, context.resources.getQuantityText(
                        R.plurals.archiveGoalToast, sortedDescriptionIds.size
                    ), Toast.LENGTH_SHORT).show()
//                    if (goalAdapterData.isEmpty()) showHint()
                    clearGoalSelection()
                    resetToolbar()
                    dialog.dismiss()
                }
            }
            setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }

    // the handler for deleting goals
    private fun deleteGoalHandler() {
        val sortedDescriptionIds = selectedGoals.sortedDescending().map {
            goalAdapterData[it].description.description.id
        }

        AlertDialog.Builder(requireActivity()).apply {
            setMessage(context.resources.getQuantityText(
                R.plurals.deleteGoalDialogMessage, sortedDescriptionIds.size)
            )
            setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                lifecycleScope.launch {
                    PracticeTime.goalDescriptionDao.deleteGoals(sortedDescriptionIds)
                    sortedDescriptionIds.forEach { goalDescriptionId ->
                        goalAdapterData.indexOfFirst{ (_, d) ->
                            d.description.id == goalDescriptionId
                        }.also { index ->
                            goalAdapterData.removeAt(index)
                            goalAdapter.notifyItemRemoved(index)
                        }
                    }
                    Toast.makeText(context, context.resources.getQuantityText(
                        R.plurals.deleteGoalToast, sortedDescriptionIds.size
                    ), Toast.LENGTH_SHORT).show()
//                    if (goalAdapterData.isEmpty()) showHint()
                    clearGoalSelection()
                    resetToolbar()
                    dialog.dismiss()
                }
            }
            setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }

//    private fun showHint() {
//        requireView().apply {
//            findViewById<TextView>(R.id.goalsHint).visibility = View.VISIBLE
//            findViewById<RecyclerView>(R.id.goalList).visibility = View.GONE
//        }
//    }
//
//    private fun hideHint() {
//        requireView().apply {
//            findViewById<TextView>(R.id.goalsHint).visibility = View.GONE
//            findViewById<RecyclerView>(R.id.goalList).visibility = View.VISIBLE
//        }
//    }
}
