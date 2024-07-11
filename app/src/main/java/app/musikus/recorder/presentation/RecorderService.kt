/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.recorder.presentation

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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import app.musikus.R
import app.musikus.core.presentation.RECORDER_NOTIFICATION_CHANNEL_ID
import app.musikus.core.di.ApplicationScope
import app.musikus.activesession.presentation.ActiveSessionActions
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.recorder.domain.IllegalRecorderStateException
import app.musikus.recorder.domain.Recorder
import app.musikus.recorder.domain.RecorderState
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.utils.getDurationString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val RECORDER_NOTIFICATION_ID = 97


data class RecorderServiceState(
    val recorderState: RecorderState,
    val recordingDuration: Duration
)


/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class RecorderServiceEvent {
    data object StartRecording: RecorderServiceEvent()
    data object PauseRecording: RecorderServiceEvent()
    data object ResumeRecording: RecorderServiceEvent()
    data object DeleteRecording: RecorderServiceEvent()
    data class SaveRecording(val recordingName: String): RecorderServiceEvent()
}

sealed class RecorderServiceException(override val message: String) : Exception(message) {
    data class IllegalRecorderState(override val message: String) : RecorderServiceException(message)

}

@AndroidEntryPoint
class RecorderService : Service() {

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /** Interface object for clients that bind */
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getServiceState() = serviceState
        fun getEventHandler() = ::onEvent
        fun getExceptionChannel() = exceptionChannel
    }

    /**
     *  --------------- Local variables ---------------
     */

    private val _exceptionChannel = Channel<RecorderServiceException>()

    private val recorder by lazy {
        Recorder(
            context = this,
            timeProvider = timeProvider
        )
    }

    /** Own state flows */

    private val _recordingDuration = MutableStateFlow(0.seconds)

    private var _recordingTimer : Timer? = null

    private var pendingIntentTapAction : PendingIntent? = null

    private val dependenciesInjected = MutableStateFlow(false)

    /**
     *  ----------- Interface for Activity / ViewModel -------
     */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recorderState = dependenciesInjected.flatMapLatest {
        if (!it) flowOf(RecorderState.UNINITIALIZED)
        else recorder.state
    }

    val serviceState = combine(
        recorderState,
        _recordingDuration
    ) { recorderState, recordingDuration ->
        RecorderServiceState(
            recorderState = recorderState,
            recordingDuration = recordingDuration
        )
    }

    fun onEvent(event: RecorderServiceEvent) {
        when(event) {
            is RecorderServiceEvent.StartRecording -> startRecording()
            is RecorderServiceEvent.PauseRecording -> pauseRecording()
            is RecorderServiceEvent.ResumeRecording -> resumeRecording()
            is RecorderServiceEvent.DeleteRecording -> deleteRecording()
            is RecorderServiceEvent.SaveRecording -> saveRecording(event.recordingName)
        }
    }

    val exceptionChannel = _exceptionChannel.receiveAsFlow()

    /**
     *  --------------- Private methods ---------------
     */

    private fun startRecording() {
        try {
            recorder.start()
            startTimer()
        } catch (e: IllegalRecorderStateException) {
            applicationScope.launch { _exceptionChannel.send(
                RecorderServiceException.IllegalRecorderState(e.message)
            ) }
        }
    }

    private fun pauseRecording() {
        try {
            recorder.pause()
            _recordingTimer?.cancel()
        } catch (e: IllegalRecorderStateException) {
            applicationScope.launch { _exceptionChannel.send(
                RecorderServiceException.IllegalRecorderState(e.message)
            ) }
        }
    }

    private fun resumeRecording() {
        try {
            recorder.resume()
            startTimer()
        } catch (e: IllegalRecorderStateException) {
            applicationScope.launch { _exceptionChannel.send(
                RecorderServiceException.IllegalRecorderState(e.message)
            ) }
        }
    }

    private fun deleteRecording() {
        try {
            recorder.delete()
            reset(keepNotification = false)
        } catch (e: IllegalRecorderStateException) {
            applicationScope.launch { _exceptionChannel.send(
                RecorderServiceException.IllegalRecorderState(e.message)
            ) }
        }
    }

    private fun saveRecording(recordingName: String) {
        try {
            recorder.save(recordingName)
            setFinalNotification()
            reset(keepNotification = true)
        } catch (e: IllegalRecorderStateException) {
            applicationScope.launch { _exceptionChannel.send(
                RecorderServiceException.IllegalRecorderState(e.message)
            ) }
        }
    }

    // TODO check if timer drifts over long durations
    private fun startTimer() {
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

    private fun reset(keepNotification: Boolean) {
        _recordingDuration.update { 0.seconds }
        _recordingTimer?.cancel()
        stopForeground( if(keepNotification) {
            STOP_FOREGROUND_DETACH
        } else {
            STOP_FOREGROUND_REMOVE
        })
    }


    /**
     * --------------- Service Boilerplate ------------
     */

    override fun onCreate() {
        super.onCreate()
        dependenciesInjected.update { true }
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createPendingIntent()
        val notification = getNotification(0.seconds)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(RECORDER_NOTIFICATION_ID, notification)
        } else {
            ServiceCompat.startForeground(
                this,
                RECORDER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        }

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
            .setSmallIcon(R.drawable.ic_microphone)    // without icon, setOngoing does not work
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