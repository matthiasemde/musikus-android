/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 * Additions and modifications, author Matthias Emde
 */

package de.practicetime.practicetime.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Section
import de.practicetime.practicetime.ui.activesession.ActiveSessionActivity
import de.practicetime.practicetime.utils.secondsDurationToHoursMinSec
import java.util.*
import kotlin.math.roundToInt


class SessionForegroundService : Service() {
    private val CHANNEL_ID = "PT_Channel_ID"
    private val NOTIFICATION_ID = 42
    private val binder = LocalBinder()         // interface for clients that bind
    private var allowRebind: Boolean = true    // indicates whether onRebind should be used

    var sessionActive = false               // keep track of whether a session is active
    // the sectionBuffer will keep track of all the section in the current session
    var sectionBuffer = ArrayList<Pair<Section, Int>>()
    var paused = false                      // flag if session is currently paused
    private var lastPausedState = false     // paused variable the last tick (to detect transition)
    private var pauseBeginTimestamp: Long = 0
    private var pauseDurationBuffer = 0     // just a buffer to
    var pauseDuration = 0                   // pause duration, ONLY for displaying on the fab, section pause duration is saved in sectionBuffer!
    var currLibraryItemName = ""                   // the name of the active libraryItem

    var stopDialogTimestamp: Long = 0

    var totalPracticeDuration = 0

