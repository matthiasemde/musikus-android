/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.presentation

import java.util.UUID

data class SessionsActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
)

data class SessionsContentUiState(
    val monthData: List<MonthUiDatum>,
    val selectedSessions: Set<UUID>,

    val showHint: Boolean,
)

data class SessionsDeleteDialogUiState(
    val numberOfSelections: Int,
)

data class SessionsUiState(
    val actionModeUiState: SessionsActionModeUiState,
    val contentUiState: SessionsContentUiState,
    val deleteDialogUiState: SessionsDeleteDialogUiState?,
)
