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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import app.musikus.database.MusikusDatabase
import app.musikus.services.ActiveSessionServiceActions
import app.musikus.services.SessionService
import app.musikus.usecase.activesession.ActiveSessionUseCases
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

    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var importLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeExportImportLaunchers()

        setContent {
            MusikusApp(timeProvider)
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

    private fun initializeExportImportLaunchers() {

        exportLauncher = registerForActivityResult(
            ExportDatabaseContract("*/*")
        ) {
            if (it == null) {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            contentResolver.openOutputStream(it)?.let { outputStream ->
                database.export(outputStream)
            }

            Toast.makeText(this, "Backup successful", Toast.LENGTH_LONG).show()

            triggerRestart()
        }

        importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) {
            if (it == null) {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            contentResolver.openInputStream(it)?.let { inputStream ->
                database.import(inputStream)
            }

            Toast.makeText(
                this,
                "Backup loaded successfully",
                Toast.LENGTH_LONG
            ).show()

            triggerRestart()
        }
    }

    fun exportDatabase() {
        exportLauncher.launch("musikus_backup")
    }

    fun importDatabase() {
        importLauncher.launch(arrayOf("*/*"))
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

private class ExportDatabaseContract(
    mimeType: String
) : ActivityResultContracts.CreateDocument(mimeType) {
    override fun createIntent(context: Context, input: String) =
        super.createIntent(context, input).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}

private class ImportDatabaseContract : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>) =
        super.createIntent(context, input).apply {
            type = "application/octet-stream"
            addCategory(Intent.CATEGORY_OPENABLE)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}