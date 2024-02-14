package app.musikus.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import app.musikus.R
import app.musikus.ui.activesession.ActiveSession
import app.musikus.ui.home.HomeScreen
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
    open val displayData: DisplayData? = null,
) {

    data object Home : Screen(
        route = "home"
    )

    sealed class HomeTab(
        val subRoute: String,
        override val displayData: DisplayData
    ) : Screen("home/$subRoute") {
        data object Sessions : HomeTab(
            subRoute = "sessionList",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationSessionsTitle),
                icon = UiIcon.IconResource(R.drawable.ic_sessions),
                animatedIcon = R.drawable.avd_sessions,
            )
        )

        data object Goals : HomeTab(
            subRoute = "goals",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationGoalsTitle),
                icon = UiIcon.IconResource(R.drawable.ic_goals),
                animatedIcon = R.drawable.avd_goals
            )
        )

        data object Statistics : HomeTab(
            subRoute = "statistics",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationStatisticsTitle),
                icon = UiIcon.IconResource(R.drawable.ic_bar_chart),
                animatedIcon = R.drawable.avd_bar_chart
            )
        )

        data object Library : HomeTab(
            subRoute = "library",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationLibraryTitle),
                icon = UiIcon.IconResource(R.drawable.ic_library),
                animatedIcon = R.drawable.avd_library
            )
        )
        companion object {
            val allTabs = listOf(Sessions, Goals, Statistics, Library)
            val defaultTab = Sessions
        }
    }

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

    data object Settings : Screen(
        route = "settings",
    )

    sealed class SettingsOption(
        subRoute: String,
        override val displayData: DisplayData
    ) : Screen("settings/$subRoute") {
        data object About : SettingsOption(
            subRoute = "about",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.about_app_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.Info),
            )
        )

        data object Backup : SettingsOption(
            subRoute = "backup",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.backup_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.CloudUpload),
            )
        )

        data object Donate : SettingsOption(
            subRoute = "donate",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.donations_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.Favorite),
            )
        )

        data object Appearance : SettingsOption(
            subRoute = "appearance",
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

@Composable
fun MusikusApp(
    timeProvider: TimeProvider,
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = mainViewModel::onUiEvent

    // This line ensures, that the app is only drawn when the proper theme is loaded
    // TODO: make sure this is the right way to do it
    val theme = uiState.activeTheme ?: return


    MusikusTheme(
        theme = theme
    ) {

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                getEnterTransition()
            },
            exitTransition = {
                getExitTransition()
            }
        ) {
            navigation(
                route = Screen.Home.route,
                startDestination = Screen.HomeTab.defaultTab.route
            ) {
                composable(
                    route = "home/{tab}",
                    arguments = listOf(navArgument("tab") {
                        nullable = true
                    })
                ) { backStackEntry ->
                    val tabRoute = backStackEntry.arguments?.getString("tab")
                    val tab = Screen.HomeTab.allTabs.firstOrNull { it.subRoute == tabRoute }

                    HomeScreen(
                        mainUiState = uiState,
                        mainEventHandler = eventHandler,
                        initialTab = tab,
                        navController = navController,
                        timeProvider = timeProvider
                    )
                }
            }

            composable(
                route = Screen.EditSession.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType})
            ) {backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                    ?: return@composable navController.navigate(Screen.HomeTab.Sessions.route)

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
            )

            // Settings
            addSettingsNavigationGraph(navController)
        }
    }
}

const val ANIMATION_BASE_DURATION = 400

fun AnimatedContentTransitionScope<NavBackStackEntry>.getEnterTransition() : EnterTransition {
    val initialRoute = initialState.destination.route ?: return fadeIn()
    val targetRoute = targetState.destination.route ?: return fadeIn()

    return when {
        // when changing to active session, slide in from the bottom
        targetRoute == Screen.ActiveSession.route -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseOut),
                initialOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when changing from active session, stay invisible until active session has slid in from the bottom
        initialRoute == Screen.ActiveSession.route -> {
            fadeIn(
                initialAlpha = 1f,
                animationSpec = tween(durationMillis = ANIMATION_BASE_DURATION)
            )
        }
        else -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetY = { fullHeight -> -(fullHeight / 10) }
            ) + fadeIn(
                animationSpec = tween(
                    ANIMATION_BASE_DURATION / 2,
                    ANIMATION_BASE_DURATION / 2
                )
            )
        }
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.getExitTransition() : ExitTransition {
    val initialRoute = initialState.destination.route ?: return fadeOut()
    val targetRoute = targetState.destination.route ?: return fadeOut()

    return when {
        // when changing to active session, show immediately
        targetRoute == Screen.ActiveSession.route -> {
            fadeOut(tween(durationMillis = 1, delayMillis = ANIMATION_BASE_DURATION))
        }

        // when changing from active session, slide out to the bottom
        initialRoute == Screen.ActiveSession.route -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseIn),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        }

        // default animation
        else -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetY = { fullHeight -> (fullHeight / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }
    }
}
