/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.musikus.R
import app.musikus.RECORDER_NOTIFICATION_CHANNEL_ID
import app.musikus.ui.activesession.ActiveSessionActivity
import app.musikus.utils.DurationFormat
import app.musikus.utils.Recorder
import app.musikus.utils.getDurationString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val RECORDER_NOTIFICATION_ID = 97


data class RecorderServiceState(
    val isRecording: Boolean
)


/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class RecorderServiceEvent {
    data object ToggleRecording: RecorderServiceEvent()
}

class RecorderService : Service() {

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
        Recorder(context = this)
    }

    /** Own state flows */
    private val _isRecording = MutableStateFlow(false)


    /**
     *  ----------- Interface for Activity / ViewModel -------
     */

    val serviceState = _isRecording.map {
        RecorderServiceState(
            isRecording = it
        )
    }

    fun onEvent(event: RecorderServiceEvent) {
        when(event) {
            is RecorderServiceEvent.ToggleRecording -> toggleIsRecording()
        }
    }

    /**
     *  --------------- Private methods ---------------
     */


    private fun toggleIsRecording() {
        _isRecording.update { !it }
        if (_isRecording.value) {
//            recorder.startRecording()
        } else {
            recorder.stop()
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        recorder.stop()
        super.onDestroy()
        Log.d("RecService", "Destroyed")
    }

//    private fun updateNotification(duration: Duration) {
//        val notification: Notification = getNotification(duration)
//        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        mNotificationManager.notify(RECORDER_NOTIFICATION_ID, notification)
//    }

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
