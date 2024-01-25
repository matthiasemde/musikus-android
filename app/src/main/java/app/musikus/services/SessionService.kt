package app.musikus.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import app.musikus.R
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.activesession.PracticeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.seconds


const val CHANNEL_ID = "session_channel"
const val NOTIFCATION_ID = 42

class SessionService : Service() {

    private val binder = LocalBinder()         // interface for clients that bind

    inner class LocalBinder : Binder() {
        // Return this instance of SessionService so clients can call public methods
        fun getService(): SessionService = this@SessionService
    }

    // ################ Session State ################

    private val _sections = MutableStateFlow<List<PracticeSection>>(listOf())
    val sections = _sections.asStateFlow()

    private val _currentSectionDuration = MutableStateFlow(0.seconds)
    val currentSectionDuration = _currentSectionDuration.asStateFlow()

    private val _pauseDuration = MutableStateFlow(0.seconds)
    val pauseDuration = _pauseDuration.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private var _timer: java.util.Timer? = null
    private val timerInterval = 1.seconds


    override fun onBind(intent: Intent?): IBinder {
        Log.d("TAG", "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        Log.d("Service", "Service unbound")
        return true
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TAG", "onStartCommand")
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFCATION_ID,
            Notification.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setContentTitle("Practice Session")
                .setContentText("You are currently practicing!")
                .build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        return START_NOT_STICKY
    }

    fun startTimer() {
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
        }
    }

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
    }

    fun stopTimer() {
        _timer?.cancel()
        stopSelf()
    }

    fun togglePause() {
        _isPaused.update { !it }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_settings_description)
            val descriptionText = "Notification to keep track of the running session"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}