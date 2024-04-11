/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.musikus.Musikus
import app.musikus.database.MusikusDatabase
import app.musikus.services.ActiveSessionServiceActions
import app.musikus.services.SessionService
import app.musikus.usecase.activesession.ActiveSessionUseCases
import app.musikus.utils.ExportDatabaseContract
import app.musikus.utils.ImportDatabaseContract
import app.musikus.utils.PermissionChecker
import app.musikus.utils.PermissionCheckerActivity
import app.musikus.utils.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider


@AndroidEntryPoint
class MainActivity : PermissionCheckerActivity() {

    @Inject
    override lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var databaseProvider: Provider<MusikusDatabase>

    @Inject
    lateinit var activeSessionUseCases: ActiveSessionUseCases

    private val database: MusikusDatabase by lazy { databaseProvider.get() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Musikus.exportLauncher = registerForActivityResult(
            ExportDatabaseContract()
        ) { exportDatabaseCallback(applicationContext, it) }

        Musikus.importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) { importDatabaseCallback(applicationContext, it) }

        // enable Edge-to-Edge drawing with new API
        enableEdgeToEdge()
        setContent {
            MusikusApp(timeProvider)
        }
    }

    private fun importDatabaseCallback(context: Context, uri: Uri?) {
        uri?.let {
            // close the database to collect all logs
            database.close()
            val databaseFile = context.getDatabasePath(MusikusDatabase.DATABASE_NAME)
            // delete old database
            databaseFile.delete()
            // copy new database
            databaseFile.outputStream().let { outputStream ->
                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                }
                outputStream.close()
                Toast.makeText(context, "Backup loaded successfully, restart your app to complete the process.", Toast.LENGTH_LONG).show()
            }
        }

        // open database again
//                openDatabase(context)
    }

    private fun exportDatabaseCallback(context: Context, uri: Uri?) {
        uri?.let {

            // close the database to collect all logs
            database.close()
            val databaseFile = context.getDatabasePath(MusikusDatabase.DATABASE_NAME)
            // copy database
            context.contentResolver.openOutputStream(it)?.let { outputStream ->
                databaseFile.inputStream().let { inputStream ->
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

    override fun onResume() {
        super.onResume()
        // remove notification if session is not running
        if (activeSessionUseCases.isSessionRunning()) {
            val intent = Intent(application, SessionService::class.java)
            intent.setAction(ActiveSessionServiceActions.STOP.name)
            startService(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        if (activeSessionUseCases.isSessionRunning()) {
            Log.d("MainActivity", "Session is running, starting service")

            val intent = Intent(applicationContext, SessionService::class.java)
            intent.setAction(ActiveSessionServiceActions.START.name)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        else {
            Log.d("MainActivity", "Session is not running")
        }

    }

}