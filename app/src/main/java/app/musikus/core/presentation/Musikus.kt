/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.Executors

const val CURRENT_ANNOUNCEMENT_ID = 1

@HiltAndroidApp
class Musikus : Application() {
    companion object {
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

        fun ioThread(f: () -> Unit) {
            IO_EXECUTOR.execute(f)
        }
    }
}
