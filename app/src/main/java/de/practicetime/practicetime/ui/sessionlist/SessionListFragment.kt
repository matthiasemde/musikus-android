/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 * Additions and modifications, author Matthias Emde
 */

package de.practicetime.practicetime.ui.sessionlist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.practicetime.practicetime.R
import de.practicetime.practicetime.datastore.ThemeSelections
import de.practicetime.practicetime.shared.*
import de.practicetime.practicetime.spacing
import de.practicetime.practicetime.ui.activesession.ActiveSessionActivity
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY
import de.practicetime.practicetime.utils.epochSecondsToDate
import de.practicetime.practicetime.utils.getDurationString
import de.practicetime.practicetime.viewmodel.MainViewModel
import de.practicetime.practicetime.viewmodel.SessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun SessionListFragmentHolder(
    mainViewModel: MainViewModel,
    activity: AppCompatActivity?,
    sessionsViewModel: SessionsViewModel = viewModel(),
) {
    val sessionsUiState by sessionsViewModel.sessionsUiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val sessionsListState = rememberLazyListState()
    // The FAB is initially expanded. Once the first visible item is past the first item we
    // collapse the FAB. We use a remembered derived state to minimize unnecessary compositions.
    val fabExpanded by remember {
        derivedStateOf {
            sessionsListState.firstVisibleItemIndex == 0
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "new session"
                    )
                },
                text = { Text(text = "Start Session") },
                onClick = {
                    activity?.let {
                        val i = Intent(it, ActiveSessionActivity::class.java)
                        it.startActivity(i)
                        it.overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
                    }
                },
                expanded = fabExpanded,
            )
        },
        topBar = {
            val topBarUiState = sessionsUiState.topBarUiState
            LargeTopAppBar(
                title = { Text(text = topBarUiState.title) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = {
                        mainViewModel.showMainMenu.value = true
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainViewModel.showMainMenu.value,
                            onDismissHandler = { mainViewModel.showMainMenu.value = false },
                            onSelectionHandler = { commonSelection ->
                                mainViewModel.showMainMenu.value = false

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainViewModel.showThemeSubMenu.value = true
                                    }
                                    CommonMenuSelections.BACKUP -> {
                                        mainViewModel.showExportImportDialog.value = true
                                    }
                                }
                            },
                            uniqueMenuItems = { /* TODO UNIQUE Session MENU */ }
                        )
                        ThemeMenu(
                            expanded = mainViewModel.showThemeSubMenu.value,
                            currentTheme = mainViewModel.activeTheme.collectAsState(initial = ThemeSelections.DAY).value,
                            onDismissHandler = { mainViewModel.showThemeSubMenu.value = false },
                            onSelectionHandler = { theme ->
                                mainViewModel.showThemeSubMenu.value = false
                                mainViewModel.setTheme(theme)
                            }
                        )
                    }
                }
            )

            // Action bar
            val actionModeUiState = sessionsUiState.actionModeUiState
            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = sessionsViewModel::clearActionMode,
                    onEditHandler = sessionsViewModel::onEditAction,
                    onDeleteHandler = sessionsViewModel::onDeleteAction,
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = sessionsUiState.contentUiState

            // Session list
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 56.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                state = sessionsListState,
            ) {
                contentUiState.sessionsForDaysForMonths.forEach { sessionsForDaysForMonth ->
                    item {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                        ) {
                            MonthHeader(
                                month = epochSecondsToDate(
                                    sessionsForDaysForMonth.sessionsForDays.first().sessions.first().sections.first().section.timestamp
                                ).month.name,
                                onClickHandler = {
                                    sessionsViewModel.onMonthHeaderClicked(sessionsForDaysForMonth.specificMonth)
                                }
                            )
                        }
                    }
                    val monthVisible = sessionsForDaysForMonth.specificMonth in contentUiState.expandedMonths
                    sessionsForDaysForMonth.sessionsForDays.forEach { sessionsForDay ->
                        item {
                            AnimatedVisibility(
                                modifier = Modifier.animateItemPlacement(),
                                visible = monthVisible,
                                enter = scaleIn(),
                                exit = fadeOut()
                            ) {
                                DayHeader(
                                    timestamp = sessionsForDay.sessions.first().sections.first().section.timestamp,
                                    totalPracticeDuration = sessionsForDay.totalPracticeDuration
                                )
                            }
                        }
                        items(
                            items = sessionsForDay.sessions,
                            key = { it.session.id }
                        ) { session ->
                            AnimatedVisibility(
                                modifier = Modifier.animateItemPlacement(),
                                visible = monthVisible,
                                enter = scaleIn(),
                                exit = fadeOut()
                            ) {
                                Selectable(
                                    selected = session in contentUiState.selectedSessions,
                                    onShortClick = {
                                        sessionsViewModel.onSessionClicked(session)
                                    },
                                    onLongClick = {
                                        sessionsViewModel.onSessionClicked(session, longClick = true)
                                    },
                                ) {
                                    SessionCard(sessionWithSectionsWithLibraryItems = session)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MonthHeader(
    month: String,
    onClickHandler: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = onClickHandler
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun DayHeader(
    timestamp: Long,
    totalPracticeDuration: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val dateFormat = SimpleDateFormat("E dd.MM.yyyy", Locale.getDefault())

        Text(
            text = dateFormat.format(Date(timestamp * 1000)),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = getDurationString(totalPracticeDuration, TIME_FORMAT_HUMAN_PRETTY).toString(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
//
//class SessionListFragment : Fragment(R.layout.fragment_sessions_list) {
//
//    private lateinit var fabNewSessionView: FloatingActionButton
//    private lateinit var fabRunningSessionView: FloatingActionButton
//    private lateinit var sessionListView: RecyclerView
//    private var handler: Handler = Handler(Looper.getMainLooper())
//    private var runnable: Runnable = Runnable { }
//
//    private lateinit var sessionListAdapter: ConcatAdapter
//    private val sessionListAdapterData = ArrayList<ArrayList<SessionWithSectionsWithLibraryItems>>()
//
//    private lateinit var sessionListToolbar: MaterialToolbar
//    private lateinit var sessionListCollapsingToolbarLayout: CollapsingToolbarLayout
//
//    private lateinit var deleteSessionDialog: AlertDialog
//
//    private val selectedSessions = ArrayList<Pair<Int, SessionSummaryAdapter>>()
//
//    // catch the back press for the case where the selection should be reverted
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if(selectedSessions.isNotEmpty()){
//                    selectedSessions.clear()
//                    notifyAllItems()
//                    resetToolbar()
//                } else {
//                    isEnabled = false
//                    activity?.onBackPressed()
//                }
//            }
//        })
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        fabNewSessionView = view.findViewById(R.id.fab_new_session)
//        fabRunningSessionView = view.findViewById(R.id.fab_running_session)
//
//        sessionListView = view.findViewById(R.id.sessionList)
//
//        // create the dialog for deleting sessions
//        initDeleteSessionDialog()
//
//        // initialize the sessions list
//        initSessionList()
//
//        val clickListener = View.OnClickListener {
//            selectedSessions.clear()
//            notifyAllItems()
//            resetToolbar()
//
//            val i = Intent(requireContext(), ActiveSessionActivity::class.java)
//            requireActivity().startActivity(i)
//            requireActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
//        }
//        fabNewSessionView.setOnClickListener(clickListener)
//        fabRunningSessionView.setOnClickListener(clickListener)
//
//        sessionListToolbar = view.findViewById(R.id.session_list_toolbar)
//        sessionListCollapsingToolbarLayout = view.findViewById(R.id.session_list_collapsing_toolbar_layout)
//        resetToolbar()  // initialize the toolbar with all its listeners
//    }
//
//    private fun initSessionList() {
//
//        sessionListAdapter = ConcatAdapter(ConcatAdapter.Config.Builder().let {
//            it.setIsolateViewTypes(true)
//            it.build()
//        })
//
//        sessionListView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = sessionListAdapter
//            itemAnimator?.apply{
//                changeDuration = 100L // default is 250
//                moveDuration = 200L
//                removeDuration = 100L
//            }
//        }
//
//        lifecycleScope.launch {
//            // fetch all sessions from the database
//            PTDatabase.getInstance(requireContext()).sessionDao.getAllWithSectionsWithLibraryItems().sortedBy {
//                it.sections.firstOrNull()?.section?.timestamp ?: 0L
//            }.also { sessions ->
//                if (sessions.isEmpty()) {
//                    showHint()
//                    return@also
//                } else {
//                    hideHint()
//                    PracticeTime.noSessionsYet = false
//                }
//
//                // initialize variables to keep track of the current month
//                // and the index of its first session
//                var firstSessionOfCurrentMonth = 0
//                // TODO move logic to TimeConversion.kt
//                var currentMonth = Calendar.getInstance().let { newDate ->
//                    newDate.timeInMillis =
//                        sessions.first().sections.first().section.timestamp * 1000L
//                    newDate.get(Calendar.MONTH)
//                }
//
//                // then loop trough the rest of the sessions...
//                sessions.forEachIndexed { i, session ->
//                    // ...get the month...
//                    val sessionMonth = Calendar.getInstance().let { newDate ->
//                        newDate.timeInMillis =
//                            session.sections.first().section.timestamp * 1000L
//                        newDate.get(Calendar.MONTH)
//                    }
//
//                    // ...and compare it to the current month.
//                    // if it is the same, create a new summary adapter
//                    // with the respective subList of sessions
//                    if (sessionMonth != currentMonth) {
//                        sessionListAdapterData.add(0, ArrayList(
//                            sessions.slice(firstSessionOfCurrentMonth until i).reversed()
//                        ))
//                        sessionListAdapter.addAdapter(
//                            0,
//                            SessionSummaryAdapter(
//                                requireContext(),
//                                false,
//                                sessionListAdapterData.first(),
//                                selectedSessions,
//                                ::shortClickOnSessionHandler,
//                                ::longClickOnSessionHandler,
//                            ),
//                        )
//                        sessionListAdapter.notifyItemInserted(0)
//
//                        // set the current month to this sessions month and save its index
//                        currentMonth = sessionMonth
//                        firstSessionOfCurrentMonth = i
//                    }
//                }
//
//                // create an adapter for the last subList
//                sessionListAdapterData.add(0, ArrayList(
//                    sessions.slice(firstSessionOfCurrentMonth until sessions.size).reversed()
//                ))
//                sessionListAdapter.addAdapter(
//                    0,
//                    SessionSummaryAdapter(
//                        requireContext(),
//                        true,
//                        sessionListAdapterData.first(),
//                        selectedSessions,
//                        ::shortClickOnSessionHandler,
//                        ::longClickOnSessionHandler,
//                    )
//                )
//                sessionListAdapter.notifyItemInserted(0)
//            }
//        }
//    }
//
//    private fun shortClickOnSessionHandler(
//        layoutPosition: Int,
//        adapter: SessionSummaryAdapter,
//    ) {
//        // if there are already sessions selected,
//        // add or remove the clicked session from the selection
//        if(selectedSessions.isNotEmpty()) {
//            if(selectedSessions.remove(Pair(layoutPosition, adapter))) {
//                adapter.notifyItemChanged(layoutPosition)
//                if(selectedSessions.size == 1) {
//                    sessionListToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible = true
//                } else if(selectedSessions.isEmpty()) {
//                    resetToolbar()
//                }
//            } else {
//                longClickOnSessionHandler(layoutPosition, adapter, vibrate = false)
//            }
//        } else {
//            openSessionInFullscreen(
//                sessionListAdapter.adapters.indexOf(adapter).let {
//                    sessionListAdapterData[it][layoutPosition - 1].session.id
//                }
//            )
//        }
//    }
//
//    // the handler for dealing with long clicks on session
//    private fun longClickOnSessionHandler(
//        layoutPosition: Int,
//        adapter: SessionSummaryAdapter,
//        vibrate: Boolean = true,
//    ): Boolean {
//        // if there is no session selected already, change the toolbar
//        if(selectedSessions.isEmpty()) {
//            sessionListToolbar.apply {
//                // clear the base menu from the toolbar and inflate the new menu
//                menu?.clear()
//                inflateMenu(R.menu.sessions_list_menu_for_selection)
//
//                // set the back button and its click listener
//                setNavigationIcon(R.drawable.ic_nav_back)
//                setNavigationOnClickListener {
//                    selectedSessions.clear()
//                    notifyAllItems()
//                    resetToolbar()
//                }
//
//                // set the click listeners for the menu options here
//                setOnMenuItemClickListener { item ->
//                    when (item.itemId) {
//                        R.id.topToolbarSelectionDelete -> deleteSessionDialog.apply {
//                            setMessage(context.getString(
//                                if(selectedSessions.size > 1) R.string.deleteSessionsDialogMessage
//                                else R.string.deleteSessionDialogMessage
//                            ))
//                            show()
//                        }
//                        R.id.topToolbarSelectionEdit -> {
//                            openSessionInFullscreen(
//                                sessionListAdapter.adapters.indexOf(adapter).let {
//                                    sessionListAdapterData[it][
//                                        selectedSessions.first().first - 1
//                                    ].session.id
//                                }
//                            )
//                            selectedSessions.clear()
//                            notifyAllItems()
//                            resetToolbar()
//                        }
//                    }
//                    return@setOnMenuItemClickListener true
//                }
//            }
//            // change the background color of the App Bar
//            val typedValue = TypedValue()
//            requireActivity().theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
//            val color = typedValue.data
//            sessionListCollapsingToolbarLayout.setBackgroundColor(color)
//        }
//
//        Pair(layoutPosition, adapter).also {
//            if (!selectedSessions.contains(it)) {
//                if (vibrate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    (requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
//                            as VibratorManager
//                            ).defaultVibrator.apply {
//                            cancel()
//                            vibrate(
//                                VibrationEffect.createOneShot(
//                                    100,
//                                    100
//                                )
//                            )
//                        }
//                }
//                // now add the newly selected session to the list...
//                selectedSessions.add(it)
//                adapter.notifyItemChanged(layoutPosition)
//            }
//        }
//
//        sessionListToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible =
//                selectedSessions.size == 1
//
//        // we consumed the event so we return true
//        return true
//    }
//
//    private fun notifyAllItems() {
//        sessionListAdapter.adapters.zip(sessionListAdapterData).forEach { (adapter, list) ->
//            // notify data index + 1 because first item in adapter is the month header
//            for(i in 0 until list.size) { adapter.notifyItemChanged(i + 1) }
//        }
//    }
//
//    // reset the toolbar
//    private fun resetToolbar() {
//        sessionListToolbar.apply {
//            menu?.clear()
//            setCommonToolbar(requireActivity(), this) {
////                Place menu item click handler here
////                when(it) {
////                }
//            }
//            inflateMenu(R.menu.sessions_list_menu_base)
//            navigationIcon = null
//        }
//        sessionListCollapsingToolbarLayout.background = null
//    }
//
//
//    // initialize the session delete dialog
//    private fun initDeleteSessionDialog() {
//        deleteSessionDialog = AlertDialog.Builder(requireActivity()).let { builder ->
//            builder.apply {
//                setMessage(R.string.deleteSessionDialogMessage)
//                setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
//                    deleteSessionsHandler()
//                    dialog.dismiss()
//                }
//                setNegativeButton(R.string.dialogDismiss) { dialog, _ ->
//                    dialog.cancel()
//                }
//            }
//            builder.create()
//        }
//    }
//
//    // the handler for deleting sessions
//    private fun deleteSessionsHandler() {
//        lifecycleScope.launch {
//            selectedSessions.sortedByDescending { it.first }.forEach { (layoutPosition ,adapter) ->
//                val adapterIndex = sessionListAdapter.adapters.indexOf(adapter)
//                val adapterData = sessionListAdapterData[adapterIndex]
//
//                val (session, sectionsWithLibraryItems) = adapterData[layoutPosition - 1]
//                val sections = sectionsWithLibraryItems.map { s -> s.section }
//
//                val goalProgress = PTDatabase.getInstance(requireContext()).goalDescriptionDao.computeGoalProgressForSession(
//                    PTDatabase.getInstance(requireContext()).sessionDao.getWithSectionsWithLibraryItemsWithGoals(session.id),
//                    checkArchived = true
//                )
//
//                // get all active goal instances at the time of the session
//                val updatedGoalInstances = PTDatabase.getInstance(requireContext()).goalInstanceDao.get(
//                    goalDescriptionIds = goalProgress.keys.toList(),
//                    from = sections.first().timestamp
//                // subtract the progress
//                ).onEach { instance ->
//                    goalProgress[instance.goalDescriptionId].also { progress ->
//                        if (progress != null && progress > 0) {
//                            // progress should not get lower than 0
//                            instance.progress = maxOf(0 , instance.progress - progress)
//                        }
//                    }
//                }
//
//                // update goal instances and delete session in a single transaction
//                PTDatabase.getInstance(requireContext()).sessionDao.delete(session.id, updatedGoalInstances)
//
//                // find the session in the session list adapter data and delete it
//                adapterData.removeAt(layoutPosition - 1)
//                if (adapterData.isEmpty()) {
//                    sessionListAdapterData.removeAt(adapterIndex)
//                    sessionListAdapter.removeAdapter(adapter)
//                    sessionListAdapter.notifyItemRemoved(adapterIndex)
//                // finally we notify the adapter about the removed item
//                } else if(adapter.isExpanded) {
//                    adapter.notifyItemRemoved(layoutPosition)
//                }
//            }
//
//            if(sessionListAdapterData.isEmpty()) showHint()
//            selectedSessions.clear()
//
//            resetToolbar()
//
//            delay(300L)
//
//            notifyAllItems()
//        }
//    }
//
//    private fun openSessionInFullscreen(sessionId: UUID) {
//        val intent = Intent(requireContext(), FullscreenSessionActivity::class.java)
//        val pBundle = Bundle()
////        pBundle.putLong("KEY_SESSION", sessionId)
//        intent.putExtras(pBundle)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//        startActivity(intent)
//    }
//
//    // check if session is running every second to respond the fab design to it
//    override fun onResume() {
//        super.onResume()
//        val fabRunningSession = requireView().findViewById<FloatingActionButton>(R.id.fab_running_session)
//        // set correct FAB depending if session is running.
//        // wait 100ms and check again to give Service time to stop after discarding session
//        runnable = object : Runnable {
//            override fun run() {
//                if (PracticeTime.serviceIsRunning) {
//                    // apparently there is a bug with hide() / View.GONE which causes the Toolbar to jump down
//                    // so use Invisible so that views don't get broken
//                    fabNewSessionView.visibility = View.INVISIBLE
//                    fabRunningSession.show()
//                } else {
//                    fabRunningSession.visibility = View.INVISIBLE
//                    fabNewSessionView.show()
//                }
//                if (PracticeTime.serviceIsRunning)
//                    handler.postDelayed(this, 500)
//            }
//        }
//        handler = Handler(Looper.getMainLooper()).also {
//            it.post(runnable)
//        }
//    }
//
//    // remove the callback. Otherwise, the runnable will keep going and when entering the activity again,
//    // there will be twice as much and so on...
//    override fun onStop() {
//        super.onStop()
//        handler.removeCallbacks(runnable)
//    }
//
//    private fun showHint() {
//        requireView().apply {
//            findViewById<TextView>(R.id.sessionsHint).visibility = View.VISIBLE
//            findViewById<RecyclerView>(R.id.sessionList).visibility = View.GONE
//        }
//    }
//
//    private fun hideHint() {
//        requireView().apply {
//            findViewById<TextView>(R.id.sessionsHint).visibility = View.GONE
//            findViewById<RecyclerView>(R.id.sessionList).visibility = View.VISIBLE
//        }
//    }
//}
