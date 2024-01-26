package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.utils.TimeProvider
import app.musikus.utils.UiText
import kotlin.time.Duration

data class GoalInstanceWithProgress(
    val instance: GoalInstance,
    val progress: Duration
)

data class GoalDescriptionWithInstancesWithProgressAndLibraryItems(
    val description: GoalDescription,
    val instances: List<GoalInstanceWithProgress>,
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