/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package app.musikus.core.data

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import app.musikus.BuildConfig
import app.musikus.core.presentation.Musikus.Companion.ioThread
import app.musikus.goals.data.daos.GoalDescriptionDao
import app.musikus.goals.data.daos.GoalInstanceDao
import app.musikus.goals.data.entities.GoalDescriptionLibraryItemCrossRefModel
import app.musikus.goals.data.entities.GoalDescriptionModel
import app.musikus.goals.data.entities.GoalInstanceModel
import app.musikus.library.data.daos.LibraryFolderDao
import app.musikus.library.data.daos.LibraryItemDao
import app.musikus.library.data.entities.LibraryFolderModel
import app.musikus.library.data.entities.LibraryItemModel
import app.musikus.sessionslist.data.daos.SectionDao
import app.musikus.sessionslist.data.daos.SessionDao
import app.musikus.sessionslist.data.entities.SectionModel
import app.musikus.sessionslist.data.entities.SessionModel
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.TimeProvider
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Provider

@Database(
    version = 3,
    entities = [
        SessionModel::class,
        SectionModel::class,
        LibraryItemModel::class,
        LibraryFolderModel::class,
        GoalDescriptionModel::class,
        GoalInstanceModel::class,
        GoalDescriptionLibraryItemCrossRefModel::class,
    ],
    exportSchema = true,
)
@TypeConverters(
    UUIDConverter::class,
    NullableUUIDConverter::class,
    NullableIntConverter::class,
    ZonedDateTimeConverter::class,
    NullableZonedDateTimeConverter::class,
)
abstract class MusikusDatabase : RoomDatabase() {
    abstract val libraryItemDao : LibraryItemDao
    abstract val libraryFolderDao : LibraryFolderDao
    abstract val goalDescriptionDao : GoalDescriptionDao
    abstract val goalInstanceDao : GoalInstanceDao
    abstract val sessionDao : SessionDao
    abstract val sectionDao : SectionDao

    lateinit var timeProvider: TimeProvider
    lateinit var idProvider: IdProvider

    companion object {
        const val DATABASE_NAME = "musikus-database"

        fun buildDatabase(
            app: Application,
            databaseProvider: Provider<MusikusDatabase>,
        ) =
            Room.databaseBuilder(
                app,
                MusikusDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(
                DatabaseMigrationOneToTwo,
                DatabaseMigrationTwoToThree
            ).addCallback(object : Callback() {
                // prepopulate the database after onCreate was called
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // prepopulate the database if in debug configuration
                    if (BuildConfig.DEBUG) {
                        ioThread { runBlocking {
                            prepopulateDatabase(databaseProvider.get())
                        } }
                    }
                }
            }).build()
    }

