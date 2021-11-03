package de.practicetime.practicetime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.addSection).setOnClickListener {
            val categoryId = findViewById<EditText>(R.id.categoryId).text
            val duration = findViewById<EditText>(R.id.duration).text

            Toast.makeText(this, "Played category $categoryId for ${duration}h.", Toast.LENGTH_SHORT).show()
        }
    }
}