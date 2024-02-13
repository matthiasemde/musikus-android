/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.sessions

import java.util.UUID

typealias SessionsUiEventHandler = (SessionsUiEvent) -> Unit

sealed class SessionsUiEvent {
    data class SessionPressed(
        val sessionId: UUID,
        val longClick: Boolean
    ) : SessionsUiEvent()

    data class MonthHeaderPressed(val specificMonth: Int) : SessionsUiEvent()
    data class EditButtonPressed(val editSession: (id: UUID) -> Unit) : SessionsUiEvent()
    data object DeleteButtonPressed : SessionsUiEvent()
    data object DeleteDialogDismissed : SessionsUiEvent()
    data object DeleteDialogConfirmed : SessionsUiEvent()

    data object UndoButtonPressed : SessionsUiEvent()

    data object ClearActionMode : SessionsUiEvent()
}