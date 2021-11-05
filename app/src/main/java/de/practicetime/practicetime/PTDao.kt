package de.practicetime.practicetime

import androidx.room.*
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import de.practicetime.practicetime.entities.SessionWithSections

@Dao
interface PTDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: PracticeSection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Delete
    suspend fun deleteSection(section: PracticeSection)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Update
    suspend fun archiveCategory(category: Category)

    @Query("SELECT * FROM PracticeSession")
    suspend fun getAllSessions(): List<PracticeSession>

    @Query("SELECT * FROM PracticeSection")
    suspend fun getAllSections(): List<PracticeSection>

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM Category WHERE NOT archived")
    suspend fun getActiveCategories(): List<Category>

    @Transaction
    @Query("SELECT * FROM PracticeSession WHERE rating > :rating")
    suspend fun getSessionsWithSections(rating: Int): List<SessionWithSections>
}