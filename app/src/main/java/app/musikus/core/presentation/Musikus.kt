/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation

import android.app.Application
import android.content.Context
import app.musikus.R
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@HiltAndroidApp
class Musikus : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        val executorService: ExecutorService = Executors.newFixedThreadPool(4)
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

        fun ioThread(f: () -> Unit) {
            IO_EXECUTOR.execute(f)
        }

        var noSessionsYet = true
        var serviceIsRunning = false

        fun getRandomQuote(context: Context): CharSequence {
            return context.resources.getTextArray(R.array.quotes).random()
        }

        fun dp(context: Context, dp: Int): Float {
            return context.resources.displayMetrics.density * dp
        }
    }
}
