/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.utils

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

sealed class UiIcon {
    data class DynamicIcon(val value: ImageVector) : UiIcon()

    data class IconResource(
        @DrawableRes val resId: Int
    ) : UiIcon()

    @Composable
    fun asIcon(): ImageVector {
        return when (this) {
            is DynamicIcon -> value
            is IconResource -> ImageVector.vectorResource(resId)
        }
    }
}