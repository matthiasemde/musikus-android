/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.activesession.presentation.ActiveSessionServiceActions
import app.musikus.activesession.presentation.SessionService
import app.musikus.core.data.MIME_TYPE_DATABASE
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.domain.TimeProvider
import app.musikus.permissions.domain.PermissionChecker
import app.musikus.permissions.domain.PermissionCheckerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

const val DATABASE_IMPORT_BACKUP_FILE = "database_import_backup.tmp"

@AndroidEntryPoint
class MainActivity : PermissionCheckerActivity() {

    @Inject
    override lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var activeSessionUseCases: ActiveSessionUseCases

    @Inject
    lateinit var database: MusikusDatabase

    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var importLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeExportImportLaunchers()

        setContent {
            MainScreen(timeProvider)
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
        } else {
            Log.d("MainActivity", "Session is not running")
        }
    }

    private fun initializeExportImportLaunchers() {
        exportLauncher = registerForActivityResult(
            ExportDatabaseContract()
        ) { uri ->
            if (uri == null) {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            contentResolver.openOutputStream(uri)?.let { outputStream ->
                database.export(outputStream)
                outputStream.close()
            }

            Toast.makeText(this, "Backup successful", Toast.LENGTH_LONG).show()

            // Restart the app to allow dagger hilt to reopen the database
            triggerRestart()
        }

        importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) { uri ->
            if (uri == null) {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // Close the database to collect all logs
            database.close()

            // Create a backup of the current database before loading the new one
            val backupFile = File(filesDir, DATABASE_IMPORT_BACKUP_FILE)
            database.databaseFile.copyTo(backupFile, overwrite = true)

            var message = "Error loading backup. Aborting..."

            lifecycleScope.launch {
                try {
                    // Load the new database
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        database.import(inputStream)
                        inputStream.close()
                    }

                    // Open the new database locally to check if the import was successful
                    val newDatabase = MusikusDatabase.buildDatabase(context = this@MainActivity)

                    val isNewDatabaseValid = newDatabase.validate()

                    newDatabase.close()

                    if (isNewDatabaseValid) {
                        message = "Backup successfully loaded"
                    } else {
                        throw Exception("Invalid database")
                    }
                } catch (e: Exception) {
                    // If an error occurred, restore the backup
                    Log.e("MainActivity", "Error loading backup: ${e.message}")
                    backupFile.copyTo(database.databaseFile, overwrite = true)
                } finally {
                    // Finally, delete the backup file
                    backupFile.delete()

                    // Show a toast containing either the success or error message
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    // Restart the app to allow dagger hilt to load the new database
                    triggerRestart()
                }
            }
        }
    }

    fun exportDatabase() {
        exportLauncher.launch("musikus_backup")
    }

    fun importDatabase() {
        importLauncher.launch(
            arrayOf(
                MIME_TYPE_DATABASE,
                "application/vnd.sqlite3",
                "application/x-sqlite3",
            )
        )
    }

    // source: https://gist.github.com/easterapps/7127ce0749cfce2edf083e55b6eecec5
    private fun triggerRestart() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
        finish()
        kotlin.system.exitProcess(0)
    }
}

/**
 * Contracts for exporting/importing the database
 */

private class ExportDatabaseContract : ActivityResultContracts.CreateDocument(MIME_TYPE_DATABASE) {
    override fun createIntent(context: Context, input: String) =
        super.createIntent(context, input).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}

private class ImportDatabaseContract : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>) =
        super.createIntent(context, input).apply {
            type = MIME_TYPE_DATABASE
            addCategory(Intent.CATEGORY_OPENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}
