package app.musikus.services

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.musikus.CHANNEL_ID
import app.musikus.R
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.activesession.PracticeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.seconds


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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TAG", "onStartCommand")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)    // without icon, setOngoing does not work
            .setOngoing(true)  // does not work on Android 14: https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
            .setContentTitle("Practice Session")
            .setContentText("You are currently practicing!")
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFCATION_ID,
            notification,
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

}