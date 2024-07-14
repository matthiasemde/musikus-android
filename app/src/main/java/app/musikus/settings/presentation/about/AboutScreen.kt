/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import app.musikus.BuildConfig
import app.musikus.R
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.components.TwoLiner
import app.musikus.core.presentation.components.TwoLinerData
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit
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

        val aboutScreenItems = listOf(
            listOf(
                TwoLinerData(
                firstLine = UiText.StringResource(R.string.development_title),
                secondLine = UiText.StringResource(R.string.development_text)
            )
            ),
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
                    onClick = { navigateTo(Screen.License) }
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
    }
}