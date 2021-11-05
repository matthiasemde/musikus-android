package de.practicetime.practicetime

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.text.SimpleDateFormat
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

        lifecycleScope.launch {
            val prefs = getPreferences(Context.MODE_PRIVATE)


            // FIRST RUN routine
            if (prefs.getBoolean("firstrun", true)) {

                // populate the category table on first run
                listOf(
                    Category(0, "Die Schöpfung", 0, false, 1),
                    Category(0, "Beethoven Septett", 0, false, 1),
                    Category(0, "Schostakowitsch 9.", 0, false, 1),
                ).forEach {
                    dao?.insertCategory(it)
                }

                prefs.edit().putBoolean("firstrun", false).commit();
            }
        }

        // the sectionBuffer will keep track of all the section in the current session
        val sectionBuffer = ArrayList<PracticeSection>()

        // keep track of whether a session is active
        var sessionActive = false

        // the routine for handling presses to category buttons
        fun categoryPressed(categoryView: View) {
            // get the category id from the view tag and calculate current timestamp
            val categoryId = categoryView.tag as Int
            val now = Date().time / 1000

            // if there is no active session set sessionActive to true
            if(!sessionActive) {
                sessionActive = true
            } else {
                var lastSection = sectionBuffer.last()

                // store the duration of the now ending section
                lastSection.duration = (
                    now -
                    lastSection.timestamp
                ).toInt()

                // show the new section in the listView
                fillSectionListView(sectionBuffer)
            }

            // start a new section for the chosen category and add it to the section buffer
            sectionBuffer.add(
                PracticeSection(
                    0,
                    null,
                    categoryId,
                    null,
                    now,
                )
            )

            Snackbar.make(categoryView, "Category $categoryId Pressed!", Snackbar.LENGTH_SHORT).show()
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

        // add session button functionality
        findViewById<Button>(R.id.addSession).setOnClickListener {
            val rating = findViewById<EditText>(R.id.rating).text.toString()
            val comment = findViewById<EditText>(R.id.comment).text.toString()

            if (rating.isEmpty()) {
                Snackbar.make(it, "Please fill out all Session fields!", Snackbar.LENGTH_SHORT).show()
            } else {
                // finish up the final section
                var lastSection = sectionBuffer.last()

                // store the duration of the now ending section
                lastSection.duration = (
                    Date().time / 1000 -
                    lastSection.timestamp
                ).toInt()

                // TODO: Check id comment is empty -> insert null
                // id=0 means not assigned, autoGenerate=true will do it for us
                val newSession = PracticeSession(
                    0,
                    0,
                    rating.toInt(),
                    comment ,
                    1
                )

                lifecycleScope.launch {
                    // create a new session row and save its id
                    val sessionId = dao?.insertSession(newSession)

                    // add the new sessionId to every section in the section buffer
                    for(section in sectionBuffer) {
                        section.practice_session_id = sessionId?.toInt()
                        // and insert them into the database
                        dao?.insertSection(section)
                    }

                    // show the new session
                    fillSessionListView()

                    // reset section buffer and session status
                    sectionBuffer.clear()
                    fillSectionListView(sectionBuffer)
                    sessionActive = false
                }
            }
        }

        // show current list of sessions
        fillSessionListView()

        // calling practiceTimer will start a handler, which executes the code in the post() method once per second
        fun practiceTimer() {

            // get the text views.
            val timeView = findViewById<TextView>(R.id.practiceTimer)
            val totalTimeView = findViewById<TextView>(R.id.totalTime)

            // creates a new Handler
            Handler(Looper.getMainLooper()).also {

                // the post() method executes immediately
                it.post(object : Runnable {
                    override fun run() {
                        if (sessionActive && sectionBuffer.isNotEmpty()) {
                            // load the current section from the sectionBuffer
                            var firstSection = sectionBuffer.first()
                            var currentSection = sectionBuffer.last()

                            val now = Date().time / 1000

                            // store the duration of the now ending section
                            val currentDuration = (now - currentSection.timestamp).toInt()

                            // set the text view text to the formatted time
                            timeView.text = "%02d:%02d:%02d".format(
                                currentDuration / 3600,
                                currentDuration % 3600 / 60,
                                currentDuration % 60
                            )

                            // do the same for the total time
                            val totalDuration = (now - firstSection.timestamp).toInt()

                            totalTimeView.text = "%02d:%02d:%02d".format(
                                totalDuration / 3600,
                                totalDuration % 3600 / 60,
                                totalDuration % 60
                            )

                        } else {
                            totalTimeView.text = "00:00:00"
                            timeView.text = "00:00:00"
                        }

                        // post the code again with a delay of 1 second
                        it.postDelayed(this, 1000)
                    }
                })
            }
        }
        practiceTimer()
    }

    private fun fillSectionListView(sections: ArrayList<PracticeSection>) {
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
            var sessionsWithSections: List<SessionWithSections>? = dao?.getSessionsWithSections()
            if(sessionsWithSections != null) {
                var listItems = ArrayList<String>()
                for ((session, sections) in sessionsWithSections) {
                    var totalDuration: Int = 0
                    for (section in sections) {
                        totalDuration += section.duration!!
                    }
                    listItems.add("d: " + sections.first().timestamp +
                            " | dur: " + totalDuration +
                            " | brk: " + session.break_duration +
                            " | r: " + session.rating +
                            " | c: " + session.comment
                    )
                }
                val sessionsList = findViewById<ListView>(R.id.currentSessions)
                var adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, listItems)
                sessionsList.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }
}

class CategoryAdapter(
    private val dataSet: ArrayList<Category>,
    private val callback: (input: View) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: Button = view.findViewById(R.id.button)

        init {
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

