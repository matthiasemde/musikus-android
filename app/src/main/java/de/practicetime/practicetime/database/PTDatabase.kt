package de.practicetime.practicetime.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.*
import de.practicetime.practicetime.database.daos.CategoryDao
import de.practicetime.practicetime.database.daos.GoalDescriptionDao
import de.practicetime.practicetime.database.daos.GoalInstanceDao
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.utils.getCurrTimestamp

@Database(
    version = 2,
    entities = [
        PracticeSession::class,
        PracticeSection::class,
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
    abstract val ptDao : PTDao
    abstract val categoryDao : CategoryDao
    abstract val goalDescriptionDao : GoalDescriptionDao
    abstract val goalInstanceDao : GoalInstanceDao

    @RenameTable(fromTableName = "Category", toTableName = "category")
    @RenameColumn(tableName = "Category", fromColumnName = "colorIndex", toColumnName = "color_index")

    @RenameTable(fromTableName = "GoalInstance", toTableName = "goal_instance")
    @RenameColumn(tableName = "GoalInstance", fromColumnName = "goalDescriptionId", toColumnName = "goal_description_id")
    @RenameColumn(tableName = "GoalInstance", fromColumnName = "startTimestamp", toColumnName = "start_timestamp")
    @RenameColumn(tableName = "GoalInstance", fromColumnName = "periodInSeconds", toColumnName = "period_in_seconds")

    @RenameTable(fromTableName = "GoalDescription", toTableName = "goal_description")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "oneTime", toColumnName = "repeat") // invert all entries :D TODO
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "periodInPeriodUnits", toColumnName = "period_in_period_units")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "periodUnit", toColumnName = "period_unit")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "progressType", toColumnName = "progress_type")
    @RenameColumn(tableName = "GoalDescription", fromColumnName = "profileId", toColumnName = "profile_id")

    @RenameTable(fromTableName = "GoalDescriptionCategoryCrossRef", toTableName = "goal_description_category_cross_ref")
    @RenameColumn(tableName = "GoalDescriptionCategoryCrossRef", fromColumnName = "goalDescriptionId", toColumnName = "goal_description_id")
    @RenameColumn(tableName = "GoalDescriptionCategoryCrossRef", fromColumnName = "categoryId", toColumnName = "category_id")
    class AutoMigrationFromOneToTwo : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            val cursor = db.query(SupportSQLiteQueryBuilder.builder("category").let {
                it.columns(arrayOf("id"))
                it.create()
            })
            while(cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                db.update("category", SQLiteDatabase.CONFLICT_IGNORE , ContentValues().let {
                    it.put("created_at", getCurrTimestamp() + id)
                    it.put("modified_at", getCurrTimestamp() + id)
                    it
                }, "id=?", arrayOf(id))
            }
            Log.d("POST_MIGRATION", "Migration complete")
        }
    }
}

