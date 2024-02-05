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
import app.musikus.R
import app.musikus.RECORDER_NOTIFICATION_CHANNEL_ID
import app.musikus.ui.activesession.ActiveSessionActivity
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import app.musikus.utils.Recorder
import app.musikus.utils.TimeProvider
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
        ServiceCompat.startForeground(
            this,
            RECORDER_NOTIFICATION_ID,
            getNotification(0.seconds),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        return START_NOT_STICKY
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

    private fun getNotification(duration: Duration) : Notification {
        val resultIntent = Intent(this, ActiveSessionActivity::class.java)
        // Create the TaskStackBuilder for artificially creating
        // a back stack based on android:parentActivityName in AndroidManifest.xml
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }
        return  NotificationCompat.Builder(this, RECORDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record)
            .setContentTitle(getString(R.string.recording_notification_settings_description))
            .setContentText(getDurationString(duration, DurationFormat.HMS_DIGITAL))
            .setContentIntent(resultPendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }
}
