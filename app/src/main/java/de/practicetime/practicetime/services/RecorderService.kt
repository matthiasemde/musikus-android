/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.ui.activesession.ActiveSessionActivity
import de.practicetime.practicetime.utils.TIME_FORMAT_HMS_DIGITAL
import de.practicetime.practicetime.utils.getDurationString
import java.io.FileDescriptor
import java.util.*

class RecorderService : Service() {

    companion object {
        var recording = false
        var recordingUri: Uri? = null
        var recordingName: String? = null
        private const val CHANNEL_ID = "PT_Recording_Channel_ID"
        private const val NOTIFICATION_ID = 69
    }


    private var recordingStartTime: Long? = null
    private val recordingTimeHandler = Handler(Looper.getMainLooper())

    private lateinit var recorder: MediaRecorder

    override fun onBind(intent: Intent?): Binder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RecService", "Started")
        val fileDescriptor = Uri.parse(intent?.extras?.getString("URI"))?.let {
            try {
                recordingUri = it
                contentResolver.openFileDescriptor(it,"w")?.fileDescriptor
            } catch (e: Exception) {
                null
            }
        }

        if(fileDescriptor != null) {
            startRecording(fileDescriptor)
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, getNotification(durationSecs = 0))
        }
        else
            Log.d("REC_SERVICE", "onStart: No valid file descriptor passed")

        return START_NOT_STICKY
    }

    private fun startRecording(outputFile: FileDescriptor) {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioChannels(2)
            setAudioEncodingBitRate(256_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile)
            prepare()
            start()
        }
        recording = true
        recordingStartTime = Date().time
        recordingTimeHandler.post(incrementRecordingTime)
    }

    override fun onDestroy() {
        recordingTimeHandler.removeCallbacks(incrementRecordingTime)
        stopForeground(true)
        super.onDestroy()
        PracticeTime.executorService.execute {
            stopRecording()
        }
        Log.d("RecService", "Destroyed")
    }

    private fun stopRecording() {
        Log.d("RecService", "Stop reocrding")
        if(recording) {
            recorder.apply {
                stop()
                release()
            }
            recording = false
            recordingStartTime = null
            val intent = Intent("RecordingStopped")
            LocalBroadcastManager
                .getInstance(this@RecorderService)
                .sendBroadcast(intent)
        }
    }

    private var notificationCounter = 0

    private val incrementRecordingTime = object : Runnable {
        override fun run() {
            if(recording) {
                recordingStartTime?.also {
                    val diff = (Date().time - it).toInt()
                    val intent = Intent("RecordingDurationUpdate")
                    intent.putExtra("DURATION", diff.toString())
                    LocalBroadcastManager
                        .getInstance(this@RecorderService)
                        .sendBroadcast(intent)
                    if(notificationCounter == 0) updateNotification(diff / 1000)
                }
                notificationCounter = (notificationCounter + 1) % 5
                recordingTimeHandler.postDelayed(this, 20L)
            }
        }
    }

    private fun updateNotification(durationSecs: Int) {
        val notification: Notification = getNotification(durationSecs)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getNotification(durationSecs: Int) : Notification {
        val resultIntent = Intent(this, ActiveSessionActivity::class.java)
        // Create the TaskStackBuilder for artificially creating
        // a back stack based on android:parentActivityName in AndroidManifest.xml
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }
        return  NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record)
            .setContentTitle(getString(R.string.recording_notification_settings_description))
            .setContentText(getDurationString(durationSecs, TIME_FORMAT_HMS_DIGITAL))
            .setContentIntent(resultPendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    // the "channel" is required for new Notifications from Oreo onwards https://stackoverflow.com/a/47533338
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.recording_notification_settings_description)
            val descriptionText = "Notification to display recording"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
