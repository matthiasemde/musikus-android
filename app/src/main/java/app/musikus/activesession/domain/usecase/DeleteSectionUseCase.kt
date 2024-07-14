/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.activesession.domain.usecase


import app.musikus.activesession.domain.ActiveSessionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class DeleteSectionUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(sectionId: UUID) {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Sections cannot be deleted when state is null")

        activeSessionRepository.setSessionState(
            state.copy(
                completedSections = state.completedSections.filter { it.id != sectionId }
            )
        )
    }
}