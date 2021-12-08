package de.practicetime.practicetime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

class FullscreenSessionActivity : AppCompatActivity() {

    private lateinit var dao: PTDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_session)

        openDatabase()

        val sessionId = intent.extras?.getInt("KEY_SESSION")

        if (sessionId != null) {
            showFullscreenSession(sessionId)
        } else {
            exitActivity()
        }
    }

    private fun showFullscreenSession(id: Int) {
//        lifecycleScope.launch {
//            dao.getSessionWithSectionsWithCategories(id)
//        }
    }

    /*************************************************************************
     * Utility functions
     *************************************************************************/

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            this,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private fun exitActivity() {
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}