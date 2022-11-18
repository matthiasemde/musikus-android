/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.ui

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.shared.ThemeSelections
import de.practicetime.practicetime.ui.goals.GoalsSortMode
import de.practicetime.practicetime.ui.library.LibraryFolderSortMode
import de.practicetime.practicetime.ui.library.LibraryItemSortMode
import de.practicetime.practicetime.ui.sessionlist.SessionsForDay
import de.practicetime.practicetime.ui.sessionlist.SessionsForDaysForMonth
import de.practicetime.practicetime.utils.getSpecificDay
import de.practicetime.practicetime.utils.getSpecificMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle() = when (this) {
        ASCENDING -> DESCENDING
        DESCENDING -> ASCENDING
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberMainState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberAnimatedNavController()
) = remember(coroutineScope) { MainState(coroutineScope, navController) }

class MainState(
    private val coroutineScope: CoroutineScope,
    val navController: NavHostController,
) {

    /** Initialization */

    init {
        loadDatabase()
    }

    fun loadDatabase() {
        coroutineScope.launch {
            loadSessions()

            loadLibraryFolders()
            sortLibraryFolders()

            loadLibraryItems()
            sortLibraryItems()

            loadGoals()
            sortGoals()
        }
    }


    /** Navigation */

    fun navigateTo(destination: String) {
        Log.d("MainState", "navigateTo: $destination")
        navController.navigate(destination)
    }

    /** Menu */

    var showMainMenu = mutableStateOf(false)
    var showThemeSubMenu = mutableStateOf(false)

    /** Import/Export */
    var showExportImportDialog = mutableStateOf(false)

    /** Content Scrim over NavBar for Multi FAB etc */

    val showNavBarScrim = mutableStateOf(false)


    /** Theme */

    val activeTheme = mutableStateOf(ThemeSelections.SYSTEM)

    fun setTheme(theme: ThemeSelections) {
        PracticeTime.prefs.edit().putInt(PracticeTime.PREFERENCES_KEY_THEME, theme.ordinal).apply()
        activeTheme.value = theme
        AppCompatDelegate.setDefaultNightMode(theme.ordinal)
    }


    /** Sessions */

    private val _sessions = MutableStateFlow(emptyList<SessionsForDaysForMonth>())
    val sessions = _sessions.asStateFlow()

    /** Accessors */
    /** Load */
    private suspend fun loadSessions() {
        _sessions.update {
            val sessionsForDaysForMonths = mutableListOf<SessionsForDaysForMonth>()
            // fetch all sessions from the database
            PracticeTime.sessionDao.getAllWithSectionsWithLibraryItems().takeUnless {
                it.isEmpty()
            }?.sortedByDescending { it.sections.first().section.timestamp }?.let { fetchedSessions ->
                // initialize variables to keep track of the current month, current day,
                // the index of its first session and the total duration of the current day
                var (currentDay, currentMonth) = fetchedSessions.first()
                    .sections.first()
                    .section.timestamp.let { timestamp ->
                    Pair(getSpecificDay(timestamp), getSpecificMonth(timestamp))
                }
                var firstSessionOfDayIndex = 0
                var totalPracticeDuration = 0


                val sessionsForDaysForMonth = mutableListOf<SessionsForDay>()

                // then loop trough all of the sessions...
                fetchedSessions.forEachIndexed { index, session ->
                    // ...get the month and day...
                    val sessionTimestamp = session.sections.first().section.timestamp
                    val (day, month) = sessionTimestamp.let { timestamp ->
                        Pair(getSpecificDay(timestamp), getSpecificMonth(timestamp))
                    }

                    totalPracticeDuration += session.sections.sumOf { it.section.duration ?: 0 }

                    // ...and compare them to the current day first.
                    // if it differs, create a new SessionsForDay object
                    // with the respective subList of sessions
                    if(day == currentDay) return@forEachIndexed

                    sessionsForDaysForMonth.add(SessionsForDay(
                        specificDay = currentDay,
                        totalPracticeDuration = totalPracticeDuration,
                        sessions = fetchedSessions.slice(firstSessionOfDayIndex until index)
                    ))

                    // reset / set tracking variables appropriately
                    currentDay = day
                    firstSessionOfDayIndex = index
                    totalPracticeDuration = 0

                    // then compare the month to the current month.
                    // if it differs, create a new SessionsForDaysForMonth object
                    // storing the specific month along with the list of SessionsForDay objects
                    if(month == currentMonth) return@forEachIndexed

                    sessionsForDaysForMonths.add(SessionsForDaysForMonth(
                        specificMonth = currentMonth,
                        sessionsForDays = sessionsForDaysForMonth.toList()
                    ))

                    // set tracking variable and reset list
                    currentMonth = month
                    sessionsForDaysForMonth.clear()
                }

                // importantly, add the last SessionsForDaysForMonth object
                sessionsForDaysForMonth.add(SessionsForDay(
                    specificDay = currentDay,
                    totalPracticeDuration = totalPracticeDuration,
                    sessions = fetchedSessions.slice(firstSessionOfDayIndex until fetchedSessions.size)
                ))
                sessionsForDaysForMonths.add(SessionsForDaysForMonth(
                    specificMonth = currentMonth,
                    sessionsForDays = sessionsForDaysForMonth
                ))
            }
            sessionsForDaysForMonths.toList()
        }
    }

    /** Mutators */
    /** Add */
    fun addSession(
        newSession: SessionWithSections,
    ) {
        coroutineScope.launch {
            val insertedSessionId = PracticeTime.sessionDao.insertSessionWithSections(newSession)
            val insertedSession = PracticeTime.sessionDao.getWithSectionsWithLibraryItems(insertedSessionId)

            val (day, month) = newSession.sections.first().timestamp.let { timestamp ->
                Pair(getSpecificDay(timestamp), getSpecificMonth(timestamp))
            }

            _sessions.update { sessionsForDaysForMonths ->
                sessionsForDaysForMonths.map { sessionsForDaysForMonth ->
                    if (sessionsForDaysForMonth.specificMonth != month)
                        sessionsForDaysForMonth
                    else {
                        sessionsForDaysForMonth.copy(
                            sessionsForDays = sessionsForDaysForMonth.sessionsForDays.map { sessionsForDay ->
                                if (sessionsForDay.specificDay != day)
                                    sessionsForDay
                                else
                                    sessionsForDay.copy(
                                        sessions = (sessionsForDay.sessions + insertedSession).sortedByDescending {
                                            it.sections.first().section.timestamp
                                        }
                                    )
                            }
                        )
                    }
                }
            }
        }
    }


    /** Edit */
    fun editSession(
        editedSession: Session,
    ) {
        // TODO: Implement
    }

    /** Archive */
    fun deleteSessions(sessionIds: List<Long>) {
        coroutineScope.launch {
            PracticeTime.sessionDao.getAndDelete(sessionIds)
            _sessions.update { sessions ->
                sessions.map { sessionsForDaysForMonth ->
                    sessionsForDaysForMonth.copy(
                        sessionsForDays = sessionsForDaysForMonth.sessionsForDays.map { sessionsForDay ->
                            val filteredSessions = sessionsForDay.sessions.filter {
                                it.session.id !in sessionIds
                            }
                            sessionsForDay.copy(
                                sessions = filteredSessions,
                                totalPracticeDuration = filteredSessions.sumOf { session ->
                                    session.sections.sumOf { it.section.duration ?: 0 }
                                }
                            )
                        }.filter { it.sessions.isNotEmpty() }
                    )
                }.filter { it.sessionsForDays.isNotEmpty() }
            }
        }
    }


    /** Library Items and Folders */

    private val _libraryFolders = MutableStateFlow(emptyList<LibraryFolder>())
    private val _libraryItems = MutableStateFlow(emptyList<LibraryItem>())

    val libraryFolders = _libraryFolders.asStateFlow()
    var libraryItems = _libraryItems.asStateFlow()

    var libraryItemSortMode = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_MODE,
            LibraryItemSortMode.DATE_ADDED.name
        )?.let { LibraryItemSortMode.valueOf(it) } ?: LibraryItemSortMode.DATE_ADDED
    } catch (ex: Exception) {
        LibraryItemSortMode.DATE_ADDED
    })

    var libraryItemSortDirection = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_DIRECTION,
            SortDirection.ASCENDING.name
        )?.let { SortDirection.valueOf(it) } ?: SortDirection.ASCENDING
    } catch (ex: Exception) {
        SortDirection.ASCENDING
    })

    var libraryFolderSortMode = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_MODE,
            LibraryFolderSortMode.DATE_ADDED.name
        )?.let { LibraryFolderSortMode.valueOf(it) } ?: LibraryFolderSortMode.DATE_ADDED
    } catch (ex: Exception) {
        LibraryFolderSortMode.DATE_ADDED
    })

    var libraryFolderSortDirection = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_DIRECTION,
            SortDirection.ASCENDING.name
        )?.let { SortDirection.valueOf(it) } ?: SortDirection.ASCENDING
    } catch (ex: Exception) {
        SortDirection.ASCENDING
    })

    /** Accessors */
    /** Load */
    private suspend fun loadLibraryFolders() {
        _libraryFolders.update { PracticeTime.libraryFolderDao.getAll() }
    }

    private suspend fun loadLibraryItems() {
        _libraryItems.update {
            PracticeTime.libraryItemDao.get(activeOnly = true).onEach { item ->
                // check if the items folderId actually exists
                item.libraryFolderId?.let {
                    if(PracticeTime.libraryFolderDao.get(it) == null) {
                        // and return item to the main screen if it doesn't
                        item.libraryFolderId = null
                        PracticeTime.libraryItemDao.update(item)
                    }

                }
            }
        }
    }

    /** Mutators */
    /** Add */
    fun addLibraryFolder(newFolder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.insertAndGet(newFolder)?.let { insertedFolder ->
                _libraryFolders.update { listOf(insertedFolder) + it }
                sortLibraryFolders()
            }
        }
    }

    fun addLibraryItem(item: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.insertAndGet(item)?.let { insertedItem ->
                _libraryItems.update { listOf(insertedItem) + it }
                sortLibraryItems()
            }
        }
    }

    /** Edit */
    fun editFolder(editedFolder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.update(editedFolder)
            _libraryFolders.update { folders ->
                folders.map { if (it.id == editedFolder.id) editedFolder else it }
            }
            sortLibraryFolders()
        }
    }

    fun editItem(item: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.update(item)
            sortLibraryItems()
        }
    }

    /** Delete / Archive */
    fun deleteFolders(folderIds: List<Long>) {
        coroutineScope.launch {
            folderIds.forEach { folderId ->
                PracticeTime.libraryFolderDao.deleteAndResetItems(folderId)
            }
            _libraryFolders.update { folders ->
                folders.filter { it.id !in folderIds }
            }
            loadLibraryItems() // reload items to show those which were in a folder
            sortLibraryItems()
            sortLibraryFolders()
        }
    }

    fun archiveItems(itemIds: List<Long>) {
        coroutineScope.launch {
            itemIds.forEach { itemId ->
                // returns false in case item couldnt be archived
                if (PracticeTime.libraryItemDao.archive(itemId)) {
                    _libraryItems.update { items ->
                        items.filter { it.id != itemId }
                    }
                }
            }
        }
    }

    /** Sort */
    fun sortLibraryFolders(mode: LibraryFolderSortMode? = null) {
        if(mode != null) {
            if (mode == libraryFolderSortMode.value) {
                when (libraryFolderSortDirection.value) {
                    SortDirection.ASCENDING -> libraryFolderSortDirection.value = SortDirection.DESCENDING
                    SortDirection.DESCENDING -> libraryFolderSortDirection.value = SortDirection.ASCENDING
                }
            } else {
                libraryFolderSortDirection.value = SortDirection.ASCENDING
                libraryFolderSortMode.value = mode
                PracticeTime.prefs.edit().putString(
                    PracticeTime.PREFERENCES_KEY_LIBRARY_FOLDER_SORT_MODE,
                    libraryFolderSortMode.value.name
                ).apply()
            }
            PracticeTime.prefs.edit().putString(
                PracticeTime.PREFERENCES_KEY_LIBRARY_FOLDER_SORT_DIRECTION,
                libraryFolderSortDirection.value.name
            ).apply()
        }
        when (libraryFolderSortDirection.value) {
            SortDirection.ASCENDING -> {
                when (libraryFolderSortMode.value) {
                    LibraryFolderSortMode.DATE_ADDED -> {
                        _libraryFolders.update { folders -> folders.sortedBy { it.createdAt } }
                    }
                    LibraryFolderSortMode.LAST_MODIFIED -> {
                        _libraryFolders.update { folders -> folders.sortedBy { it.modifiedAt } }
                    }
                    LibraryFolderSortMode.CUSTOM -> {}
                }
            }
            SortDirection.DESCENDING -> {
                when (libraryFolderSortMode.value) {
                    LibraryFolderSortMode.DATE_ADDED -> {
                        _libraryFolders.update { folders -> folders.sortedByDescending { it.createdAt } }
                    }
                    LibraryFolderSortMode.LAST_MODIFIED -> {
                        _libraryFolders.update { folders -> folders.sortedByDescending { it.modifiedAt } }
                    }
                    LibraryFolderSortMode.CUSTOM -> {}
                }
            }
        }
    }

    fun sortLibraryItems(mode: LibraryItemSortMode? = null) {
        if(mode != null) {
            if (mode == libraryItemSortMode.value) {
                when (libraryItemSortDirection.value) {
                    SortDirection.ASCENDING -> libraryItemSortDirection.value = SortDirection.DESCENDING
                    SortDirection.DESCENDING -> libraryItemSortDirection.value = SortDirection.ASCENDING
                }
            } else {
                libraryItemSortDirection.value = SortDirection.ASCENDING
                libraryItemSortMode.value = mode
                PracticeTime.prefs.edit().putString(
                    PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_MODE,
                    libraryItemSortMode.value.name
                ).apply()
            }
            PracticeTime.prefs.edit().putString(
                PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_DIRECTION,
                libraryItemSortDirection.value.name
            ).apply()
        }
        when (libraryItemSortDirection.value) {
            SortDirection.ASCENDING -> {
                when (libraryItemSortMode.value) {
                    LibraryItemSortMode.DATE_ADDED -> {
                        _libraryItems.update { items -> items.sortedBy { it.createdAt } }
                    }
                    LibraryItemSortMode.LAST_MODIFIED -> {
                        _libraryItems.update { items -> items.sortedBy { it.modifiedAt } }
                    }
                    LibraryItemSortMode.NAME -> {
                        _libraryItems.update { items -> items.sortedBy { it.name } }
                    }
                    LibraryItemSortMode.COLOR -> {
                        _libraryItems.update { items -> items.sortedBy { it.colorIndex } }
                    }
                    LibraryItemSortMode.CUSTOM -> { }
                }
            }
            SortDirection.DESCENDING -> {
                when (libraryItemSortMode.value) {
                    LibraryItemSortMode.DATE_ADDED -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.createdAt } }
                    }
                    LibraryItemSortMode.LAST_MODIFIED -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.modifiedAt } }
                    }
                    LibraryItemSortMode.NAME -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.name } }
                    }
                    LibraryItemSortMode.COLOR -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.colorIndex } }
                    }
                    LibraryItemSortMode.CUSTOM -> { }
                }
            }
        }
    }

    /** Goals */
    private val _goals = MutableStateFlow(emptyList<GoalInstanceWithDescriptionWithLibraryItems>())
    val goals = _goals.asStateFlow()

    var goalsSortMode = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_GOALS_SORT_MODE,
            GoalsSortMode.DATE_ADDED.name
        )?.let { GoalsSortMode.valueOf(it) } ?: GoalsSortMode.DATE_ADDED
    } catch (ex: Exception) {
        GoalsSortMode.DATE_ADDED
    })

    var goalsSortDirection = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_GOALS_SORT_DIRECTION,
            SortDirection.ASCENDING.name
        )?.let { SortDirection.valueOf(it) } ?: SortDirection.ASCENDING
    } catch (ex: Exception) {
        SortDirection.ASCENDING
    })

    /** Accessors */
    /** Load */
    private suspend fun loadGoals() {
        _goals.update {
            PracticeTime.goalInstanceDao.getWithDescriptionsWithLibraryItems()
        }
    }

    /** Mutators */
    /** Add */
    fun addGoal(
        newGoal: GoalDescriptionWithLibraryItems,
        target: Int,
    ) {
        coroutineScope.launch {
            PracticeTime.goalDescriptionDao.insertGoal(
                newGoal,
                target,
            )?.let { insertedGoal ->
                _goals.update { listOf(insertedGoal) + it }
                sortGoals()
            }
        }
    }

    /** Edit */
    fun editGoalTarget(
        editedGoalDescriptionId: Long,
        newTarget: Int,
    ) {
        coroutineScope.launch {
            PracticeTime.goalDescriptionDao.updateTarget(editedGoalDescriptionId, newTarget)
            _goals.update { goals ->
                goals.map {
                    if (it.description.description.id == editedGoalDescriptionId)
                        it.instance.target = newTarget
                    it
                }
            }
            sortGoals()
        }
    }

    /** Archive */
    fun archiveGoals(goalDescriptionIds: List<Long>) {
        coroutineScope.launch {
            PracticeTime.goalDescriptionDao.getAndArchive(goalDescriptionIds)
            _goals.update { goals ->
                goals.filter { it.description.description.id !in goalDescriptionIds }
            }
        }
    }

    /** Sort */
    fun sortGoals(mode: GoalsSortMode? = null) {
        if(mode != null) {
            if (mode == goalsSortMode.value) {
                when (goalsSortDirection.value) {
                    SortDirection.ASCENDING -> goalsSortDirection.value = SortDirection.DESCENDING
                    SortDirection.DESCENDING -> goalsSortDirection.value = SortDirection.ASCENDING
                }
            } else {
                goalsSortDirection.value = SortDirection.ASCENDING
                goalsSortMode.value = mode
                PracticeTime.prefs.edit().putString(
                    PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_MODE,
                    goalsSortMode.value.name
                ).apply()
            }
            PracticeTime.prefs.edit().putString(
                PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_DIRECTION,
                goalsSortDirection.value.name
            ).apply()
        }
        when (goalsSortDirection.value) {
            SortDirection.ASCENDING -> {
                when (goalsSortMode.value) {
                    GoalsSortMode.DATE_ADDED -> {
                        _goals.update { goals -> goals.sortedBy { it.description.description.createdAt } }
                    }
                    GoalsSortMode.TARGET -> {
                        _goals.update { goals -> goals.sortedBy { it.instance.target } }
                    }
                    GoalsSortMode.PERIOD -> {
                        _goals.update { goals -> goals.sortedBy { it.instance.periodInSeconds } }
                    }
                    GoalsSortMode.CUSTOM -> TODO()
                }
            }
            SortDirection.DESCENDING -> {
                when (goalsSortMode.value) {
                    GoalsSortMode.DATE_ADDED -> {
                        _goals.update { goals -> goals.sortedByDescending { it.description.description.createdAt } }
                    }
                    GoalsSortMode.TARGET -> {
                        _goals.update { goals -> goals.sortedByDescending { it.instance.target } }
                    }
                    GoalsSortMode.PERIOD -> {
                        _goals.update { goals -> goals.sortedByDescending { it.instance.periodInSeconds } }
                    }
                    GoalsSortMode.CUSTOM -> TODO()
                }
            }
        }
    }
}