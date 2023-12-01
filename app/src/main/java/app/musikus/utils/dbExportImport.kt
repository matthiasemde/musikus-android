/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.musikus.Musikus
import app.musikus.spacing

class ExportDatabaseContract : ActivityResultContracts.CreateDocument("*/*") {
    override fun createIntent(context: Context, input: String) =
        super.createIntent(context, input).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            }
        }
}

class ImportDatabaseContract : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>) =
        super.createIntent(context, input).apply {

        }
}

@Composable
fun ExportImportDialog(
    show: Boolean,
    onDismissHandler: () -> Unit,
) {
    if(!show) return
    Dialog(onDismissRequest = onDismissHandler) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /** Hero Icon */
                Icon(Icons.Default.Backup, contentDescription = null)

                /** Title */
                Text(
                    modifier = Modifier.padding(vertical = 16.dp),
                    text = "Backup Data",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                "With this dialog you can export your data to a file for backing up or transferring it to another device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledTonalButton(onClick = {
                    Musikus.exportDatabase()
                    onDismissHandler()
                }) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create backup")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = {
                    Musikus.importDatabase()
                    onDismissHandler()
                }) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load backup")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "*Note: Loading a backup will overwrite your current data.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                )
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissHandler) {
                    Text("Cancel")
                }
            }
        }
    }
}