    object DatabaseMigrationOneToTwo : Migration(1,2) {
        /**
         * Thanks Elif
         */
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_goal_description` (`type` TEXT NOT NULL, `repeat` INTEGER NOT NULL, `period_in_period_units` INTEGER NOT NULL, `period_unit` TEXT NOT NULL, `progress_type` TEXT NOT NULL, `archived` INTEGER NOT NULL, `profile_id` INTEGER NOT NULL, `order` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL DEFAULT 0, `modified_at` INTEGER NOT NULL DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            db.execSQL("INSERT INTO `_new_goal_description` (`period_in_period_units`, `archived`,`progress_type`,`period_unit`,`profile_id`,`repeat`,`id`,`type`) SELECT `periodInPeriodUnits`, `archived`,`progressType`,`periodUnit`,`profileId`,`oneTime`,`id`,`type` FROM `GoalDescription`")
            db.execSQL("DROP TABLE `GoalDescription`")
            db.execSQL("ALTER TABLE `_new_goal_description` RENAME TO `goal_description`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_description_profile_id` ON `goal_description` (`profile_id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_category` (`name` TEXT NOT NULL, `color_index` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `profile_id` INTEGER NOT NULL, `order` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL DEFAULT 0, `modified_at` INTEGER NOT NULL DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            db.execSQL("INSERT INTO `_new_category` (`archived`,`profile_id`,`name`,`id`,`color_index`) SELECT `archived`,`profile_id`,`name`,`id`,`colorIndex` FROM `Category`")
            db.execSQL("DROP TABLE `Category`")
            db.execSQL("ALTER TABLE `_new_category` RENAME TO `category`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_profile_id` ON `category` (`profile_id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_section` (`session_id` INTEGER, `category_id` INTEGER NOT NULL, `duration` INTEGER, `timestamp` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            db.execSQL("INSERT INTO `_new_section` (`duration`,`category_id`,`session_id`,`id`,`timestamp`) SELECT `duration`,`category_id`,`practice_session_id`,`id`,`timestamp` FROM `PracticeSection`")
            db.execSQL("DROP TABLE `PracticeSection`")
            db.execSQL("ALTER TABLE `_new_section` RENAME TO `section`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_section_session_id` ON `section` (`session_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_section_category_id` ON `section` (`category_id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_goal_instance` (`goal_description_id` INTEGER NOT NULL, `start_timestamp` INTEGER NOT NULL, `period_in_seconds` INTEGER NOT NULL, `target` INTEGER NOT NULL, `progress` INTEGER NOT NULL, `renewed` INTEGER NOT NULL, `created_at` INTEGER NOT NULL DEFAULT 0, `modified_at` INTEGER NOT NULL DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            db.execSQL("INSERT INTO `_new_goal_instance` (`goal_description_id`, `period_in_seconds`, `renewed`,`start_timestamp`,`progress`,`id`,`target`) SELECT `goalDescriptionId`, `periodInSeconds`, `renewed`,`startTimestamp`,`progress`,`id`,`target` FROM `GoalInstance`")
            db.execSQL("DROP TABLE `GoalInstance`")
            db.execSQL("ALTER TABLE `_new_goal_instance` RENAME TO `goal_instance`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_goal_description_category_cross_ref` (`goal_description_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL, PRIMARY KEY(`goal_description_id`, `category_id`))")
            db.execSQL("INSERT INTO `_new_goal_description_category_cross_ref` (`category_id`, `goal_description_id`) SELECT `categoryId`, `goalDescriptionId` FROM `GoalDescriptionCategoryCrossRef`")
            db.execSQL("DROP TABLE `GoalDescriptionCategoryCrossRef`")
            db.execSQL("ALTER TABLE `_new_goal_description_category_cross_ref` RENAME TO `goal_description_category_cross_ref`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_description_category_cross_ref_goal_description_id` ON `goal_description_category_cross_ref` (`goal_description_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_description_category_cross_ref_category_id` ON `goal_description_category_cross_ref` (`category_id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_session` (`break_duration` INTEGER NOT NULL, `rating` INTEGER NOT NULL, `comment` TEXT, `profile_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL DEFAULT 0, `modified_at` INTEGER NOT NULL DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            db.execSQL("INSERT INTO `_new_session` (`profile_id`,`rating`,`comment`,`id`,`break_duration`) SELECT `profile_id`,`rating`,`comment`,`id`,`break_duration` FROM `PracticeSession`")
            db.execSQL("DROP TABLE `PracticeSession`")
            db.execSQL("ALTER TABLE `_new_session` RENAME TO `session`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_profile_id` ON `session` (`profile_id`)")

            Log.d("POST_MIGRATION", "Starting Post Migration...")

            for (tableName in listOf("session", "category", "goal_description", "goal_instance")) {
                val cursor = db.query(SupportSQLiteQueryBuilder.builder(tableName).let {
                    it.columns(arrayOf("id"))
                    it.create()
                })
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val now = ZonedDateTime.now().toEpochSecond()
                    db.update(tableName, SQLiteDatabase.CONFLICT_IGNORE, ContentValues().let {
                        it.put("created_at", now + id)
                        it.put("modified_at", now + id)
                        it
                    }, "id=?", arrayOf(id))
                }
                Log.d("POST_MIGRATION", "Added timestamps for $tableName")
            }

            val cursor = db.query(SupportSQLiteQueryBuilder.builder("goal_description").let {
                it.columns(arrayOf("id", "repeat"))
                it.create()
            })
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val oneTime = cursor.getInt(cursor.getColumnIndexOrThrow("repeat"))
                db.update("goal_description", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().let {
                    it.put("repeat", if (oneTime == 0) "1" else "0")
                    it
                }, "id=?", arrayOf(id))
            }
            Log.d("POST_MIGRATION", "Migrated 'oneTime' to 'repeat' for goal descriptions")

            Log.d("POST_MIGRATION", "Post Migration complete")
        }
    }

    object DatabaseMigrationTwoToThree : Migration (2,3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val longToUuidInBytes: (Long) -> ByteArray = {value ->
                ByteBuffer.wrap(ByteArray(16)).let { buffer ->
                    buffer.putLong(0L)
                    buffer.putLong(value)
                    buffer.array()
                }
            }

            val longToZonedDateTimeString: (Long) -> String = { value ->
                ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(value),
                    ZoneId.systemDefault()
                ).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            }

            // Create library folder table
            db.execSQL("CREATE TABLE IF NOT EXISTS `library_folder` (`name` TEXT NOT NULL, `custom_order` INTEGER DEFAULT null, `deleted` INTEGER NOT NULL DEFAULT false, `created_at` TEXT NOT NULL, `modified_at` TEXT NOT NULL, `id` BLOB NOT NULL, PRIMARY KEY(`id`))")

            // Rename order to custom_order in library_item, formerly category
            // Delete archived and profile_id
            db.execSQL("CREATE TABLE IF NOT EXISTS `library_item` (`name` TEXT NOT NULL, `color_index` INTEGER NOT NULL, `library_folder_id` BLOB DEFAULT null, `custom_order` INTEGER DEFAULT null, `deleted` INTEGER NOT NULL DEFAULT false, `created_at` TEXT NOT NULL, `modified_at` TEXT NOT NULL, `id` BLOB NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`library_folder_id`) REFERENCES `library_folder`(`id`) ON UPDATE NO ACTION ON DELETE SET DEFAULT )")
            db.query("SELECT * FROM `category`").use { cursor ->
                while (cursor.moveToNext()) {
                    val id = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val colorIndex = cursor.getInt(cursor.getColumnIndexOrThrow("color_index"))
                    val createdAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                    val modifiedAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("modified_at")))

                    db.execSQL(
                        "INSERT INTO `library_item` (`name`,`color_index`,`custom_order`,`created_at`,`modified_at`,`id`) VALUES (?,?,NULL,?,?,?)",
                        arrayOf(name, colorIndex, createdAt, modifiedAt, id)
                    )
                }
            }
            db.execSQL("DROP TABLE `category`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_item_library_folder_id` ON `library_item` (`library_folder_id`)")

