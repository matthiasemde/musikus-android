/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule

fun AndroidComposeTestRule<*, *>.waitUntilRuns(
    attempts: Int = 3,
    assertion: () -> Unit
) {
    try {
        assertion()
    } catch (e: Throwable) {
        if (attempts > 0) {
            Thread.sleep(1000)
            waitUntilRuns(attempts - 1, assertion)
        } else {
            throw e
        }
    }
}

fun SemanticsNodeInteraction.assertIsDisplayedWithLease(
    attempts: Int = 3,
): SemanticsNodeInteraction {
    try {
        assertIsDisplayed()
    } catch (e: Throwable) {
        if (attempts > 0) {
            Thread.sleep(1000)
            assertIsDisplayedWithLease(attempts - 1)
        } else {
            throw e
        }
    }

    return this
}
