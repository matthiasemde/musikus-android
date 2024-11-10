/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.about

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import app.musikus.R
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    navigateUp: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MusikusTopBar(
                isTopLevel = false,
                title = UiText.StringResource(R.string.settings_licenses_title),
                scrollBehavior = scrollBehavior,
                navigateUp = navigateUp
            )
        }
    ) { paddingValues ->
        val licenses = LocalContext.current.resources.openRawResource(R.raw.third_party_licenses).bufferedReader().use {
            it.readText().split("\n")
        }

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.small)
        ) {
            licenses.forEach {
                item {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
