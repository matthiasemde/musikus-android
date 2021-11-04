package de.practicetime.practicetime

import androidx.room.Database
import androidx.room.RoomDatabase
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession

@Database(
    entities = [
        PracticeSession::class,
        PracticeSection::class
    ],
    version = 1
)
abstract class PTDatabase : RoomDatabase() {
    abstract val ptDao : PTDao
}