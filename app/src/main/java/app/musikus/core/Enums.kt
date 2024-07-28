/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core

import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText

interface EnumWithLabel {
    val label: UiText
}

interface EnumWithIcon {
    val icon: UiIcon
}

interface EnumWithDescription {
    val description: UiText
}