package de.practicetime.practicetime.ui.overflowitems

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.R
import de.practicetime.practicetime.ui.intro.AppIntroActivity

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        findViewById<MaterialButton>(R.id.help_replay_intro).setOnClickListener {
            val i = Intent(this, AppIntroActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(i)
        }
    }
}