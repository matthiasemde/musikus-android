/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.activesession.presentation

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeoutException

@HiltAndroidTest
class SessionServiceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val serviceRule = ServiceTestRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    @Throws(TimeoutException::class)
    fun testWithBoundService() {
        // Create the start intent
        val startIntent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            SessionService::class.java
        ).apply {
            action = ActiveSessionServiceActions.START.name
        }

        val binder = serviceRule.bindService(startIntent)

        // Verify that the service has started correctly
        assertThat(binder.pingBinder()).isTrue()
    }
}
