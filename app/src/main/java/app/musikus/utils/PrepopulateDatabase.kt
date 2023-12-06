package app.musikus.utils

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.repository.GoalRepositoryImpl
import app.musikus.repository.LibraryRepositoryImpl
import app.musikus.repository.SessionRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit
import kotlin.math.pow

suspend fun prepopulateDatabase(
    db: SupportSQLiteDatabase,
) {
    val musikusDb = db as MusikusDatabase

    val libraryRepository = LibraryRepositoryImpl(
        itemDao = musikusDb.libraryItemDao,
        folderDao = musikusDb.libraryFolderDao,
    )

    val goalRepository = GoalRepositoryImpl(
        goalInstanceDao = musikusDb.goalInstanceDao,
        goalDescriptionDao = musikusDb.goalDescriptionDao,
    )

    val sessionRepository = SessionRepositoryImpl(
        sessionDao = musikusDb.sessionDao,
        sectionDao = musikusDb.sectionDao,
    )

    listOf(
        LibraryFolderCreationAttributes(name = "Schupra"),
        LibraryFolderCreationAttributes(name = "Fagott"),
        LibraryFolderCreationAttributes(name = "Gesang"),
    ).forEach {
        libraryRepository.addFolder(it)
        Log.d("MainActivity", "Folder ${it.name} created")
        delay(10) //make sure folders have different createdAt values
    }

    libraryRepository.folders.first().let { folders ->
        // populate the libraryItem table on first run
        listOf(
            LibraryItemCreationAttributes(name = "Die Schöpfung", colorIndex = 0, libraryFolderId = Nullable(folders[0].id)),
            LibraryItemCreationAttributes(name = "Beethoven Septett",colorIndex = 1,libraryFolderId = Nullable(folders[0].id)),
            LibraryItemCreationAttributes(name = "Schostakowitsch 9.", colorIndex = 2, libraryFolderId = Nullable(folders[1].id)),
            LibraryItemCreationAttributes(name = "Trauermarsch c-Moll", colorIndex = 3, libraryFolderId = Nullable(folders[1].id)),
            LibraryItemCreationAttributes(name = "Adagio", colorIndex = 4, libraryFolderId = Nullable(folders[2].id)),
            LibraryItemCreationAttributes(name = "Eine kleine Gigue", colorIndex = 5, libraryFolderId = Nullable(folders[2].id)),
            LibraryItemCreationAttributes(name = "Andantino", colorIndex = 6),
            LibraryItemCreationAttributes(name = "Klaviersonate", colorIndex = 7),
            LibraryItemCreationAttributes(name = "Trauermarsch", colorIndex = 8),
        ).forEach {
            libraryRepository.addItem(it)
            Log.d("MainActivity", "LibraryItem ${it.name} created")
            delay(10) //make sure items have different createdAt values
        }
    }


    libraryRepository.items.first().let { items ->
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
            goalRepository.add(
                goalDescriptionCreationAttributes,
                getCurrentDateTime().minus(
                    (
                            (if (goalDescriptionCreationAttributes.repeat) 10L else 1L) *
                                    goalDescriptionCreationAttributes.periodInPeriodUnits
                            ),
                    when(goalDescriptionCreationAttributes.periodUnit) {
                        GoalPeriodUnit.DAY -> ChronoUnit.DAYS
                        GoalPeriodUnit.WEEK -> ChronoUnit.WEEKS
                        GoalPeriodUnit.MONTH -> ChronoUnit.MONTHS
                    },
                ),
                if (goalDescriptionCreationAttributes.type == GoalType.NON_SPECIFIC) null else listOf(items.random()),
                ((1..6).random() * 10 + 30) * 60
            )
            delay(10)
        }

        goalRepository.updateGoals()

        (0..80).map { sessionNum ->
            sessionNum to SessionCreationAttributes(
                breakDuration = (5..20).random() * 60,
                rating = (1..5).random(),
                comment = "",
            )
        }.forEach { (sessionNum, session) ->
            sessionRepository.add(
                session,
                (1..(1..5).random()).map { SectionCreationAttributes(
                    libraryItemId = Nullable(items.random().id),
                    startTimestamp = getCurrentDateTime().minus(
                        (
                                (sessionNum / 2) * // two sessions per day initially
                                        24 * 60 * 60 *
                                        1.02.pow(sessionNum.toDouble()) // exponential growth
                                ).toLong(),
                        ChronoUnit.SECONDS
                    ),
                    duration = (10..20).random() * 60,
                )
                }
            )
            delay(10)
        }
    }
}