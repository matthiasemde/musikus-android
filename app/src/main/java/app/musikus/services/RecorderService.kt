/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import app.musikus.R
import app.musikus.RECORDER_NOTIFICATION_CHANNEL_ID
import app.musikus.ui.activesession.ActiveSessionActions
import app.musikus.utils.DurationFormat
import app.musikus.utils.Recorder
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val RECORDER_NOTIFICATION_ID = 97


data class RecorderServiceState(
    val isRecording: Boolean,
    val recordingDuration: Duration
)


/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class RecorderServiceEvent {
    data object ToggleRecording: RecorderServiceEvent()
}

@AndroidEntryPoint
class RecorderService : Service() {

    @Inject
    lateinit var timeProvider: TimeProvider

    /** Interface object for clients that bind */
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getServiceState() = serviceState
        fun getEventHandler() = ::onEvent
    }

    /**
     *  --------------- Local variables ---------------
     */

    private val recorder by lazy {
        Recorder(
            context = this,
            timeProvider = timeProvider
        )
    }

    /** Own state flows */

    private val _isRecording = MutableStateFlow(false)
    private val _recordingDuration = MutableStateFlow(0.seconds)

    private var _recordingTimer : Timer? = null

    private var pendingIntentTapAction : PendingIntent? = null

    /**
     *  ----------- Interface for Activity / ViewModel -------
     */

    val serviceState = combine(
        _isRecording,
        _recordingDuration
    ) { isRecording, recordingDuration ->
        RecorderServiceState(
            isRecording = isRecording,
            recordingDuration = recordingDuration
        )
    }

    fun onEvent(event: RecorderServiceEvent) {
        when(event) {
            is RecorderServiceEvent.ToggleRecording -> {
                if (_isRecording.value) {
                    stopRecording()
                } else {
                    startRecording()
                }
            }
        }
    }

    /**
     *  --------------- Private methods ---------------
     */

    private fun startRecording() : Result<Unit> {
        return recorder.start("Musikus").also { result ->
            if(result.isSuccess) {
                _isRecording.update { true }
                _recordingTimer = timer(
                    name = "recording_timer",
                    period = 0.01.seconds.inWholeMilliseconds, // 10ms
                    initialDelay = 0.01.seconds.inWholeMilliseconds
                ) {
                    _recordingDuration.update { it + 0.01.seconds }
                    if (_recordingDuration.value.inWholeMilliseconds % 1000 == 0L) {
                        updateNotification(_recordingDuration.value)
                    }
                }
            }
        }
    }

    private fun stopRecording() : Result<Unit> {
        return recorder.stop().also {
            if(it.isSuccess) {
                _isRecording.update { false }
                _recordingDuration.update { 0.seconds }
                _recordingTimer?.cancel()
                setFinalNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    stopForeground(false)
                }
            }
        }
    }


    /**
     * --------------- Service Boilerplate ------------
     */

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createPendingIntent()
        ServiceCompat.startForeground(
            this,
            RECORDER_NOTIFICATION_ID,
            getNotification(0.seconds),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        return START_NOT_STICKY
    }

    private fun createPendingIntent() {
        // trigger deep link to open ActiveSession https://stackoverflow.com/a/72769863
        pendingIntentTapAction = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(
                Intent(Intent.ACTION_VIEW, "musikus://activeSession/${ActiveSessionActions.RECORDER}".toUri())
            )
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override fun onDestroy() {
        recorder.stop()
        super.onDestroy()
    }

    private fun updateNotification(duration: Duration) {
        val notification: Notification = getNotification(duration)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(RECORDER_NOTIFICATION_ID, notification)
    }

    private fun setFinalNotification() {
        val notification: Notification = getBasicNotification("Recording finished", "click to open", false)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(RECORDER_NOTIFICATION_ID, notification)
    }

    private fun getNotification(duration: Duration) : Notification {
        return getBasicNotification(
            title = getString(R.string.recording_notification_settings_description),
            text = getDurationString(duration, DurationFormat.HMS_DIGITAL)
        )
    }

    private fun getBasicNotification(title: String, text: CharSequence, persistent: Boolean = true) : Notification {
        return  NotificationCompat.Builder(this, RECORDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record)    // without icon, setOngoing does not work
            .setOngoing(persistent) // does not work on Android 14: https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntentTapAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // only relevant below Oreo, else channel priority is used
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}