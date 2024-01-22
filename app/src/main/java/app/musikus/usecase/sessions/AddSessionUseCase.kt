/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.sessions

import app.musikus.database.UUIDConverter
import app.musikus.database.daos.InvalidSectionException
import app.musikus.database.daos.InvalidSessionException
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository

class AddSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val libraryRepository: LibraryRepository,
) {

    @Throws(InvalidSessionException::class, InvalidSectionException::class)
    suspend operator fun invoke(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionCreationAttributes: List<SectionCreationAttributes>
    ) {

        if(sessionCreationAttributes.rating !in 1..5) {
            throw InvalidSessionException("Rating must be between 1 and 5")
        }

        if(sessionCreationAttributes.comment.length > 500) {
            throw InvalidSessionException("Comment must be less than 500 characters")
        }

        if(sessionCreationAttributes.breakDuration.inWholeSeconds < 0) {
            throw InvalidSessionException("Break duration must be greater than or equal to 0")
        }

        if(sectionCreationAttributes.isEmpty()) {
            throw InvalidSectionException("Each session must include at least one section")
        }

        if(sectionCreationAttributes.any { it.duration.inWholeSeconds <= 0 }) {
            throw InvalidSectionException("Section duration must be greater than 0")
        }

        if(sectionCreationAttributes.any { it.sessionId != UUIDConverter.deadBeef }) {
            throw InvalidSectionException("Session id must not be set, it is set automatically")
        }

        val libraryItems = sectionCreationAttributes.map { it.libraryItemId }.toSet()
        val nonExistentLibraryItemIds = libraryItems.filter { !libraryRepository.existsItem(it) }

        if(nonExistentLibraryItemIds.isNotEmpty()) {
            throw InvalidSectionException("Library items do not exist: $nonExistentLibraryItemIds")
        }

        // TODO change start to be saved in the session instead of the section

        sessionRepository.add(
            sessionCreationAttributes,
            sectionCreationAttributes
        )
    }
}