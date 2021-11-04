package de.practicetime.practicetime

import androidx.room.*
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import de.practicetime.practicetime.entities.SessionWithSections

@Dao
interface PTDao {
    @Insert
    suspend fun insertSession(session: PracticeSession): Long

    @Insert
    suspend fun insertSection(section: PracticeSection)

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Delete
    suspend fun deleteSection(section: PracticeSection)

    @Query("SELECT * FROM PracticeSession")
    suspend fun getAllSessions(): List<PracticeSession>

    @Query("SELECT * FROM PracticeSection")
    suspend fun getAllSections(): List<PracticeSection>

    @Transaction
    @Query("SELECT * FROM PracticeSession")
    suspend fun getSessionWithSections(): List<SessionWithSections>
}