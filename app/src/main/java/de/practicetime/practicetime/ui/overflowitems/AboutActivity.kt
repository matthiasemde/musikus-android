/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.overflowitems

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.practicetime.practicetime.MIME_TYPE_DATABASE
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.ui.MainActivity
import de.practicetime.practicetime.utils.ExportContract
import de.practicetime.practicetime.utils.ExportImportDialog
import de.practicetime.practicetime.utils.ExportSessionsCSVDialog
import de.practicetime.practicetime.utils.ImportDatabaseContract
import kotlinx.coroutines.launch

class AboutActivity : AppCompatActivity() {

    // initExportImport Dialog
    private lateinit var exportImportDialog: ExportImportDialog
    private lateinit var exportCSVDialog: ExportSessionsCSVDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // initExportImport Dialog
        exportImportDialog = ExportImportDialog(this)
        exportCSVDialog = ExportSessionsCSVDialog(this)

        setSupportActionBar(findViewById(R.id.about_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.settings)
        }

        /**
         initialize callback handlers for export and import features
         */
        PracticeTime.exportLauncher = registerForActivityResult(
            ExportContract(MIME_TYPE_DATABASE)
        ) {
            PracticeTime.exportDatabaseCallback(applicationContext, it)
        }

        PracticeTime.importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) {
            lifecycleScope.launch {
                val success = PracticeTime.importDatabaseCallback(this@AboutActivity, it)
                if (success) {
                    val intent = Intent(this@AboutActivity, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }

        PracticeTime.csvExportLauncher = registerForActivityResult(
            ExportContract("text/comma-separated-values")
        ) { lifecycleScope.launch {
            PracticeTime.exportSessionsAsCsvCallback(applicationContext, it)
        } }


        findViewById<TextView>(R.id.about_tv_help).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        findViewById<TextView>(R.id.about_tv_backup).setOnClickListener {
            exportImportDialog.show()
        }

        findViewById<TextView>(R.id.about_tv_export_sessions).setOnClickListener {
            exportCSVDialog.show()
        }

        findViewById<TextView>(R.id.about_tv_support).setOnClickListener {
            startActivity(Intent(this, DonationsActivity::class.java))
        }

        findViewById<TextView>(R.id.about_tv_legal).setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java))
        }

        findViewById<TextView>(R.id.about_tv_licences).setOnClickListener {
            startActivity(Intent(this, LicenseActivity::class.java))
        }

        findViewById<TextView>(R.id.about_appinfo).setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }
    }
}
