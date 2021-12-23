package de.practicetime.practicetime

import android.app.Activity
import android.graphics.Color
import android.media.*
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import androidx.room.Room
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import android.view.MotionEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs
import kotlin.math.round
import android.os.*
import android.text.TextUtils
import androidx.activity.result.contract.ActivityResultContracts

import androidx.documentfile.provider.DocumentFile

import java.text.SimpleDateFormat
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE


class ActiveSessionActivity : AppCompatActivity() {

    private var dao: PTDao? = null

    private var activeCategories = ArrayList<Category>()

    private var addCategoryDialog: CategoryDialog? = null
    private var discardSessionDialog: AlertDialog? = null

    private lateinit var sectionsListAdapter: SectionsListAdapter

    private lateinit var mService: SessionForegroundService
    private var mBound: Boolean = false
    private var stopDialogShown = false

    private lateinit var metronomeBottomSheet: LinearLayout
    private lateinit var metronomeBottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

    private lateinit var recordingBottomSheet: LinearLayout
    private lateinit var recordingBottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

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

            //
            initMetronomeBottomSheet()

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
        initRecordBottomSheet()

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

        var rowNums = 3

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

           adjustSpanCountCatList()
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
                    adjustSpanCountCatList()
                }
            }
        }

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(this, ::addCategoryHandler)
    }

    private fun adjustSpanCountCatList() {
        // change the span count if only few categories are displayed
        val l = findViewById<RecyclerView>(R.id.categoryList).layoutManager as GridLayoutManager
        l.spanCount = when {
            (activeCategories.size < 3) -> 1
            (activeCategories.size < 6) -> 2
            else -> 3
        }
    }

    /** called when we have a connection to the Service holding the data */
    private fun initSectionsList() {
        val sectionsRecyclerView = findViewById<RecyclerView>(R.id.currentSections)
        // disable item changed fade animation which would otherwise occur every second on the first item
        (sectionsRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        sectionsListAdapter = SectionsListAdapter(mService.sectionBuffer)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.apply {
            reverseLayout = true        // reverse the layout so that the last item (the most recent) is on top
            stackFromEnd = true         // this makes the last (most recent) item appear at the top of the view instead of the bottom
        }
//        (findViewById<ViewGroup>(R.id.ll_active_session)).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        sectionsRecyclerView.layoutManager = layoutManager
        sectionsRecyclerView.adapter = sectionsListAdapter

        // swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback( 0, ItemTouchHelper.LEFT) {
            override fun onMove(v: RecyclerView, h: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(h: RecyclerView.ViewHolder, dir: Int) = sectionsListAdapter.removeAt(h.absoluteAdapterPosition)
        }).attachToRecyclerView(sectionsRecyclerView)
    }

    /*********************************************
     *  Metronome Bottom sheet init
     ********************************************/

    private lateinit var bpmSliderView: SeekBar

    private lateinit var bpbView: TextView
    private lateinit var cpbView: TextView

    private var tabTempoLastTab: Long? = null
    private var tabTempoLastBpm = 120F

    private fun initMetronomeBottomSheet() {
        val metronomePlayStopView = findViewById<MaterialButton>(R.id.metronome_sheet_play_stop)

        bpmSliderView = findViewById(R.id.metronome_sheet_slider)

        bpbView = findViewById(R.id.metronome_sheet_bpb)
        cpbView = findViewById(R.id.metronome_sheet_cpb)

        findViewById<MaterialButton>(R.id.metronome_sheet_minus_five)
            .setOnClickListener { mService.metronomeBeatsPerMinute -= 5; syncUIToNewBPM() }
        findViewById<MaterialButton>(R.id.metronome_sheet_minus_one)
            .setOnClickListener { mService.metronomeBeatsPerMinute -= 1; syncUIToNewBPM() }
        findViewById<MaterialButton>(R.id.metronome_sheet_plus_one)
            .setOnClickListener { mService.metronomeBeatsPerMinute += 1; syncUIToNewBPM() }
        findViewById<MaterialButton>(R.id.metronome_sheet_plus_five)
            .setOnClickListener { mService.metronomeBeatsPerMinute += 5; syncUIToNewBPM() }

        mService.beat1 = applicationContext.resources.openRawResource(R.raw.beat_1).let {
            it.skip(44) // skip 44 bytes for the .wav header
            val byteArray = it.readBytes()
            it.close()
            byteArray
        }
        mService.beat2 = applicationContext.resources.openRawResource(R.raw.beat_2).let {
            it.skip(44) // skip 44 bytes for the .wav header
            val byteArray = it.readBytes()
            it.close()
            byteArray
        }
        mService.beat3 = applicationContext.resources.openRawResource(R.raw.beat_3).let {
            it.skip(44) // skip 44 bytes for the .wav header
            val byteArray = it.readBytes()
            it.close()
            byteArray
        }

        if(mService.metronomePlaying)
            metronomePlayStopView.icon = ContextCompat.getDrawable(this, R.drawable.ic_stop)
        metronomePlayStopView.setOnClickListener {
            val playToStop = ContextCompat.getDrawable(this, R.drawable.avd_play_to_stop) as AnimatedVectorDrawable
            val stopToPlay = ContextCompat.getDrawable(this, R.drawable.avd_stop_to_play) as AnimatedVectorDrawable

            if(mService.metronomePlaying) {
                metronomePlayStopView.icon = stopToPlay
                stopToPlay.start()
                mService.stopMetronome()
            } else {
                metronomePlayStopView.icon = playToStop
                playToStop.start()
                mService.startMetronome()
            }
        }

        bpmSliderView.apply {
            max = mService.metronomeMaxBpm - mService.metronomeMinBpm
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if(fromUser) {
                        mService.metronomeBeatsPerMinute = progress + mService.metronomeMinBpm
                        syncUIToNewBPM()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        findViewById<MaterialButton>(R.id.metronome_sheet_bpb_minus).setOnClickListener {
            mService.metronomeBeatsPerBar -= 1
            bpbView.text = mService.metronomeBeatsPerBar.toString()
        }

        findViewById<MaterialButton>(R.id.metronome_sheet_bpb_plus).setOnClickListener {
            mService.metronomeBeatsPerBar += 1
            bpbView.text = mService.metronomeBeatsPerBar.toString()
        }

        findViewById<MaterialButton>(R.id.metronome_sheet_cpb_minus).setOnClickListener {
            mService.metronomeClicksPerBeat -= 1
            cpbView.text = mService.metronomeClicksPerBeat.toString()
        }

        findViewById<MaterialButton>(R.id.metronome_sheet_cpb_plus).setOnClickListener {
            mService.metronomeClicksPerBeat += 1
            cpbView.text = mService.metronomeClicksPerBeat.toString()
        }

        findViewById<MaterialButton>(R.id.metronome_sheet_tab).setOnClickListener {
            val now = Date().time

            if(tabTempoLastTab != null) {
                mService.metronomeBeatsPerMinute = (60_000F / (now - tabTempoLastTab!!).toFloat()).let { newValue ->
                    val alpha = when(abs(tabTempoLastBpm - newValue)) {
                        in 0F..10F -> 0.1F
                        in 10F..20F -> 0.2F
                        in 20F..50F -> 0.3F
                        else -> 1F
                    }
                    tabTempoLastBpm = (1 - alpha) * tabTempoLastBpm + alpha * newValue
                    round(tabTempoLastBpm).toInt()
                }
                syncUIToNewBPM()
            }
            tabTempoLastTab = now
        }

        metronomeBottomSheet = findViewById(R.id.metronome_sheet_layout)
        metronomeBottomSheetBehaviour = BottomSheetBehavior.from(metronomeBottomSheet)

        metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        findViewById<MaterialButton>(R.id.bottom_metronome).setOnClickListener {
            metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }

        bpbView.text = mService.metronomeBeatsPerBar.toString()
        cpbView.text = mService.metronomeClicksPerBeat.toString()

        syncUIToNewBPM()
    }

    private fun syncUIToNewBPM() {
        findViewById<TextView>(R.id.metronome_sheet_bpm)
            .text = mService.metronomeBeatsPerMinute.toString()
        findViewById<TextView>(R.id.metronome_sheet_tempo_description)
            .text = when(mService.metronomeBeatsPerMinute) {
            in 20..40 -> "Grave"
            in 40..55 -> "Largo"
            in 55..66 -> "Lento"
            in 66..76 -> "Adagio"
            in 76..92 -> "Andante"
            in 92..108 -> "Andante moderato"
            in 108..116 -> "Moderato"
            in 116..120 -> "Allegro moderato"
            in 120..156 -> "Allegro"
            in 156..172 -> "Vivace"
            in 172..200 -> "Presto"
            else -> "Prestissimo"
        }
        bpmSliderView.progress = mService.metronomeBeatsPerMinute - mService.metronomeMinBpm
    }

    /*********************************************
     *  Recording Bottom sheet init
     ********************************************/

    private lateinit var recordingTimeView: TextView
    private lateinit var recordingTimeCsView: TextView

    private lateinit var recordingStartStopButtonView: MaterialButton

    private lateinit var recordingSaveLocationView: TextView
    private lateinit var recordingSelectSaveLocationView: MaterialButton
    private var recordSaveDirectory: DocumentFile? = null

    private fun initRecordBottomSheet() {
        /* Find all views in bottom sheet */
        recordingTimeView = findViewById(R.id.record_sheet_recording_time)
        recordingTimeCsView = findViewById(R.id.record_sheet_recording_time_cs)

        recordingStartStopButtonView = findViewById(R.id.record_sheet_start_pause)

        recordingSaveLocationView = findViewById(R.id.record_sheet_save_location)
        recordingSelectSaveLocationView = findViewById(R.id.record_sheet_select_save_location)

        /* Initialize local variables */
        val recordingNameFormat = SimpleDateFormat("_dd-MM-yyyy'T'H_mm_ss", Locale.getDefault())

        var recordingButtonLocked = false

        /* Modify views */
        recordingBottomSheet = findViewById(R.id.record_sheet_layout)
        recordingBottomSheetBehaviour = BottomSheetBehavior.from(recordingBottomSheet)

        recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        recordingTimeView.text = "00:00:00"
        recordingTimeCsView.text = "00"

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.extras?.getString("DURATION")?.toInt()?.also {
                        recordingTimeView.text = getTimeString(it / 1000)
                        recordingTimeCsView.text = "%02d".format(it % 1000 / 10)
                    }
                }
            },
            IntentFilter("RecordingDurationUpdate")
        )

        var newRecordingFile: DocumentFile? = null

        if(RecorderService.recording) {
            recordingStartStopButtonView.icon = ContextCompat.getDrawable(this, R.drawable.ic_stop)
            recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        recordingStartStopButtonView.setOnClickListener {
            // Return if button is still locked
            if (recordingButtonLocked) return@setOnClickListener

            // if not, lock it for one second to avoid double presses
            recordingButtonLocked = true
            Handler(Looper.getMainLooper()).postDelayed({
                recordingButtonLocked = false
            }, 1000L)

            if(!RecorderService.recording) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(Array(1) { Manifest.permission.RECORD_AUDIO }, 69)
                } else {
                    newRecordingFile = recordSaveDirectory?.createFile(
                        "audio/mp4",
                        (if(mService.sectionBuffer.isNotEmpty()) mService.currCategoryName
                        else "PracticeTime") + recordingNameFormat.format(Date().time)
                    )

                    Intent(this, RecorderService::class.java).also {
                        it.putExtra("URI", newRecordingFile?.uri.toString())
                        startService(it)
                    }
                    val startToStop = ContextCompat.getDrawable(
                        this, R.drawable.avd_start_to_stop
                    ) as AnimatedVectorDrawable

                    recordingSelectSaveLocationView.isEnabled = false
                    recordingStartStopButtonView.icon = startToStop
                    startToStop.start()
                }
            } else {
                val stopToStart = ContextCompat.getDrawable(
                    this, R.drawable.avd_stop_to_start
                ) as AnimatedVectorDrawable

                recordingSelectSaveLocationView.isEnabled = true
                recordingStartStopButtonView.icon = stopToStart
                stopToStart.start()

                Intent(this, RecorderService::class.java).also {
                    stopService(it)
                }

                if(recordingBottomSheetBehaviour.state == BottomSheetBehavior.STATE_COLLAPSED)
                    recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

                (application as PracticeTime).executorService.execute {
                    while(RecorderService.recording) {}
                    Snackbar.make(
                        findViewById(R.id.coordinator_layout_active_session),
                        resources.getString(R.string.record_snackbar_message).format(
                            RecorderService.recordingName ?: "Recording"
                        ),
                        7000
                    ).apply {
                        setAction(R.string.record_snackbar_undo) {
                            newRecordingFile?.delete()
                            this.dismiss()
                        }
                        anchorView = recordingBottomSheet
                        show()
                    }
                    RecorderService.recordingName = null
                }
            }
        }

        recordSaveDirectory = getPreferences(Context.MODE_PRIVATE)
            .getString("recordings_directory", null)
            ?.let {
                val df = DocumentFile.fromTreeUri(this, Uri.parse(it))
                recordingSaveLocationView.text = df?.uri?.lastPathSegment?.split(':')?.last()
                recordingStartStopButtonView.isEnabled = true
                df
            }

        recordingSaveLocationView.apply {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isSingleLine = true
            marqueeRepeatLimit = -1 // minus one repeats indefinitely
            isSelected = true
        }

        val getSaveDirectoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                recordSaveDirectory = result.data?.data?.let { uri ->
                    val df = DocumentFile.fromTreeUri(this, uri)
                    if(df != null) {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        recordingStartStopButtonView.isEnabled = true
                        recordingSaveLocationView.text = df.uri.lastPathSegment?.split(':')?.last()
                        getPreferences(Context.MODE_PRIVATE)
                            .edit()
                            .putString("recordings_directory", df.uri.toString())
                            .apply()
                    } else {
                        recordingStartStopButtonView.isEnabled = false
                        recordingSaveLocationView.text = ""
                    }
                    df
                }
            }
        }

        recordingSelectSaveLocationView.setOnClickListener {
            getSaveDirectoryLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
        }

        findViewById<MaterialButton>(R.id.record_sheet_open_save_location).setOnClickListener {
            recordSaveDirectory?.uri.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW)
                if(DocumentFile.isDocumentUri(this, uri)) {
                    intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    startActivity(intent)
                }
            }
        }

        findViewById<MaterialButton>(R.id.bottom_record).setOnClickListener {
            recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /*********************************************
     *  Timing stuff
     ********************************************/

    // the routine for handling presses to category buttons
    private fun categoryPressed(index: Int) {
        val categoryId = activeCategories[index].id

        if (!mService.sessionActive) {   // session starts now
            //start the service so that timer starts
            Intent(this, SessionForegroundService::class.java).also {
                startService(it)
            }
            setPauseStopBtnVisibility(true)

            // when the session start, also update the goals
            updateGoals(dao!!, lifecycleScope)
        } else if (mService.sectionBuffer.last().let {         // when session is running, don't allow starting if...
            (categoryId == it.first.category_id) ||           // ... in the same category
            (it.first.duration ?: 0 - it.second < 1)           // ... section running for less than 1sec
        }) {
            return  // ignore press then
        }

        // start a new section for the chosen category
        mService.startNewSection(categoryId, activeCategories[index].name)

        updateActiveSectionView()

        // this only re-draws the new item, also, it adds animation which would not occur with notifyDataSetChanged()
        // we use the second last item because the last one is always hidden (in onBindViewHolder()). This ensures insertion
        // animation even when list is full and should scroll
        sectionsListAdapter.notifyItemInserted(mService.sectionBuffer.size - 2)

    }

    private fun updateActiveSectionView() {
        val sName = findViewById<TextView>(R.id.running_section_name)
        val sDur = findViewById<TextView>(R.id.running_section_duration)

        if (mService.sectionBuffer.isNotEmpty()) {
            val categoryName = activeCategories.find { category ->
                category.id == mService.sectionBuffer.last().first.category_id
            }?.name
            sName.text = categoryName

            var sectionDur: Int
            mService.sectionBuffer.last().apply {
                sectionDur = (first.duration ?: 0).minus(second)
            }
            sDur.text = getTimeString(sectionDur)
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
        theme.resolveAttribute(R.attr.colorOnSurfaceLowerContrast, typedValue, true)
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

        // remove time the "stop session" AlertDialog was shown from last section
        mService.subtractStopDialogTime()

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
            val sessionId = dao?.insertSession(newSession)!!.toInt()

            // traverse all sections for post-processing before writing into db
            for (section in mService.sectionBuffer) {
                // add the new sessionId to every section in the section buffer
                section.first.practice_session_id = sessionId
                // update section durations to exclude break durations
                section.first.duration = section.first.duration?.minus(section.second)
                // and insert them into the database
                dao?.insertSection(section.first)
            }

            // reset section buffer and session status
            mService.sectionBuffer.clear()
            // refresh the adapter otherwise the app will crash because of "inconsistency detected"
            findViewById<RecyclerView>(R.id.currentSections).adapter = sectionsListAdapter
            exitActivity(sessionId)
        }
    }

    private fun exitActivity(sessionId: Int) {
        // stop the service
        Intent(this, SessionForegroundService::class.java).also {
            stopService(it)
        }
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, ProgressUpdateActivity::class.java)
        val pBundle = Bundle()
        pBundle.putInt("KEY_SESSION", sessionId)
        intent.putExtras(pBundle)
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

        dialogRatingBar.setOnTouchListener (object : View.OnTouchListener {
            var handleUp = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handleUp = true
                        v?.dispatchTouchEvent(MotionEvent.obtain(
                            event.downTime,
                            event.eventTime,
                            MotionEvent.ACTION_UP,
                            event.x,
                            event.y,
                            event.metaState
                        ))
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // When true is returned, view will not handle this event.
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        // When true is returned, view will not handle this event.
                        if(handleUp) {
                            handleUp = false
                            false
                        } else
                            true
                    }
                    else -> false
                }
            }
        })

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
                stopDialogShown = false
                dialog.cancel()
            }
        }
        val endSessionDialog: AlertDialog = endSessionDialogBuilder.create()
        endSessionDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.dialog_background)
        )

        // stop session button functionality
        findViewById<MaterialButton>(R.id.bottom_stop).setOnClickListener {
            mService.stopDialogTimestamp = Date().time / 1000L
            stopDialogShown = true
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
        discardSessionDialog = AlertDialog.Builder(this).let { builder ->
            builder.apply {
                setMessage(R.string.discard_session_dialog_title)
                setPositiveButton(R.string.discard_dialog_ok) { dialog, _ ->
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
            builder.create()
        }
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
                    if (mBound && !stopDialogShown) {
                        updateViews()
                    }
                    // post the code again with a delay of 100 milliseconds so that ui is more responsive
                    it.postDelayed(this, 100)
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
            // make discard option visible
            findViewById<ImageButton>(R.id.btn_discard).visibility = View.VISIBLE
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
            updateActiveSectionView()
        } else {
            practiceTimeView.text = "00:00:00"
            findViewById<TextView>(R.id.tv_hint_start_new_session).text = getString(R.string.start_new_session_hint)
        }
    }

    /**
     * Adapter for SectionList RecyclerView.
     */
    private inner class SectionsListAdapter(
        // TODO this should be a list of SectionWithCategories or a custom data class
        private val practiceSections: ArrayList<Pair<PracticeSection, Int>>,
    ) : RecyclerView.Adapter<SectionsListAdapter.ViewHolder>() {

        private val VIEW_TYPE_HEADER = 1
        private val VIEW_TYPE_GOAL = 2

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sectionName: TextView = view.findViewById(R.id.sectionName)
            val sectionDuration: TextView = view.findViewById(R.id.sectionDuration)
            val rootLayoutItem: LinearLayout = view.findViewById(R.id.ll_activesession_list)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.view_active_session_section, viewGroup, false)

            // don't display the most recent item (=last item in sectionBuffer) because we have a TextView for it
            if (viewType == VIEW_TYPE_HEADER) {
                view.layoutParams.height = 0
            }
            return ViewHolder(view)
        }

        override fun getItemViewType(position: Int): Int {
            return when (position) {
                practiceSections.size-1 -> VIEW_TYPE_HEADER
                else -> VIEW_TYPE_GOAL
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            // immediately return on uppermost view because its height is 0 anyways
            if (position == practiceSections.size-1) {
                return
            }

            // Get element from your dataset at this position
            val categoryName = activeCategories.find { category ->
                category.id == practiceSections[position].first.category_id
            }?.name

            // calculate duration of each session (minus pauses)
            var sectionDuration: Int
            practiceSections[position].apply {
                sectionDuration = (first.duration ?: 0).minus(second)
            }

            // contents of the view with that element
            viewHolder.sectionName.text = categoryName
            viewHolder.sectionDuration.text = getTimeString(sectionDuration)

        }

        // Return the size of your dataset (invoked by the layout manager)
        // -1 because don't include most recent item (we have a dedicated TextView for that)
        override fun getItemCount() = practiceSections.size

        /**
         * called when section is removed by swipe. Remove it from the list and notify adapter
         */
        fun removeAt(index: Int) {
            val elem = practiceSections[index]
            practiceSections.removeAt(index)   // items is a MutableList
            notifyItemRemoved(index)

            Snackbar.make(
                findViewById(R.id.coordinator_layout_active_session),
                getString(R.string.item_removed),
                7000
            ).apply {
                setAction(R.string.undo) {
                    practiceSections.add(index, elem)
                    notifyItemInserted(index)
                    this.dismiss()
                }
                anchorView = findViewById<ConstraintLayout>(R.id.constraintLayout_Bottom_btn)
                show()
            }
        }
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
        when {
            recordingBottomSheetBehaviour.state != BottomSheetBehavior.STATE_HIDDEN ->
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
            metronomeBottomSheetBehaviour.state != BottomSheetBehavior.STATE_HIDDEN ->
                metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
            else -> {
                super.onBackPressed()
                overridePendingTransition(0, R.anim.slide_out_down)
            }
        }
    }
}