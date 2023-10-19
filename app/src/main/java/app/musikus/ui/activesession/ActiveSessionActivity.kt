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

package app.musikus.ui.activesession

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.transition.Fade
import androidx.transition.TransitionManager
import app.musikus.Musikus
import app.musikus.R
import app.musikus.components.NonDraggableRatingBar
import app.musikus.database.PTDatabase
import app.musikus.database.SessionWithSections
import app.musikus.database.entities.LibraryItem
import app.musikus.database.entities.Section
import app.musikus.database.entities.Session
import app.musikus.services.RecorderService
import app.musikus.services.SessionForegroundService
import app.musikus.ui.MainActivity
import app.musikus.ui.goals.updateGoals
import app.musikus.ui.library.LibraryItemAdapter
import app.musikus.ui.library.LibraryItemDialog
import app.musikus.utils.TIME_FORMAT_HMS_DIGITAL
import app.musikus.utils.TIME_FORMAT_MS_DIGITAL
import app.musikus.utils.getCurrTimestamp
import app.musikus.utils.getDurationString
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.round


class ActiveSessionActivity : AppCompatActivity() {

    private var activeLibraryItems = ArrayList<LibraryItem>()

    private var addLibraryItemDialog: LibraryItemDialog? = null
    private var discardSessionDialog: AlertDialog? = null

    private lateinit var sectionsListAdapter: SectionsListAdapter

    private lateinit var mService: SessionForegroundService
    private var mBound: Boolean = false
    private var stopDialogShown = false

    private var quote: CharSequence = ""

    private lateinit var metronomeBottomSheet: LinearLayout
    private lateinit var metronomeBottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

    private lateinit var recordingBottomSheet: RecyclerView
    private lateinit var recordingBottomSheetBehaviour: BottomSheetBehavior<RecyclerView>

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

            // initialize persistent bottom sheet dialog for metronome
            initMetronomeBottomSheet()
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
            stopRecordingReceiver,
            IntentFilter("RecordingStopped")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_session)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        practiceTimer()

        // initialize adapter and recyclerView for showing libraryItem buttons from database
        initLibraryItemList()

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
        quote = Musikus.getRandomQuote(this)
        findViewById<TextView>(R.id.tv_overlay_pause).text = quote
        findViewById<TextView>(R.id.tv_quote).text = quote
    }

    override fun onRestart() {
        super.onRestart()
        updateRecordingsList()
    }

    private fun initLibraryItemList() {
        val libraryItemAdapter = LibraryItemAdapter(
            activeLibraryItems,
            context = this,
            showInActiveSession = true,
            shortClickHandler = ::libraryItemPressed,
            addLibraryItemHandler = { addLibraryItemDialog?.show() }
        )

        val libraryItemList = findViewById<RecyclerView>(R.id.libraryItemList)

        val rowNums = 3

        libraryItemList.apply {
            layoutManager = GridLayoutManager(
                context,
                rowNums,
                GridLayoutManager.HORIZONTAL,
                false
            )
            adapter = libraryItemAdapter
            itemAnimator?.apply {
//                addDuration = 200L
//                moveDuration = 500L
//                removeDuration = 200L
            }
        }

        // load all active libraryItems from the database and notify the adapter
        lifecycleScope.launch {
            PTDatabase.getInstance(applicationContext).libraryItemDao.getAsFlow(activeOnly = true).first().let { activeLibraryItems.addAll(it.reversed())
                libraryItemAdapter.notifyItemRangeInserted(0, it.size)
            }
            libraryItemList.apply {
                scrollToPosition(0)
                visibility = View.VISIBLE
            }

            adjustSpanCountCatList()
        }

        // the handler for creating new libraryItems
        fun addLibraryItemHandler(newLibraryItem: LibraryItem) {
            lifecycleScope.launch {
                PTDatabase.getInstance(applicationContext).libraryItemDao.insert(newLibraryItem)
                activeLibraryItems.add(0, newLibraryItem)
                libraryItemAdapter.notifyItemInserted(0)
                libraryItemList.scrollToPosition(0)
                adjustSpanCountCatList()
            }
        }

        // create a new library item dialog for adding new libraryItems
        addLibraryItemDialog = LibraryItemDialog(this, ::addLibraryItemHandler)
    }

    private fun adjustSpanCountCatList() {
        // change the span count if only few libraryItems are displayed
        val l = findViewById<RecyclerView>(R.id.libraryItemList).layoutManager as GridLayoutManager
        l.spanCount = when {
            (activeLibraryItems.size < 3) -> 1
            (activeLibraryItems.size < 6) -> 2
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
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                recordingBottomSheet.scrollToPosition(0)
                metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
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

    private lateinit var openRecorderButtonView: MaterialButton

    data class Recording (
        val id: Long,
        val displayName: String,
        val duration: Int,
        val date: Long,
        val contentUri: Uri,
        var playing: Boolean = false
    )

    private val recordingList = mutableListOf<Recording>()
    private val recordingListAdapter = RecordingListAdapter(
        recordingList,
        this,
        ::startRecording,
        ::stopRecording
    )

    private val updateRecordTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            intent?.extras?.getString("DURATION")?.toInt()?.also {
//                recordingTimeView.text = getDurationString(it / 1000, TIME_FORMAT_HMS_DIGITAL)
//                recordingTimeCsView.text = "%02d".format(it % 1000 / 10)
//            }
        }
    }

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
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.ALBUM} = 'Practice Time'"

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} ASC"

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
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val duration = cursor.getString(durationColumn).toIntOrNull() ?: 0
                val date = (cursor.getString(dateColumn).toLongOrNull() ?: 0L) * 1000L
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                recordingList.add(0, Recording(id, displayName, duration, date, contentUri))
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

    private val stopRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val uri = RecorderService.recordingUri ?: return
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
            }
