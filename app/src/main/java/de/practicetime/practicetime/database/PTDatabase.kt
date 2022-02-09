package de.practicetime.practicetime.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.*
import de.practicetime.practicetime.database.daos.*
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.utils.getCurrTimestamp

@Database(
    version = 2,
    entities = [
        Session::class,
        Section::class,
        Category::class,
        GoalDescription::class,
        GoalInstance::class,
        GoalDescriptionCategoryCrossRef::class,
    ],
    exportSchema = true,
)
abstract class PTDatabase : RoomDatabase() {
    abstract val categoryDao : CategoryDao
    abstract val goalDescriptionDao : GoalDescriptionDao
    abstract val goalInstanceDao : GoalInstanceDao
    abstract val sessionDao : SessionDao
    abstract val sectionDao : SectionDao
}

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
                db.update(tableName, SQLiteDatabase.CONFLICT_IGNORE, ContentValues().let {
                    it.put("created_at", getCurrTimestamp() + id)
                    it.put("modified_at", getCurrTimestamp() + id)
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
