/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.test.filters.SmallTest
import app.musikus.database.MusikusDatabase
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.SessionModel
import app.musikus.di.AppModule
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.hours

@HiltAndroidTest
@UninstallModules(AppModule::class)
@SmallTest
class SessionDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var sessionDao: SessionDao

    @Inject
    lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        sessionDao = database.sessionDao
    }


    @Test
    fun cleanSessions() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "",
            ),
            SessionModel(
                breakDuration = 20,
                rating = 2,
                comment = "",
            ),
        ))

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(2),
        ))

        // move fake time to the next month
        fakeTimeProvider.advanceTimeBy((24 * 32).hours)

        sessionDao.clean()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Restoring cleaned items should be impossible
                sessionDao.restore(
                    listOf(
                        UUIDConverter.fromInt(1),
                        UUIDConverter.fromInt(2)
                    )
                )
            }
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [" +
                    "00000000-0000-0000-0000-000000000001, " +
                    "00000000-0000-0000-0000-000000000002" +
                    "]"
        )
    }
}