/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.musikus.ui.components.TwoLiner
import app.musikus.ui.components.TwoLinerData
import app.musikus.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    navigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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

        val appearanceMenuItems = listOf(
            TwoLinerData(
                firstLine = UiText.DynamicString("Language"),
                secondLine = UiText.DynamicString("System default"),
            ),
            TwoLinerData(
                firstLine = UiText.DynamicString("Theme"),
                secondLine = UiText.DynamicString("System default"),
            ),
        )

        Column(Modifier.padding(paddingValues)) {
            for (item in appearanceMenuItems) {
                 TwoLiner(data = item)
            }
        }
    }
}