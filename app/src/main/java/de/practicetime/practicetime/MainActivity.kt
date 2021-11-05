package de.practicetime.practicetime

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize Database
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao

        // populate the category table
        lifecycleScope.launch {
            val prefs = getPreferences(Context.MODE_PRIVATE)


            // FIRST RUN routine
            if (prefs.getBoolean("firstrun", true)) {

                val categories = listOf(
                    Category(0, "Die Schöpfung", 0, false, 1),
                    Category(0, "Beethoven Septett", 0, false, 1),
                    Category(0, "Schostakowitsch 9.", 0, false, 1),
                )
                categories.forEach {
                    dao?.insertCategory(it)
                }

                prefs.edit().putBoolean("firstrun", false).commit();
            }
        }

        fun categoryPressed(categoryView: View) {
            Snackbar.make(categoryView, "Category ${categoryView.tag} Pressed!", Snackbar.LENGTH_SHORT).show()
        }

        // initialize adapter and recyclerView
        var categories = ArrayList<Category>()
        val categoryAdapter = CategoryAdapter(categories, ::categoryPressed)

        val categoryList = findViewById<RecyclerView>(R.id.categoryList)
        categoryList.layoutManager = GridLayoutManager(this, 2)
        categoryList.adapter = categoryAdapter

        lifecycleScope.launch {
            val activeCategories = dao?.getActiveCategories()
            if(activeCategories != null) {
                categories.addAll(activeCategories)
                categoryAdapter.notifyDataSetChanged()
            }
        }

        // the sectionBuffer will keep track of all the section in the current session
        val sectionBuffer = mutableListOf<PracticeSection>()

        // add section button functionality
//        findViewById<Button>(R.id.addSection).setOnClickListener {
//            val categoryId = findViewById<EditText>(R.id.categoryId).text.toString()
//            val duration = findViewById<EditText>(R.id.duration).text.toString()
//
//            if (categoryId.isEmpty() || duration.isEmpty()) {
//                Snackbar.make(it, "Please fill out all Section fields!", Snackbar.LENGTH_SHORT).show()
//            } else {
//
//                val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()).toLong()
//
//                // id=0 means not assigned, autoGenerate=true will do it for us
//                val newSection = PracticeSection(
//                    0,
//                    null,
//                    categoryId.toInt(),
//                    duration.toInt(),
//                    timeStamp
//                )
//
//                sectionBuffer.add(newSection)
//                lifecycleScope.launch {
//                    fillSectionListView(sectionBuffer.toList())
//                }
//            }
//        }

        // add session button functionality
        findViewById<Button>(R.id.addSession).setOnClickListener {
            val date = findViewById<EditText>(R.id.date).text.toString()
            val breakDuration = findViewById<EditText>(R.id.breakDuration).text.toString()
            val rating = findViewById<EditText>(R.id.rating).text.toString()
            val comment = findViewById<EditText>(R.id.comment).text.toString()

            if (date.isEmpty() || breakDuration.isEmpty() || rating.isEmpty() || comment.isEmpty()) {
                Snackbar.make(it, "Please fill out all Session fields!", Snackbar.LENGTH_SHORT).show()
            } else {
                // id=0 means not assigned, autoGenerate=true will do it for us
                val newSession = PracticeSession(
                    0,
                    date.toLong(),
                    breakDuration.toInt(),
                    rating.toInt(),
                    comment,
                    1
                )

                lifecycleScope.launch {
                    val sessionId = dao?.insertSession(newSession)
                    for(section in sectionBuffer) {
                        section.practice_session_id = sessionId?.toInt()
                        dao?.insertSection(section)
                    }
                    sectionBuffer.clear()
                    fillSectionListView(sectionBuffer.toList())
                    fillSessionListView()
                }
            }
        }

        fillSectionListView(sectionBuffer.toList())
        fillSessionListView()
    }

    private fun fillSectionListView(sections: List<PracticeSection>) {
        // show all sections in listview
        lifecycleScope.launch {
            var listItems = ArrayList<String>()
            sections.forEach {
                listItems.add("categ: " + it.category_id + "   |   dur: " + it.duration)
            }
            val sectionsList = findViewById<ListView>(R.id.currentSections)
            var adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, listItems)
            sectionsList.adapter = adapter
            adapter.notifyDataSetChanged()

        }
    }

    private fun fillSessionListView() {
        // show all sections in listview
        lifecycleScope.launch {
            var sessions: List<PracticeSession> = dao!!.getAllSessions()
            var listItems = ArrayList<String>()
            sessions.forEach {
                listItems.add("d: " + it.date +
                        " | brk: " + it.break_duration +
                        " | r: " + it.rating +
                        " | c: " + it.comment
                )
            }
            val sessionsList = findViewById<ListView>(R.id.currentSessions)
            var adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, listItems)
            sessionsList.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }
}

class CategoryAdapter(
    private val dataSet: ArrayList<Category>,
    private val callback: (input: View) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: Button

        init {
            button = view.findViewById(R.id.button)
            // Define click listener for the ViewHolder's View.
            button.setOnClickListener(callback)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.category_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position
        val category = dataSet[position]

        // store the id of the category on the button
        viewHolder.button.tag = category.id

        // archived categories should not be displayed
        if(category.archived) {
            viewHolder.button.visibility = View.GONE
        }

        // contents of the view with that element
        viewHolder.button.text = category.name
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}