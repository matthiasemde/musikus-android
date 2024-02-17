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
import android.os.IBinder
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


const val NOTIFCATION_ID = 42
const val BROADCAST_INTENT_FILTER = "activeSessionAction"


/**
 * Encapsulates the state of the current session, is passed to the binding Activity / ViewModel
 */
data class SessionState(
    val sections: List<PracticeSection> = emptyList(),
    val currentSectionDuration: Duration = 0.seconds,
    val pauseDuration: Duration = 0.seconds,
    val isPaused: Boolean = false
)

/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class SessionEvent {
    data object StopTimer: SessionEvent()
    data object TogglePause: SessionEvent()
    data class StartNewSection(val item: LibraryItem): SessionEvent()
    data class DeleteSection(val sectionId: Int): SessionEvent()
}

/**
 * Data Structure for a Button inside the Notification
 */
data class NotificationActionButtonConfig(
    @DrawableRes val icon: Int,
    val text: String,
    val tapIntent: PendingIntent?
)

enum class SessionServiceAction {
    START, STOP
}

@AndroidEntryPoint
class SessionService : Service() {

    @Inject
    lateinit var timeProvider: TimeProvider

    private val binder = LocalBinder()         // interface object for clients that bind
    inner class LocalBinder : Binder() {
        // Return this instance of SessionService so clients can call public methods
        fun getSessionStateFlow() = sessionState
        fun getOnEvent(): (SessionEvent) -> Unit = ::onEvent
    }

    private val sessionState = MutableStateFlow(SessionState())  // Session State
    private var _timer: java.util.Timer? = null
    private val timerInterval = 1.seconds
    private var _sectionIdCounter = 0

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
    fun onEvent(event: SessionEvent) {
        when(event) {
            is SessionEvent.StopTimer -> stopTimer()
            is SessionEvent.TogglePause -> togglePause()
            is SessionEvent.StartNewSection -> newSection(event.item)
            is SessionEvent.DeleteSection -> removeSection(event.sectionId)
        }
    }


    /**
     * ----------------------------------- private functions ------------------------------------
     */

    private fun newSection(item: LibraryItem) {
        // prevent starting a new section too fast
        if (sessionState.value.sections.isNotEmpty() && sessionState.value.currentSectionDuration < 1.seconds) return
        // prevent starting the same section twice in a row
        if (item == sessionState.value.sections.lastOrNull()?.libraryItem) return

        startTimer()
        // end current section and store its duration

        sessionState.update {
            val finishedSectionDur = it.currentSectionDuration
            val flooredFinishedDur = finishedSectionDur.inWholeSeconds.seconds  // floor to seconds

            // update last section with tracked duration
            val updatedLastSection = it.sections.lastOrNull()?.copy(duration = flooredFinishedDur)
            val newItem = PracticeSection(
                id = _sectionIdCounter++,
                libraryItem = item,
                duration = null,
                startTimestamp = timeProvider.now()
            )
            it.copy(
                // the remaining part (sub-seconds) will be added to the new section upfront
                currentSectionDuration =  finishedSectionDur - flooredFinishedDur,
                sections =
                    if (updatedLastSection == null) {
                        listOf(newItem)   // first section started, only add new item
                    } else {
                        it.sections.dropLast(1) + updatedLastSection + newItem
                    }
            )
        }
        updateNotification()
    }

    private fun removeSection(sectionId: Int) {
        val updatedSections = sessionState.value.sections.filter { it.id != sectionId }
        sessionState.update { it.copy(sections = updatedSections) }
    }

    private fun stopTimer() {
        _timer?.cancel()
        stopSelf()
    }

    private fun togglePause() {
        sessionState.update { it.copy(isPaused = !it.isPaused) }
        updateNotification()
    }

    private fun startTimer() {
        if (_timer != null) {
            return
        }
        _timer = timer(
            name = "Timer",
            initialDelay = timerInterval.inWholeMilliseconds,
            period = timerInterval.inWholeMilliseconds
        ) {
            if (sessionState.value.isPaused) {
                sessionState.update { it.copy(
                    pauseDuration = it.pauseDuration + timerInterval)
                }
            } else {
                sessionState.update { it.copy(
                    currentSectionDuration = it.currentSectionDuration + timerInterval)
                }
            }
            updateNotification()
        }
    }

    private fun updateNotification() {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFCATION_ID, createNotification())
    }

    /**
     * Creates a notification object based on current session state
     */
    private fun createNotification() : Notification {
        val totalPracticeDuration = sessionState.value.sections.sumOf {
            (it.duration ?: sessionState.value.currentSectionDuration).inWholeMilliseconds }.milliseconds
        val totalPracticeDurationStr =
            getDurationString(totalPracticeDuration, DurationFormat.HMS_DIGITAL)

        val currentSectionName = sessionState.value.sections.lastOrNull()?.libraryItem?.name ?: "No Section"

        val title: String
        val description: String

        if (sessionState.value.isPaused) {
            title = "Practicing Paused"
            description = "$currentSectionName - Total: $totalPracticeDurationStr"
        } else {
            title = "Practicing for $totalPracticeDurationStr"
            description = currentSectionName
        }

        val actionButton1Intent = if(sessionState.value.isPaused) {
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
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action?.let { SessionServiceAction.valueOf(it) }

        if (action == SessionServiceAction.STOP) {
            stopTimer()
            sessionState.update { SessionState() }
            stopSelf()
        }

        if (action == SessionServiceAction.START) {
            // register Broadcast Receiver for Pause Action
            ContextCompat.registerReceiver(
                this,
                myReceiver,
                IntentFilter(BROADCAST_INTENT_FILTER),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            createPendingIntents()

            ServiceCompat.startForeground(
                this,
                NOTIFCATION_ID,
                createNotification(),
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
}