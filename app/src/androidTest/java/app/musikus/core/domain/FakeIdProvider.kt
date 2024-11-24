/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.core.domain

import java.util.UUID

class FakeIdProvider : IdProvider {
    private var _currentId = 1L

    override fun generateId(): UUID {
        return UUID.fromString(
            "00000000-0000-0000-0000-${_currentId++.toString().padStart(12, '0')}"
        )
    }
}
