/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Matthias Emde
 */

package app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot

/**
 * Asserts that the given SemanticsNodeInteractions are vertically ordered on the screen.
 *
 * This function iterates through the provided nodes and verifies that each node is
 * positioned above the subsequent node in the list. It checks if the bottom bound
 * of the current node is less than or equal to the top bound of the next node.
 *
 * If any two consecutive nodes violate this vertical order, an assertion error is thrown.
 *
 * @param nodes The SemanticsNodeInteractions to be checked for vertical order.
 *
 * @throws AssertionError If any two consecutive nodes are not vertically ordered as expected.
 */
fun assertNodesInVerticalOrder(vararg nodes: SemanticsNodeInteraction) {
    for (i in 0 until nodes.size - 1) {
        val currentBounds = nodes[i].getBoundsInRoot()
        val nextBounds = nodes[i + 1].getBoundsInRoot()

        check(currentBounds.bottom <= nextBounds.top) {
            "Expected node ${i + 1} to be above node ${i + 2}, but was not. " +
                "Bounds: current = $currentBounds, next = $nextBounds"
        }
    }
}
