package de.practicetime.practicetime.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
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
    autoMigrations = [
        AutoMigration (
            from = 1,
            to = 2,
            spec = PTDatabase.AutoMigrationFromOneToTwo::class
        ),
    ],
    exportSchema = true,
)
abstract class PTDatabase : RoomDatabase() {
    abstract val categoryDao : CategoryDao
    abstract val goalDescriptionDao : GoalDescriptionDao
    abstract val goalInstanceDao : GoalInstanceDao
    abstract val sessionDao : SessionDao
    abstract val sectionDao : SectionDao

    @RenameTable(fromTableName = "Category", toTableName = "category")
    @RenameColumn(tableName = "Category", fromColumnName = "colorIndex", toColumnName = "color_index")

    @RenameTable(fromTableName = "GoalInstance", toTableName = "goal_instance")
    @RenameColumn(tableName = "GoalInstance", fromColumnName = "goalDescriptionId", toColumnName = "goal_description_id")
    @RenameColumn(tableName = "GoalInstance", fromColumnName = "startTimestamp", toColumnName = "start_timestamp")
    @RenameColumn(tableName = "GoalInstance", fromColumnName = "periodInSeconds", toColumnName = "period_in_seconds")

    @RenameTable(fromTableName = "GoalDescription", toTableName = "goal_description")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "oneTime", toColumnName = "repeat")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "periodInPeriodUnits", toColumnName = "period_in_period_units")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "periodUnit", toColumnName = "period_unit")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "progressType", toColumnName = "progress_type")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "profileId", toColumnName = "profile_id")

    @RenameTable(fromTableName = "GoalDescriptionCategoryCrossRef", toTableName = "goal_description_category_cross_ref")
    @RenameColumn(tableName = "GoalDescriptionCategoryCrossRef", fromColumnName = "goalDescriptionId", toColumnName = "goal_description_id")
    @RenameColumn(tableName = "GoalDescriptionCategoryCrossRef", fromColumnName = "categoryId", toColumnName = "category_id")

    @RenameTable(fromTableName = "PracticeSession", toTableName = "session")
    @RenameColumn(tableName = "PracticeSession", fromColumnName = "breakDuration", toColumnName = "break_duration")
    @RenameColumn(tableName = "PracticeSession", fromColumnName = "profileId", toColumnName = "profile_id")

    @RenameTable(fromTableName = "PracticeSection", toTableName = "section")
    @RenameColumn(tableName = "PracticeSection", fromColumnName = "practice_session_id", toColumnName = "session_id")
    @RenameColumn(tableName = "PracticeSection", fromColumnName = "category_id", toColumnName = "category_id")
    class AutoMigrationFromOneToTwo : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            Log.d("POST_MIGRATION", "Starting Post Migration...")

            for (tableName in listOf("session", "category", "goal_description", "goal_instance")) {
                val cursor = db.query(SupportSQLiteQueryBuilder.builder(tableName).let {
                    it.columns(arrayOf("id"))
                    it.create()
                })
                while(cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    db.update(tableName, SQLiteDatabase.CONFLICT_IGNORE , ContentValues().let {
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
            while(cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val oneTime = cursor.getInt(cursor.getColumnIndexOrThrow("repeat"))
                db.update("goal_description", SQLiteDatabase.CONFLICT_IGNORE , ContentValues().let {
                    it.put("repeat", if(oneTime == 0) "1" else "0")
                    it
                }, "id=?", arrayOf(id))
            }
            Log.d("POST_MIGRATION", "Migrated 'oneTime' to 'repeat' for goal descriptions")

            Log.d("POST_MIGRATION", "Post Migration complete")
        }
    }
}

