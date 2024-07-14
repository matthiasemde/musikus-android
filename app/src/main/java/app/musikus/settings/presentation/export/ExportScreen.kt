/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import app.musikus.R
import app.musikus.core.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navigateUp: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export session data") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.large),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = stringResource(id = R.string.export_csv_dialog_text)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.extraLarge),
                onClick = { /*TODO*/ }
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text("Export sessions".capitalize(Locale.current))
            }
            Text(
                text = stringResource(id = R.string.export_csv_dialog_note),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}