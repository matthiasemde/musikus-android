/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val METRONOME_NOTIFICATION_CHANNEL_ID = "metronome_notification_channel"
const val METRONOME_NOTIFICATION_CHANNEL_NAME = "Metronome notification"

const val SESSION_NOTIFICATION_CHANNEL_ID = "session_notification_channel"
const val SESSION_NOTIFICATION_CHANNEL_NAME = "Session notification"

const val RECORDER_NOTIFICATION_CHANNEL_ID = "recorder_notification_channel"
const val RECORDER_NOTIFICATION_CHANNEL_NAME = "Recorder notification"

fun createNotificationChannels(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val sessionNotificationChannel = NotificationChannel(
            SESSION_NOTIFICATION_CHANNEL_ID,
            SESSION_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification to keep track of the running session"
        }

        // Register the channel with the system
        notificationManager.createNotificationChannel(sessionNotificationChannel)

        val metronomeNotificationChannel = NotificationChannel(
            METRONOME_NOTIFICATION_CHANNEL_ID,
            METRONOME_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notification to keep track of the metronome"
        }

        // Register the channel with the system
        notificationManager.createNotificationChannel(metronomeNotificationChannel)

        val recorderNotificationChannel = NotificationChannel(
            RECORDER_NOTIFICATION_CHANNEL_ID,
            RECORDER_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification to keep track of the recorder"
        }
        // Register the channel with the system
        notificationManager.createNotificationChannel(recorderNotificationChannel)
    }
}
