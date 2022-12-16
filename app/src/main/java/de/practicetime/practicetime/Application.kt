/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package de.practicetime.practicetime

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.preferencesDataStore
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.repository.UserPreferencesRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

val Context.dataStore by preferencesDataStore(name = PracticeTime.USER_PREFERENCES_NAME)

class PracticeTime : Application() {

    companion object {
        val executorService: ExecutorService = Executors.newFixedThreadPool(4)
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

        fun ioThread(f : () -> Unit) {
            IO_EXECUTOR.execute(f)
        }

        private lateinit var db: PTDatabase
        private lateinit var dbFile: File

        var exportLauncher: ActivityResultLauncher<String>? = null
        var importLauncher: ActivityResultLauncher<Array<String>>? = null

        // the database accessing objects of the application
//        lateinit var libraryItemDao: LibraryItemDao
//        lateinit var libraryFolderDao: LibraryFolderDao
//        lateinit var goalDescriptionDao: GoalDescriptionDao
//        lateinit var goalInstanceDao: GoalInstanceDao
//        lateinit var sessionDao: SessionDao
//        lateinit var sectionDao: SectionDao

        var noSessionsYet = true
        var serviceIsRunning = false

        lateinit var prefs: SharedPreferences

        const val USER_PREFERENCES_NAME = "user_preferences"



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

        fun getLibraryItemColors(context: Context): MutableList<Int> {
            return context.resources?.getIntArray(R.array.library_item_colors)
                ?.toCollection(mutableListOf()) ?: mutableListOf()
        }

        fun getRandomQuote(context: Context) : CharSequence {
            return context.resources.getTextArray(R.array.quotes).random()
        }

        fun dp(context: Context, dp: Int): Float {
            return context.resources.displayMetrics.density * dp
        }


        fun importDatabase() {
            importLauncher?.launch(arrayOf("*/*"))
        }

        fun importDatabaseCallback(context: Context, uri: Uri?) {
            uri?.let {
                // close the database to collect all logs
                db.close()

                // delete old database
                dbFile.delete()

                // copy new database
                dbFile.outputStream().let { outputStream ->
                    context.contentResolver.openInputStream(it)?.let { inputStream ->
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                    }
                    outputStream.close()

                    Toast.makeText(context, "Backup successful", Toast.LENGTH_LONG).show()
                }

                // open new database
//                openDatabase(context)
            }
        }

        fun exportDatabase() {
            exportLauncher?.launch("practice_time_backup")
        }

        fun exportDatabaseCallback(context: Context, uri: Uri?) {
            uri?.let {
                // close the database to collect all logs
                db.close()

                // copy database
                context.contentResolver.openOutputStream(it)?.let { outputStream ->
                    dbFile.inputStream().let { inputStream ->
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                    }
                    outputStream.close()

                    Toast.makeText(context, "Backup successful", Toast.LENGTH_LONG).show()
                }

                // open database again
//                openDatabase(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences(getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)

        val userPreferencesRepository = UserPreferencesRepository(dataStore)

        MainScope().launch {
            userPreferencesRepository.userPreferences.map { preferences ->
                preferences.theme
            }.collect {
                Log.d("Theme", "Theme changed to ${it.name}")
                AppCompatDelegate.setDefaultNightMode(it.ordinal)
            }
        }

        dbFile = getDatabasePath("practice_time.db")
    }
}
