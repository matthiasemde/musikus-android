/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.daos.InvalidLibraryFolderException
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.database.UUIDConverter
import app.musikus.library.domain.usecase.AddFolderUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class AddFolderUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    /** SUT */
    private lateinit var addFolder: AddFolderUseCase

    private val validFolderCreationAttributes = LibraryFolderCreationAttributes(
        name = "Test",
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)

        /** SUT */
        addFolder = AddFolderUseCase(fakeLibraryRepository)
    }

    @Test
    fun `Add folder with empty name, InvalidLibraryFolderException('Folder name can not be empty')`() = runTest {
        val exception = assertThrows<InvalidLibraryFolderException> {
            addFolder(validFolderCreationAttributes.copy(name = ""))
        }
        assertThat(exception.message).isEqualTo("Folder name can not be empty")
    }

    @Test
    fun `Add folder with valid name, folder is added`() = runTest {
        addFolder(validFolderCreationAttributes)

        val folder = fakeLibraryRepository.folders.first().first().folder
        assertThat(folder).isEqualTo(LibraryFolder(
            id = UUIDConverter.fromInt(1),
            createdAt = FakeTimeProvider.START_TIME,
            modifiedAt = FakeTimeProvider.START_TIME,
            name = validFolderCreationAttributes.name,
            customOrder = null
        ))
    }
}