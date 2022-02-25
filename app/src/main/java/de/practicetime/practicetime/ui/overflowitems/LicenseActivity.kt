package de.practicetime.practicetime.ui.overflowitems

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.practicetime.practicetime.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LicenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        setSupportActionBar(findViewById(R.id.license_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.licences_title)
        }

        val lib1 = findViewById<LinearLayout>(R.id.layout_license_lib1)
        val tvName1 = lib1.findViewById<TextView>(R.id.tv_license_name)
        val tvAuthor1 = lib1.findViewById<TextView>(R.id.tv_license_author)

        tvName1.text = getString(R.string.lib1_name)
        tvAuthor1.text = getString(R.string.lib1_author)

        lib1.setOnClickListener {
            val items = arrayOf<CharSequence>("View website", "View license")
            AlertDialog.Builder(this)
                .setTitle(tvName1.text)
                .setItems(items) { _, which ->
                    if (which == 0) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.lib1_link))))
                    } else if (which == 1) {
                        showLicenseText("LICENSE_APACHE-2.0.txt")
                    }
                }
                .show()
        }


        val lib2 = findViewById<LinearLayout>(R.id.layout_license_lib2)
        val tvName2 = lib2.findViewById<TextView>(R.id.tv_license_name)
        val tvAuthor2 = lib2.findViewById<TextView>(R.id.tv_license_author)

        tvName2.text = getString(R.string.lib2_name)
        tvAuthor2.text = getString(R.string.lib2_author)

        lib2.setOnClickListener {
            val items = arrayOf<CharSequence>("View website", "View license")
            AlertDialog.Builder(this)
                .setTitle(tvName2.text)
                .setItems(items) { _, which ->
                    if (which == 0) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.lib2_link))))
                    } else if (which == 1) {
                        showLicenseText("LICENSE_APACHE-2.0.txt")
                    }
                }
                .show()
        }
    }

    private fun showLicenseText(licenseTextFile: String) {
        try {
            val reader = BufferedReader(InputStreamReader(
                assets.open(licenseTextFile), "UTF-8"))
            val licenseText = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                licenseText.append(line).append("\n")
            }
            AlertDialog.Builder(this)
                .setMessage(licenseText)
                .show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}