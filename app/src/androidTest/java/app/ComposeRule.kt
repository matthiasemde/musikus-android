/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule

const val LeaseSleepDurationMilliseconds = 1000L
const val LeaseDefaultAttempts = 5

fun AndroidComposeTestRule<*, *>.assertWithLease(
    attempts: Int = LeaseDefaultAttempts,
    assertion: () -> Unit
) {
    try {
        assertion()
    } catch (e: Throwable) {
        if (attempts > 0) {
            Thread.sleep(LeaseSleepDurationMilliseconds)
            assertWithLease(attempts - 1, assertion)
        } else {
            throw e
        }
    }
}

fun SemanticsNodeInteraction.assertWithLease(
    attempts: Int = LeaseDefaultAttempts,
    assertion: SemanticsNodeInteraction.() -> Unit
): SemanticsNodeInteraction {
    try {
        assertion()
    } catch (e: Throwable) {
        if (attempts > 0) {
            Thread.sleep(LeaseSleepDurationMilliseconds)
            assertWithLease(attempts - 1, assertion)
        } else {
            throw e
        }
    }

    return this
}
