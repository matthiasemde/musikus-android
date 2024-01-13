/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package de.practicetime.practicetime

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.room.Room
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.PTDatabaseMigrationOneToTwo
import de.practicetime.practicetime.database.daos.CategoryDao
import de.practicetime.practicetime.database.daos.GoalDescriptionDao
import de.practicetime.practicetime.database.daos.GoalInstanceDao
import de.practicetime.practicetime.database.daos.SectionDao
import de.practicetime.practicetime.database.daos.SessionDao
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val MIME_TYPE_DATABASE = "application/octet-stream"

class PracticeTime : Application() {
    val executorService: ExecutorService = Executors.newFixedThreadPool(4)

    companion object {
        private lateinit var db: PTDatabase
        private lateinit var dbFile: File

        var exportLauncher: ActivityResultLauncher<String>? = null
        var importLauncher: ActivityResultLauncher<Array<String>>? = null

        var csvExportLauncher: ActivityResultLauncher<String>? = null

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
        const val PREFERENCES_KEY_UPDATE_1_1_0 = "update_1_1_0"

        const val DATABASE_NAME = "pt-database"
        const val DATABASE_NAME_BACKUP = "pt-database.bkp"

        private fun openDatabase(applicationContext: Context) {
            db = Room.databaseBuilder(
                applicationContext,
                PTDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(
                PTDatabaseMigrationOneToTwo
            ).build()
                .also {
                    categoryDao = it.categoryDao
                    goalDescriptionDao = it.goalDescriptionDao
                    goalInstanceDao = it.goalInstanceDao
                    sessionDao = it.sessionDao
                    sectionDao = it.sectionDao
                }

            dbFile = applicationContext.getDatabasePath(DATABASE_NAME)
        }

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
            return context.resources.displayMetrics.density * dp
        }


        fun importDatabase() {
            importLauncher?.launch(arrayOf(MIME_TYPE_DATABASE, "application/vnd.sqlite3", "application/x-sqlite3"))
        }

        suspend fun importDatabaseCallback(context: Context, uri: Uri?) : Boolean {
            if(uri == null) {
                Toast.makeText(context, "Restore aborted", Toast.LENGTH_LONG).show()
                return false
            }

            // close the database to collect all logs
            db.close()

            // create a backup of the original database
            val tempDbBackup = context.getDatabasePath(DATABASE_NAME_BACKUP)
            copyFile(source = dbFile, destination = tempDbBackup)

            // delete old database
            dbFile.delete()

            // copy new database
            dbFile.outputStream().let { outputStream ->
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                }
                outputStream.close()
            }

            // open new database
            openDatabase(context)

            // validate database
            if (!db.validate()) {
                // restore backup
                db.close()
                dbFile.delete()
                copyFile(source = tempDbBackup, destination = dbFile)
                tempDbBackup.delete()

                openDatabase(context)
                // show error message as dialog
                AlertDialog.Builder(context)
                    .setTitle("Import failed")
                    .setMessage("The imported database is invalid. No changes were made.")
                    .setPositiveButton("OK") { _, _ ->
                        Toast.makeText(context, ":-(", Toast.LENGTH_LONG).show()
                    }
                    .show()
                return false
            }
            Toast.makeText(context, "Import successful", Toast.LENGTH_LONG).show()
            return true
        }

        fun exportDatabase() {
            exportLauncher?.launch("practice_time_backup")
        }


        private fun copyFile(source: File, destination: File) {
            source.inputStream().let { inputStream ->
                destination.outputStream().let { outputStream ->
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                }
            }
        }

        fun exportDatabaseCallback(context: Context, uri: Uri?) {
            if(uri == null) {
                Toast.makeText(context, "Backup aborted", Toast.LENGTH_LONG).show()
                return
            }

            // close the database to collect all logs
            db.close()

            // copy database
            val outputStream = context.contentResolver.openOutputStream(uri)
            if(outputStream == null) {
                Toast.makeText(context, "Failed to create Backup", Toast.LENGTH_LONG).show()
                return
            }

            dbFile.inputStream().let { inputStream ->
                val bytes = inputStream.copyTo(outputStream)
                if(bytes == 0L) {
                    Toast.makeText(context, "Failed to create Backup", Toast.LENGTH_LONG).show()
                }
                inputStream.close()
            }
            outputStream.close()

            Toast.makeText(context, "Backup successful", Toast.LENGTH_LONG).show()

            // open database again
            openDatabase(context)
        }

        fun exportSessionsAsCsv() {
            csvExportLauncher?.launch("practice_sessions.csv")
        }

        suspend fun exportSessionsAsCsvCallback(context: Context, uri: Uri?) {
            uri?.let { nonNullUri ->
                // copy database
                context.contentResolver.openOutputStream(nonNullUri)?.let { outputStream ->
                    outputStream.write(
                        "time/date;practice_duration;break_duration;rating;comment\n".toByteArray()
                    )

                    db.sessionDao.getAllWithSectionsWithCategories().forEach { sessionWithSectionsWithCategories ->
                        val session = sessionWithSectionsWithCategories.session
                        val duration = sessionWithSectionsWithCategories.sections.sumOf { it.section.duration ?: 0 }
                        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(session.createdAt), ZoneId.systemDefault()).format(
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        outputStream.write(
                            "$dateTime;$duration;${session.breakDuration};${session.rating};${session.comment}\n".toByteArray()
                        )
                    }

                    outputStream.close()

                    Toast.makeText(context, "Export successful", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        openDatabase(applicationContext)
    }
}
