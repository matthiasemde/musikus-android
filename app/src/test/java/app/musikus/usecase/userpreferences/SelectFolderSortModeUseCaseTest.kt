/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.userpreferences

import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.settings.domain.usecase.SelectFolderSortModeUseCase
import app.musikus.core.domain.LibraryFolderSortMode
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class SelectFolderSortModeUseCaseTest {
    private lateinit var selectFolderSortMode: SelectFolderSortModeUseCase
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        selectFolderSortMode = SelectFolderSortModeUseCase(
            userPreferencesRepository = fakeUserPreferencesRepository,
        )
    }

    @Test
    fun `Select new sort mode, sort mode is updated`() = runTest {
        // Set initial sort mode
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            sortInfo = SortInfo(
                mode = LibraryFolderSortMode.DATE_ADDED,
                direction = SortDirection.DEFAULT
            ),
        )

        // Select a new sort mode
        selectFolderSortMode(
            sortMode = LibraryFolderSortMode.NAME,
        )

        // Assert that the sort mode was updated with Default sort direction
        val sortInfo = fakeUserPreferencesRepository.folderSortInfo.first()
        assertThat(sortInfo)
            .isEqualTo(
                SortInfo(
                mode = LibraryFolderSortMode.NAME,
                direction = SortDirection.DEFAULT,
            )
            )
    }

    @Test
    fun `Select current sort mode, sort direction is inverted`() = runTest {
        // Set initial sort mode
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            sortInfo = SortInfo(
                mode = LibraryFolderSortMode.DATE_ADDED,
                direction = SortDirection.DESCENDING
            ),
        )

        // Select the current sort mode
        selectFolderSortMode(
            sortMode = LibraryFolderSortMode.DATE_ADDED,
        )

        // Assert that the sort mode was updated with inverted sort direction
        val sortInfo = fakeUserPreferencesRepository.folderSortInfo.first()
        assertThat(sortInfo)
            .isEqualTo(
                SortInfo(
                mode = LibraryFolderSortMode.DATE_ADDED,
                direction = SortDirection.ASCENDING,
            )
            )
    }
}