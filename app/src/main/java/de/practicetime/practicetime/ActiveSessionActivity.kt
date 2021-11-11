package de.practicetime.practicetime

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null
// the sectionBuffer will keep track of all the section in the current session
private val sectionBuffer = ArrayList<PracticeSection>()
private var sessionActive = false   // keep track of whether a session is active
private var activeCategories: List<Category>? = listOf<Category>()

class ActiveSessionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_session)

        openDatabase()

        findViewById<FloatingActionButton>(R.id.fab_stop).hide()

        // start the practice timer Runnable
        practiceTimer()

        // initialize adapter and recyclerView for showing category buttons from database
        var categories = ArrayList<Category>()
        var categoryAdapter = CategoryAdapter(categories, ::categoryPressed)

        val categoryList = findViewById<RecyclerView>(R.id.categoryList)
        categoryList.layoutManager = GridLayoutManager(this, 2)
        categoryList.adapter = categoryAdapter

        lifecycleScope.launch {
            activeCategories = dao?.getActiveCategories()
            if (activeCategories != null) {
                categories.addAll(activeCategories!!)
            }
            // notifyDataSetChanged necessary here since all items might have changed
            categoryAdapter.notifyDataSetChanged()
        }

        initEndSessionDialog()
    }

    private fun initEndSessionDialog() {
        // instantiate the builder for the alert dialog
        val endSessionDialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater;
        val dialogView = inflater.inflate(R.layout.dialog_view_end_session, null)

        val dialogRatingBar = dialogView.findViewById<RatingBar>(R.id.dialogRatingBar)
        val dialogComment = dialogView.findViewById<EditText>(R.id.dialogComment)

        endSessionDialogBuilder.apply {
            setView(dialogView)
            setPositiveButton(R.string.endSessionAlertOk) { dialog, _ ->
                val rating = dialogRatingBar.rating.toInt()
                endSession(rating, dialogComment.text.toString())
            }
            setNegativeButton(R.string.endSessionAlertCancel) { dialog, _ ->
                dialog.cancel()
            }
        }
        // create the end session dialog
        val endSessionDialog: AlertDialog = endSessionDialogBuilder.create()
        // end session button functionality
        findViewById<FloatingActionButton>(R.id.fab_stop).setOnClickListener {
            // show the end session dialog
            endSessionDialog.show()
            endSessionDialog.also {
                val positiveButton = it.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.isEnabled = false
                dialogRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
                    positiveButton.isEnabled = rating.toInt() > 0
                }
            }
        }
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            this,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    // the routine for handling presses to category buttons
    private fun categoryPressed(categoryView: View) {
        // get the category id from the view tag and calculate current timestamp
        val categoryId = categoryView.tag as Int
        val now = Date().time / 1000L

        findViewById<FloatingActionButton>(R.id.fab_stop).show()
        val sessBtn = categoryView as Button
        findViewById<TextView>(R.id.activeSectionName).text = sessBtn.text.toString()

        // change background color of button
        // if there is no active session set sessionActive to true
        if (!sessionActive) {
            sessionActive = true
        } else {    // Session should be switched
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
        Log.d("TAG", "Category $categoryId Pressed!")
    }

    private fun endSession(rating: Int, comment: String?) {
        // finish up the final section
        var lastSection = sectionBuffer.last()

        // store the duration of the now ending section
        lastSection.duration = (
                Date().time / 1000 -
                        lastSection.timestamp
                ).toInt()

        // TODO: Check if comment is empty -> insert null
        // id=0 means not assigned, autoGenerate=true will do it for us
        val newSession = PracticeSession(
            0,
            0,
            rating.toInt(),
            comment,
            1
        )

        lifecycleScope.launch {
            // create a new session row and save its id
            val sessionId = dao?.insertSession(newSession)

            // add the new sessionId to every section in the section buffer
            for (section in sectionBuffer) {
                section.practice_session_id = sessionId?.toInt()
                // and insert them into the database
                dao?.insertSection(section)
            }

            // reset section buffer and session status
            sectionBuffer.clear()
            sessionActive = false
        }

        startActivity(Intent(this, MainActivity::class.java))
    }

    // calling practiceTimer will start a handler, which executes the code in the post() method once per second
    private fun practiceTimer() {
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

                        // store the duration of the currently running section
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

    private fun fillSectionListView(sections: ArrayList<PracticeSection>) {
        // show all sections in listview
        var listItems = ArrayList<String>()
        sections.forEach {
            listItems.add(activeCategories?.get(it.category_id - 1)?.name + "   |   duration: " + it.duration)
        }
        val sectionsList = findViewById<ListView>(R.id.currentSections)
        var adapter =
            ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems)
        sectionsList.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /**
     *  Adapter for the Category selection button grid.
     */

    private inner class CategoryAdapter(
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
                .inflate(R.layout.view_category_item, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            // Get element from your dataset at this position
            val category = dataSet[position]

            // store the id of the category on the button
            viewHolder.button.tag = category.id

            // archived categories should not be displayed
            if (category.archived) {
                viewHolder.button.visibility = View.GONE
            }

            // contents of the view with that element
            viewHolder.button.text = category.name
            viewHolder.button.setBackgroundColor(category.color)
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size
    }
}