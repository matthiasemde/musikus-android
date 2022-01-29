package de.practicetime.practicetime.ui.sessionlist

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.R
import de.practicetime.practicetime.SessionSummaryAdapter
import de.practicetime.practicetime.Singleton
import de.practicetime.practicetime.database.PTDao
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.database.entities.SessionWithSectionsWithCategories
import de.practicetime.practicetime.ui.activesession.ActiveSessionActivity
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class SessionListFragment : Fragment(R.layout.fragment_sessions_list) {

    private var dao: PTDao? = null
    private lateinit var fabNewSession: FloatingActionButton
    private lateinit var fabRunningSession: FloatingActionButton
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable { }

    private lateinit var sessionListAdapter: ConcatAdapter
    private val sessionListAdapterData = ArrayList<ArrayList<SessionWithSectionsWithCategories>>()

    private lateinit var sessionListToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var sessionListCollapsingToolbarLayout: CollapsingToolbarLayout

    private lateinit var deleteSessionDialog: AlertDialog

    private val selectedSessions = ArrayList<Pair<Int, SessionSummaryAdapter>>()

    // catch the back press for the case where the selection should be reverted
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(selectedSessions.isNotEmpty()){
                    selectedSessions.clear()
                    notifyAllItems()
                    resetToolbar()
                } else {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        openDatabase()
        createDatabaseFirstRun()

        // create the dialog for deleting sessions
        initDeleteSessionDialog()

        // initialize the sessions list
        initSessionList()

        fabNewSession = view.findViewById(R.id.fab_new_session)
        fabRunningSession = view.findViewById(R.id.fab_running_session)
        val clickListener = View.OnClickListener {
            selectedSessions.clear()
            notifyAllItems()
            resetToolbar()

            val i = Intent(requireContext(), ActiveSessionActivity::class.java)
            requireActivity().startActivity(i)
            requireActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
        }
        fabNewSession.setOnClickListener(clickListener)
        fabRunningSession.setOnClickListener(clickListener)

        sessionListToolbar = view.findViewById(R.id.session_list_toolbar)
        sessionListCollapsingToolbarLayout = view.findViewById(R.id.session_list_collapsing_toolbar_layout)
    }

    private fun initSessionList() {

        sessionListAdapter = ConcatAdapter(ConcatAdapter.Config.Builder().let {
            it.setIsolateViewTypes(true)
            it.build()
        })

        lifecycleScope.launch {
            // fetch all sessions from the database
            dao?.getSessionsWithSectionsWithCategories()!!.also { sessions ->
                if (sessions.isEmpty()) {
                    showHint()
                    return@also
                } else {
                    hideHint()
                }

                // initialize variables to keep track of the current month
                // and the index of its first session
                var firstSessionOfCurrentMonth = 0
                var currentMonth = Calendar.getInstance().let { newDate ->
                    newDate.timeInMillis =
                        sessions.first().sections.first().section.timestamp * 1000L
                    newDate.get(Calendar.MONTH)
                }

                // then loop trough the rest of the sessions...
                sessions.forEachIndexed { i, session ->
                    // ...get the month...
                    val sessionMonth = Calendar.getInstance().let { newDate ->
                        newDate.timeInMillis =
                            session.sections.first().section.timestamp * 1000L
                        newDate.get(Calendar.MONTH)
                    }

                    // ...and compare it to the current month.
                    // if it is the same, create a new summary adapter
                    // with the respective subList of sessions
                    if (sessionMonth != currentMonth) {
                        sessionListAdapterData.add(0, ArrayList(
                            sessions.slice(firstSessionOfCurrentMonth until i).reversed()
                        ))
                        sessionListAdapter.addAdapter(
                            0,
                            SessionSummaryAdapter(
                                requireContext(),
                                false,
                                sessionListAdapterData.first(),
                                selectedSessions,
                                ::shortClickOnSessionHandler,
                                ::longClickOnSessionHandler,
                            ),
                        )
                        sessionListAdapter.notifyItemInserted(0)

                        // set the current month to this sessions month and save its index
                        currentMonth = sessionMonth
                        firstSessionOfCurrentMonth = i
                    }
                }

                // create an adapter for the last subList
                sessionListAdapterData.add(0, ArrayList(
                    sessions.slice(firstSessionOfCurrentMonth until sessions.size).reversed()
                ))
                sessionListAdapter.addAdapter(
                    0,
                    SessionSummaryAdapter(
                        requireContext(),
                        true,
                        sessionListAdapterData.first(),
                        selectedSessions,
                        ::shortClickOnSessionHandler,
                        ::longClickOnSessionHandler,
                    )
                )
                sessionListAdapter.notifyItemInserted(0)
            }

            requireActivity().findViewById<RecyclerView>(R.id.sessionList).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = sessionListAdapter
                itemAnimator?.apply{
                    changeDuration = 100L // default is 250
                    moveDuration = 200L
                    removeDuration = 100L
                }
            }
        }
    }

    private fun shortClickOnSessionHandler(
        layoutPosition: Int,
        adapter: SessionSummaryAdapter,
    ) {
        // if there are already sessions selected,
        // add or remove the clicked session from the selection
        if(selectedSessions.isNotEmpty()) {
            if(selectedSessions.remove(Pair(layoutPosition, adapter))) {
                adapter.notifyItemChanged(layoutPosition)
                if(selectedSessions.size == 1) {
                    sessionListToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible = true
                } else if(selectedSessions.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnSessionHandler(layoutPosition, adapter, vibrate = false)
            }
        }
    }

    // the handler for dealing with long clicks on session
    private fun longClickOnSessionHandler(
        layoutPosition: Int,
        adapter: SessionSummaryAdapter,
        vibrate: Boolean = true,
    ): Boolean {
        // if there is no session selected already, change the toolbar
        if(selectedSessions.isEmpty()) {
            sessionListToolbar.apply {
                // clear the base menu from the toolbar and inflate the new menu
                menu?.clear()
                inflateMenu(R.menu.library_toolbar_menu_for_selection)

                // set the back button and its click listener
                setNavigationIcon(R.drawable.ic_nav_back)
                setNavigationOnClickListener {
                    selectedSessions.clear()
                    notifyAllItems()
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.topToolbarSelectionDelete -> deleteSessionDialog.apply {
                            setMessage(context.getString(
                                if(selectedSessions.size > 1) R.string.deleteSessionsDialogMessage
                                else R.string.deleteSessionDialogMessage
                            ))
                            show()
                        }
                        R.id.topToolbarSelectionEdit -> {
                            openSessionInFullscreen(
                                sessionListAdapter.adapters.indexOf(adapter).let {
                                    sessionListAdapterData[it][
                                        selectedSessions.first().first - 1
                                    ].session.id
                                }
                            )
                            selectedSessions.clear()
                            notifyAllItems()
                            resetToolbar()
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            // change the background color of the App Bar
            val typedValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
            val color = typedValue.data
            sessionListCollapsingToolbarLayout.setBackgroundColor(color)
        }

        Pair(layoutPosition, adapter).also {
            if (!selectedSessions.contains(it)) {
                if (vibrate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                            as VibratorManager
                            ).defaultVibrator.apply {
                            cancel()
                            vibrate(
                                VibrationEffect.createOneShot(
                                    100,
                                    100
                                )
                            )
                        }
                }
                // now add the newly selected session to the list...
                selectedSessions.add(it)
                adapter.notifyItemChanged(layoutPosition)
            }
        }

        sessionListToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible =
                selectedSessions.size == 1

        // we consumed the event so we return true
        return true
    }

    private fun notifyAllItems() {
        sessionListAdapter.adapters.zip(sessionListAdapterData).forEach { (adapter, list) ->
            // notify data index + 1 because first item in adapter is the month header
            for(i in 0 until list.size) { adapter.notifyItemChanged(i + 1) }
        }
    }


    // reset the toolbar
    private fun resetToolbar() {
        sessionListToolbar.apply {
            menu?.clear()
            inflateMenu(R.menu.library_toolbar_menu_base)
            navigationIcon = null
        }
        sessionListCollapsingToolbarLayout.background = null
    }

    // initialize the session delete dialog
    private fun initDeleteSessionDialog() {
        deleteSessionDialog = AlertDialog.Builder(requireActivity()).let { builder ->
            builder.apply {
                setMessage(R.string.deleteSessionDialogMessage)
                setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                    deleteSessionsHandler()
                    dialog.dismiss()
                }
                setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                    dialog.cancel()
                }
            }
            builder.create()
        }
    }

    // the handler for deleting sessions
    private fun deleteSessionsHandler() {
        lifecycleScope.launch {
            selectedSessions.sortedByDescending { it.first }.forEach { (layoutPosition ,adapter) ->
                val adapterIndex = sessionListAdapter.adapters.indexOf(adapter)
                val adapterData = sessionListAdapterData[adapterIndex]

                val (session, sectionsWithCategories) = adapterData[layoutPosition - 1]
                val sections = sectionsWithCategories.map { s -> s.section }

                val goalProgress = dao!!.computeGoalProgressForSession(
                    dao!!.getSessionWithSectionsWithCategoriesWithGoals(session.id),
                    checkArchived = true
                )

                // get all active goal instances at the time of the session
                val updatedGoalInstances = dao!!.getGoalInstances(
                    descriptionIds = goalProgress.keys.toList(),
                    now = sections.first().timestamp
                // subtract the progress
                ).onEach { instance ->
                    goalProgress[instance.goalDescriptionId].also { progress ->
                        if (progress != null && progress > 0) {
                            // progress should not get lower than 0
                            instance.progress = maxOf(0 , instance.progress - progress)
                        }
                    }
                }

                // update goal instances and delete session in a single transaction
                dao!!.deleteSession(session.id, updatedGoalInstances)

                // find the session in the session list adapter data and delete it
                adapterData.removeAt(layoutPosition - 1)
                if (adapterData.isEmpty()) {
                    sessionListAdapterData.removeAt(adapterIndex)
                    sessionListAdapter.removeAdapter(adapter)
                    sessionListAdapter.notifyItemRemoved(adapterIndex)
                // finally we notify the adapter about the removed item
                } else if(adapter.isExpanded) {
                    adapter.notifyItemRemoved(layoutPosition)
                }
            }

            if(sessionListAdapterData.isEmpty()) showHint()
            selectedSessions.clear()

            resetToolbar()

            delay(300L)

            notifyAllItems()
        }
    }

    private fun openSessionInFullscreen(sessionId: Int) {
        val intent = Intent(requireContext(), FullscreenSessionActivity::class.java)
        val pBundle = Bundle()
        pBundle.putInt("KEY_SESSION", sessionId)
        intent.putExtras(pBundle)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private fun createDatabaseFirstRun() {
        lifecycleScope.launch {
            val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

            // FIRST RUN routine
            if (prefs.getBoolean("firstrun", true)) {

                // populate the category table on first run
                listOf(
                    Category(name="Die Schöpfung", colorIndex=0),
                    Category(name="Beethoven Septett", colorIndex=1),
                    Category(name="Schostakowitsch 9.", colorIndex=2),
                    Category(name="Trauermarsch c-Moll", colorIndex=3),
                    Category(name="Adagio", colorIndex=4),
                    Category(name="Eine kleine Gigue", colorIndex=5),
                    Category(name="Andantino", colorIndex=6),
                    Category(name="Klaviersonate", colorIndex=7),
                    Category(name="Trauermarsch", colorIndex=8),
                ).forEach {
                    dao?.insertCategory(it)
                }

                prefs.edit().putBoolean("firstrun", false).apply()
            }
        }
    }

    // check if session is running every second to respond the fab design to it
    override fun onResume() {
        super.onResume()
        val fabRunningSession = requireView().findViewById<FloatingActionButton>(R.id.fab_running_session)
        // set correct FAB depending if session is running.
        // wait 100ms and check again to give Service time to stop after discarding session
        runnable = object : Runnable {
            override fun run() {
                if (Singleton.serviceIsRunning) {
                    fabRunningSession.show()
                    fabNewSession.hide()
                } else {
                    fabNewSession.show()
                    fabRunningSession.hide()
                }
                if (Singleton.serviceIsRunning)
                    handler.postDelayed(this, 1000)
            }
        }
        handler = Handler(Looper.getMainLooper()).also {
            it.post(runnable)
        }
    }

    // remove the callback. Otherwise, the runnable will keep going and when entering the activity again,
    // there will be twice as much and so on...
    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }

    private fun showHint() {
        requireView().apply {
            findViewById<TextView>(R.id.sessionsHint).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.sessionList).visibility = View.GONE
        }
    }

    private fun hideHint() {
        requireView().apply {
            findViewById<TextView>(R.id.sessionsHint).visibility = View.GONE
            findViewById<RecyclerView>(R.id.sessionList).visibility = View.VISIBLE
        }
    }
}