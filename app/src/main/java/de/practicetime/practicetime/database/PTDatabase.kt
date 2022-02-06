package de.practicetime.practicetime.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.database.daos.CategoryDao
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

    @RenameTable(fromTableName = "Category", toTableName = "category")
    @RenameColumn(tableName = "Category", fromColumnName = "colorIndex", toColumnName = "color_index")
    class AutoMigrationFromOneToTwo : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
//            super.onPostMigrate(db)
            Log.d("POST_MIGRATION", "Migration complete")
            db.update("category", SQLiteDatabase.CONFLICT_IGNORE , ContentValues().let {
                it.put("created_at", getCurrTimestamp())
                it.put("modified_at", getCurrTimestamp())
                it
            }, null, null)
        }
    }
}

