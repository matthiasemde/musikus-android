package app.musikus.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.musikus.R
import app.musikus.ui.activesession.ActiveSession
import app.musikus.ui.components.MultiFabState
import app.musikus.ui.components.conditional
import app.musikus.ui.goals.GoalsScreen
import app.musikus.ui.goals.ProgressUpdate
import app.musikus.ui.library.Library
import app.musikus.ui.sessions.Sessions
import app.musikus.ui.sessions.editsession.EditSession
import app.musikus.ui.settings.addSettingsNavigationGraph
import app.musikus.ui.statistics.addStatisticsNavigationGraph
import app.musikus.ui.theme.MusikusTheme
import app.musikus.utils.TimeProvider
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText
import java.util.UUID

const val DEEP_LINK_KEY = "argument"

sealed class Screen(
    val route: String,
    val displayData: DisplayData? = null,
) {

    data object Sessions : Screen(
        route = "sessionList",
        displayData = DisplayData(
            title = UiText.StringResource(R.string.navigationSessionsTitle),
            icon = UiIcon.IconResource(R.drawable.ic_sessions),
            animatedIcon = R.drawable.avd_sessions,
        )
    )

    data object Goals : Screen(
        route = "goals",
        displayData = DisplayData(
            title = UiText.StringResource(R.string.navigationGoalsTitle),
            icon = UiIcon.IconResource(R.drawable.ic_goals),
            animatedIcon = R.drawable.avd_goals
        )
    )

    data object Statistics : Screen(
        route = "statistics",
        displayData = DisplayData(
            title = UiText.StringResource(R.string.navigationStatisticsTitle),
            icon = UiIcon.IconResource(R.drawable.ic_bar_chart),
            animatedIcon = R.drawable.avd_bar_chart
        )
    )

    data object Library : Screen(
        route = "library",
        displayData = DisplayData(
            title = UiText.StringResource(R.string.navigationLibraryTitle),
            icon = UiIcon.IconResource(R.drawable.ic_library),
            animatedIcon = R.drawable.avd_library
        )
    )

    data object ActiveSession : Screen(
        route = "activeSession",
    )

    data object EditSession : Screen(
        route = "editSession/{sessionId}",
    )


    data object SessionStatistics : Screen(
        route = "sessionStatistics",
    )
    data object GoalStatistics : Screen(
        route = "goalStatistics",
    )

    data object ProgressUpdate : Screen(
        route = "progressUpdate",
    )

    data object Settings : Screen(
        route = "settings",
    )

    sealed class SettingsOptions(
        route: String,
        displayData: DisplayData
    ) : Screen(route, displayData) {
        data object About : Screen(
            route = "settings/about",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.about_app_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.Info),
            )
        )

        data object Backup : Screen(
            route = "settings/backup",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.backup_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.CloudUpload),
            )
        )

        data object Donate : Screen(
            route = "settings/donate",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.donations_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.Favorite),
            )
        )

        data object Appearance : Screen(
            route = "settings/appearance",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.appearance_title),
                icon = UiIcon.IconResource(R.drawable.ic_appearance),
            )
        )
    }


    data class DisplayData(
        val title: UiText,
        val icon: UiIcon,
        @DrawableRes val animatedIcon: Int? = null
    )
}


fun NavController.navigateTo(screen: Screen) {
    navigate(screen.route)
}


@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MusikusBottomBar(
    navController: NavHostController,
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
) {

    val navItems = listOf(
        Screen.Sessions,
        Screen.Goals,
        Screen.Statistics,
        Screen.Library
    )

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
                    image = screen.displayData!!.icon.asIcon()
                )
                val animatedPainters = (0..painterCount).map {
                    rememberAnimatedVectorPainter(
                        animatedImageVector = AnimatedImageVector.animatedVectorResource(
                            screen.displayData.animatedIcon!!
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
                            text = screen.displayData.title.asAnnotatedString(),
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
                        onClick = { mainEventHandler(MainUiEvent.CollapseMultiFab) }
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
                    mainEventHandler = mainViewModel::onUiEvent,
                    navController = navController,
                    mainUiState = uiState
                )
            }
        ) { innerPadding ->

            val animationDuration = 400

            val addBottomPadding = navController.currentDestination?.route != Screen.ActiveSession.route

            NavHost(
                navController = navController,
                startDestination = Screen.Sessions.route,
                modifier = Modifier
                    .conditional(addBottomPadding) {
                        padding(bottom = innerPadding.calculateBottomPadding())
                    },
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
                        mainEventHandler = mainViewModel::onUiEvent,
                        navigateTo = navController::navigateTo,
                        onSessionEdit = { sessionId: UUID ->
                            navController.navigate(
                                Screen.EditSession.route.replace(
                                    "{sessionId}",
                                    sessionId.toString()
                                )
                            )
                        },
                        onSessionStart = { navController.navigate(Screen.ActiveSession.route) },
                    )
                }
                composable(
                    route = Screen.Goals.route,
                ) { GoalsScreen(
                    mainUiState = uiState,
                    mainEventHandler = mainViewModel::onUiEvent,
                    navigateTo = navController::navigateTo,
                    timeProvider = timeProvider
                ) }
                composable(
                    route = Screen.Library.route,
                ) { Library (
                    mainUiState = uiState,
                    mainEventHandler = mainViewModel::onUiEvent,
                    navigateTo = navController::navigateTo
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
                    deepLinks = listOf(navDeepLink {
                        uriPattern = "musikus://activeSession/{$DEEP_LINK_KEY}"
                    })
                ) { backStackEntry ->
                    ActiveSession(
                        navigateUp = navController::navigateUp,
                        deepLinkArgument = backStackEntry.arguments?.getString(DEEP_LINK_KEY)
                    )
                }

                // Statistics
                addStatisticsNavigationGraph(
                    navController = navController,
                    mainUiState = uiState,
                    mainEventHandler = mainViewModel::onUiEvent,
                    timeProvider = timeProvider
                )

                // Settings
                addSettingsNavigationGraph(navController)
            }
        }
    }
}