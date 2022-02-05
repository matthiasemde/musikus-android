package de.practicetime.practicetime.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.practicetime.practicetime.database.entities.*

@Database(
    entities = [
        PracticeSession::class,
        PracticeSection::class,
        Category::class,
        GoalDescription::class,
        GoalInstance::class,
        GoalDescriptionCategoryCrossRef::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class PTDatabase : RoomDatabase() {
    abstract val ptDao : PTDao
}