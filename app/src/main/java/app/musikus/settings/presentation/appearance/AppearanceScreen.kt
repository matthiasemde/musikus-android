/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.core.presentation.components.TwoLiner
import app.musikus.core.presentation.components.TwoLinerData
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel = hiltViewModel(),
    navigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

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
                onClick = { eventHandler(AppearanceUiEvent.ShowLanguageDialog) }
            ),
            TwoLinerData(
                firstLine = UiText.DynamicString("Theme"),
                secondLine = UiText.DynamicString(uiState.themeUiState.currentTheme.label),
                onClick = { eventHandler(AppearanceUiEvent.ShowThemeDialog) }
            ),
            TwoLinerData(
                firstLine = UiText.DynamicString("Color scheme"),
                secondLine = UiText.DynamicString(uiState.colorSchemeUiState.currentColorScheme.label),
                onClick = { eventHandler(AppearanceUiEvent.ShowColorSchemeDialog) }
            ),
        )

        Column(Modifier.padding(paddingValues)) {
            for (item in appearanceMenuItems) {
                 TwoLiner(data = item)
            }
        }
    }

    if(uiState.languageUiState.languageDialogShowing) {
        Dialog(
            onDismissRequest = { eventHandler(AppearanceUiEvent.HideLanguageDialog) }
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.medium)
                        .padding(horizontal = (MaterialTheme.spacing.medium + MaterialTheme.spacing.small)),
                ) {
                    Text(
                        text = "Language",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                    text =
                        "More languages are coming soon! " +
                        "If you want to help in translating the app into your native language, " +
                        "please contact us at contribute@musikus.app",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacing.large,
                            vertical = MaterialTheme.spacing.medium
                        ),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { eventHandler(AppearanceUiEvent.HideLanguageDialog) }
                    ) {
                        Text (text = "Awesome!")
                    }
                }
            }
        }
    }

    if(uiState.themeUiState.themeDialogShowing) {
        Dialog(
            onDismissRequest = { eventHandler(AppearanceUiEvent.HideThemeDialog) }
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.medium)
                        .padding(horizontal = (MaterialTheme.spacing.medium + MaterialTheme.spacing.small)),
                ) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                for (selection in ThemeSelections.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { eventHandler(AppearanceUiEvent.ChangeTheme(selection)) }
                            .padding(horizontal = MaterialTheme.spacing.medium)
                        ,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.themeUiState.currentTheme == selection,
                            onClick = { eventHandler(AppearanceUiEvent.ChangeTheme(selection)) }
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(text = selection.label)
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }
        }
    }

    if(uiState.colorSchemeUiState.colorSchemeDialogShowing) {
        Dialog(
            onDismissRequest = { eventHandler(AppearanceUiEvent.HideColorSchemeDialog) }
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.medium)
                        .padding(horizontal = (MaterialTheme.spacing.medium + MaterialTheme.spacing.small)),
                ) {
                    Text(
                        text = "ColorScheme",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                for (selection in ColorSchemeSelections.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { eventHandler(AppearanceUiEvent.ChangeColorScheme(selection)) }
                            .padding(horizontal = MaterialTheme.spacing.medium)
                        ,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.colorSchemeUiState.currentColorScheme == selection,
                            onClick = { eventHandler(AppearanceUiEvent.ChangeColorScheme(selection)) }
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        TwoLiner(
                            data = TwoLinerData(
                                firstLine = UiText.DynamicString(selection.label),
                                secondLine = UiText.DynamicString(selection.description)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }
        }
    }
}