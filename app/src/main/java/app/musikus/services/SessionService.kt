/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 */

package app.musikus.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.musikus.R
import app.musikus.SESSION_NOTIFICATION_CHANNEL_ID
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.activesession.ActiveSessionActions
import app.musikus.ui.activesession.PracticeSection
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import app.musikus.utils.minus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


const val SESSION_NOTIFICATION_ID = 42
const val BROADCAST_INTENT_FILTER = "activeSessionAction"


/**
 * Encapsulates the state of the current session, is passed to the binding Activity / ViewModel
 */
data class SessionServiceState(
    val sections: List<PracticeSection> = emptyList(),      // list of all sections
    val currentSectionDuration: Duration = 0.seconds,       // duration of the current section WITHOUT pauses
    val currentSectionPauseDuration: Duration = 0.seconds,  // total duration of pauses in the current section
    val totalPauseDuration: Duration = 0.seconds,           // total duration of all pauses, including ongoing
    val ongoingPauseDuration: Duration = 0.seconds,         // duration of the current ongoing pause
    val isPaused: Boolean = false                           // true if the session is currently paused
)

/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class SessionServiceEvent {
    data object StopTimerAndFinish: SessionServiceEvent()
    data object TogglePause: SessionServiceEvent()
    data class StartNewSection(val item: LibraryItem): SessionServiceEvent()
    data class DeleteSection(val sectionId: Int): SessionServiceEvent()
}

/**
 * Data Structure for a Button inside the Notification
 */
data class NotificationActionButtonConfig(
    @DrawableRes val icon: Int,
    val text: String,
    val tapIntent: PendingIntent?
)

@AndroidEntryPoint
class SessionService : Service() {

    @Inject
    lateinit var timeProvider: TimeProvider

    private val binder = LocalBinder()         // interface object for clients that bind
    inner class LocalBinder : Binder() {
        // Return this instance of SessionService so clients can call public methods
        fun getServiceState() = serviceState
        fun getOnEvent(): (SessionServiceEvent) -> Unit = ::onEvent
    }

    /** Own state flows */
    private val _sections = MutableStateFlow<List<PracticeSection>>(emptyList())
    private val _isPaused = MutableStateFlow(false)
    private val _currentTickTime = MutableStateFlow<ZonedDateTime>(ZonedDateTime.now())
    // contains all pause timestamps of the current section
    private val _pausesCurrentSection = MutableStateFlow<List<Pair<ZonedDateTime, ZonedDateTime?>>>(emptyList())


    /** Derived state flows */
    private val sectionDuration = combine(
        _sections,
        _currentTickTime,
        _pausesCurrentSection
    ) { sections, currentTickTime, pauseTimes ->
        calculateCurrentSectionDuration(
            section = sections.lastOrNull(),
            now = currentTickTime,
            pauseTimestamps = pauseTimes
        ).first
    }

    private val sectionPauseDuration = _pausesCurrentSection.map {
        sumPauseTimestamps(it)
    }

    // sub-second addition to the current section duration because of rounding
    private var _subSecondOverflow = 0.seconds


    /** global variables */
    private val timerInterval = 1.seconds
    private var _timer: java.util.Timer? = null
    private var _sectionIdCounter = 0   // gives us unique ids for all sections

