package de.practicetime.practicetime

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession

@Dao
interface PTDao {
    @Insert
    suspend fun insertSession(session: PracticeSession)

    @Insert
    suspend fun insertSection(section: PracticeSection)

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Delete
    suspend fun deleteSection(section: PracticeSection)

    @Query("SELECT * FROM practicesession")
    suspend fun getAllSessions(): List<PracticeSession>

    @Query("SELECT * FROM practicesection")
    suspend fun getAllSections(): List<PracticeSection>
}