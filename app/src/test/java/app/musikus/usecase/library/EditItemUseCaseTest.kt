/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.entities.InvalidLibraryItemException
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EditItemUseCaseTest {
    private lateinit var  editItem: EditItemUseCase
    private lateinit var fakeRepository: FakeLibraryRepository
    @BeforeEach
    fun setUp() {
        fakeRepository = FakeLibraryRepository()
        editItem = EditItemUseCase(fakeRepository)

        val itemCreationAttributes = ('a'..'z').mapIndexed { index, name ->
            LibraryItemCreationAttributes(
                name = name.toString(),
                libraryFolderId = Nullable(null),
                colorIndex = index % 10
            )
        }

        runBlocking {
            itemCreationAttributes.shuffled().forEach {
                fakeRepository.addItem(it)
            }
        }
    }


    @Test
    fun `Edit item with empty name, InvalidLibraryItemException('Item name cannot be empty')`() {
        val exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                val item = fakeRepository.items.first().first()
                editItem(
                    id = item.id,
                    updateAttributes = LibraryItemUpdateAttributes(
                        name = "",
                    )
                )
            }
        }
        assertThat(exception.message).isEqualTo("Item name cannot be empty")
    }
}