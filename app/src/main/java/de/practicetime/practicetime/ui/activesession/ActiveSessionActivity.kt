package de.practicetime.practicetime.ui.activesession

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.*
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.components.NonDraggableRatingBar
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.database.entities.Section
import de.practicetime.practicetime.database.entities.Session
import de.practicetime.practicetime.database.entities.SessionWithSections
import de.practicetime.practicetime.services.RecorderService
import de.practicetime.practicetime.services.SessionForegroundService
import de.practicetime.practicetime.ui.goals.ProgressUpdateActivity
import de.practicetime.practicetime.ui.goals.updateGoals
import de.practicetime.practicetime.ui.library.CategoryAdapter
import de.practicetime.practicetime.ui.library.CategoryDialog
import de.practicetime.practicetime.utils.TIME_FORMAT_HMS_DIGITAL
import de.practicetime.practicetime.utils.getCurrTimestamp
import de.practicetime.practicetime.utils.getDurationString
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round

class ActiveSessionActivity : AppCompatActivity() {

    private var activeCategories = ArrayList<Category>()

    private var addCategoryDialog: CategoryDialog? = null
    private var discardSessionDialog: AlertDialog? = null

    private lateinit var sectionsListAdapter: SectionsListAdapter

    private lateinit var mService: SessionForegroundService
    private var mBound: Boolean = false
    private var stopDialogShown = false

    private var quote: CharSequence = ""

    private lateinit var metronomeBottomSheet: LinearLayout
    private lateinit var metronomeBottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

    private lateinit var recordingBottomSheet: NestedScrollView
    private lateinit var recordingBottomSheetBehaviour: BottomSheetBehavior<NestedScrollView>

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

            initMetronomeBottomSheet()

            // sync UI with service data
            updateViews()
            adaptUIPausedState(mService.paused)
            setPauseStopBtnVisibility(mService.sessionActive)
            adaptBottomTextView(mService.sessionActive)
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

