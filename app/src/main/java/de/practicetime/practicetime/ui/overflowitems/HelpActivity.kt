package de.practicetime.practicetime.ui.overflowitems

import android.content.Intent
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
import de.practicetime.practicetime.ui.intro.AppIntroActivity

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        setSupportActionBar(findViewById(R.id.help_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.help_title)
        }

        findViewById<MaterialButton>(R.id.help_replay_intro).setOnClickListener {
            val i = Intent(this, AppIntroActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(i)
        }

        val arr = resources.getStringArray(R.array.array_help_tips_text_bulletlist)
        val ssb = SpannableStringBuilder()

        arr.forEachIndexed { i, elem ->
            val ss = SpannableString(elem)
            ss.setSpan(
                BulletSpan(PracticeTime.dp(this,10).toInt()),
                0,
                elem.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.append(ss)

            //avoid last "\n"
            if (i != arr.lastIndex)
                ssb.append("\n")
        }
        findViewById<TextView>(R.id.help_tips_text).text = TextUtils.concat(
            getString(R.string.help_tips_text),
            "\n",
            ssb
        )

    }
}