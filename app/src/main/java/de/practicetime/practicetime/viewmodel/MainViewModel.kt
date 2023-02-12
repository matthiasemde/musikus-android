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
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.datastore.ThemeSelections
import de.practicetime.practicetime.repository.LibraryRepository
import de.practicetime.practicetime.repository.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Initialization */


    /** Database */
    private val database = PTDatabase.getInstance(application, ::prepopulateDatabase)

    /** Repositories */
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)
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
}