/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.practicetime.practicetime.dataStore
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.SessionWithSections
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.database.entities.Session
import de.practicetime.practicetime.datastore.ThemeSelections
import de.practicetime.practicetime.repository.LibraryRepository
import de.practicetime.practicetime.repository.UserPreferencesRepository
import de.practicetime.practicetime.ui.sessionlist.SessionsForDay
import de.practicetime.practicetime.ui.sessionlist.SessionsForDaysForMonth
import de.practicetime.practicetime.utils.getSpecificDay
import de.practicetime.practicetime.utils.getSpecificMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*


class   MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Initialization */


    /** Database */
    private val database = PTDatabase.getInstance(application, ::prepopulateDatabase)

    /** Repositories */
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore, application)
    private val libraryRepository = LibraryRepository(database)

    private fun prepopulateDatabase() {
        Log.d("MainViewModel", "prepopulateDatabase")
        val folders = listOf(
            LibraryFolder(name = "Schupra"),
            LibraryFolder(name = "Fagott"),
            LibraryFolder(name = "Gesang"),
        )

        // populate the libraryItem table on first run
        val items = listOf(
            LibraryItem(name = "Die Schöpfung", colorIndex = 0, libraryFolderId = folders[0].id),
            LibraryItem(name = "Beethoven Septett",colorIndex = 1,libraryFolderId = folders[0].id),
            LibraryItem(name = "Schostakowitsch 9.", colorIndex = 2, libraryFolderId = folders[1].id),
            LibraryItem(name = "Trauermarsch c-Moll", colorIndex = 3, libraryFolderId = folders[1].id),
            LibraryItem(name = "Adagio", colorIndex = 4, libraryFolderId = folders[2].id),
            LibraryItem(name = "Eine kleine Gigue", colorIndex = 5, libraryFolderId = folders[2].id),
            LibraryItem(name = "Andantino", colorIndex = 6),
            LibraryItem(name = "Klaviersonate", colorIndex = 7),
            LibraryItem(name = "Trauermarsch", colorIndex = 8),
        )
        viewModelScope.launch {
            folders.forEach {
                libraryRepository.addFolder(it)
                Log.d("MainActivity", "Folder ${it.name} created")
                delay(1500) //make sure folders have different createdAt values
            }

            items.forEach {
                libraryRepository.addItem(it)
                Log.d("MainActivity", "LibraryItem ${it.name} created")
                delay(1500) //make sure items have different createdAt values
            }
        }
    }
//
//    /** Navigation */
//
//    fun navigateTo(destination: String) {
//        Log.d("MainState", "navigateTo: $destination")
//        navController.navigate(destination)
//    }

    /** Menu */

    var showMainMenu = mutableStateOf(false)
    var showThemeSubMenu = mutableStateOf(false)

    /** Import/Export */
    var showExportImportDialog = mutableStateOf(false)

    /** Content Scrim over NavBar for Multi FAB etc */

    val showNavBarScrim = mutableStateOf(false)


    /** Theme */

    val activeTheme = userPreferencesRepository.userPreferences.map { it.theme }

    fun setTheme(theme: ThemeSelections) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(theme)
        }
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
            database.sessionDao.getAllWithSectionsWithLibraryItems().takeUnless {
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
        viewModelScope.launch {
            database.sessionDao.insert(newSession)
            val insertedSession = database.sessionDao.getWithSectionsWithLibraryItems(newSession.session.id)

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
    fun deleteSessions(sessionIds: List<UUID>) {
        viewModelScope.launch {
            database.sessionDao.getAndDelete(sessionIds)
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
}