//
//            recordingTimeView.text = "00:00:00"
//            recordingTimeCsView.text = "00"

            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                cursor.moveToNext()
                val id = cursor.getLong(idColumn)
                val duration = cursor.getString(durationColumn).toIntOrNull() ?: 0
                val date = (cursor.getString(dateColumn).toLongOrNull() ?: 0L) * 1000L
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                RecorderService.recordingName?.let {
                    recordingList.add(0, Recording(id, it, duration, date,  contentUri))
                    recordingListAdapter.addRecording()
                }
            }

            Snackbar.make(
                findViewById(R.id.coordinator_layout_active_session),
                resources.getString(R.string.record_snackbar_message).format(
                    RecorderService.recordingName ?: "Recording"
                ),
                10000
            ).apply {
                anchorView =
                    if(recordingBottomSheetBehaviour.state != BottomSheetBehavior.STATE_EXPANDED)
                        recordingBottomSheet
                    else findViewById<ConstraintLayout>(R.id.constraintLayout_Bottom_btn)

                setAction(R.string.record_snackbar_undo) {
                    contentResolver.delete(uri, null, null)
                    recordingList.removeAt(0)
                    recordingListAdapter.deleteRecording()
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

    private fun startRecording() : Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(Array(1) { Manifest.permission.RECORD_AUDIO }, 69)
            return false
        } else {
            val recordingNameFormat = SimpleDateFormat("_dd-MM-yyyy'T'H_mm_ss", Locale.getDefault())

            val displayName = (if(mService.sectionBuffer.isNotEmpty()) mService.currLibraryItemName
            else "Musikus") + recordingNameFormat.format(Date().time)

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
                        != PackageManager.PERMISSION_GRANTED) return false
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
                openRecorderButtonView.isSelected = true

            } catch(e: IOException) {
                e.printStackTrace()
                return false
            }
            return true
        }
    }

    private fun stopRecording() {
        openRecorderButtonView.isSelected = false
        Intent(this, RecorderService::class.java).also {
            stopService(it)
        }
    }

    private fun initRecordBottomSheet() {

        openRecorderButtonView = findViewById(R.id.bottom_record)

        /* Modify views */
        recordingBottomSheet = findViewById(R.id.record_sheet_list)
        recordingBottomSheetBehaviour = BottomSheetBehavior.from(recordingBottomSheet)

        recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        recordingBottomSheetBehaviour.halfExpandedRatio = 0.34F

        if(RecorderService.recording) {
            recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            openRecorderButtonView.isSelected = true
        }

        // load recordings from recordings directory
        findViewById<RecyclerView>(R.id.record_sheet_list).apply {
            layoutManager = LinearLayoutManager(this@ActiveSessionActivity)
            adapter = recordingListAdapter
            itemAnimator?.changeDuration = 1L
            setHasFixedSize(false)
        }

        updateRecordingsList()

        findViewById<MaterialButton>(R.id.bottom_record).setOnClickListener {
            findViewById<CoordinatorLayout>(R.id.active_session_bottom_sheet_container).visibility = View.VISIBLE
            if(recordingBottomSheetBehaviour.state == BottomSheetBehavior.STATE_HIDDEN) {
                metronomeBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                if(requestRecorderPermissions()) {
                    recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
            } else {
                recordingBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                recordingBottomSheet.scrollToPosition(0)
            }
        }
    }


    class FrontFragment(
        private val recording: Recording
    ) : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = inflater.inflate(R.layout.fragment_recording_front, container, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val timeAgo = when(val timeDiffInMins = ((Date().time - recording.date) / 60_000)) {
                0L -> "Just now"
                in 1..59 -> "$timeDiffInMins minutes ago"
                in 60..1439 -> "${timeDiffInMins / 60} hours ago"
                else -> "${timeDiffInMins / 1440} days ago"
            }

            view.apply {
                findViewById<TextView>(R.id.recording_file_name).text = recording.displayName
                findViewById<TextView>(R.id.recording_file_date).text = timeAgo
                findViewById<TextView>(R.id.recording_file_length).text = getDurationString(
                    recording.duration / 1000,
                    format = TIME_FORMAT_MS_DIGITAL
                )
            }
        }
    }

    class BackFragment(
        private val mediaPlayer: MediaPlayer,
    ) : Fragment() {

        private lateinit var timePlayedView: TextView
        private lateinit var timeLeftView: TextView
        private lateinit var playBarView: SeekBar

        private fun syncUIToPlayTime() {
            timePlayedView.text = getDurationString(
                mediaPlayer.currentPosition / 1000,
                format = TIME_FORMAT_MS_DIGITAL
            )

            timeLeftView.text = getDurationString(
                (mediaPlayer.duration / 1000 -  mediaPlayer.currentPosition / 1000 ),
                format = TIME_FORMAT_MS_DIGITAL
            )
            playBarView.progress = mediaPlayer.currentPosition / 10
        }

        private val uiHandler = Handler(Looper.getMainLooper())

        private val recordingTracker = object : Runnable {
            override fun run() {
                syncUIToPlayTime()
                uiHandler.postDelayed(this, 10)
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = inflater.inflate(R.layout.fragment_recording_back, container, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            playBarView = view.findViewById(R.id.recording_playbar)
            playBarView.apply {
                max = mediaPlayer.duration / 10
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if(fromUser) {
                            mediaPlayer.seekTo(progress * 10)
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            timePlayedView = view.findViewById(R.id.recording_time_played)
            timeLeftView = view.findViewById(R.id.recording_time_left)
            uiHandler.post(recordingTracker)
        }

        fun stopUISync() {
            uiHandler.removeCallbacks(recordingTracker)
        }

        override fun onDestroy() {
            super.onDestroy()
            uiHandler.removeCallbacks(recordingTracker)
        }
    }

    private class RecordingListAdapter(
        private val recordings: List<Recording>,
        private val context: AppCompatActivity,
        private var startRecordingHandler: () -> Boolean,
        private var stopRecordingHandler: () -> Unit,
        ) : RecyclerView.Adapter<RecordingListAdapter.ViewHolder>() {

        companion object {
            private const val VIEW_TYPE_ITEM = 1
            private const val VIEW_TYPE_HEADER = 2
        }

        private var playingRecording = -1

        private val mediaPlayer = MediaPlayer()

        private lateinit var recyclerView: RecyclerView

        init {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
        }

        fun deleteRecording() {
            this.notifyItemRemoved(1)
            if(playingRecording != -1) {
                playingRecording--
            }
        }

        fun addRecording() {
            this.notifyItemInserted(1)
            if(playingRecording != -1) {
                playingRecording++
            }
        }

        fun setPlayingRecording(newPlayingRecording: Int) {
            // there is a bug here with recycler view for elements which are recycled but also not visible when new Recording is played
            this.recyclerView.findViewHolderForAdapterPosition(playingRecording + 1)?.apply {
                if (this !is ViewHolder.ItemViewHolder) return@apply
                backFragment?.stopUISync()
                frontFragment?.let { fFragment ->
                    context.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.animator.flip_in,
                            R.animator.flip_out,
                        )
                        .replace(containerView.id, fFragment)
                        .commitNow()
                }
                playerShowing = false
                playFileView.icon = pauseToPlay
                pauseToPlay.start()
            }
            playingRecording = newPlayingRecording
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            this.recyclerView = recyclerView
        }

        override fun getItemCount() = recordings.size + 1

        override fun getItemViewType(position: Int) =
            if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when(viewType) {
                VIEW_TYPE_ITEM -> ViewHolder.ItemViewHolder(
                    inflater.inflate(R.layout.listitem_recording, parent, false),
                    context,
                    ::setPlayingRecording,
                )
                else -> ViewHolder.HeaderViewHolder(
                    inflater
                        .inflate(R.layout.listitem_recording_header, parent, false),
                    context,
                    startRecordingHandler,
                    stopRecordingHandler,
                )
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (holder) {
                is ViewHolder.ItemViewHolder -> {
                    holder.bind(
                        recordings[position-1],
                        mediaPlayer,
                        playingRecording == position - 1,
                        position == recordings.size,
                    )
                }
                is ViewHolder.HeaderViewHolder -> holder.bind()
            }
        }

        override fun onViewAttachedToWindow(holder: ViewHolder) {
            when(holder) {
                is ViewHolder.ItemViewHolder -> {
                    (if (holder.playerShowing) holder.backFragment else holder.frontFragment)?.let { fragment ->
                        context.supportFragmentManager.beginTransaction().apply {
                            replace(holder.containerView.id, fragment)
                            commit()
                        }
                    }
                }
                is ViewHolder.HeaderViewHolder -> {
                    holder.registerReceiver()
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: ViewHolder) {
            when(holder) {
                is ViewHolder.HeaderViewHolder -> holder.unregisterReceiver()
                else -> {}
            }
        }

        sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            class HeaderViewHolder(
                view: View,
                private val context: AppCompatActivity,
                private val startRecordingHandler: () -> Boolean,
                private val stopRecordingHandler: () -> Unit,
            ) : ViewHolder(view) {

                private val recordingTimeView = view.findViewById<TextView>(R.id.record_sheet_recording_time)
                private val recordingTimeCsView = view.findViewById<TextView>(R.id.record_sheet_recording_time_cs)

                private val recordingStartStopButtonView = view.findViewById<MaterialButton>(R.id.record_sheet_start_pause)

                /* Initialize local variables */
                var recordingButtonLocked = false

                private val updateRecordTimeReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        intent?.extras?.getString("DURATION")?.toInt()?.also {
                            recordingTimeView.text = getDurationString(it / 1000, format = TIME_FORMAT_HMS_DIGITAL)
                            recordingTimeCsView.text = "%02d".format(it % 1000 / 10)
                        }
                    }
                }

                fun registerReceiver() {
                    LocalBroadcastManager.getInstance(context).registerReceiver(
                        updateRecordTimeReceiver,
                        IntentFilter("RecordingDurationUpdate")
                    )
                }

                fun unregisterReceiver() {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(
                        updateRecordTimeReceiver,
                    )
                }

                fun bind() {
                    recordingStartStopButtonView.icon = ContextCompat.getDrawable(
                        context,
                        if(RecorderService.recording) R.drawable.ic_stop else R.drawable.ic_record
                    )

                    recordingStartStopButtonView.setOnClickListener {
                        // Return if button is still locked
                        if (recordingButtonLocked) return@setOnClickListener

                        // if not, lock it for one second to avoid double presses
                        recordingButtonLocked = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            recordingButtonLocked = false
                        }, 1000L)

                        if(!RecorderService.recording) {
                            if (startRecordingHandler()) {
                            val startToStop = ContextCompat.getDrawable(
                                context, R.drawable.avd_start_to_stop
                            ) as AnimatedVectorDrawable

                            recordingStartStopButtonView.icon = startToStop
                            startToStop.start()
                            }
                        } else {
                            stopRecordingHandler()
                            val stopToStart = ContextCompat.getDrawable(
                                context, R.drawable.avd_stop_to_start
                            ) as AnimatedVectorDrawable

                            recordingStartStopButtonView.icon = stopToStart
                            stopToStart.start()
                        }
                    }
                }
            }

            class ItemViewHolder(
                view: View,
                private val context: AppCompatActivity,
                private val setPlayingRecording: (new: Int) -> Unit,
            ) : ViewHolder(view) {
                var frontFragment: FrontFragment? = null
                var backFragment: BackFragment? = null

                var playerShowing = false

                val playFileView: MaterialButton = view.findViewById(R.id.recording_file_open)
                val containerView: FrameLayout = view.findViewById(R.id.recording_container)
                private val dividerView: View = view.findViewById(R.id.recording_file_divider)

                private val pause = ContextCompat.getDrawable(context, R.drawable.ic_pause)
                private val play = ContextCompat.getDrawable(context, R.drawable.ic_play)

                private val playToPause = ContextCompat.getDrawable(
                context, R.drawable.avd_play_to_pause
                ) as AnimatedVectorDrawable
                val pauseToPlay = ContextCompat.getDrawable(
                    context, R.drawable.avd_pause_to_play
                ) as AnimatedVectorDrawable


                init {
                    containerView.id = View.generateViewId()
                }

                fun bind(recording: Recording, mediaPlayer: MediaPlayer, playerShowing: Boolean, lastItem: Boolean) {
                    this.playerShowing = playerShowing

                    frontFragment = FrontFragment(recording)
                    backFragment = BackFragment(mediaPlayer)

                    playFileView.icon = if(playerShowing && mediaPlayer.isPlaying) pause else play
                    playFileView.setOnClickListener {
                        if(this.playerShowing) {
                            if(mediaPlayer.isPlaying) {
                                playFileView.icon = pauseToPlay
                                pauseToPlay.start()
                                mediaPlayer.pause()
                            } else {
                                if(mediaPlayer.currentPosition == mediaPlayer.duration)
                                    mediaPlayer.seekTo(0)
                                playFileView.icon = playToPause
                                playToPause.start()
                                mediaPlayer.start()
                            }
                        } else {
                            mediaPlayer.apply {
                                reset()
                                setDataSource(context, recording.contentUri)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    playFileView.icon = play
                                }
                            }

                            backFragment?.let { backFragment ->
                                context.supportFragmentManager.beginTransaction()
                                    .setCustomAnimations(
                                        R.animator.flip_in,
                                        R.animator.flip_out,
                                    )
                                    .replace(containerView.id, backFragment)
                                    .commitNow()
                            }
                            setPlayingRecording(layoutPosition - 1)

                            this.playerShowing = true
                            playFileView.icon = playToPause
                            playToPause.start()
                        }
                    }
                    dividerView.visibility = if(lastItem) View.GONE else View.VISIBLE
                }
            }
        }
    }

    /*********************************************
     *  Timing stuff
     ********************************************/

    // the routine for handling presses to library item buttons
    private fun libraryItemPressed(index: Int) {
        val libraryItemId = activeLibraryItems[index].id

        if (!mService.sessionActive) {   // session starts now

            //start the service so that timer starts
            Intent(this, SessionForegroundService::class.java).also {
                startService(it)
            }
            setPauseStopBtnVisibility(true)

            // when the session start, also update the goals
            lifecycleScope.launch { updateGoals(applicationContext) }
        } else if (mService.sectionBuffer.last().let {         // when session is running, don't allow starting if...
                (libraryItemId == it.first.libraryItemId) ||   // ... in the same library item
                        ((it.first.duration
                            ?: (0 - it.second)) < 1)           // ... section running for less than 1sec
        }) {
            return  // ignore press then
        }

        // start a new section for the chosen library item
        mService.startNewSection(libraryItemId, activeLibraryItems[index].name)

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
            val libraryItemName = activeLibraryItems.find { libraryItem ->
                libraryItem.id == mService.sectionBuffer.last().first.libraryItemId
            }?.name
            sName.text = libraryItemName

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
            if (Musikus.noSessionsYet) {
                tvHint.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.hint_start_new_session)
                }
            } else {
                tvQuote.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!Musikus.serviceIsRunning)
                        tvHint.visibility = View.VISIBLE
                    tvQuote.visibility = View.GONE
                }, 11000)
            }
        } else {
            // Session active
            if (Musikus.noSessionsYet) {
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
            PTDatabase.getInstance(applicationContext).sessionDao.insert(
                SessionWithSections(
                    session = newSession,
                    sections = mService.sectionBuffer.map { it.first }
                )
            )

            // reset section buffer and session status
            mService.sectionBuffer.clear()
            // refresh the adapter otherwise the app will crash because of "inconsistency detected"
            findViewById<RecyclerView>(R.id.currentSections).adapter = sectionsListAdapter
            exitActivity(newSession.id)
        }
    }

    private fun exitActivity(sessionId: UUID) {
        // stop the service
        Intent(this, SessionForegroundService::class.java).also {
            stopService(it)
        }
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, MainActivity::class.java)
        val pBundle = Bundle()
//        pBundle.putLong("KEY_SESSION", sessionId)
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
        // TODO this should be a list of SectionWithLibraryItems or a custom data class
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
            val libraryItemName = activeLibraryItems.find { libraryItem ->
                libraryItem.id == sections[position].first.libraryItemId
            }?.name

            // calculate duration of each session (minus pauses)
            var sectionDuration: Int
            sections[position].apply {
                sectionDuration = (first.duration ?: 0).minus(second)
            }

            // contents of the view with that element
            viewHolder.sectionName.text = libraryItemName
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
