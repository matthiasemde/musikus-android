package de.practicetime.practicetime.ui.overflowitems

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.R

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        findViewById<MaterialButton>(R.id.help_replay_intro).setOnClickListener {
            Toast.makeText(this, "Replay Intro", Toast.LENGTH_SHORT).show()
        }
    }
}