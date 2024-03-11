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
import app.musikus.di.ApplicationScope
import app.musikus.ui.activesession.ActiveSessionActions
import app.musikus.usecase.activesession.ActiveSessionUseCases
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.UUID
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.milliseconds

const val SESSION_NOTIFICATION_ID = 42
const val BROADCAST_INTENT_FILTER = "activeSessionAction"


/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class SessionServiceEvent {
    data object StopService: SessionServiceEvent()
    data object TogglePause: SessionServiceEvent()
    data class StartNewSection(val item: LibraryItem): SessionServiceEvent()
    data class DeleteSection(val sectionId: UUID): SessionServiceEvent()
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
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var useCases : ActiveSessionUseCases
    private var _timer : Timer? = null

    private val binder = LocalBinder()         // interface object for clients that bind
    inner class LocalBinder : Binder() {
        fun getOnEvent(): (SessionServiceEvent) -> Unit = ::onEvent
    }

    /** Broadcast receiver (currently only for pause action) */
    private val myReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("TAG", "Received Broadcast")
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

    fun onEvent(event: SessionServiceEvent) {
        when(event) {
            is SessionServiceEvent.StopService -> stopService()
            is SessionServiceEvent.TogglePause -> togglePause()
            is SessionServiceEvent.StartNewSection -> newSection(event.item)
            is SessionServiceEvent.DeleteSection -> removeSection(event.sectionId)
        }
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
            initialDelay = 0,
            period = 1000.milliseconds.inWholeMilliseconds
        ) {
            updateNotification()
        }
    }

    private fun togglePause() {
        applicationScope.launch {
            if (useCases.getPausedState()) {
                useCases.resume()
            } else {
                useCases.pause()
            }
        }
        updateNotification()
    }


    private fun newSection(item: LibraryItem) {
        startTimer()
        applicationScope.launch {
            try {
                useCases.selectItem(item)
            } catch (e: IllegalStateException) {
                Log.e("TAG", "Cannot start new section: ${e.message}")
            }
        }
        updateNotification()
    }

    private fun removeSection(sectionId: UUID) {
        applicationScope.launch {
            useCases.deleteSection(sectionId)
        }
    }


    /** -------------------------------------------- Service Boilerplate -------------------------*/

    private fun updateNotification() {
        applicationScope.launch {
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(SESSION_NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * Creates a notification object based on current session state
     */
    private suspend fun createNotification() : Notification {
        var title: String
        var description: String

        try {
            val totalPracticeDurationStr =
                getDurationString(useCases.getPracticeTime(), DurationFormat.HMS_DIGITAL)
            val currentSectionName = useCases.getRunningSection().first.name

            if (useCases.getPausedState()) {
                title = "Practicing Paused"
                description = "$currentSectionName - Total: $totalPracticeDurationStr"
            } else {
                title = "Practicing for $totalPracticeDurationStr"
                description = currentSectionName
            }
        } catch (e: IllegalStateException) {
            Log.d("TAG", "Could not get session info for notification: ${e.message}")
            title = "Practicing"
            description = "No section selected"
        }

        val actionButton1Intent =
            try {
                if (useCases.getPausedState()) {
                    resumeActionButton
                } else {
                    pauseActionButton
                }
            } catch (e: IllegalStateException) {
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


    override fun onCreate() {
        super.onCreate()
        createPendingIntents()
    }

    override fun onBind(intent: Intent?): IBinder {
        // register Broadcast Receiver for Pause Action
        Log.d("TAG", "onBind")
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

        applicationScope.launch {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(SESSION_NOTIFICATION_ID, notification)
            } else {
                ServiceCompat.startForeground(
                    this@SessionService,
                    SESSION_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
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
            SESSION_NOTIFICATION_ID,
            Intent(BROADCAST_INTENT_FILTER).apply {
                putExtra("action", ActiveSessionActions.PAUSE.toString())
             },
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
        _timer?.cancel()
        super.onDestroy()
    }

    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}