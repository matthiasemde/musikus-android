/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.musikus.R
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.MainMenu
import app.musikus.core.presentation.components.fadingEdge
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusTheme
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.ObserveAsEvents
import app.musikus.menu.domain.ColorSchemeSelections
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    timeProvider: TimeProvider,
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    // This line ensures, that the app is only drawn when the proper theme is loaded
    // TODO: make sure this is the right way to do it
    val themeUiState = uiState.themeUiState
    val theme = themeUiState.activeTheme ?: return
    val colorScheme = themeUiState.activeColorScheme ?: return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember {
        derivedStateOf {
            navBackStackEntry?.toScreen()
        }
    }
    val currentTab by remember {
        derivedStateOf {
            currentRoute?.let { if (it is Screen.Home) it.tab else null }
        }
    }

    MusikusTheme(
        theme = theme,
        colorScheme = colorScheme
    ) {
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        ObserveAsEvents(viewModel.eventChannel) { event ->
            when (event) {
                is MainEvent.OpenMainDrawer -> drawerState.open()
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    MainMenu(
                        navigateTo = { navController.navigate(it) },
                        onDismiss = { scope.launch { drawerState.close() } },
                        theme = theme
                    )
                }
            },
            gesturesEnabled = drawerState.isOpen, // only allow gestures to close the drawer once it is open
        ) {
            // This is the main scaffold of the app which contains the bottom navigation,
            // the snackbar host and the nav host
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = uiState.snackbarHost)
                },
                bottomBar = {
                    MusikusBottomBar(
                        mainUiState = uiState,
                        mainEventHandler = eventHandler,
                        currentTab = currentTab,
                        onTabSelected = { selectedTab ->
                            navController.navigate(Screen.Home(selectedTab)) {
                                popUpTo(Screen.Home(HomeTab.default)) {
                                    inclusive = selectedTab == HomeTab.default
                                }
                            }
                        },
                    )
                }
            ) { innerPadding ->

                // Calculate the height of the bottom bar so we can add it as  padding in the home tabs
                val bottomBarHeight = innerPadding.calculateBottomPadding()

                MusikusNavHost(
                    navController = navController,
                    mainUiState = uiState,
                    mainEventHandler = eventHandler,
                    bottomBarHeight = bottomBarHeight,
                    timeProvider = timeProvider,
                    isMainMenuOpen = drawerState.isOpen,
                    closeMainMenu = { scope.launch { drawerState.close() } }
                )
            }
        }

        // Announcement dialog
        if (uiState.showAnnouncement) {
            AnnouncementDialog(
                onDismissRequest = { eventHandler(MainUiEvent.DismissAnnouncement) }
            )
        }
    }
}

@Composable
fun AnnouncementDialog(
    onDismissRequest: () -> Unit,
) {
    Dialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier.padding(vertical = 64.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.large)
                        .fadingEdge(scrollState)
                        .verticalScroll(scrollState)
                        .weight(1f, fill = false)
                ) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                    // Heading 1
                    Text(
                        style = MaterialTheme.typography.headlineMedium,
                        text = stringResource(id = R.string.core_announcement_heading_1)
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    // Paragraph 1
                    Text(text = stringResource(id = R.string.core_announcement_paragraph_1))

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    // Heading 2
                    Text(
                        style = MaterialTheme.typography.titleLarge,
                        text = stringResource(id = R.string.core_announcement_heading_2)
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    // Paragraph 2
                    Text(text = stringResource(id = R.string.core_announcement_paragraph_2))

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    // Heading 3
                    Text(
                        style = MaterialTheme.typography.titleLarge,
                        text = stringResource(id = R.string.core_announcement_heading_3)
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    // Paragraph 3
                    Text(text = stringResource(id = R.string.core_announcement_paragraph_3))

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    // Paragraph 4
                    Text(text = stringResource(id = R.string.core_announcement_paragraph_4))

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    // Paragraph 5
                    Text(text = stringResource(id = R.string.core_announcement_paragraph_5))
                }

                DialogActions(
                    confirmButtonText = stringResource(id = R.string.core_announcement_confirm),
                    onConfirmHandler = onDismissRequest,
                )
            }
        }
    }
}

@MusikusPreviewElement1
@Composable
private fun PreviewAnnouncementDialog(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme) {
        AnnouncementDialog(onDismissRequest = {})
    }
}
