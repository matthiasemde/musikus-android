package app.musikus.goals.domain

import app.musikus.core.data.GoalDescriptionWithLibraryItems
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.library.data.daos.LibraryItem
import app.musikus.utils.TimeProvider
import app.musikus.utils.UiText
import kotlin.time.Duration

data class GoalDescriptionWithInstancesWithProgressAndLibraryItems(
    val description: GoalDescription,
    val instancesWithProgress: List<Pair<GoalInstance, Duration>>,
    val libraryItems: List<LibraryItem>
) {
    val goalDescriptionWithLibraryItems by lazy {
        GoalDescriptionWithLibraryItems(
            description = description,
            libraryItems = libraryItems
        )
    }
}

data class GoalInstanceWithProgressAndDescriptionWithLibraryItems(
    val description: GoalDescriptionWithLibraryItems,
    val instance: GoalInstance,
    val progress: Duration
) {
    val title : UiText
        get() = description.title

    val subtitle by lazy { description.subtitle(instance) }

    fun endTimestampInLocalTimezone(timeProvider: TimeProvider) =
        description.endOfInstanceInLocalTimezone(instance, timeProvider)
}