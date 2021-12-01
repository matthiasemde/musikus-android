package de.practicetime.practicetime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.room.Room
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList





class ActiveSessionActivity : AppCompatActivity() {

    private var dao: PTDao? = null

    private var activeCategories = ArrayList<Category>()

    private var addCategoryDialog: CategoryDialog? = null
    private var discardSessionDialog: AlertDialog? = null

    private lateinit var sectionsListAdapter: SectionsListAdapter

    private var listItems = ArrayList<String>()

    private lateinit var mService: SessionForegroundService

    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        /** called by service when we have connection to the service => we have mService reference */
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the IBinder and get SessionForegroundService instance
            val binder = service as SessionForegroundService.LocalBinder
            mService = binder.getService()
            mBound = true

            // set adapter for sections List
            initSectionsList()

            // sync UI with service data
            updateViews()
            adaptUIPausedState(mService.paused)
            setPauseStopBtnVisibility(mService.sessionActive)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to SessionForegroundService
        Intent(this, SessionForegroundService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_session)

        openDatabase()

        practiceTimer()

        // initialize adapter and recyclerView for showing category buttons from database
        initCategoryList()

        // create the dialog for finishing the current session
        initEndSessionDialog()

        // create the dialog for discarding the current session
        initDiscardSessionDialog()

        val btnPause = findViewById<MaterialButton>(R.id.bottom_pause)
        btnPause.setOnClickListener {
            if (mService.paused) {
                mService.paused = false
                mService.pauseDuration = 0
            } else {
                mService.paused = true
            }
            // adapt UI to changes
            adaptUIPausedState(mService.paused)
        }


