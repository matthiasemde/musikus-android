/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.database

import androidx.core.database.getBlobOrNull
import androidx.core.database.getStringOrNull
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.data.UUIDConverter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// https://developer.android.com/training/data-storage/room/migrating-db-versions

@RunWith(AndroidJUnit4::class)
class MusikusDatabaseTest {

    private lateinit var db: SupportSQLiteDatabase

    private val uuidConverter = UUIDConverter()

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MusikusDatabase::class.java,
    )

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Test migration from version 3 to 4.
     *
     * Version 4 removed all default values from all entities which were causing issues in the
     * migration from version 2 to 3. Default values for boolean properties like 'deleted' or
     * 'paused' were erroneously set to 'false' instead of '0' or '1' which then falsely evaluates
     * to true in some cases. The migration from version 3 to 4 now restores data integrity by
     * updating all boolean properties which aren't explicitly set to '1', to '0'. Therefore, this
     * test checks if the migration updates the affected properties without changing anything else.
     *
     * Once the project is built, you can find the automigration in the generated Java file:
     * app/build/generated/ksp/debug/java/app/musikus/database/MusikusDatabase_AutoMigration_3_4_Impl.java
     */
    @Test
    fun migrate3To4() = runTest {

        // Prepare

        db = migrationTestHelper.createDatabase(TEST_DB, 3)

        db.apply {
            execSQL(
                sql = """
                    INSERT INTO library_folder 
                        (id, created_at, modified_at, deleted, name, custom_order)
                        VALUES 
                            (?,?,?,?,?,?),
                            (?,?,?,?,?,?),
                            (?,?,?,?,?,?),
                            (?,?,?,?,?,?),
                            (?,?,?,?,?,?),
                            (?,?,?,?,?,?)
                """.trimIndent(),
                bindArgs = arrayOf(
                    uuidConverter.toByte(UUIDConverter.fromInt(0)), TEST_TIME, TEST_TIME, "true", "folder1", null,
                    uuidConverter.toByte(UUIDConverter.fromInt(1)), TEST_TIME, TEST_TIME, "false", "folder2", null,
                    uuidConverter.toByte(UUIDConverter.fromInt(2)), TEST_TIME, TEST_TIME, "1", "folder3", null,
                    uuidConverter.toByte(UUIDConverter.fromInt(3)), TEST_TIME, TEST_TIME, "0", "folder4", null,
                    uuidConverter.toByte(UUIDConverter.fromInt(4)), TEST_TIME, TEST_TIME, 1, "folder5", null,
                    uuidConverter.toByte(UUIDConverter.fromInt(5)), TEST_TIME, TEST_TIME, 0, "folder6", null,
                )
            )
            execSQL(
                sql="""
                    INSERT INTO library_item
                        (id, created_at, modified_at, deleted, name, color_index, library_folder_id, custom_order)
                        VALUES
                            (?,?,?,?,?,?,?,?),
                            (?,?,?,?,?,?,?,?)
                """.trimIndent(),
                bindArgs = arrayOf(
                    uuidConverter.toByte(UUIDConverter.fromInt(0)), TEST_TIME, TEST_TIME, "false", "item1", 0, null, null,
                    uuidConverter.toByte(UUIDConverter.fromInt(1)), TEST_TIME, TEST_TIME, 1, "item2", 1, uuidConverter.toByte(
                        UUIDConverter.fromInt(1)), null,
                )
            )
            execSQL(
                sql = """
                    INSERT INTO session
                        (id, created_at, modified_at, deleted, break_duration_seconds, rating, comment)
                        VALUES
                            (?,?,?,?,?,?,?),
                            (?,?,?,?,?,?,?)
                """.trimIndent(),
                bindArgs = arrayOf(
                    uuidConverter.toByte(UUIDConverter.fromInt(0)), TEST_TIME, TEST_TIME, "false", 600, 1, "comment1",
                    uuidConverter.toByte(UUIDConverter.fromInt(1)), TEST_TIME, TEST_TIME, 1, 600, 5, "comment2",
                )
            )
            execSQL(
                sql = """
                    INSERT INTO goal_description
                        (id, created_at, modified_at, deleted, type, repeat, period_in_period_units, period_unit, progress_type, paused, archived, custom_order)
                        VALUES
                            (?,?,?,?,?,?,?,?,?,?,?,?),
                            (?,?,?,?,?,?,?,?,?,?,?,?)
                """.trimIndent(),
                bindArgs = arrayOf(
                    uuidConverter.toByte(UUIDConverter.fromInt(0)), TEST_TIME, TEST_TIME, "false", "ITEM_SPECIFIC", 0, 1, "DAY", "TIME", "false", 0, null,
                    uuidConverter.toByte(UUIDConverter.fromInt(1)), TEST_TIME, TEST_TIME, 1, "NON_SPECIFIC", 1, 1, "WEEK", "TIME", 1, 1, null,
                )
            )
            close()
        }


        // Act

        val migratedDb = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB,
            version = 4,
            validateDroppedTables = true
        )


        // Assert

        migratedDb.query("SELECT typeof(deleted) FROM library_folder").use { cursor ->
            val typeIndex = cursor.getColumnIndex("typeof(deleted)")

            assertThat(cursor.count).isEqualTo(6)
            repeat(6) {
                cursor.moveToNext()
                assertThat(cursor.getString(typeIndex)).isEqualTo("integer")
            }
        }

        migratedDb.query("SELECT * FROM library_folder").use {
            assertThat(it.count).isEqualTo(6)

            val idIndex = it.getColumnIndex("id")
            val createdAtIndex = it.getColumnIndex("created_at")
            val modifiedAtIndex = it.getColumnIndex("modified_at")
            val deletedIndex = it.getColumnIndex("deleted")
            val nameIndex = it.getColumnIndex("name")
            val customOrderIndex = it.getColumnIndex("custom_order")

            it.moveToFirst()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(0)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getString(nameIndex)).isEqualTo("folder1")
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(1)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getString(nameIndex)).isEqualTo("folder2")
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(2)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("1")
            assertThat(it.getString(nameIndex)).isEqualTo("folder3")
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(3)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getString(nameIndex)).isEqualTo("folder4")
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(4)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("1")
            assertThat(it.getString(nameIndex)).isEqualTo("folder5")
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(5)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getString(nameIndex)).isEqualTo("folder6")
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)
        }

        migratedDb.query("SELECT * FROM library_item").use {
            assertThat(it.count).isEqualTo(2)

            val idIndex = it.getColumnIndex("id")
            val createdAtIndex = it.getColumnIndex("created_at")
            val modifiedAtIndex = it.getColumnIndex("modified_at")
            val deletedIndex = it.getColumnIndex("deleted")
            val nameIndex = it.getColumnIndex("name")
            val colorIndexIndex = it.getColumnIndex("color_index")
            val libraryFolderIdIndex = it.getColumnIndex("library_folder_id")
            val customOrderIndex = it.getColumnIndex("custom_order")

            it.moveToFirst()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(0)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getString(nameIndex)).isEqualTo("item1")
            assertThat(it.getInt(colorIndexIndex)).isEqualTo(0)
            assertThat(it.getBlobOrNull(libraryFolderIdIndex)).isEqualTo(null)
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(1)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("1")
            assertThat(it.getString(nameIndex)).isEqualTo("item2")
            assertThat(it.getInt(colorIndexIndex)).isEqualTo(1)
            assertThat(it.getBlobOrNull(libraryFolderIdIndex)).isEqualTo(uuidConverter.toByte(
                UUIDConverter.fromInt(1)))
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)
        }

        migratedDb.query("SELECT * FROM session").use {
            assertThat(it.count).isEqualTo(2)

            val idIndex = it.getColumnIndex("id")
            val createdAtIndex = it.getColumnIndex("created_at")
            val modifiedAtIndex = it.getColumnIndex("modified_at")
            val deletedIndex = it.getColumnIndex("deleted")
            val breakDurationSecondsIndex = it.getColumnIndex("break_duration_seconds")
            val ratingIndex = it.getColumnIndex("rating")
            val commentIndex = it.getColumnIndex("comment")

            it.moveToFirst()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(0)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getInt(breakDurationSecondsIndex)).isEqualTo(600)
            assertThat(it.getInt(ratingIndex)).isEqualTo(1)
            assertThat(it.getString(commentIndex)).isEqualTo("comment1")

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(1)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("1")
            assertThat(it.getInt(breakDurationSecondsIndex)).isEqualTo(600)
            assertThat(it.getInt(ratingIndex)).isEqualTo(5)
            assertThat(it.getString(commentIndex)).isEqualTo("comment2")
        }

        migratedDb.query("SELECT * FROM goal_description").use {
            assertThat(it.count).isEqualTo(2)

            val idIndex = it.getColumnIndex("id")
            val createdAtIndex = it.getColumnIndex("created_at")
            val modifiedAtIndex = it.getColumnIndex("modified_at")
            val deletedIndex = it.getColumnIndex("deleted")
            val typeIndex = it.getColumnIndex("type")
            val repeatIndex = it.getColumnIndex("repeat")
            val periodInPeriodUnitsIndex = it.getColumnIndex("period_in_period_units")
            val periodUnitIndex = it.getColumnIndex("period_unit")
            val progressTypeIndex = it.getColumnIndex("progress_type")
            val pausedIndex = it.getColumnIndex("paused")
            val archivedIndex = it.getColumnIndex("archived")
            val customOrderIndex = it.getColumnIndex("custom_order")

            it.moveToFirst()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(0)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("0")
            assertThat(it.getString(typeIndex)).isEqualTo("ITEM_SPECIFIC")
            assertThat(it.getInt(repeatIndex)).isEqualTo(0)
            assertThat(it.getInt(periodInPeriodUnitsIndex)).isEqualTo(1)
            assertThat(it.getString(periodUnitIndex)).isEqualTo("DAY")
            assertThat(it.getString(progressTypeIndex)).isEqualTo("TIME")
            assertThat(it.getString(pausedIndex)).isEqualTo("0")
            assertThat(it.getInt(archivedIndex)).isEqualTo(0)
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)

            it.moveToNext()
            assertThat(it.getBlob(idIndex)).isEqualTo(uuidConverter.toByte(UUIDConverter.fromInt(1)))
            assertThat(it.getString(createdAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(modifiedAtIndex)).isEqualTo(TEST_TIME)
            assertThat(it.getString(deletedIndex)).isEqualTo("1")
            assertThat(it.getString(typeIndex)).isEqualTo("NON_SPECIFIC")
            assertThat(it.getInt(repeatIndex)).isEqualTo(1)
            assertThat(it.getInt(periodInPeriodUnitsIndex)).isEqualTo(1)
            assertThat(it.getString(periodUnitIndex)).isEqualTo("WEEK")
            assertThat(it.getString(progressTypeIndex)).isEqualTo("TIME")
            assertThat(it.getString(pausedIndex)).isEqualTo("1")
            assertThat(it.getInt(archivedIndex)).isEqualTo(1)
            assertThat(it.getStringOrNull(customOrderIndex)).isEqualTo(null)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
        private const val TEST_TIME = "2024-01-01T00:00:00Z"
    }
}