        LocalBroadcastManager.getInstance(this).registerReceiver(
            updateRecordTimeReceiver,
            IntentFilter("RecordingDurationUpdate")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            stopRecordingReceiver,
            IntentFilter("RecordingStopped")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_session)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
            super.onBackPressed()
            overridePendingTransition(0, R.anim.slide_out_down)
        }

        // show the discard session dialog when btn_discard is pressed
        findViewById<ImageButton>(R.id.btn_discard).setOnClickListener {
            discardSessionDialog?.show()
        }

        // load a random quote
        quote = PracticeTime.getRandomQuote(this)
        findViewById<TextView>(R.id.tv_overlay_pause).text = quote
        findViewById<TextView>(R.id.tv_quote).text = quote
    }

    override fun onRestart() {
        super.onRestart()
        updateRecordingsList()
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
            PracticeTime.categoryDao.get(activeOnly = true).let { activeCategories.addAll(it.reversed())
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
                PracticeTime.categoryDao.insertAndGet(newCategory)?.let {
                    activeCategories.add(0, it)
                }
                categoryAdapter.notifyItemInserted(0)
                categoryList.scrollToPosition(0)
                adjustSpanCountCatList()
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
            findViewById<CoordinatorLayout>(R.id.active_session_bottom_sheet_container).visibility = View.VISIBLE
            if (metronomeBottomSheetBehaviour.state == BottomSheetBehavior.STATE_HIDDEN) {
                metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                recordingBottomSheet.scrollY = 0
            } else {
                metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        bpbView.text = mService.metronomeBeatsPerBar.toString()
        cpbView.text = mService.metronomeClicksPerBeat.toString()

        syncUIToNewBPM()
    }

    private var slowlyFlag = false

    private fun syncUIToNewBPM() {
        if(mService.metronomeBeatsPerMinute == mService.metronomeMinBpm) {
            slowlyFlag = true
            Handler(Looper.getMainLooper()).postDelayed({slowlyFlag = false}, 300)
        } else if(mService.metronomeBeatsPerMinute == mService.metronomeMaxBpm && slowlyFlag) {
            Toast.makeText(
                this,
                "If you can play it slowly, you can play it quickly \uD83C\uDFBB\uD83C\uDFBB\uD83D\uDC1D",
                Toast.LENGTH_LONG
            ).show()
        }
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

    data class Recording (
        val id: Long,
        val displayName: String,
        val duration: String,
        val contentUri: Uri
    )

    private val recordingList = mutableListOf<Recording>()
    private val recordingListAdapter = RecordingListAdapter(recordingList)

    private val recordingPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if(granted.all { it.value }) {
                updateRecordingsList()
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            } else {
                Toast.makeText(this, R.string.recorder_permissions_toast, Toast.LENGTH_LONG).show()
            }
        }

    private fun requestRecorderPermissions(): Boolean {
        val permissions = arrayListOf<String>()

        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        recordingPermissions.launch(permissions.toTypedArray())
        return permissions.isEmpty()
    }

    private fun updateRecordingsList() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) return

        // load recordings from recordings directory
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
        )

        val selection = "${MediaStore.Audio.Media.ALBUM} = 'Practice Time'"

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val oldRecordingList = recordingList.toList()
        recordingList.clear()

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val duration = cursor.getString(durationColumn) as String?
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                recordingList.add(Recording(id, displayName, duration ?: "0", contentUri))
            }
        }

        // find all removed recordings
        oldRecordingList.forEachIndexed { pos, recording ->
            if(!recordingList.contains(recording)) {
                recordingListAdapter.notifyItemRemoved(pos)
            }
        }
        // find all newly inserted recordings
        recordingList.forEachIndexed { pos, newRecording ->
            if(!oldRecordingList.contains(newRecording)) {
                recordingListAdapter.notifyItemInserted(pos)
            }
        }
    }

    private val updateRecordTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.extras?.getString("DURATION")?.toInt()?.also {
                recordingTimeView.text = getDurationString(it / 1000, TIME_FORMAT_HMS_DIGITAL)
                recordingTimeCsView.text = "%02d".format(it % 1000 / 10)
            }
        }
    }

    private val stopRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("BROAD","Broadcast received! $this")
            val uri = RecorderService.recordingUri ?: return
            Log.d("BROAD","Uri: $uri")
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
            }

            recordingTimeView.text = "00:00:00"
            recordingTimeCsView.text = "00"

            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DURATION,
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                cursor.moveToNext()
                val id = cursor.getLong(idColumn)
                val duration = cursor.getString(durationColumn) as String?
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                RecorderService.recordingName?.let {
                    recordingList.add(0, Recording(id, it, duration ?: "0", contentUri))
                }
                recordingListAdapter.notifyItemInserted(0)
            }

            Snackbar.make(
                findViewById(R.id.coordinator_layout_active_session),
                resources.getString(R.string.record_snackbar_message).format(
                    RecorderService.recordingName ?: "Recording"
                ),
                10000
            ).apply {
                anchorView = findViewById<ConstraintLayout>(R.id.constraintLayout_Bottom_btn)
                setAction(R.string.record_snackbar_undo) {
                    contentResolver.delete(uri, null, null)
                    recordingList.removeAt(0)
                    recordingListAdapter.notifyItemRemoved(0)
                    dismiss()
                    Toast.makeText(
                        this@ActiveSessionActivity,
                        R.string.record_toast_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                show()
            }
            RecorderService.recordingUri = null
            RecorderService.recordingName = null
        }
    }

    private fun initRecordBottomSheet() {
        /* Find all views in bottom sheet */
        recordingTimeView = findViewById(R.id.record_sheet_recording_time)
        recordingTimeCsView = findViewById(R.id.record_sheet_recording_time_cs)

        recordingStartStopButtonView = findViewById(R.id.record_sheet_start_pause)

        val openRecorderButtonView = findViewById<MaterialButton>(R.id.bottom_record)

        /* Initialize local variables */
        val recordingNameFormat = SimpleDateFormat("_dd-MM-yyyy'T'H_mm_ss", Locale.getDefault())

        var recordingButtonLocked = false

        /* Modify views */
        recordingBottomSheet = findViewById(R.id.record_sheet_layout)
        recordingBottomSheetBehaviour = BottomSheetBehavior.from(recordingBottomSheet)

        recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        recordingTimeView.text = "00:00:00"
        recordingTimeCsView.text = "00"

        if(RecorderService.recording) {
            recordingStartStopButtonView.icon = ContextCompat.getDrawable(this, R.drawable.ic_stop)
            recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            openRecorderButtonView.isSelected = true
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
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    val displayName = (if(mService.sectionBuffer.isNotEmpty()) mService.currCategoryName
                    else "PracticeTime") + recordingNameFormat.format(Date().time)

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Audio.Media.TITLE, displayName)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.ALBUM, "Practice Time")
                        put(MediaStore.Audio.Media.DATE_ADDED, getCurrTimestamp())
                        put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    }

                    try {
                        val newRecordingUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Practice Time")
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, true)
                            when {
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                                    contentValues.put(MediaStore.Audio.Media.GENRE, "Recording")
                                }
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                    contentValues.put(MediaStore.Audio.Media.IS_RECORDING, 1)
                                }
                            }

                            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                        } else {
                            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) return@setOnClickListener
                            val ptDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/Practice Time")
                            if (!ptDir.exists()) {
                                ptDir.mkdirs()
                            }
                            val newRecordingFile = File(ptDir, displayName)
                            contentValues.put(MediaStore.Audio.Media.DATA, newRecordingFile.absolutePath)
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }

                        contentResolver.insert(newRecordingUri, contentValues)?.also { uri ->
                            Intent(this, RecorderService::class.java).also {
                                it.putExtra("URI", uri.toString())
                                startService(it)
                            }
                        } ?: throw IOException("Couldn't create MediaStore entry")

                        RecorderService.recordingName = displayName

                        val startToStop = ContextCompat.getDrawable(
                            this, R.drawable.avd_start_to_stop
                        ) as AnimatedVectorDrawable

                        recordingStartStopButtonView.icon = startToStop
                        startToStop.start()
                        openRecorderButtonView.isSelected = true
                    } catch(e: IOException) {
                        e.printStackTrace()
                    }
                }
            } else {
                openRecorderButtonView.isSelected = false
                val stopToStart = ContextCompat.getDrawable(
                    this, R.drawable.avd_stop_to_start
                ) as AnimatedVectorDrawable

                recordingStartStopButtonView.icon = stopToStart
                stopToStart.start()

                Intent(this, RecorderService::class.java).also {
                    stopService(it)
                }
            }
        }

        findViewById<RecyclerView>(R.id.record_sheet_list).apply {
            layoutManager = LinearLayoutManager(this@ActiveSessionActivity)
            adapter = recordingListAdapter
        }

        findViewById<MaterialButton>(R.id.bottom_record).setOnClickListener {
            findViewById<CoordinatorLayout>(R.id.active_session_bottom_sheet_container).visibility = View.VISIBLE
            if(recordingBottomSheetBehaviour.state == BottomSheetBehavior.STATE_HIDDEN) {
                metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                if(requestRecorderPermissions()) {
                    recordingBottomSheetBehaviour.halfExpandedRatio = 0.34F
                    recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
            } else {
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                recordingBottomSheet.scrollY = 0
            }
        }
    }

    private inner class RecordingListAdapter(
        private val recordings: List<Recording>
    ) : RecyclerView.Adapter<RecordingListAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_recorder, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val recording = recordings[position]
            holder.apply {
                fileNameView.text = recording.displayName
                fileLengthView.text = getDurationString((recording.duration.toIntOrNull() ?: 0) / 1000, TIME_FORMAT_HMS_DIGITAL)
                divider.visibility = if(recording == recordings.last()) View.GONE else View.VISIBLE
                openFileView.setOnClickListener {
                    Log.d("CLICK", "${recording.contentUri}")
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(recording.contentUri, "audio/mpeg")
                    startActivity(intent)
                }
            }
        }

        override fun getItemCount() = recordings.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fileNameView: TextView = view.findViewById(R.id.recording_file_name)
            val fileLengthView: TextView = view.findViewById(R.id.recording_file_length)
            val openFileView: MaterialButton = view.findViewById(R.id.recording_file_open)
            val divider: View = view.findViewById(R.id.recording_file_divider)

            init {
                fileNameView.apply {
                    ellipsize = TextUtils.TruncateAt.MARQUEE
                    isSingleLine = true
                    marqueeRepeatLimit = -1 // minus one repeats indefinitely
                    isSelected = true
                }
            }
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
            lifecycleScope.launch { updateGoals() }
        } else if (mService.sectionBuffer.last().let {         // when session is running, don't allow starting if...
            (categoryId == it.first.categoryId) ||           // ... in the same category
            (it.first.duration ?: 0 - it.second < 1)           // ... section running for less than 1sec
        }) {
            return  // ignore press then
        }

        // start a new section for the chosen category
        mService.startNewSection(categoryId, activeCategories[index].name)

        updateActiveSectionView()
        adaptBottomTextView(true)

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
                category.id == mService.sectionBuffer.last().first.categoryId
            }?.name
            sName.text = categoryName

            var sectionDur: Int
            mService.sectionBuffer.last().apply {
                sectionDur = (first.duration ?: 0).minus(second)
            }
            sDur.text = getDurationString(sectionDur, TIME_FORMAT_HMS_DIGITAL)
        }
    }

    /**
     * This function adapts all UI elements which change by pausing depending on paused
     * It can be called whenever required to sync the UI with the data in the service
     */
    private fun adaptUIPausedState(paused: Boolean) {
        if (paused) {
            // swap pause icon with play icon
            findViewById<MaterialButton>(R.id.bottom_pause).setIconResource(R.drawable.ic_play)
            // show the pause counter
            findViewById<TextView>(R.id.fab_info_popup).visibility = View.VISIBLE
            showOverlay()
        } else {
            // swap play icon with pause icon
            findViewById<MaterialButton>(R.id.bottom_pause).setIconResource(R.drawable.ic_pause)
            // hide the pause counter and reset to zero
            findViewById<TextView>(R.id.fab_info_popup).apply {
                visibility = View.GONE
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
            duration = 300
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
            duration = 300
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


    /**
     * Adapt the bottom text views to show relevant info to the user:
     * - before session start: Quote (or hint, if first session ever). After 7s, show hint
     * - after session start: None (or hint to add new section, if first session)
     * - after chosing second cat: None (or hin to delete section, if first session)
     */
    private fun adaptBottomTextView(sessionActive: Boolean) {

        val tvHint = findViewById<TextView>(R.id.activity_active_session_tv_bottom)
        val tvQuote = findViewById<TextView>(R.id.tv_quote)

        tvHint.visibility = View.GONE
        tvQuote.visibility = View.GONE

        if (!sessionActive) {
            if (PracticeTime.noSessionsYet) {
                tvHint.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.hint_start_new_session)
                }
            } else {
                tvQuote.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!PracticeTime.serviceIsRunning)
                        tvHint.visibility = View.VISIBLE
                    tvQuote.visibility = View.GONE
                }, 11000)
            }
        } else {
            // Session active
            if (PracticeTime.noSessionsYet) {
                when (mService.sectionBuffer.size) {
                    1 -> {
                        tvHint.visibility = View.VISIBLE    // must be set separately otherwise it will blink on size >2
                        tvHint.text = getString(R.string.hint_add_new_section)
                    }
                    2 -> {
                        tvHint.visibility = View.VISIBLE
                        tvHint.text = getString(R.string.hint_delete_section)
                    }
                }
            }
        }
    }

    private fun finishSession(rating: Int, comment: String?) {

        // remove time the "stop session" AlertDialog was shown from last section
        mService.subtractStopDialogTime()

        // get total break duration
        var totalBreakDuration = 0
        mService.sectionBuffer.forEach {
            totalBreakDuration += it.second
        }

        val newSession = Session(
            totalBreakDuration,
            rating,
            if(comment.isNullOrBlank()) "" else comment,
        )

        lifecycleScope.launch {
            // traverse all sections for post-processing before writing into db
            for (section in mService.sectionBuffer) {
                // update section durations to exclude break durations
                section.first.duration = section.first.duration?.minus(section.second)
            }

            // and insert it the resulting section list into the database together with the session
            val sessionId = PracticeTime.sessionDao.insertSessionWithSections(
                SessionWithSections(
                    session = newSession,
                    sections = mService.sectionBuffer.map { it.first }
                )
            )

            // reset section buffer and session status
            mService.sectionBuffer.clear()
            // refresh the adapter otherwise the app will crash because of "inconsistency detected"
            findViewById<RecyclerView>(R.id.currentSections).adapter = sectionsListAdapter
            exitActivity(sessionId)
        }
    }

    private fun exitActivity(sessionId: Long) {
        // stop the service
        Intent(this, SessionForegroundService::class.java).also {
            stopService(it)
        }
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, ProgressUpdateActivity::class.java)
        val pBundle = Bundle()
        pBundle.putLong("KEY_SESSION", sessionId)
        intent.putExtras(pBundle)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private fun initEndSessionDialog() {
        // instantiate the builder for the alert dialog
        val endSessionDialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_view_end_session, null)

        val dialogRatingBar = dialogView.findViewById<NonDraggableRatingBar>(R.id.dialogRatingBar)
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
                    super.onBackPressed()
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
            // make discard option visible
            findViewById<ImageButton>(R.id.btn_discard).visibility = View.VISIBLE
            val fabInfoPause = findViewById<TextView>(R.id.fab_info_popup)
            // load the current section from the sectionBuffer
            if (mService.paused) {
                // display pause duration on the fab, but only time after pause was activated
                fabInfoPause.text = getString(
                    R.string.pause_durationstring,
                    getDurationString(mService.pauseDuration, TIME_FORMAT_HMS_DIGITAL)
                )
            }
            practiceTimeView.text = getDurationString(mService.totalPracticeDuration, TIME_FORMAT_HMS_DIGITAL)
            updateActiveSectionView()
        } else {
            practiceTimeView.text = "00:00:00"
            findViewById<TextView>(R.id.activity_active_session_tv_bottom).text = getString(R.string.hint_start_new_session)
        }
    }

    /**
     * Adapter for SectionList RecyclerView.
     */
    private inner class SectionsListAdapter(
        // TODO this should be a list of SectionWithCategories or a custom data class
        private val sections: ArrayList<Pair<Section, Int>>,
    ) : RecyclerView.Adapter<SectionsListAdapter.ViewHolder>() {

        private val VIEW_TYPE_HEADER = 1
        private val VIEW_TYPE_GOAL = 2

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sectionName: TextView = view.findViewById(R.id.sectionName)
            val sectionDuration: TextView = view.findViewById(R.id.sectionDuration)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.listitem_active_session_section, viewGroup, false)

            // don't display the most recent item (=last item in sectionBuffer) because we have a TextView for it
            if (viewType == VIEW_TYPE_HEADER) {
                view.layoutParams.height = 0
            }
            return ViewHolder(view)
        }

        override fun getItemViewType(position: Int): Int {
            return when (position) {
                sections.size-1 -> VIEW_TYPE_HEADER
                else -> VIEW_TYPE_GOAL
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            // immediately return on uppermost view because its height is 0 anyways
            if (position == sections.size-1) {
                return
            }

            // Get element from your dataset at this position
            val categoryName = activeCategories.find { category ->
                category.id == sections[position].first.categoryId
            }?.name

            // calculate duration of each session (minus pauses)
            var sectionDuration: Int
            sections[position].apply {
                sectionDuration = (first.duration ?: 0).minus(second)
            }

            // contents of the view with that element
            viewHolder.sectionName.text = categoryName
            viewHolder.sectionDuration.text = getDurationString(sectionDuration, TIME_FORMAT_HMS_DIGITAL)

        }

        // Return the size of your dataset (invoked by the layout manager)
        // -1 because don't include most recent item (we have a dedicated TextView for that)
        override fun getItemCount() = sections.size

        /**
         * called when section is removed by swipe. Remove it from the list and notify adapter
         */
        fun removeAt(index: Int) {
            val elem = sections[index]
            sections.removeAt(index)   // items is a MutableList
            notifyItemRemoved(index)

            Snackbar.make(
                findViewById(R.id.coordinator_layout_active_session),
                getString(R.string.item_removed),
                4000
            ).apply {
                setAction(R.string.undo) {
                    sections.add(index, elem)
                    notifyItemInserted(index)
                    this.dismiss()
                }
                anchorView = findViewById<ConstraintLayout>(R.id.constraintLayout_Bottom_btn)
                show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateRecordTimeReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopRecordingReceiver)
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