        // call onBackPressed when upper left Button is pressed to respect custom animation
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // show the discard session dialog when btn_discard is pressed
        findViewById<ImageButton>(R.id.btn_discard).setOnClickListener {
            discardSessionDialog?.show()
        }
    }

    private fun initCategoryList() {
        val categoryAdapter = CategoryAdapter(
            activeCategories,
            context = this,
            showInActiveSession = true,
            shortClickHandler = ::categoryPressed,
            addCategoryHandler = { addCategoryDialog?.show() }
        )

        val categoryList = findViewById<RecyclerView>(R.id.categoryList)

        val rowNums = 3
        categoryList.apply {
            layoutManager = GridLayoutManager(
                context,
                rowNums,
                GridLayoutManager.HORIZONTAL,
                false
            )
            adapter = categoryAdapter
            itemAnimator?.apply {
//                addDuration = 200L
//                moveDuration = 500L
//                removeDuration = 200L
            }
        }

        // load all active categories from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveCategories()?.let { activeCategories.addAll(it.reversed())
                categoryAdapter.notifyItemRangeInserted(0, it.size)
            }
            categoryList.apply {
                scrollToPosition(0)
                visibility = View.VISIBLE
            }
        }

        // the handler for creating new categories
        fun addCategoryHandler(newCategory: Category) {
            lifecycleScope.launch {
                val newCategoryId = dao?.insertCategory(newCategory)?.toInt()
                if(newCategoryId != null) {
                    // we need to fetch the newly created category to get the correct id
                    dao?.getCategory(newCategoryId)?.let { activeCategories.add(0, it) }
                    categoryAdapter.notifyItemInserted(0)
                    categoryList.scrollToPosition(0)
                }
            }
        }

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(this, ::addCategoryHandler)
    }

    /** called when we have a connection to the Service holding the data */
    private fun initSectionsList() {
        val sectionsRecyclerView = findViewById<RecyclerView>(R.id.currentSections)
        // disable item changed fade animation which would otherwise occur every second on the first item
        (sectionsRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        sectionsListAdapter = SectionsListAdapter(mService.sectionBuffer)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.apply {
            reverseLayout = true
            stackFromEnd = true
        }
//        (findViewById<ViewGroup>(R.id.ll_active_session)).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        sectionsRecyclerView.layoutManager = layoutManager
        sectionsRecyclerView.adapter = sectionsListAdapter
    }

    // the routine for handling presses to category buttons
    private fun categoryPressed(category: Category, categoryView: View) {
        if (!mService.sessionActive) {   // session starts now
            //start the service so that timer starts
            Intent(this, SessionForegroundService::class.java).also {
                startService(it)
            }
            setPauseStopBtnVisibility(true)

            // when the session start, also update the goals
            updateGoals(dao!!, lifecycleScope)
        }

        // start a new section for the chosen category
        mService.startNewSection(category.id)

        // immediately update list
        // scroll up to first element so that it is visible when scrolling. Unfortunately, this breaks the animation when view is scrollable
        findViewById<RecyclerView>(R.id.currentSections).smoothScrollToPosition(mService.sectionBuffer.size)
        // this only re-draws the new item, also, it adds animation which would not occur with notifyDataSetChanged()
        sectionsListAdapter.notifyItemInserted(mService.sectionBuffer.size - 1)

        // force re-draw the whole RecyclerView after delayMs to clear the TextAppearance on items at position > 0
        // delayed in order to wait for the animation to finish introduced from notifyItemInserted()
        // this seems hacky but it's the only way to achieve both animation and proper textStyle clearing
        val delayMs: Long = 300
        Handler(Looper.getMainLooper()).also {
            it.postDelayed({
                    findViewById<RecyclerView>(R.id.currentSections).adapter = sectionsListAdapter
                },
                delayMs
            )
        }

    }

    /**
     * This function adapts all UI elements which change by pausing depending on paused
     * It can be called whenever required to sync the UI with the data in the service
     */
    private fun adaptUIPausedState(paused: Boolean) {
        if (paused) {
            // swap pause icon with play icon
            findViewById<MaterialButton>(R.id.bottom_pause).apply {
                setIconResource(R.drawable.ic_play)
            }
            // show the fab
            findViewById<ExtendedFloatingActionButton>(R.id.fab_info_popup).apply {
                show()
            }
            showOverlay()
        } else {
            // swap play icon with pause icon
            findViewById<MaterialButton>(R.id.bottom_pause).apply {
                setIconResource(R.drawable.ic_pause)
            }
            // hide the fab
            findViewById<ExtendedFloatingActionButton>(R.id.fab_info_popup).apply {
                hide()
                // reset text to zero so that on next pause old time is visible for a short moment
                text = "Pause: 00:00:00"
            }
            hideOverlay()
        }
    }

    /**
     * Shows a translucent overlay above the screen (TextView) with a fade animation and adapts the
     * rest of the UI to these changes
     */
    private fun showOverlay() {
        val transition = Fade().apply {
            duration = 600
            addTarget(R.id.tv_overlay_pause)
        }
        TransitionManager.beginDelayedTransition(
            findViewById(R.id.coordinator_layout_active_session),
            transition
        )
        findViewById<TextView>(R.id.tv_overlay_pause).visibility = View.VISIBLE

        // make down and discard button white
        findViewById<ImageButton>(R.id.btn_back).setColorFilter(Color.WHITE)
        findViewById<ImageButton>(R.id.btn_discard).setColorFilter(Color.WHITE)
    }

    private fun hideOverlay() {
        val transition = Fade().apply {
            duration = 600
            addTarget(R.id.tv_overlay_pause)
        }
        TransitionManager.beginDelayedTransition(
            findViewById(R.id.coordinator_layout_active_session),
            transition
        )
        findViewById<TextView>(R.id.tv_overlay_pause).visibility = View.GONE

        // make up button subtle grey or subtle white, according to theme
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
        val color = typedValue.data
        findViewById<ImageButton>(R.id.btn_back).setColorFilter(color)
        findViewById<ImageButton>(R.id.btn_discard).setColorFilter(color)
    }

    private fun hideHintTextView() {
        val transition = Fade().apply {
            duration = 300
            addTarget(R.id.tv_hint_start_new_session)
        }
        TransitionManager.beginDelayedTransition(
            findViewById(R.id.coordinator_layout_active_session),
            transition
        )
        findViewById<TextView>(R.id.tv_hint_start_new_session).visibility = View.GONE
    }

    private fun finishSession(rating: Int, comment: String?) {

        // get total break duration
        var totalBreakDuration = 0
        mService.sectionBuffer.forEach {
            totalBreakDuration += it.second
        }

        // TODO: Check if comment is empty -> insert null
        val newSession = PracticeSession(
            0,      // id=0 means not assigned, autoGenerate=true will do it for us
            totalBreakDuration,
            rating,
            comment,
            1
        )

        lifecycleScope.launch {
            // create a new session row and save its id
            val sessionId = dao?.insertSession(newSession)

            // traverse all sections for post-processing before writing into db
            for (section in mService.sectionBuffer) {
                // add the new sessionId to every section in the section buffer
                section.first.practice_session_id = sessionId?.toInt()
                // update section durations to exclude break durations
                section.first.duration = section.first.duration?.minus(section.second)
                // and insert them into the database
                dao?.insertSection(section.first)
            }

            // reset section buffer and session status
            mService.sectionBuffer.clear()
            // refresh the adapter otherwise the app will crash because of "inconsistency detected"
            findViewById<RecyclerView>(R.id.currentSections).adapter = sectionsListAdapter
        }

        // stop the service
        Intent(this, SessionForegroundService::class.java).also {
            stopService(it)
        }
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, ProgressUpdateActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private fun initEndSessionDialog() {
        // instantiate the builder for the alert dialog
        val endSessionDialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_view_end_session, null)

        val dialogRatingBar = dialogView.findViewById<RatingBar>(R.id.dialogRatingBar)
        val dialogComment = dialogView.findViewById<EditText>(R.id.dialogComment)

        // Dialog Setup
        endSessionDialogBuilder.apply {
            setView(dialogView)
            setCancelable(false)
            setPositiveButton(R.string.endSessionDialogOk) { _, _ ->
                val rating = dialogRatingBar.rating.toInt()
                finishSession(rating, dialogComment.text.toString())
            }
            setNegativeButton(R.string.endSessionDialogCancel) { dialog, _ ->
                if (!mService.paused) {
                    hideOverlay()
                }
                dialog.cancel()
            }
        }
        val endSessionDialog: AlertDialog = endSessionDialogBuilder.create()
        endSessionDialog.window?.setBackgroundDrawable(
            this.getDrawable(R.drawable.dialog_background)
        )

        // stop session button functionality
        findViewById<MaterialButton>(R.id.bottom_stop).setOnClickListener {
            // show the end session dialog
            endSessionDialog.show()
            endSessionDialog.also {
                val positiveButton = it.getButton(AlertDialog.BUTTON_POSITIVE)
                dialogRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
                    positiveButton.isEnabled = rating.toInt() > 0
                }
            }
            showOverlay()
        }
    }

    // initialize the dialog for discarding the current session
    private fun initDiscardSessionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(R.string.discard_session_dialog_title)
            setPositiveButton(R.string.discard_session_dialog_ok) { dialog, _ ->
                // clear the sectionBuffer so that runnable dies
                mService.sectionBuffer.clear()
                // refresh the adapter otherwise the app will crash because of "inconsistency detected"
                findViewById<RecyclerView>(R.id.currentSections).adapter = sectionsListAdapter
                // stop the service
                Intent(this@ActiveSessionActivity, SessionForegroundService::class.java).also {
                    stopService(it)
                }
                // terminate and go back to MainActivity
                finish()
            }
            setNegativeButton(R.string.discard_session_dialog_cancel) { dialog, _ ->
                if (!mService.paused) {
                    hideOverlay()
                }
                dialog.cancel()
            }
        }
        discardSessionDialog = builder.create()
    }
    private fun openDatabase() {
        val db = Room.databaseBuilder(
            this,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    /**
     * calculates total Duration (including pauses) of a section
     */
    private fun getDuration(section: PracticeSection): Int {
        val now = Date().time / 1000L
        return (now - section.timestamp).toInt()
    }

    /**
     * TODO should be replaced by functions triggered from the service rather than polling every 100ms
     */
    private fun practiceTimer() {
        // creates a new Handler
        Handler(Looper.getMainLooper()).also {
            // the post() method executes immediately
            it.post(object : Runnable {
                override fun run() {
                    if (mBound) {
                        updateViews()
                    }
                    // post the code again with a delay of 100 milliseconds so that ui is more responsive
                    it.postDelayed(this, 1000)
                }
            })
        }
    }

    private fun setPauseStopBtnVisibility(sessionActive: Boolean) {
        if (mBound) {
            if (sessionActive) {
                findViewById<MaterialButton>(R.id.bottom_pause).visibility = View.VISIBLE
                findViewById<MaterialButton>(R.id.bottom_stop).visibility = View.VISIBLE
                findViewById<MaterialButton>(R.id.bottom_metronome).apply {
                    text = ""       // remove text from Button
                    iconPadding = 0 // center icon
                }
                findViewById<MaterialButton>(R.id.bottom_record).apply {
                    text = ""       // remove text from Button
                    iconPadding = 0 // center icon
                }
            } else {
                findViewById<MaterialButton>(R.id.bottom_pause).visibility = View.GONE
                findViewById<MaterialButton>(R.id.bottom_stop).visibility = View.GONE
            }
            adaptUIPausedState(mService.paused)
        }

    }

    /**
     * Called whenever UI should sync with Service data, e.g. on Service connect or on new Tick every second
     */
    private fun updateViews() {
        val practiceTimeView = findViewById<TextView>(R.id.practiceTimer)
        if (mService.sessionActive) {
            hideHintTextView()
            val fabInfoPause = findViewById<ExtendedFloatingActionButton>(R.id.fab_info_popup)
            // load the current section from the sectionBuffer
            if (mService.paused) {
                // display pause duration on the fab, but only time after pause was activated
                fabInfoPause.text = "Pause: %02d:%02d:%02d".format(
                    mService.pauseDuration / 3600,
                    mService.pauseDuration % 3600 / 60,
                    mService.pauseDuration % 60
                )
            }
            practiceTimeView.text = "%02d:%02d:%02d".format(
                mService.totalPracticeDuration / 3600,
                mService.totalPracticeDuration % 3600 / 60,
                mService.totalPracticeDuration % 60
            )
            // notify adapter that first item changed on tick update (when activity has started the view is redrawn anyways)
            sectionsListAdapter.notifyItemChanged(mService.sectionBuffer.size - 1)
        } else {
            practiceTimeView.text = "00:00:00"
            findViewById<TextView>(R.id.tv_hint_start_new_session).text = getString(R.string.start_new_session_hint)
        }
    }

    /**
     * Adapter for SectionList RecyclerView.
     */
    private inner class SectionsListAdapter(
        // TODO this should be a list of SectionWithCategories
        private val practiceSections: ArrayList<Pair<PracticeSection, Int>>,
    ) : RecyclerView.Adapter<SectionsListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sectionName: TextView = view.findViewById(R.id.sectionName)
            val sectionDuration: TextView = view.findViewById(R.id.sectionDuration)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.view_active_session_section, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            // Get element from your dataset at this position
            val categoryName = activeCategories.find { category ->
                category.id == practiceSections[position].first.category_id
            }?.name

            // calculate duration of each session (minus pauses)
            var sectionDuration: Int
            practiceSections[position].apply {
                sectionDuration = (first.duration?:0).minus(second)
            }

            // contents of the view with that element
            viewHolder.sectionName.text = categoryName
            viewHolder.sectionDuration.text = getTimeString(sectionDuration)

            if (position == practiceSections.size - 1) {
                viewHolder.sectionName.setTypeface(viewHolder.sectionName.typeface, Typeface.BOLD);
                viewHolder.sectionDuration.setTypeface(viewHolder.sectionName.typeface, Typeface.BOLD);

                val typedValue = TypedValue()
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                val color = typedValue.data
                viewHolder.sectionName.setTextColor(color)
                viewHolder.sectionDuration.setTextColor(color)
            }

            // possible animation for first item: https://stackoverflow.com/a/26748274
            // bit laggy when animation is triggered shortly before it redraws anyways due to secondly
            // updates. DEPRECATED since notifyItemInserted() supports nice native animation!
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = practiceSections.size
    }

    private fun getTimeString(durationSecs: Int) : String {
        return "%02d:%02d:%02d".format(
            durationSecs / 3600,
            durationSecs % 3600 / 60,
            durationSecs % 60
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("TAH", "OnBack")
        overridePendingTransition(0, R.anim.slide_out_down)
    }
}