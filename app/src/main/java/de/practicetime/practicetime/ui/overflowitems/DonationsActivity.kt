/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.overflowitems

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.BulletSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R

class DonationsActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donations)

        setSupportActionBar(findViewById(R.id.donations_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.donations_title)
        }

        findViewById<MaterialButton>(R.id.donate_btn).setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate)))
            )
        }

        val arr = resources.getStringArray(R.array.array_donations_text_bulletlist)
        val ssb = SpannableStringBuilder()

        arr.forEachIndexed { index, s ->
            val ss = SpannableString(s)
            ss.setSpan(
                BulletSpan(PracticeTime.dp(this, 10).toInt()),
                0,
                s.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.append(ss)

            if (index != arr.lastIndex)
                ssb.append("\n")
        }
        findViewById<TextView>(R.id.donations_text).text = TextUtils.concat(
            getString(R.string.donations_text),
            "\n",
            ssb
        )
    }
}
