/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.donate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat.startActivity
import app.musikus.R
import app.musikus.core.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_donate_title)) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.components_top_bar_back_description),
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.extraLarge)
        ) {
            Text(
                text = stringResource(R.string.settings_donate_text),
                lineHeight = 1.6.em,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            for (bulletPoint in stringArrayResource(R.array.settings_donate_bulletlist)) {
                Text(text = "\u2022\t" + bulletPoint)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val openUrlIntent = Intent(Intent.ACTION_VIEW)
                    openUrlIntent.data = Uri.parse(context.getString(R.string.settings_donate_url))
                    startActivity(context, openUrlIntent, null)
                }
            ) {
                Text(
                    text = stringResource(R.string.settings_donate_button_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
