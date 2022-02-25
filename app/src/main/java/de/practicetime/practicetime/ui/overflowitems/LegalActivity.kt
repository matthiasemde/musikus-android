package de.practicetime.practicetime.ui.overflowitems

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.practicetime.practicetime.R

class LegalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)

        setSupportActionBar(findViewById(R.id.legal_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.legal_title)
        }
    }
}