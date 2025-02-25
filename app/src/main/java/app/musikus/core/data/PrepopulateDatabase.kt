/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.core.data

import android.util.Log
import app.musikus.core.domain.minus
import app.musikus.goals.data.GoalRepositoryImpl
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.domain.usecase.ArchiveGoalsUseCase
import app.musikus.goals.domain.usecase.CleanFutureGoalInstancesUseCase
import app.musikus.goals.domain.usecase.UpdateGoalsUseCase
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.pow
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

suspend fun prepopulateDatabase(
    database: MusikusDatabase,
) {
    listOf(
        LibraryFolderCreationAttributes(name = "Schupra"),
        LibraryFolderCreationAttributes(name = "Fagott"),
        LibraryFolderCreationAttributes(name = "Gesang"),
    ).forEach {
        database.libraryFolderDao.insert(it)
        Log.d("MainActivity", "Folder ${it.name} created")
        delay(10) // make sure folders have different createdAt values
    }

    database.libraryFolderDao.getAllAsFlow().first().let { folders ->
        // populate the libraryItem table on first run
        listOf(
            LibraryItemCreationAttributes(
                name = "Die Schöpfung",
                colorIndex = 0,
                libraryFolderId = Nullable(folders[0].id)
            ),
            LibraryItemCreationAttributes(
                name = "Beethoven Septett",
                colorIndex = 1,
                libraryFolderId = Nullable(folders[0].id)
            ),
            LibraryItemCreationAttributes(
                name = "Schostakowitsch 9.",
                colorIndex = 2,
                libraryFolderId = Nullable(folders[1].id)
            ),
            LibraryItemCreationAttributes(
                name = "Trauermarsch c-Moll",
                colorIndex = 3,
                libraryFolderId = Nullable(folders[1].id)
            ),
            LibraryItemCreationAttributes(name = "Adagio", colorIndex = 4, libraryFolderId = Nullable(folders[2].id)),
            LibraryItemCreationAttributes(
                name = "Eine kleine Gigue",
                colorIndex = 5,
                libraryFolderId = Nullable(folders[2].id)
            ),
            LibraryItemCreationAttributes(name = "Andantino", colorIndex = 6),
            LibraryItemCreationAttributes(name = "Klaviersonate", colorIndex = 7),
            LibraryItemCreationAttributes(name = "Trauermarsch", colorIndex = 8),
        ).forEach {
            database.libraryItemDao.insert(it)
            Log.d("MainActivity", "LibraryItem ${it.name} created")
            delay(10) // make sure items have different createdAt values
        }
    }

    database.libraryItemDao.getAllAsFlow().first().let { items ->
        listOf(
            GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = (1..5).random(),
                periodUnit = GoalPeriodUnit.DAY,
            ),
            GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = (1..5).random(),
                periodUnit = GoalPeriodUnit.WEEK,
            ),
            GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = false,
                periodInPeriodUnits = (1..5).random(),
                periodUnit = GoalPeriodUnit.MONTH,
            ),
            GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = (1..5).random(),
                periodUnit = GoalPeriodUnit.DAY,
            ),
            GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = (1..5).random(),
                periodUnit = GoalPeriodUnit.DAY,
            ),
            GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = false,
                periodInPeriodUnits = (1..5).random(),
                periodUnit = GoalPeriodUnit.WEEK,
            ),
        ).forEach { goalDescriptionCreationAttributes ->
            val offset = (
                if (goalDescriptionCreationAttributes.repeat) {
                    10L
                } else {
                    1L
                }
                ) * goalDescriptionCreationAttributes.periodInPeriodUnits

            database.goalDescriptionDao.insert(
                descriptionCreationAttributes = goalDescriptionCreationAttributes,
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    startTimestamp = when (goalDescriptionCreationAttributes.periodUnit) {
                        GoalPeriodUnit.DAY -> database.timeProvider.getStartOfDay().minusDays(offset)
                        GoalPeriodUnit.WEEK -> database.timeProvider.getStartOfWeek().minusWeeks(offset)
                        GoalPeriodUnit.MONTH -> database.timeProvider.getStartOfMonth().minusMonths(offset)
                    },
                    target = ((1..6).random() * 10 + 30).minutes
                ),
                libraryItemIds =
                if (goalDescriptionCreationAttributes.type == GoalType.NON_SPECIFIC) {
                    null
                } else {
                    listOf(items.random().id)
                },
            )
            delay(10)
        }

        val goalRepository = GoalRepositoryImpl(database)

        UpdateGoalsUseCase(
            goalRepository,
            ArchiveGoalsUseCase(
                goalRepository = goalRepository,
                cleanFutureGoalInstances = CleanFutureGoalInstancesUseCase(
                    goalRepository = goalRepository,
                    timeProvider = database.timeProvider
                )
            ),
            database.timeProvider
        )()

        (0..80).map { sessionNum ->
            sessionNum to SessionCreationAttributes(
                breakDuration = (5..20).random().minutes,
                rating = (1..5).random(),
                comment = "",
            )
        }.forEach { (sessionNum, session) ->
            var partialSessionDuration = 0.seconds
            database.sessionDao.insert(
                session,
                (1..(1..5).random()).map {
                    val duration = (10..20).random().minutes
                    val newSection = SectionCreationAttributes(
                        libraryItemId = items.random().id,
                        startTimestamp = database.timeProvider.now() - (
                            (
                                (sessionNum / 2) * // two sessions per day initially
                                    24 * 60 * 60 *
                                    1.02.pow(sessionNum.toDouble()) // exponential growth
                                ).toLong().seconds + duration + partialSessionDuration
                            ),
                        duration = duration,
                    )
                    partialSessionDuration += duration
                    return@map newSection
                }
            )
            delay(10)
        }
    }
}
