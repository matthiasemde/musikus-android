package app.musikus.ui

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.musikus.R
import app.musikus.shared.MultiFabState
import app.musikus.ui.activesession.ActiveSession
import app.musikus.ui.goals.Goals
import app.musikus.ui.goals.ProgressUpdate
import app.musikus.ui.library.Library
import app.musikus.ui.sessions.Sessions
import app.musikus.ui.sessions.editsession.EditSession
import app.musikus.ui.statistics.Statistics
import app.musikus.ui.statistics.goalstatistics.GoalStatistics
import app.musikus.ui.statistics.sessionstatistics.SessionStatistics
import app.musikus.ui.theme.MusikusTheme
import app.musikus.utils.ExportImportDialog
import app.musikus.utils.TimeProvider
import java.util.UUID


sealed class Screen(
    val route: String,
    val navigationBarData: NavigationBarData? = null
) {
    data object Sessions : Screen(
        route = "sessionList",
        NavigationBarData(
            title = R.string.navigationSessionsTitle,
            staticIcon = R.drawable.ic_sessions,
            animatedIcon = R.drawable.avd_sessions,
        )
    )

    data object EditSession : Screen(
        route = "editSession/{sessionId}",
    )

    data object ActiveSession : Screen(
        route = "activeSession",
    )

    data object Goals : Screen(
        route = "goals",
        NavigationBarData(
            title = R.string.navigationGoalsTitle,
            staticIcon = R.drawable.ic_goals,
            animatedIcon = R.drawable.avd_goals
        )
    )
    data object Statistics : Screen(
        route = "statistics",
        NavigationBarData(
            title = R.string.navigationStatisticsTitle,
            staticIcon = R.drawable.ic_bar_chart,
            animatedIcon = R.drawable.avd_bar_chart
        )
    )
    data object SessionStatistics : Screen(
        route = "sessionStatistics",
    )
    data object GoalStatistics : Screen(
        route = "goalStatistics",
    )
    data object Library : Screen(
        route = "library",
        NavigationBarData(
            title = R.string.navigationLibraryTitle,
            staticIcon = R.drawable.ic_library,
            animatedIcon = R.drawable.avd_library
        )
    )
    data object ProgressUpdate : Screen(
        route = "progressUpdate",
    )

    data class NavigationBarData(
        @StringRes val title: Int,
        @DrawableRes val staticIcon: Int,
        @DrawableRes val animatedIcon: Int
    )
}


private val navItems = listOf(
    Screen.Sessions,
    Screen.Goals,
    Screen.Statistics,
    Screen.Library
)


