/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.musikus.METRONOME_NOTIFICATION_CHANNEL_ID
import app.musikus.R
import app.musikus.di.ApplicationScope
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes


const val METRONOME_NOTIFICATION_ID = 69


data class MetronomeServiceState(
    val isPlaying: Boolean
)


/**
 * Exposed interface for events between Service and Activity / ViewModel
 */
sealed class MetronomeServiceEvent {
    data object TogglePlaying: MetronomeServiceEvent()
}

@AndroidEntryPoint
class MetronomeService : Service() {

    @Inject
    lateinit var userPreferencesUseCases: UserPreferencesUseCases

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /** Interface object for clients that bind */
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getServiceState() = serviceState
        fun getEventHandler() = ::onEvent
    }

    /** Imported flows */
    private val metronomeSettings by lazy {
        userPreferencesUseCases.getMetronomeSettings()
    }

    /** Own state flows */
    private val _isPlaying = MutableStateFlow(false)


    /** Composing the service state */
    val serviceState = _isPlaying.map {
        MetronomeServiceState(
            isPlaying = it
        )
    }

    /**
     *  Interface for Activity / ViewModel
     */
    fun onEvent(event: MetronomeServiceEvent) {
        when(event) {
            is MetronomeServiceEvent.TogglePlaying -> toggleIsPlaying()
        }
    }


    private fun toggleIsPlaying() {
        _isPlaying.update { !it }
        if (_isPlaying.value) {
            startMetronome()
        } else {
            _metronomeJob?.cancel()
            _metronomeTimer?.cancel()
        }
    }

    /**
     * Service Boilerplate
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
        Log.d("MetronomeService", "onStartCommand")

        ServiceCompat.startForeground(
            this,
            METRONOME_NOTIFICATION_ID,
            getNotification("Metronome", "Metronome is running"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        return START_NOT_STICKY
    }

    private fun getNotification(
        title: String,
        description: String
    ): Notification {

        val icon = R.drawable.ic_launcher_foreground

        val builder = NotificationCompat.Builder(this, METRONOME_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(icon)    // without icon, setOngoing does not work
            .setOngoing(true)  // does not work on Android 14: https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // only relevant below Oreo, else channel priority is used
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        return builder.build()

    }

    /** Metronome playing logic */
    private var _metronomeTimer : Timer? = null

    private var _metronomeJob : Job? = null

    private var _soundPool: SoundPool? = null
    private var _soundId: Int? = null
    private fun startMetronome() {

        if(_soundPool == null) {
            _soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()

            _soundId = _soundPool?.load(this, R.raw.beat_1, 1)
        }

        _metronomeJob?.cancel() // make sure any old jobs are canceled
        _metronomeJob = applicationScope.launch {
            metronomeSettings.collectLatest { settings ->
                Log.d("MetronomeService", "metronomeSettings: $settings")
                _metronomeTimer?.cancel() // cancel previous timer if it exists
                _metronomeTimer = timer(
                    name = "MetronomeTimer",
                    initialDelay = 0,
                    period = 1.minutes.inWholeMilliseconds / settings.bpm
                ) {
//                    Log.d("MetronomeService", "tick")
                    _soundId?.let { _soundPool?.play(it, 1f, 1f, 0, 0, 1f) }
                }
            }
        }

    }
}