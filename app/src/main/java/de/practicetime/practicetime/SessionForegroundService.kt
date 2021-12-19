package de.practicetime.practicetime

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import de.practicetime.practicetime.entities.PracticeSection
import java.util.*
import kotlin.collections.ArrayList


class SessionForegroundService : Service() {
    private val CHANNEL_ID = "PT_Channel_ID"
    private val NOTIFICATION_ID = 42
    private val binder = LocalBinder()         // interface for clients that bind
    private var allowRebind: Boolean = true    // indicates whether onRebind should be used

    var sessionActive = false               // keep track of whether a session is active
    // the sectionBuffer will keep track of all the section in the current session
    var sectionBuffer = ArrayList<Pair<PracticeSection, Int>>()
    var paused = false                      // flag if session is currently paused
    private var lastPausedState = false     // paused variable the last tick (to detect transition)
    private var pauseBeginTimestamp: Long = 0
    private var pauseDurationBuffer = 0     // just a buffer to
    var pauseDuration = 0                   // pause duration, ONLY for displaying on the fab, section pause duration is saved in sectionBuffer!
    var currCategoryName = ""                   // the name of the active category

    var stopDialogTimestamp: Long = 0

    var totalPracticeDuration = 0

    override fun onCreate() {
        // The service is being created
        Log.d("Service", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is starting, due to a call to startService()

        Singleton.serviceIsRunning = true
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
        val title = getString(R.string.notification_title,
            totalPracticeDuration / 3600,
            totalPracticeDuration % 3600 / 60,
            totalPracticeDuration % 60)
        val desc = if (paused) {
            getString(R.string.paused_practicing)
        } else {
            currCategoryName
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

    override fun onBind(intent: Intent): IBinder? {
        // A client is binding to the service with bindService()
//        Log.d("Service", "onBind()")
        return binder
    }

    fun startNewSection(categoryId: Int, categoryName: String) {
        currCategoryName = categoryName
        val now = Date().time / 1000L
        sectionBuffer.add(
            Pair(
                PracticeSection(
                    0,  // 0 means auto-increment
                    null,
                    categoryId,
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
    private fun getDuration(section: PracticeSection): Int {
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Service", "Service destroyed")
        Singleton.serviceIsRunning = false
    }
}