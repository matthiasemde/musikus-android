/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde
 */

package app.musikus.core.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import app.musikus.R

class MusikusNotificationManager(context: Context) {
    /**
     * These are the notification channels for the different services.
     * They are stored in the [MusikusNotificationManager] for a simple reason:
     * To force us to inject it into any service that needs to create a notification.
     * This way we can ensure that the notification channels are created. Otherwise, you
     * will get a crash with the error message: "Bad notification for startForeground()".
     */
    val METRONOME_NOTIFICATION_CHANNEL_ID = "metronome_notification_channel"
    val METRONOME_NOTIFICATION_CHANNEL_NAME = "Metronome notification"

    val SESSION_NOTIFICATION_CHANNEL_ID = "session_notification_channel"
    val SESSION_NOTIFICATION_CHANNEL_NAME = "Session notification"

    val RECORDER_NOTIFICATION_CHANNEL_ID = "recorder_notification_channel"
    val RECORDER_NOTIFICATION_CHANNEL_NAME = "Recorder notification"

    val notificationManager: NotificationManager = context.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager

    init {
        createNotificationChannels(context)
    }

    private fun createNotificationChannels(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val sessionNotificationChannel = NotificationChannel(
                SESSION_NOTIFICATION_CHANNEL_ID,
                SESSION_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.active_session_service_notification_channel_description)
            }

            val metronomeNotificationChannel = NotificationChannel(
                METRONOME_NOTIFICATION_CHANNEL_ID,
                METRONOME_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.metronome_service_notification_channel_description)
            }

            val recorderNotificationChannel = NotificationChannel(
                RECORDER_NOTIFICATION_CHANNEL_ID,
                RECORDER_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.recorder_service_notification_channel_description)
            }

            // Register the channels with the system
            notificationManager.createNotificationChannels(
                listOf(
                    sessionNotificationChannel,
                    recorderNotificationChannel,
                    metronomeNotificationChannel
                )
            )
        }
    }
}
