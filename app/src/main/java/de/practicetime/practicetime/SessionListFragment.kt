package de.practicetime.practicetime

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class SessionListFragment : Fragment(R.layout.fragment_sessions_list) {

    private var dao: PTDao? = null
    private lateinit var fabNewSession: FloatingActionButton
    private lateinit var fabRunningSession: FloatingActionButton
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private lateinit var sessionListAdapter: ConcatAdapter
    private val sessionListAdapterData = ArrayList<ArrayList<SessionWithSectionsWithCategories>>()

    private lateinit var sessionListToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var sessionListCollapsingToolbarLayout: CollapsingToolbarLayout

    private lateinit var deleteSessionDialog: AlertDialog

    private val selectedSessions = ArrayList<Triple<Int, View, SessionSummaryAdapter>>()

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
            it.setIsolateViewTypes(false)
            it.build()
        })

        requireActivity().findViewById<RecyclerView>(R.id.sessionList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sessionListAdapter
        }

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
                        sessionListAdapterData.add(ArrayList(
                            sessions.slice(firstSessionOfCurrentMonth until i).reversed()
                        ))
                        sessionListAdapter.addAdapter(
                            0,
                            SessionSummaryAdapter(
                                requireContext(),
                                false,
                                sessionListAdapterData.last(),
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
                sessionListAdapterData.add(ArrayList(
                    sessions.slice(firstSessionOfCurrentMonth until sessions.size).reversed()
                ))
                sessionListAdapter.addAdapter(
                    0,
                    SessionSummaryAdapter(
                        requireContext(),
                        true,
                        sessionListAdapterData.last(),
                        ::shortClickOnSessionHandler,
                        ::longClickOnSessionHandler,
                    )
                )
                sessionListAdapter.notifyItemInserted(0)
            }
        }
    }

    private fun shortClickOnSessionHandler(
        session: PracticeSession,
        sessionView: View,
        adapter: SessionSummaryAdapter,
    ) {
        // if there are already sessions selected,
        // add or remove the clicked session from the selection
        if(selectedSessions.isNotEmpty()) {
            if(selectedSessions.remove(Triple(session.id, sessionView, adapter))) {
                setSessionSelected(false, sessionView)
                if(selectedSessions.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnSessionHandler(session.id, sessionView, adapter)
            }
//            // if no selection is in progress show the edit dialog with the session
//        } else {
//            openSessionInFullscreen(session.id)
        }
    }

    // the handler for dealing with long clicks on session
    private fun longClickOnSessionHandler(
        sessionId: Int,
        sessionView: View,
        adapter: SessionSummaryAdapter,
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
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.topToolbarSelectionDelete -> deleteSessionDialog.apply {
                            setMessage(context.getString(
                                if(selectedSessions.size > 1) R.string.deleteSessionsDialogMessage
                                else R.string.deleteSessionDialogMessage
                            ))
                            show()
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

        // now add the newly selected session to the list...
        selectedSessions.add(Triple(sessionId, sessionView, adapter))

        // and tint its foreground to mark it as selected
        setSessionSelected(true, sessionView)

        // we consumed the event so we return true
        return true
    }

    // reset the toolbar and associated data
    private fun resetToolbar() {
        sessionListToolbar.apply {
            menu?.clear()
            inflateMenu(R.menu.library_toolbar_menu_base)
            navigationIcon = null
        }
        sessionListCollapsingToolbarLayout.apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        for ((_, view) in selectedSessions) {
            setSessionSelected(false, view)
        }
        selectedSessions.clear()
    }

    private fun setSessionSelected(selected: Boolean, view: View) {
        // TODO does not work with recycler view isSelected needs to be set on bind
        view.isSelected = selected // set selected so that background changes
    }

    // initialize the session delete dialog
    private fun initDeleteSessionDialog() {
        deleteSessionDialog = AlertDialog.Builder(requireActivity()).let { builder ->
            builder.apply {
                setMessage(R.string.deleteSessionDialogMessage)
                setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                    deleteSessionsHandler(selectedSessions.map{ p -> Pair(p.first, p.third)})
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
    private fun deleteSessionsHandler(sessionIdsWithAdapter: List<Pair<Int, SessionSummaryAdapter>>) {
        lifecycleScope.launch {
            for ((sessionId, adapter) in sessionIdsWithAdapter) {
                val session = dao!!.getSessionWithSections(sessionId)
                val goalProgress = dao!!.computeGoalProgressForSession(sessionId, checkArchived = true)

                // get all active goal instances at the time of the session
                val updatedGoalInstances = dao!!.getGoalInstances(
                    descriptionIds = goalProgress.keys.toList(),
                    now = session.sections.first().timestamp
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
                dao!!.deleteSession(sessionId, updatedGoalInstances)

                // find the session in the session list adapter data and delete it
                sessionListAdapter.adapters.indexOf(adapter).also { listIndex ->
                    sessionListAdapterData[listIndex].also { list ->
                        list.indexOfFirst {
                            it.session.id == sessionId
                        }.also { index ->
                            if (index != -1) {
                                list.removeAt(index)
                                if (list.isEmpty()) {
                                    sessionListAdapterData.removeAt(listIndex)
                                    sessionListAdapter.notifyItemRemoved(listIndex)
                                // finally we notify the adapter about the removed item
                                } else adapter.notifyItemRemoved(index + 1)
                            }
                        }
                    }
                }
            }
            if(sessionListAdapterData.isEmpty()) showHint()
            resetToolbar()
        }
    }
//
//    private fun openSessionInFullscreen(sessionId: Int) {
//        val intent = Intent(requireContext(), FullscreenSessionActivity::class.java)
//        val pBundle = Bundle()
//        pBundle.putInt("KEY_SESSION", sessionId)
//        intent.putExtras(pBundle)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//        startActivity(intent)
//    }

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