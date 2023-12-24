/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.utils

import java.time.ZonedDateTime

// Good comment from Jon Skeet on this topic: https://stackoverflow.com/a/5622222/20420131

interface TimeProvider {
    fun getCurrentDateTime(): ZonedDateTime
}

class TimeProviderImpl : TimeProvider {
    override fun getCurrentDateTime(): ZonedDateTime {
        return ZonedDateTime.now()
    }
}