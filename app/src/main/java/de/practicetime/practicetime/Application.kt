package de.practicetime.practicetime

import android.app.Application
import androidx.room.Room
import de.practicetime.practicetime.database.PTDao
import de.practicetime.practicetime.database.PTDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PracticeTime : Application() {
    val executorService: ExecutorService = Executors.newFixedThreadPool(4)

    companion object {
        lateinit var dao: PTDao
        var serviceIsRunning = false
        var isRecording = false
    }

    override fun onCreate() {
        super.onCreate()

        openDatabase()
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }


}