            // Rename break_duration to break_duration_seconds in session
            // Delete profile_id
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_session` (`break_duration_seconds` INTEGER NOT NULL, `rating` INTEGER NOT NULL, `comment` TEXT NOT NULL, `deleted` INTEGER NOT NULL DEFAULT false, `created_at` TEXT NOT NULL, `modified_at` TEXT NOT NULL, `id` BLOB NOT NULL, PRIMARY KEY(`id`))")
            db.query("SELECT * FROM `session`").use { cursor ->
                while (cursor.moveToNext()) {
                    val breakDuration = cursor.getInt(cursor.getColumnIndexOrThrow("break_duration"))
                    val rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating"))
                    val comment = cursor.getString(cursor.getColumnIndexOrThrow("comment"))
                    val createdAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                    val modifiedAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("modified_at")))
                    val id = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("id")))

                    db.execSQL(
                        "INSERT INTO `_new_session` (`break_duration_seconds`,`rating`,`comment`,`created_at`,`modified_at`,`id`) VALUES (?,?,?,?,?,?)",
                        arrayOf(breakDuration, rating, comment, createdAt, modifiedAt, id)
                    )
                }
            }
            db.execSQL("DROP TABLE `session`")
            db.execSQL("ALTER TABLE `_new_session` RENAME TO `session`")

            // Rename duration to duration_seconds, category_id to library_item_id and timestamp to start_timestamp in section
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_section` (`session_id` BLOB NOT NULL, `library_item_id` BLOB NOT NULL, `duration_seconds` INTEGER NOT NULL, `start_timestamp` TEXT NOT NULL, `id` BLOB NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`session_id`) REFERENCES `session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`library_item_id`) REFERENCES `library_item`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
            db.query("SELECT * FROM `section`").use { cursor ->
                while (cursor.moveToNext()) {
                    val sessionId = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("session_id")))
                    val libraryItemId = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("category_id")))
                    val durationSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                    val startTimestamp = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
                    val id = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("id")))

                    db.execSQL(
                        "INSERT INTO `_new_section` (`session_id`,`library_item_id`,`duration_seconds`,`start_timestamp`,`id`) VALUES (?,?,?,?,?)",
                        arrayOf(sessionId, libraryItemId, durationSeconds, startTimestamp, id)
                    )
                }
            }
            db.execSQL("DROP TABLE `section`")
            db.execSQL("ALTER TABLE `_new_section` RENAME TO `section`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_section_session_id` ON `section` (`session_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_section_library_item_id` ON `section` (`library_item_id`)")

            // Rename order to custom_order in goal_description
            // Delete profile_id
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_goal_description` (`type` TEXT NOT NULL, `repeat` INTEGER NOT NULL, `period_in_period_units` INTEGER NOT NULL, `period_unit` TEXT NOT NULL, `progress_type` TEXT NOT NULL, `paused` INTEGER NOT NULL DEFAULT false, `archived` INTEGER NOT NULL, `custom_order` INTEGER DEFAULT null, `deleted` INTEGER NOT NULL DEFAULT false, `created_at` TEXT NOT NULL, `modified_at` TEXT NOT NULL, `id` BLOB NOT NULL, PRIMARY KEY(`id`))")
            db.query("SELECT * FROM `goal_description`").use { cursor ->
                while (cursor.moveToNext()) {
                    val type = cursor.getString(cursor.getColumnIndexOrThrow("type")).let {
                        if (it == "CATEGORY_SPECIFIC") "ITEM_SPECIFIC" else it
                    }
                    val repeat = cursor.getInt(cursor.getColumnIndexOrThrow("repeat"))
                    val periodInPeriodUnits = cursor.getInt(cursor.getColumnIndexOrThrow("period_in_period_units"))
                    val periodUnit = cursor.getString(cursor.getColumnIndexOrThrow("period_unit"))
                    val progressType = cursor.getString(cursor.getColumnIndexOrThrow("progress_type"))
                    val archived = cursor.getInt(cursor.getColumnIndexOrThrow("archived"))
                    val createdAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                    val modifiedAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("modified_at")))
                    val id = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("id")))

                    db.execSQL(
                        "INSERT INTO `_new_goal_description` (`type`,`repeat`,`period_in_period_units`,`period_unit`,`progress_type`,`archived`,`custom_order`,`created_at`,`modified_at`,`id`) VALUES (?,?,?,?,?,?,NULL,?,?,?)",
                        arrayOf(type, repeat, periodInPeriodUnits, periodUnit, progressType, archived, createdAt, modifiedAt, id)
                    )
                }
            }
            db.execSQL("DROP TABLE `goal_description`")
            db.execSQL("ALTER TABLE `_new_goal_description` RENAME TO `goal_description`")

            // Rename target to target_seconds in goal_instance
            // Delete period_in_seconds, renewed, progress
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_goal_instance` (`goal_description_id` BLOB NOT NULL, `previous_goal_instance_id` BLOB, `start_timestamp` TEXT NOT NULL, `end_timestamp` TEXT, `target_seconds` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `modified_at` TEXT NOT NULL, `id` BLOB NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goal_description_id`) REFERENCES `goal_description`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.query("SELECT * FROM `goal_instance` ORDER BY `goal_description_id`, `start_timestamp`").use { cursor ->
                var previousGoalInstanceId: ByteArray? = null
                var previousGoalDescriptionId: ByteArray? = null
                var notDone = cursor.moveToFirst()
                while (notDone) {
                    val goalDescriptionId = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("goal_description_id")))
                    val startTimestamp = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("start_timestamp")))
                    val targetSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("target"))
                    val createdAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                    val modifiedAt = longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("modified_at")))
                    val id = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("id")))

                    if(!previousGoalDescriptionId.contentEquals(goalDescriptionId)) {
                        previousGoalInstanceId = null
                    }
                    previousGoalDescriptionId = goalDescriptionId

                    notDone = cursor.moveToNext()


                    val endTimestamp = if (
                        notDone &&
                        previousGoalDescriptionId.contentEquals(longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("goal_description_id"))))
                    ) {
                        // end_timestamp is the start_timestamp of the next goal_instance
                        longToZonedDateTimeString(cursor.getLong(cursor.getColumnIndexOrThrow("start_timestamp")))
                    } else {
                        null
                    }

                    db.execSQL(
                        "INSERT INTO `_new_goal_instance` (`goal_description_id`,`previous_goal_instance_id`,`start_timestamp`,`end_timestamp`,`target_seconds`,`created_at`,`modified_at`,`id`) VALUES (?,?,?,?,?,?,?,?)",
                        arrayOf(goalDescriptionId, previousGoalInstanceId, startTimestamp, endTimestamp, targetSeconds, createdAt, modifiedAt, id)
                    )

                    previousGoalInstanceId = id
                }
            }
            db.execSQL("DROP TABLE `goal_instance`")
            db.execSQL("ALTER TABLE `_new_goal_instance` RENAME TO `goal_instance`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_instance_goal_description_id` ON `goal_instance` (`goal_description_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_instance_previous_goal_instance_id` ON `goal_instance` (`previous_goal_instance_id`)")

            // Rename category_id to library_item_id in goal_description_library_item_cross_ref, formerly goal_description_category_cross_ref
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_goal_description_library_item_cross_ref` (`goal_description_id` BLOB NOT NULL, `library_item_id` BLOB NOT NULL, PRIMARY KEY(`goal_description_id`, `library_item_id`), FOREIGN KEY(`goal_description_id`) REFERENCES `goal_description`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`library_item_id`) REFERENCES `library_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.query("SELECT * FROM `goal_description_category_cross_ref`").use { cursor ->
                while (cursor.moveToNext()) {
                    val goalDescriptionId = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("goal_description_id")))
                    val libraryItemId = longToUuidInBytes(cursor.getLong(cursor.getColumnIndexOrThrow("category_id")))

                    db.execSQL(
                        "INSERT INTO `_new_goal_description_library_item_cross_ref` (`goal_description_id`,`library_item_id`) VALUES (?,?)",
                        arrayOf(goalDescriptionId, libraryItemId)
                    )
                }
            }
            db.execSQL("DROP TABLE `goal_description_category_cross_ref`")
            db.execSQL("ALTER TABLE `_new_goal_description_library_item_cross_ref` RENAME TO `goal_description_library_item_cross_ref`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_description_library_item_cross_ref_goal_description_id` ON `goal_description_library_item_cross_ref` (`goal_description_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_description_library_item_cross_ref_library_item_id` ON `goal_description_library_item_cross_ref` (`library_item_id`)")
        }
    }
}