    /** Broadcast receiver (currently only for pause action) */
    private val myReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("action")) {
                ActiveSessionActions.PAUSE.toString() -> togglePause()
            }
        }
    }

    /** Intents */
    private var pendingIntentTapAction : PendingIntent? = null
    private var pendingIntentActionPause : PendingIntent? = null
    private var pendingIntentActionFinish : PendingIntent? = null

    /** Notification Button Configs */
    private var pauseActionButton : NotificationActionButtonConfig? = null
    private var resumeActionButton : NotificationActionButtonConfig? = null
    private var finishActionButton : NotificationActionButtonConfig? = null


    /**
     *  ---------------------- Interface for Activity / ViewModel ----------------------
     */

    // Pack the service state into a single object
    val serviceState = combine(
        _sections,
        sectionDuration,
        sectionPauseDuration,
        _isPaused,
        _pausesCurrentSection
    ) { sections, sectionDuration, sectionPauseDuration, isPaused, pausesTimestamps ->
        val totalPauseDuration = sumPauseTimestamps(pausesTimestamps) +
            _sections.value.sumOf {
                (it.pauseDuration ?: 0.seconds).inWholeMilliseconds
            }.milliseconds

        SessionServiceState(
            sections = sections,
            currentSectionDuration = sectionDuration,
            currentSectionPauseDuration = sectionPauseDuration,
            ongoingPauseDuration = getOngoingPauseDuration(),
            totalPauseDuration = totalPauseDuration,
            isPaused = isPaused
        )
    }

    fun onEvent(event: SessionServiceEvent) {
        when(event) {
            is SessionServiceEvent.StopTimerAndFinish -> stopTimerAndDestroy()
            is SessionServiceEvent.TogglePause -> togglePause()
            is SessionServiceEvent.StartNewSection -> newSection(event.item)
            is SessionServiceEvent.DeleteSection -> removeSection(event.sectionId)
        }
    }

    /**
     * ----------------------------------- private functions ------------------------------------
     */

    private fun newSection(item: LibraryItem) {
        // prevent starting a new section too fast
//        if (_sections.value.lastOrNull()?.let {
//            calculateSectionDuration(it, _currentTickTime.value) < 1.seconds
//        } == true) return
        // prevent starting the same section twice in a row
//        if (item == _sections.value.lastOrNull()?.libraryItem) return

        // snapshot current time
        val changeoverTime = timeProvider.now()
        startTimer()

        // calculate duration fields of the previous section
        if (_sections.value.isNotEmpty()) {
            val completedSection = _sections.value.last()
            val durationInfo = calculateCurrentSectionDuration(
                section = _sections.value.lastOrNull(),
                now = changeoverTime,
                pauseTimestamps = _pausesCurrentSection.value
            )
            completedSection.duration = durationInfo.first
            completedSection.pauseDuration = sumPauseTimestamps(_pausesCurrentSection.value)
            _subSecondOverflow = durationInfo.second

            _sections.update {
                it.dropLast(1) + completedSection
            }
        }

        // reset tracked pauses
        _pausesCurrentSection.update { emptyList() }

        // add the new section item
        val newItem = PracticeSection(
            id = _sectionIdCounter++,
            libraryItem = item,
            duration = null,
            pauseDuration = null,
            startTimestamp = changeoverTime
        )
        _sections.update {
            it + newItem
        }

        updateNotification()
    }

    private fun removeSection(sectionId: Int) {
        _sections.update { sectionList ->
            sectionList.filter { it.id != sectionId } }
    }

    private fun stopTimerAndDestroy() {
        _timer?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun togglePause() {
        if (_isPaused.value) {
            stopPause()
        } else {
            startPause()
        }
        updateNotification()
    }

    private fun startPause() {
        _isPaused.update { true }
        _pausesCurrentSection.update { it + Pair(timeProvider.now(), null) }
    }

    private fun stopPause() {
        _isPaused.update { false }
        _pausesCurrentSection.update {
            it.dropLast(1) + it.last().copy(second = timeProvider.now())
        }
    }

    private fun startTimer() {
        if (_timer != null) {
            return
        }
        _timer = timer(
            name = "Timer",
            initialDelay = 0,
            period = timerInterval.inWholeMilliseconds
        ) {
            _currentTickTime.update { timeProvider.now() }
            updateNotification()
        }
    }


    /**
     * Calculates the duration of the current section.
     *
     * The flow "sectionDuration" is fed from this function. But it is also accessed when the value
     * is needed for calculation (since the Flow does not provide a value directly).
     *
     * @param section current PracticeSections object. If it is null, 0.seconds is returned.
     * @param now Current time
     * @param pauseTimestamps List of all pause timestamps
     * @return Pair of the floored duration to seconds and the sub-second overflow time
     */
    private fun calculateCurrentSectionDuration(
        section: PracticeSection?,
        now: ZonedDateTime,
        pauseTimestamps: List<Pair<ZonedDateTime, ZonedDateTime?>>
    ) : Pair<Duration, Duration> {
        if(section == null) return Pair(0.seconds, 0.seconds)

        val timeDelta = now - section.startTimestamp
        val practiceDuration = timeDelta - sumPauseTimestamps(pauseTimestamps)
        val flooredPracticeDuration = practiceDuration.inWholeSeconds.seconds  // floor to seconds
        val subSecondOverflow = practiceDuration - flooredPracticeDuration
        return Pair(flooredPracticeDuration, subSecondOverflow)
    }

    /**
     * Calculates the duration of the pause which is currently ongoing.
     */
    private fun getOngoingPauseDuration() : Duration {
        val lastTimeStampPair = _pausesCurrentSection.value.lastOrNull() ?: return 0.seconds
        if (lastTimeStampPair.second != null) return 0.seconds  // no pause
        return timeProvider.now() - lastTimeStampPair.first
    }

    private fun sumPauseTimestamps(
        timestampPairs : List<Pair<ZonedDateTime, ZonedDateTime?>>
    ) : Duration {
        return timestampPairs.sumOf {
            ((it.second ?: timeProvider.now()) - it.first).inWholeMilliseconds
        }.milliseconds
    }

    private fun updateNotification() {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(SESSION_NOTIFICATION_ID, createNotification())
    }

    /**
     * Creates a notification object based on current session state
     */
    private fun createNotification() : Notification {
        val totalPracticeDuration = _sections.value.sumOf {
            val lastSectionDuration = calculateCurrentSectionDuration(
                section = it,
                now = _currentTickTime.value,
                pauseTimestamps = _pausesCurrentSection.value
            ).first
            (it.duration ?: lastSectionDuration).inWholeMilliseconds
        }.milliseconds

        val totalPracticeDurationStr =
            getDurationString(totalPracticeDuration, DurationFormat.HMS_DIGITAL)

        val currentSectionName = _sections.value.lastOrNull()?.libraryItem?.name ?: "No Section"

        val title: String
        val description: String

        if (_isPaused.value) {
            title = "Practicing Paused"
            description = "$currentSectionName - Total: $totalPracticeDurationStr"
        } else {
            title = "Practicing for $totalPracticeDurationStr"
            description = currentSectionName
        }

        val actionButton1Intent = if(_isPaused.value) {
            resumeActionButton
        } else {
            pauseActionButton
        }
        val actionButton2Intent = finishActionButton

        return getNotification(
            title = title,
            description = description,
            actionButton1 = actionButton1Intent,
            actionButton2 = actionButton2Intent
        )
    }


    /**
     *  ----------------------------------- Service Boilerplate -----------------------------------
     */

    override fun onBind(intent: Intent?): IBinder {
        // register Broadcast Receiver for Pause Action
        ContextCompat.registerReceiver(
            this,
            myReceiver,
            IntentFilter(BROADCAST_INTENT_FILTER),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        Log.d("TAG", "onUnbind")
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createPendingIntents()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(SESSION_NOTIFICATION_ID, notification)
        } else {
            ServiceCompat.startForeground(
                this,
                SESSION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        }

        return START_NOT_STICKY
    }

    private fun getNotification(
        title: String,
        description: String,
        actionButton1: NotificationActionButtonConfig?,
        actionButton2: NotificationActionButtonConfig?): Notification {

        val icon = R.drawable.ic_launcher_foreground

        val builder = NotificationCompat.Builder(this, SESSION_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(icon)    // without icon, setOngoing does not work
            .setOngoing(true)  // does not work on Android 14: https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // only relevant below Oreo, else channel priority is used
            .setContentIntent(pendingIntentTapAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (actionButton1 != null) {
            builder.addAction(actionButton1.icon, actionButton1.text, actionButton1.tapIntent)
        }
        if (actionButton2 != null) {
            builder.addAction(actionButton2.icon, actionButton2.text, actionButton2.tapIntent)
        }
        return builder.build()

    }

    /**
     * Creates all pending intents for the notification. Has to be done when Context is available.
     */
    private fun createPendingIntents() {
        // trigger deep link to open ActiveSession https://stackoverflow.com/a/72769863
        pendingIntentTapAction = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(
                Intent(Intent.ACTION_VIEW, "musikus://activeSession/${ActiveSessionActions.OPEN}".toUri())
            )
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        pendingIntentActionPause = PendingIntent.getBroadcast(
            this,
            0,
            Intent(BROADCAST_INTENT_FILTER).also { it.putExtra("action", ActiveSessionActions.PAUSE.toString()) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntentActionFinish = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(
                Intent(Intent.ACTION_VIEW, "musikus://activeSession/${ActiveSessionActions.FINISH}".toUri())
            )
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        pauseActionButton = NotificationActionButtonConfig(
            icon = R.drawable.ic_pause,
            text = "Pause",
            tapIntent = pendingIntentActionPause
        )

        resumeActionButton = NotificationActionButtonConfig(
            icon = R.drawable.ic_play,
            text = "Resume",
            tapIntent = pendingIntentActionPause
        )

        finishActionButton = NotificationActionButtonConfig(
            icon = R.drawable.ic_stop,
            text = "Finish",
            tapIntent = pendingIntentActionFinish
        )

    }

    override fun onDestroy() {
        Log.d("Tag", "onDestroy")
        unregisterReceiver(myReceiver)
        super.onDestroy()
    }
}