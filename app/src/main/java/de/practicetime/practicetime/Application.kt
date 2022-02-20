package de.practicetime.practicetime

import android.app.Application
import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.room.Room
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.PTDatabaseMigrationOneToTwo
import de.practicetime.practicetime.database.daos.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PracticeTime : Application() {
    val executorService: ExecutorService = Executors.newFixedThreadPool(4)

    companion object {
        // the database accessing objects of the application
        lateinit var categoryDao: CategoryDao
        lateinit var goalDescriptionDao: GoalDescriptionDao
        lateinit var goalInstanceDao: GoalInstanceDao
        lateinit var sessionDao: SessionDao
        lateinit var sectionDao: SectionDao

        var noSessionsYet = true
        var serviceIsRunning = false
        const val PREFERENCES_KEY_FIRSTRUN = "firstrun"
        const val PREFERENCES_KEY_THEME = "theme"
        const val PREFERENCES_KEY_APPINTRO_DONE = "appintro_done"

        /**
         * Get a color int from a theme attribute.
         * Activity context must be used instead of applicationContext: https://stackoverflow.com/q/34052810
         * Access like PracticeTime.getThemeColor() in Activity
         * */
        @ColorInt
        fun getThemeColor(@AttrRes color: Int, activityContext: Context): Int {
            val typedValue = TypedValue()
            activityContext.theme.resolveAttribute(color, typedValue, true)
            return typedValue.data
        }

        fun getCategoryColors(context: Context): MutableList<Int> {
            return context.resources?.getIntArray(R.array.category_colors)
                ?.toCollection(mutableListOf()) ?: mutableListOf()
        }

        fun getRandomQuote(context: Context) : CharSequence {
            return context.resources.getTextArray(R.array.quotes).random()
        }

        fun dp(context: Context, dp: Int): Float {
            return context.resources.displayMetrics.density * dp;
        }

    }

    override fun onCreate() {
        super.onCreate()
        openDatabase()
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).addMigrations(
            PTDatabaseMigrationOneToTwo
        ).build()
        categoryDao = db.categoryDao
        goalDescriptionDao = db.goalDescriptionDao
        goalInstanceDao = db.goalInstanceDao
        sessionDao = db.sessionDao
        sectionDao = db.sectionDao
    }

}