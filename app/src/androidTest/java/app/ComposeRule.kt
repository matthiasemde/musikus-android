/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun AndroidComposeTestRule<*, *>.waitUntilRuns(
    timeout: Duration = 1.seconds,
    assertion: () -> Unit
) {
    waitUntil(timeout.inWholeMilliseconds) {
        try {
            assertion()
            true
        } catch (_: Throwable) {
            false
        }
    }
}
