/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package de.practicetime.practicetime.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R

class ExportContract(
    private val mimeType: String
) : ActivityResultContracts.CreateDocument() {
    override fun createIntent(context: Context, input: String) =
        super.createIntent(context, input).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type=mimeType
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}

class ImportDatabaseContract : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>) =
        super.createIntent(context, input).apply {
            type = "application/octet-stream"
            addCategory(Intent.CATEGORY_OPENABLE)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}


class ExportImportDialog(
    context: Activity,
) {

    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater
    private val dialogView = inflater.inflate(
        R.layout.dialog_export_import,
        null,
    )

    private val exportButton = dialogView.findViewById<MaterialButton>(R.id.btn_export)
    private val importButton = dialogView.findViewById<MaterialButton>(R.id.btn_import)

    private lateinit var alertDialog: AlertDialog

    init {
        alertDialogBuilder.apply {
            setView(dialogView)
            setCancelable(false)

            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }

        exportButton.setOnClickListener {
            PracticeTime.exportDatabase()
            alertDialog.dismiss()
        }

        importButton.setOnClickListener {
            PracticeTime.importDatabase()
            alertDialog.dismiss()
        }

        alertDialog = alertDialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(context, R.drawable.dialog_background)
        )
    }

    fun show() {
        alertDialog.show()
    }
}


class ExportSessionsCSVDialog(
    context: Activity,
) {

    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater
    private val dialogView = inflater.inflate(
        R.layout.dialog_export_csv,
        null,
    )

    private val exportButton = dialogView.findViewById<MaterialButton>(R.id.btn_export)

    private lateinit var alertDialog: AlertDialog

    init {
        alertDialogBuilder.apply {
            setView(dialogView)
            setCancelable(false)

            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }

        exportButton.setOnClickListener {
            PracticeTime.exportSessionsAsCsv()
            alertDialog.dismiss()
        }

        alertDialog = alertDialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(context, R.drawable.dialog_background)
        )
    }

    fun show() {
        alertDialog.show()
    }
}
