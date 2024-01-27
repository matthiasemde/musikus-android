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
import app.musikus.CHANNEL_ID
import app.musikus.R
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.activesession.PracticeSection
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


const val NOTIFCATION_ID = 42
const val BROADCAST_INTENT_FILTER = "activeSessionAction"

/**
 * Actions that can be triggered by the Notification
 */
enum class Actions {
    OPEN, PAUSE, FINISH
}

/**
 * Data Structure for a Button inside the Notification
 */
data class NotificationActionButtonConfig(
    @DrawableRes val icon: Int,
    val text: String,
    val tapIntent: PendingIntent?
)

class SessionService : Service() {

    private var _timer: java.util.Timer? = null
    private val timerInterval = 1.seconds

    private val binder = LocalBinder()         // interface object for clients that bind
    inner class LocalBinder : Binder() {
        // Return this instance of SessionService so clients can call public methods
        fun getService(): SessionService = this@SessionService
    }

    /** Broadcast receiver (currently only for pause action) */
    private val myReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("action")) {
                Actions.PAUSE.toString() -> togglePause()
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


    /** ---------------------------------- Session State ---------------------------------- */

    // TODO: bundle these in a data class

    private val _sections = MutableStateFlow<List<PracticeSection>>(listOf())
    val sections = _sections.asStateFlow()

    private val _currentSectionDuration = MutableStateFlow(0.seconds)
    val currentSectionDuration = _currentSectionDuration.asStateFlow()

    private val _pauseDuration = MutableStateFlow(0.seconds)
    val pauseDuration = _pauseDuration.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    /**
     *  ---------------------- Interface for Activity / ViewModel ----------------------
     */

    fun newSection(item: LibraryItem) {
        // prevent starting a new section too fast
        if (_sections.value.isNotEmpty() && _currentSectionDuration.value < 1.seconds) return
        // prevent starting the same section twice in a row
        if (item == _sections.value.lastOrNull()?.libraryItem) return

        startTimer()
        // end current section and store its duration
        var currentSectionDuration = 0.seconds
        _currentSectionDuration.update {
            // only store whole seconds, so floor current section duration
            currentSectionDuration = it.inWholeSeconds.seconds
            // the remaining part (sub-seconds) will be added to the new section upfront
            it - currentSectionDuration
        }

        _sections.update {
            // update duration in newest section
            val updatedLastSection =
                it.lastOrNull()?.copy(duration = currentSectionDuration)
            // create new section
            val newItem = PracticeSection(
                libraryItem = item,
                duration = null,
                startTimestamp = ZonedDateTime.now() //TODO: use timeprovider
            )

            if(updatedLastSection != null) {
                // return new list with updated item
                it.dropLast(1) + updatedLastSection + newItem
            } else {
                listOf(newItem)
            }
        }
        updateNotification()
    }

    fun stopTimer() {
        _timer?.cancel()
        stopSelf()
    }

    fun togglePause() {
        _isPaused.update { !it }
        updateNotification()
    }


    /**
     * ----------------------------------- private functions ------------------------------------
     */

    private fun startTimer() {
        if (_timer != null) {
            return
        }
        _timer = timer(
            name = "Timer",
            initialDelay = timerInterval.inWholeMilliseconds,
            period = timerInterval.inWholeMilliseconds
        ) {
            if (_isPaused.value) {
                _pauseDuration.update { it + timerInterval }
            } else {
                _currentSectionDuration.update { it + timerInterval }
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
        val totalPracticeDuration = _sections.value.sumOf {
            (it.duration ?: _currentSectionDuration.value).inWholeMilliseconds }.milliseconds
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
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        return START_NOT_STICKY
    }

    private fun getNotification(
        title: String,
        description: String,
        actionButton1: NotificationActionButtonConfig?,
        actionButton2: NotificationActionButtonConfig?): Notification {

        val icon = R.drawable.ic_launcher_foreground

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
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
                Intent(Intent.ACTION_VIEW, "musikus://activeSession/${Actions.OPEN}".toUri())
            )
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        pendingIntentActionPause = PendingIntent.getBroadcast(
            this,
            0,
            Intent(BROADCAST_INTENT_FILTER).also { it.putExtra("action", Actions.PAUSE.toString()) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntentActionFinish = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(
                Intent(Intent.ACTION_VIEW, "musikus://activeSession/${Actions.FINISH}".toUri())
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