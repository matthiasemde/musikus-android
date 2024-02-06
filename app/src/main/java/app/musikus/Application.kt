/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package app.musikus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import app.musikus.database.MusikusDatabase
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val METRONOME_NOTIFICATION_CHANNEL_ID = "metronome_notification_channel"
const val METRONOME_NOTIFICATION_CHANNEL_NAME = "Metronome notification"

const val SESSION_NOTIFICATION_CHANNEL_ID = "session_notification_channel"
const val SESSION_NOTIFICATION_CHANNEL_NAME = "Session notification"

const val RECORDER_NOTIFICATION_CHANNEL_ID = "recorder_notification_channel"
const val RECORDER_NOTIFICATION_CHANNEL_NAME = "Recorder notification"


@HiltAndroidApp
class Musikus : Application() {

    companion object {
        val executorService: ExecutorService = Executors.newFixedThreadPool(4)
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

        fun ioThread(f : () -> Unit) {
            IO_EXECUTOR.execute(f)
        }

        private lateinit var db: MusikusDatabase
        private lateinit var dbFile: File

        var exportLauncher: ActivityResultLauncher<String>? = null
        var importLauncher: ActivityResultLauncher<Array<String>>? = null

        var noSessionsYet = true
        var serviceIsRunning = false

        lateinit var prefs: SharedPreferences

        const val USER_PREFERENCES_NAME = "user_preferences"



        fun getRandomQuote(context: Context) : CharSequence {
            return context.resources.getTextArray(R.array.quotes).random()
        }

        fun dp(context: Context, dp: Int): Float {
            return context.resources.displayMetrics.density * dp
        }


        fun importDatabase() {
            importLauncher?.launch(arrayOf("*/*"))
        }

        fun importDatabaseCallback(context: Context, uri: Uri?) {
            uri?.let {
                // close the database to collect all logs
                db.close()

                // delete old database
                dbFile.delete()

                // copy new database
                dbFile.outputStream().let { outputStream ->
                    context.contentResolver.openInputStream(it)?.let { inputStream ->
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                    }
                    outputStream.close()

                    Toast.makeText(context, "Backup successful", Toast.LENGTH_LONG).show()
                }

                // open new database
//                openDatabase(context)
            }
        }

        fun exportDatabase() {
            exportLauncher?.launch("practice_time_backup")
        }

        fun exportDatabaseCallback(context: Context, uri: Uri?) {
            uri?.let {
                // close the database to collect all logs
                db.close()

                // copy database
                context.contentResolver.openOutputStream(it)?.let { outputStream ->
                    dbFile.inputStream().let { inputStream ->
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                    }
                    outputStream.close()

                    Toast.makeText(context, "Backup successful", Toast.LENGTH_LONG).show()
                }

                // open database again
//                openDatabase(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)
        dbFile = getDatabasePath("practice_time.db")
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val sessionNotificationChannel = NotificationChannel(
                SESSION_NOTIFICATION_CHANNEL_ID,
                SESSION_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notification to keep track of the running session"
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(sessionNotificationChannel)

            val metronomeNotificationChannel = NotificationChannel(
                METRONOME_NOTIFICATION_CHANNEL_ID,
                METRONOME_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notification to keep track of the metronome"
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(metronomeNotificationChannel)

            val recorderNotificationChannel = NotificationChannel(
                RECORDER_NOTIFICATION_CHANNEL_ID,
                RECORDER_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notification to keep track of the recorder"
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(recorderNotificationChannel)
        }
    }
}
