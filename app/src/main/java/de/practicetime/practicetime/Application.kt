package de.practicetime.practicetime

import android.app.Application
import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.room.Room
import de.practicetime.practicetime.database.PTDao
import de.practicetime.practicetime.database.PTDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PracticeTime : Application() {
    val executorService: ExecutorService = Executors.newFixedThreadPool(4)

    companion object {
        lateinit var dao: PTDao         // the central static dao object of the application
        lateinit var ctx: PracticeTime
        var serviceIsRunning = false
        var isRecording = false
    }

    override fun onCreate() {
        super.onCreate()
        ctx = this
        openDatabase()
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    /**
     * Get a color int from a theme attribute.
     * Activity context must be used instead of applicationContext: https://stackoverflow.com/q/34052810
     * Access like PracticeTime.ctx.getThemeColor() in Activity
     * */
    @ColorInt
    fun getThemeColor(@AttrRes color: Int, activityContext: Context): Int {
        val typedValue = TypedValue()
        activityContext.theme.resolveAttribute(color, typedValue, true)
        Log.d("ZTASG", "${typedValue.data} ##################################")
        return typedValue.data
    }
}