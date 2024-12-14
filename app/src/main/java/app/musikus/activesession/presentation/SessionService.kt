/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.presentation

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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.musikus.R
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.core.di.ApplicationScope
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.SESSION_NOTIFICATION_CHANNEL_ID
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.getDurationString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.milliseconds

const val SESSION_NOTIFICATION_ID = 42
const val BROADCAST_INTENT_FILTER = "activeSessionAction"

/**
 * Actions that can be triggered via intent
 */
enum class ActiveSessionServiceActions {
    START, STOP
}

const val LOG_TAG = "SessionService"

@AndroidEntryPoint
class SessionService : Service() {

    private val timerInterval = 1000.milliseconds

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var useCases: ActiveSessionUseCases
    private var _timer: Timer? = null

    @Inject
    lateinit var notificationManager: NotificationManager

    /** Broadcast receiver (currently only for pause action) */
    private val myReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(LOG_TAG, "Received Broadcast")
            val action = intent.getStringExtra("action")?.let {
                ActiveSessionActions.valueOf(it)
            } ?: throw IllegalArgumentException("No action provided in broadcast")

            when (action) {
                ActiveSessionActions.PAUSE -> togglePause()
                else -> throw IllegalArgumentException(
                    "Broadcast receiver can't handle action: $action"
                )
            }
        }
    }

    /**
     *  ---------------------- Interface for Activity / ViewModel ----------------------
     */

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate")
        val filter = IntentFilter(BROADCAST_INTENT_FILTER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                myReceiver,
                filter,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    RECEIVER_NOT_EXPORTED
                } else {
                    ContextCompat.RECEIVER_NOT_EXPORTED
                }
            )
        } else {
            ContextCompat.registerReceiver(
                this,
                myReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand")

        when (intent?.action) {
            ActiveSessionServiceActions.STOP.toString() -> {
                _timer?.cancel()
                stopService()
            }
            ActiveSessionServiceActions.START.toString() -> {
                startTimer()
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
            }
            else -> {
                Log.e(LOG_TAG, "Started SessionService with unknown Intent action")
            }
        }
        return START_NOT_STICKY
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
            period = timerInterval.inWholeMilliseconds
        ) {
            // terminate service when session is not running
            if (!useCases.isSessionRunning()) {
                _timer?.cancel()
                stopService()
                return@timer
            }
            updateNotification()
        }
    }

    private fun togglePause() {
        applicationScope.launch {
            if (useCases.isSessionPaused()) {
                useCases.resume(timeProvider.now())
            } else {
                useCases.pause(timeProvider.now())
            }
        }
        updateNotification()
    }

    /** -------------------------------------------- Service Boilerplate -------------------------*/

    private fun updateNotification() {
        Log.d(LOG_TAG, "updateNotification")
        applicationScope.launch {
            notificationManager.notify(SESSION_NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * Creates a notification object based on current session state
     */
    private suspend fun createNotification(): Notification {
        var title: String
        var description: String

        // TODO: move this logic to use case
        try {
            val state = useCases.getState().first()
            check(state != null) { "State is null. Cannot create notification." }

            val totalPracticeDurationStr = getDurationString(
                useCases.computeTotalPracticeDuration(state, timeProvider.now()),
                DurationFormat.HMS_DIGITAL
            )

            val currentSectionName = useCases.getRunningItem().first()!!.name

            if (useCases.isSessionPaused()) {
                title = getString(R.string.active_session_service_notification_title_paused)
                description = getString(
                    R.string.active_session_service_notification_description_paused,
                    currentSectionName,
                    totalPracticeDurationStr
                )
            } else {
                title = getString(R.string.active_session_service_notification_title, totalPracticeDurationStr)
                description = currentSectionName
            }
        } catch (e: IllegalStateException) {
            Log.d(LOG_TAG, "Could not get session info for notification, stopping service: ${e.message}")
            _timer?.cancel()
            stopService()
            title = "Error"
            description = "Could not get session info"
        }

        return getNotification(
            title = title,
            description = description,
        )
    }

    private suspend fun getNotification(
        title: String,
        description: String,
    ): Notification {
        val icon = R.drawable.ic_launcher_foreground

        val pauseButtonIntent = Intent(BROADCAST_INTENT_FILTER).apply {
            putExtra("action", ActiveSessionActions.PAUSE.toString())
        }

        val pauseButtonPendingIntent = PendingIntent.getBroadcast(
            this,
            SESSION_NOTIFICATION_ID,
            pauseButtonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SESSION_NOTIFICATION_CHANNEL_ID).run {
            setSmallIcon(icon) // without icon, setOngoing does not work
            setOngoing(
                true
            ) // does not work on Android 14: https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
            setOnlyAlertOnce(true)
            setContentTitle(title)
            setContentText(description)
            setPriority(NotificationCompat.PRIORITY_HIGH) // only relevant below Oreo, else channel priority is used
            setContentIntent(activeSessionIntent(null))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            if (!useCases.isSessionPaused()) {
                addAction(
                    R.drawable.ic_pause,
                    getString(R.string.active_session_service_notification_action_pause),
                    pauseButtonPendingIntent
                )
            } else {
                addAction(
                    R.drawable.ic_play,
                    getString(R.string.active_session_service_notification_action_resume),
                    pauseButtonPendingIntent
                )
            }
            addAction(
                R.drawable.ic_stop,
                getString(R.string.active_session_service_notification_action_finish),
                activeSessionIntent(ActiveSessionActions.FINISH)
            )
            build()
        }
    }

    override fun onDestroy() {
        Log.d("Tag", "onDestroy")
        unregisterReceiver(myReceiver)
        _timer?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        // This service is not supposed to be bound
        return Binder()
    }

    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun activeSessionIntent(activeSessionAction: ActiveSessionActions?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            data = (
                "https://musikus.app" +
                    (activeSessionAction?.let { "?action=$activeSessionAction" } ?: "")
                ).toUri()
        }

        return TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            // Get a PendingIntent and make it immutable
            val requestCode = 0
            getPendingIntent(requestCode, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
