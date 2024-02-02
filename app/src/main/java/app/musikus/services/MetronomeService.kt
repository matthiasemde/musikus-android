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
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.musikus.METRONOME_NOTIFICATION_CHANNEL_ID
import app.musikus.R
import app.musikus.di.ApplicationScope
import app.musikus.di.IoScope
import app.musikus.utils.Metronome
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


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

    @Inject
    @IoScope
    lateinit var ioScope: CoroutineScope

    private val metronome by lazy {
        val player = Metronome(
            applicationScope = applicationScope,
            context = this
        )

        // along with the player, start a coroutine that listens to changes
        // in the metronome settings and updates the player accordingly
        metronomeSettingsUpdateJob = applicationScope.launch {
            metronomeSettings.collect {
                player.updateSettings(it)
            }
        }
        player
    }

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

    private lateinit var metronomeSettingsUpdateJob : Job

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
            metronome.play()
        } else {
            metronome.stop()
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
        ServiceCompat.startForeground(
            this,
            METRONOME_NOTIFICATION_ID,
            getNotification("Metronome", "Metronome is running"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        return START_NOT_STICKY
    }

    // TODO manage notification and service lifecycle
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

    override fun onDestroy() {
        metronomeSettingsUpdateJob.cancel()
        metronome.stop()
        super.onDestroy()
    }
}