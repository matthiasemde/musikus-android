/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.settings.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import app.musikus.BuildConfig
import app.musikus.R
import app.musikus.ui.components.TwoLiner
import app.musikus.ui.components.TwoLinerData
import app.musikus.ui.theme.spacing
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
        val privacyPolicyUrl = stringResource(id = R.string.url_privacy)
        val context = LocalContext.current

        var showLicenses by remember { mutableStateOf(false) }

        val aboutScreenItems = listOf(
            listOf(TwoLinerData(
                firstLine = UiText.StringResource(R.string.development_title),
                secondLine = UiText.StringResource(R.string.development_text)
            )),
            listOf(
                TwoLinerData(
                    firstLine = UiText.DynamicString("Publisher"),
                    secondLine = UiText.DynamicString("Matthias Emde\nConnollystraße 25\n80809 Munich, Germany\ncontact@musikus.app"),
                ),
                TwoLinerData(
                    firstLine = UiText.StringResource(R.string.privacy_policy_title),
                    onClick = {
                        val openUrlIntent = Intent(Intent.ACTION_VIEW)
                        openUrlIntent.data = Uri.parse(privacyPolicyUrl)
                        ContextCompat.startActivity(context, openUrlIntent, null)
                    },
                    trailingIcon = UiIcon.DynamicIcon(Icons.AutoMirrored.Filled.OpenInNew)
                )
            ),
            listOf(
                TwoLinerData(
                    firstLine = UiText.DynamicString("Version"),
                    secondLine = UiText.DynamicString("${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})")
                ),
                TwoLinerData(
                    firstLine = UiText.DynamicString("Licenses"),
                    onClick = { showLicenses = true }
                ),
                TwoLinerData(
                    secondLine = UiText.DynamicString(
                        "Copyright Matthias Emde, Michael Prommersberger\n" +
                        "Licensed under the Mozilla Public License Version 2.0"
                    )
                )
            ),
        )

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {

            for (group in aboutScreenItems) {
                for (item in group) {
                    TwoLiner(data = item)
                }
                if (group != aboutScreenItems.last()) {
                    HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
                }
            }
        }

        if(showLicenses) {
            Dialog(
                onDismissRequest = { showLicenses = false },
                properties = DialogProperties(dismissOnClickOutside = true)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                        Text(text = "Licenses")
                        TextButton(onClick = { showLicenses = false }) {
                            Text(text = "Cancel")
                        }
                    }
                }
            }
        }
    }
}