    override fun onCreate() {
        // The service is being created
        Log.d("Service", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is starting, due to a call to startService()

        PracticeTime.serviceIsRunning = true
        startTimer()
        createNotificationChannel()
        // set the Service to foreground to displaying the notification
        // this is different to displaying the notification via notify() since it automatically
        // produces a non-cancellable notification
        startForeground(NOTIFICATION_ID, getNotification( "title", "content"))

        return START_NOT_STICKY
    }

    private fun startTimer() {
        sessionActive = true
        Handler(Looper.getMainLooper()).also {
            it.post(object : Runnable {
                override fun run() {
                    if (sectionBuffer.isNotEmpty()) {
                        val lastSection = sectionBuffer.last()
                        val now = Date().time / 1000L

                        // pause started
                        if (!lastPausedState && paused) {
                            pauseBeginTimestamp = now
                            // save previous pause time
                            pauseDurationBuffer = sectionBuffer.last().second
                        }

                        if (paused) {
                            val timePassed = (now - pauseBeginTimestamp).toInt()
                            // Since Pairs<> are not mutable (but ArrayList is)
                            // we have to copy the element and replace the whole element in the ArrayList
                            sectionBuffer[sectionBuffer.lastIndex] =
                                sectionBuffer.last().copy(second = timePassed + pauseDurationBuffer)

                            pauseDuration = timePassed
                        }

                        lastSection.apply {
                            // calculate section duration and update duration field
                            first.duration = getDuration(first)
                        }

                        totalPracticeDuration = 0
                        sectionBuffer.forEach { section ->
                            totalPracticeDuration += (section.first.duration ?: 0).minus(section.second)
                        }

                        updateNotification()

                        lastPausedState = paused
                    }
                    // post the code again with a delay of 1 second
                    it.postDelayed(this, 100)
                }
            })
        }
    }

    /**
     * updates the notification text continuously to show elapsed time
     */
    private fun updateNotification() {
        val (h, m, s) = secondsDurationToHoursMinSec(totalPracticeDuration)
        val title = getString(R.string.notification_title, h, m, s)
        val desc = if (paused) {
            getString(R.string.paused_practicing)
        } else {
            currLibraryItemName
        }

        val notification: Notification = getNotification(title, desc)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getNotification(title: String, contentText: String) : Notification {
        val resultIntent = Intent(this, ActiveSessionActivity::class.java)
        // Create the TaskStackBuilder for artificially creating
        // a back stack based on android:parentActivityName in AndroidManifest.xml
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }
        val icon = if (paused) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        return  NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(resultPendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    // the "channel" is required for new Notifications from Oreo onwards https://stackoverflow.com/a/47533338
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_settings_description)
            val descriptionText = "Notification to keep track of the running session"
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

    override fun onBind(intent: Intent): IBinder {
        // A client is binding to the service with bindService()
//        Log.d("Service", "onBind()")
        return binder
    }

    fun startNewSection(libraryItemId: UUID, libraryItemName: String) {
        currLibraryItemName = libraryItemName
        val now = Date().time / 1000L
        sectionBuffer.add(
            Pair(
                Section(
                    null,
                    libraryItemId,
                    null,
                    now,
                ),
                0
            )
        )
    }

    /**
     * subtracts the time passed since stopDialogTimestamp from the last section
     */
    fun subtractStopDialogTime() {
        val timePassed = (Date().time / 1000L) - stopDialogTimestamp
        if(paused) {
            // subtract from paused time
            sectionBuffer[sectionBuffer.lastIndex] =
                sectionBuffer.last().copy(second = sectionBuffer.last().second - timePassed.toInt())
        } else {
            // subtract from regular duration
            sectionBuffer.last().first.duration =
                sectionBuffer.last().first.duration?.minus(
                    timePassed.toInt()
                )
        }
    }

    /**
     * calculates total Duration (INCLUDING PAUSES!!!) of a section
     */
    private fun getDuration(section: Section): Int {
        val now = Date().time / 1000L
        return (now - section.timestamp).toInt()
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
//        Log.d("Service", "Service unbound")
        return allowRebind
    }

    override fun onRebind(intent: Intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called

        // TODO notify activity that Session is running
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of SessionForegroundService so clients can call public methods
        fun getService(): SessionForegroundService = this@SessionForegroundService
    }

    /****************************************************************************
     *  Metronome Functionality
     ***************************************************************************/

    private val metronomeMinSilence = 500
    private val sampleRate = 44_100

    val metronomeMinBpm = 20
    val metronomeMaxBpm = 250

    private val metronomeMinBpb = 1
    private val metronomeMaxBpb = 10

    private val metronomeMinCpb = 1
    private val metronomeMaxCpb = 10

    var metronomeBeatsPerMinute = 120
        set(value) {
            field = value.coerceIn(metronomeMinBpm, metronomeMaxBpm)
        }
    var metronomeBeatsPerBar = 4
        set(value) {
            field = value.coerceIn(metronomeMinBpb, metronomeMaxBpb)
        }
    var metronomeClicksPerBeat = 1
        set(value) {
            field = value.coerceIn(metronomeMinCpb, metronomeMaxCpb)
        }

    var metronomePlaying = false

    var beat1 = ByteArray(5000) {0}
    var beat2 = ByteArray(5000) {0}
    var beat3 = ByteArray(5000) {0}

    // calculate the interval for a single click in frames
    private fun cpmToBytes(cpm: Int): Int {
        // times two because each sample requires two bytes for 16BitPCM
        return (2F * sampleRate.toFloat() * 60F / cpm.toFloat()).roundToInt()
    }

    fun stopMetronome() {
        metronomePlaying = false
    }

    fun startMetronome() {
        metronomePlaying = true
        var click = 0

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()

        PracticeTime.executorService.execute {
            var intervalInBytes = 0
            var clickDuration = 0
            var silenceDuration = 0

            var silence = ByteArray(0)
            var clickHigh = ByteArray(0)
            var clickMedium = ByteArray(0)
            var clickLow = ByteArray(0)

            val filterLength = 0
            val filterArray = FloatArray(filterLength)
            filterArray.forEachIndexed { i, _ ->
                filterArray[i] = i.toFloat() / (filterLength - 1)
            }

            while (metronomePlaying) {
                // check if metronomeBeatsPerMinute was changed and if so,
                // recalculate the silence array and store the new interval
                (cpmToBytes(metronomeClicksPerBeat * metronomeBeatsPerMinute)).let {
                    if (it != intervalInBytes) {
                        silenceDuration = minOf(metronomeMinSilence, it / 2)
                        clickDuration = beat1.size
                        if (it >= clickDuration + metronomeMinSilence) {
                            silenceDuration = it - clickDuration
                        } else {
                            clickDuration = it - silenceDuration
                        }
                        silence = ByteArray(silenceDuration) { 0 }

                        clickHigh = beat1.copyOfRange(0, clickDuration)
                        clickMedium = beat2.copyOfRange(0, clickDuration)
                        clickLow = beat3.copyOfRange(0, clickDuration)

                        for (index in 0 until filterLength) {
                            clickHigh[index] = (
                                    clickHigh[index] * filterArray[index]
                                    ).toInt().toByte()
                            clickHigh[clickDuration - 1 - index] = (
                                    clickHigh[clickDuration - 1 - index] * filterArray[filterLength - 1 - index]
                                    ).toInt().toByte()
                            clickMedium[index] = (
                                    clickMedium[index] * filterArray[index]
                                    ).toInt().toByte()
                            clickMedium[clickDuration - 1 - index] = (
                                    clickMedium[clickDuration - 1 - index] * filterArray[filterLength - 1 - index]
                                    ).toInt().toByte()
                            clickLow[index] = (
                                    clickLow[index] * filterArray[index]
                                    ).toInt().toByte()
                            clickLow[clickDuration - 1 - index] = (
                                    clickLow[clickDuration - 1 - index] * filterArray[filterLength - 1 - index]
                                    ).toInt().toByte()
                        }
                        intervalInBytes = silenceDuration + clickDuration
                    }
                }

                track.write(
                    when {
                        click == 0 -> {
                            clickHigh
                        }
                        click % metronomeClicksPerBeat == 0 -> {
                            clickMedium
                        }
                        else -> {
                            clickLow
                        }
                    },
                    0,
                    clickDuration
                )

                track.write(silence, 0, silenceDuration)

                click = (click + 1) % (metronomeClicksPerBeat * metronomeBeatsPerBar)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Service", "Service destroyed")
        PracticeTime.serviceIsRunning = false
        stopMetronome()
    }

}
