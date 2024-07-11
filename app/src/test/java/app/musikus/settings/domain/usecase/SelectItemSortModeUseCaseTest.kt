/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.settings.domain.usecase

import app.musikus.settings.data.FakeUserPreferencesRepository
import app.musikus.core.domain.LibraryItemSortMode
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SelectItemSortModeUseCaseTest {
    private lateinit var selectItemSortMode: SelectItemSortModeUseCase
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        selectItemSortMode = SelectItemSortModeUseCase(
            userPreferencesRepository = fakeUserPreferencesRepository,
        )
    }

    @Test
    fun `Select new sort mode, sort mode is updated`() {
        runBlocking {
            // Set initial sort mode
            fakeUserPreferencesRepository.updateLibraryItemSortInfo(
                sortInfo = SortInfo(
                    mode = LibraryItemSortMode.DATE_ADDED,
                    direction = SortDirection.DEFAULT
                ),
            )

            // Select a new sort mode
            selectItemSortMode(
                sortMode = LibraryItemSortMode.NAME,
            )

            // Assert that the sort mode was updated with Default sort direction
            val sortInfo = fakeUserPreferencesRepository.itemSortInfo.first()
            assertThat(sortInfo)
                .isEqualTo(
                    SortInfo(
                    mode = LibraryItemSortMode.NAME,
                    direction = SortDirection.DEFAULT,
                )
                )
        }
    }

    @Test
    fun `Select current sort mode, sort direction is inverted`() {
        runBlocking {
            // Set initial sort mode
            fakeUserPreferencesRepository.updateLibraryItemSortInfo(
                sortInfo = SortInfo(
                    mode = LibraryItemSortMode.DATE_ADDED,
                    direction = SortDirection.DESCENDING
                ),
            )

            // Select the current sort mode
            selectItemSortMode(
                sortMode = LibraryItemSortMode.DATE_ADDED,
            )

            // Assert that the sort mode was updated with inverted sort direction
            val sortInfo = fakeUserPreferencesRepository.itemSortInfo.first()
            assertThat(sortInfo)
                .isEqualTo(
                    SortInfo(
                    mode = LibraryItemSortMode.DATE_ADDED,
                    direction = SortDirection.ASCENDING,
                )
                )
        }
    }
}