/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package app.musikus.database

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
import app.musikus.Musikus.Companion.ioThread
import app.musikus.database.daos.GoalDescriptionDao
import app.musikus.database.daos.GoalInstanceDao
import app.musikus.database.daos.LibraryFolderDao
import app.musikus.database.daos.LibraryItemDao
import app.musikus.database.daos.SectionDao
import app.musikus.database.daos.SessionDao
import app.musikus.database.entities.GoalDescriptionLibraryItemCrossRefModel
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalInstanceModel
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.SectionModel
import app.musikus.database.entities.SessionModel
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import app.musikus.utils.prepopulateDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
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
        private const val DATABASE_NAME = "musikus-database"

        fun buildDatabase(
            app: Application,
            databaseProvider: Provider<MusikusDatabase>,
        ) =
            Room.databaseBuilder(
                app,
                MusikusDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(
                PTDatabaseMigrationOneToTwo
            ).addCallback(object : Callback() {
                // prepopulate the database after onCreate was called
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // prepopulate the database
                    ioThread { runBlocking {
                        prepopulateDatabase(databaseProvider.get())
                    } }
                }
            }).build()
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

fun ZonedDateTime.toDatabaseStorageString(): String =
    this.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

fun ZonedDateTime.toDatabaseInterpretableString(): String =
    this.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

class ZonedDateTimeConverter {
    @TypeConverter
    fun fromZonedDateTime(zonedDateTime: ZonedDateTime?): String? {
        return zonedDateTime?.toDatabaseStorageString()
    }

    @TypeConverter
    fun toZonedDateTime(zonedDateTimeString: String?): ZonedDateTime? {
        return zonedDateTimeString?.let { ZonedDateTime.parse(it) }
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

object PTDatabaseMigrationOneToTwo : Migration(1,2) {
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

//@RenameColumn(tableName = "goal_description", fromColumnName = "oneTime", toColumnName = "repeat")
//@RenameColumn(tableName = "goal_description", fromColumnName = "oneTime", toColumnName = "repeat")
//@RenameColumn(tableName = "goal_description", fromColumnName = "oneTime", toColumnName = "repeat")
//object PTDatabaseMigrationTwoToThree : Migration(2,3) {
//}
