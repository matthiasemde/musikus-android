/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package app.musikus.ui.overflowitems

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.musikus.R

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
