/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.help

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import app.musikus.R
import app.musikus.core.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navigateUp: () -> Unit
) {

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_help_title)) },
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
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = stringResource(R.string.settings_help_tips_title),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(stringResource(R.string.settings_help_tips_text))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                for (bulletPoint in stringArrayResource(R.array.settings_help_tips_bulletlist)) {
                    Text(text = "\u2022\t" + bulletPoint)
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
            Column(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            ) {
                Text(
                    text = stringResource(R.string.settings_help_tutorial_title),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.extraLarge),
                    onClick = {
                        /* TODO restart intro */
                        Toast.makeText(context, context.getString(R.string.core_coming_soon), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.settings_help_tutorial_replay_intro))
                }
            }
        }
    }
}