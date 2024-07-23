/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 *
 */

package app.musikus.core.presentation.components

import app.musikus.core.presentation.utils.UiText

interface TopBarUiState {
    val title: UiText
    val showBackButton: Boolean
}