/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.overflowitems

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.practicetime.practicetime.R

class AboutAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        setSupportActionBar(findViewById(R.id.about_appinfo_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.about_app_title)
        }
    }
}