@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MusikusBottomBar(
    mainEventHandler: (event: MainUIEvent) -> Unit,
    navController: NavHostController,
    mainUiState: MainUiState
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showNavigationBar = currentDestination?.hierarchy?.any { dest ->
        navItems.any { it.route == dest.route }
    } == true

    if (!showNavigationBar)
        return

    Box {
        NavigationBar {
            navItems.forEach { screen ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val painterCount = 5
                var activePainter by remember { mutableIntStateOf(0) }
                val painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(screen.navigationBarData!!.staticIcon)
                )
                val animatedPainters = (0..painterCount).map {
                    rememberAnimatedVectorPainter(
                        animatedImageVector = AnimatedImageVector.animatedVectorResource(
                            screen.navigationBarData.animatedIcon
                        ),
                        atEnd = selected && activePainter == it
                    )
                }
                NavigationBarItem(
                    icon = {
                        Image(
                            painter = if (selected) animatedPainters[activePainter] else painter,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(screen.navigationBarData.title),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (!selected) activePainter =
                            (activePainter + 1) % painterCount
                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }

        /** Navbar Scrim */
        AnimatedVisibility(
            modifier = Modifier
                .matchParentSize()
                .zIndex(1f),
            visible = mainUiState.multiFabState == MultiFabState.EXPANDED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { mainEventHandler(MainUIEvent.CollapseMultiFab) }
                    )
            )
        }
    }
}

@Composable
fun MusikusApp(
    timeProvider: TimeProvider,
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    // This line ensures, that the app is only drawn when the proper theme is loaded
    // TODO: make sure this is the right way to do it
    val theme = uiState.activeTheme ?: return

    MusikusTheme(
        theme = theme
    ) {
        Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = uiState.snackbarHost)
        },
        bottomBar = {
            MusikusBottomBar(
                mainEventHandler = mainViewModel::onEvent,
                navController = navController,
                mainUiState = uiState
            )
        }
        ) { innerPadding ->

            Log.d("MainScreen", "paddingVals: $innerPadding")

            val animationDuration = 400
            NavHost(
                navController,
                startDestination = Screen.Sessions.route,
                Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                enterTransition = {
                    slideInVertically(
                        animationSpec = tween(animationDuration),
                        initialOffsetY = { fullHeight -> -(fullHeight / 10) }
                    ) + fadeIn(
                        animationSpec = tween(
                            animationDuration / 2,
                            animationDuration / 2
                        )
                    )
                },
                exitTransition = {
                    slideOutVertically(
                        animationSpec = tween(animationDuration),
                        targetOffsetY = { fullHeight -> (fullHeight / 10) }
                    ) + fadeOut(animationSpec = tween(animationDuration / 2))
                }
            ) {
                composable(
                    route = Screen.Sessions.route,
                    exitTransition = {
                        if(initialState.destination.route == Screen.ProgressUpdate.route) {
                            fadeOut(tween(0))
                        } else null
                    }
                ) {
                    Sessions(
                        mainUiState = uiState,
                        mainEventHandler = mainViewModel::onEvent,
                        onSessionEdit = { sessionId: UUID ->
                            navController.navigate(
                                Screen.EditSession.route.replace(
                                    "{sessionId}",
                                    sessionId.toString()
                                )
                            )
                        },
                        onSessionStart = {
                            navController.navigate(
                                Screen.ActiveSession.route
                            )
                        },
                    )
                }
                composable(
                    route = Screen.Goals.route,
                ) { Goals(
                    mainUiState = uiState,
                    mainEventHandler = mainViewModel::onEvent,
                    timeProvider = timeProvider
                ) }
                composable(
                    route = Screen.Statistics.route,
                ) { Statistics(
                    mainEventHandler = mainViewModel::onEvent,
                    mainViewModel = mainViewModel,
                    navigateToSessionStatistics = {
                        navController.navigate(Screen.SessionStatistics.route)
                    },
                    navigateToGoalStatistics = {
                        navController.navigate(Screen.GoalStatistics.route)
                    },
                    timeProvider = timeProvider
                ) }
                composable(
                    route = Screen.SessionStatistics.route,
                ) { SessionStatistics(navigateUp = navController::navigateUp) }
                composable(
                    route = Screen.GoalStatistics.route,
                ) { GoalStatistics(navigateUp = navController::navigateUp) }
                composable(
                    route = Screen.Library.route,
                ) { Library (
                    mainUiState = uiState,
                    mainEventHandler = mainViewModel::onEvent
                ) }
                composable(
                    route = Screen.ProgressUpdate.route,
                    enterTransition = { fadeIn(tween(0)) }
                ) { ProgressUpdate() }
                composable(
                    route = Screen.EditSession.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType})
                ) {backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getString("sessionId")
                        ?: return@composable navController.navigate(Screen.Sessions.route)

                    EditSession(
                        sessionToEditId = UUID.fromString(sessionId),
                        navigateUp = navController::navigateUp
                    )
                }
                composable(
                    route = Screen.ActiveSession.route,
                ) { ActiveSession(
                        mainUiState = uiState,
                        mainEventHandler = mainViewModel::onEvent,
                        timeProvider = timeProvider,
                        navigateUp = navController::navigateUp
                ) }
            }

            /** Export / Import Dialog */
            ExportImportDialog(
                show = uiState.showExportImportDialog,
                onDismissHandler = { mainViewModel.onEvent(MainUIEvent.HideExportImportDialog) }
            )

            // TODO remove
            // if there is a new session added to the intent, navigate to progress update
    //        intent.extras?.getLong("KEY_SESSION")?.let { _ ->
    //            intent.removeExtra("KEY_SESSION")
    ////                        mainViewModel.navigateTo(Screen.ProgressUpdate.route)
    //        }
        }

    }
}