/** Source: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/room/integration-tests/kotlintestapp/src/androidTest/java/androidx/room/integration/kotlintestapp/test/UuidColumnTypeAdapterTest.kt */
class UUIDConverter {
    @TypeConverter
    fun fromByte(bytes: ByteArray? = null): UUID? {
        if (bytes == null) return null
        val bb = ByteBuffer.wrap(bytes)
        return UUID(bb.long, bb.long)
    }

    @TypeConverter
    fun toByte(uuid: UUID? = null): ByteArray? {
        if (uuid == null) return null
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    companion object {
        fun fromInt(value: Int): UUID {
            return UUID.fromString(
                "00000000-0000-0000-0000-${value.toString().padStart(12, '0')}"
            )
        }

        val deadBeef: UUID
            get() = UUID.fromString("DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF")
    }
}



fun UUID.toDBString() =
    ByteBuffer.wrap(ByteArray(16)).let { buffer ->
        buffer.putLong(this.mostSignificantBits)
        buffer.putLong(this.leastSignificantBits)
        buffer.array().joinToString(separator = "") { "%02x".format(it) }
    }

fun ZonedDateTime.toDatabaseInterpretableString(): String =
    this.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

class ZonedDateTimeConverter {
    @TypeConverter
    fun fromZonedDateTime(zonedDateTime: ZonedDateTime?): String? {
        return zonedDateTime?.format(formatter)
    }

    @TypeConverter
    fun toZonedDateTime(zonedDateTimeString: String?): ZonedDateTime? {
        return zonedDateTimeString?.let { ZonedDateTime.parse(it, formatter) }
    }

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    }
}

data class Nullable<T>(
    val value: T?
)

abstract class NullableConverter<T> {
    @TypeConverter
    fun fromNullable(nullable: Nullable<T>? = null): T? {
        return nullable?.value
    }

    @TypeConverter
    fun toNullable(value: T): Nullable<T> {
        return Nullable(value)
    }
}

class NullableUUIDConverter : NullableConverter<UUID>()
class NullableIntConverter : NullableConverter<Int>()
class NullableZonedDateTimeConverter : NullableConverter<ZonedDateTime>()