package de.practicetime.practicetime.ui.overflowitems

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.practicetime.practicetime.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setSupportActionBar(findViewById(R.id.about_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.app_info)
        }

        findViewById<TextView>(R.id.about_tv_help).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        findViewById<TextView>(R.id.about_tv_support).setOnClickListener {
            startActivity(Intent(this, DonationsActivity::class.java))
        }

        findViewById<TextView>(R.id.about_tv_legal).setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java))
        }

        findViewById<TextView>(R.id.about_tv_licences).setOnClickListener {
            startActivity(Intent(this, LicenseActivity::class.java))
        }

        findViewById<TextView>(R.id.about_appinfo).setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }
    }
}