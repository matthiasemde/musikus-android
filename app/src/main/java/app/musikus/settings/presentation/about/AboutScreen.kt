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
                title = { Text(stringResource(R.string.settings_about_title)) },
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
        val context = LocalContext.current

        val aboutScreenItems = listOf(
            listOf(
                TwoLinerData(
                    firstLine = UiText.StringResource(R.string.settings_about_developers_first_line),
                    secondLine = UiText.StringResource(R.string.settings_about_developers_second_line)
                )
            ),
            listOf(
                TwoLinerData(
                    firstLine = UiText.StringResource(R.string.settings_about_publisher_first_line),
                    secondLine = UiText.StringResource(R.string.settings_about_publisher_second_line),
                ),
                TwoLinerData(
                    firstLine = UiText.StringResource(R.string.settings_about_privacy_policy_first_line),
                    onClick = {
                        val openUrlIntent = Intent(Intent.ACTION_VIEW)
                        openUrlIntent.data = Uri.parse(context.getString(R.string.settings_about_privacy_policy_url))
                        ContextCompat.startActivity(context, openUrlIntent, null)
                    },
                    trailingIcon = UiIcon.DynamicIcon(Icons.AutoMirrored.Filled.OpenInNew)
                )
            ),
            listOf(
                TwoLinerData(
                    firstLine = UiText.StringResource(R.string.settings_about_version_first_line),
                    secondLine = UiText.DynamicString("${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})")
                ),
                TwoLinerData(
                    firstLine = UiText.StringResource(R.string.settings_about_licenses_first_line),
                    onClick = { navigateTo(Screen.License()) }
                ),
                TwoLinerData(
                    secondLine = UiText.StringResource(R.string.settings_about_copyright_second_line)
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
