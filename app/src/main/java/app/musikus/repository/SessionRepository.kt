package app.musikus.repository

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.Section
import app.musikus.database.daos.SectionDao
import app.musikus.database.daos.Session
import app.musikus.database.daos.SessionDao
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

interface SessionRepository {
    val sessions : Flow<List<Session>>
    val sections : Flow<List<Section>>

    val sessionsWithSectionsWithLibraryItems : Flow<List<SessionWithSectionsWithLibraryItems>>
    suspend fun sessionWithSectionsWithLibraryItems(id: UUID) : SessionWithSectionsWithLibraryItems

    fun sessionsInTimeframe (timeframe: Timeframe) : Flow<List<SessionWithSectionsWithLibraryItems>>

    suspend fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) : Flow<List<Section>>
    suspend fun sectionsForGoal (
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ) : Flow<List<Section>>

    /** Mutators */
    /** Add */
    suspend fun add(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionsCreationAttributes: List<SectionCreationAttributes>
    ) : UUID

    /** Delete / Restore */
    suspend fun delete(sessions: List<Session>)
    suspend fun restore(sessions: List<Session>)

    /** Clean */
    suspend fun clean()
}

class SessionRepositoryImpl(
    private val sessionDao : SessionDao,
    private val sectionDao : SectionDao,
) : SessionRepository {


    /** Accessors */
    override val sessions = sessionDao.getAllAsFlow()
    override val sections = sectionDao.getAllAsFlow()

    override val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItemsAsFlow()
    override suspend fun sessionWithSectionsWithLibraryItems(id: UUID) = sessionDao.getWithSectionsWithLibraryItems(id)

    override fun sessionsInTimeframe (timeframe: Timeframe) : Flow<List<SessionWithSectionsWithLibraryItems>> {
        assert (timeframe.first < timeframe.second)
        return sessionDao.getFromTimeframe(
            startTimestamp = timeframe.first,
            endTimestamp = timeframe.second
        )
    }

    private suspend fun sectionsForGoal (
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>? = null
    ) = if (itemIds == null) sectionDao.getInTimeframe(
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
    ) else sectionDao.getInTimeframeForItemId(
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        itemIds = itemIds
    )

    override suspend fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) = sectionsForGoal(
        startTimestamp = goal.instance.startTimestamp,
        endTimestamp = goal.endTimestampInLocalTimezone,
        itemIds = goal.description.libraryItems.map { it.id }.takeIf { it.isNotEmpty() }
    )

    override suspend fun sectionsForGoal(
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ) = sectionsForGoal(
        startTimestamp = instance.startTimestamp,
        endTimestamp = description.endOfInstanceInLocalTimezone(instance),
        itemIds = libraryItems.map { it.id }.takeIf { it.isNotEmpty() }
    )

    /** Mutators */
    /** Add */
    override suspend fun add(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionsCreationAttributes: List<SectionCreationAttributes>
    ) : UUID {
        val (sessionId, _) = sessionDao.insert(
            sessionCreationAttributes,
            sectionsCreationAttributes
        )
        return sessionId
    }

    /** Delete / Restore */
    override suspend fun delete(sessions: List<Session>) {
        sessionDao.delete(sessions.map { it.id })
    }

    override suspend fun restore(sessions: List<Session>) {
        sessionDao.restore(sessions.map { it.id })
    }

    /** Clean */
    override suspend fun clean() {
        sessionDao.clean()
    }
}