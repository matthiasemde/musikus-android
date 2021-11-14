package de.practicetime.practicetime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null
// the sectionBuffer will keep track of all the section in the current session
private var sectionBuffer = ArrayList<Pair<PracticeSection, Int>>()
private var sessionActive = false   // keep track of whether a session is active
private var paused = false            // flag if session is currently paused
private var pauseDuration = 0         // pause duration, ONLY for displaying on the fab, section pause duration is safed in sectionBuffer!
private var activeCategories: List<Category>? = listOf<Category>()
private lateinit var sectionsAdapter: ArrayAdapter<String>
private var listItems = ArrayList<String>()

class ActiveSessionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_session)

        openDatabase()

        // start the practice timer Runnable
        practiceTimer()
        // initialize adapter and recyclerView for showing category buttons from database
        initCategoryList()

        initEndSessionDialog()

        val btnPause = findViewById<ImageButton>(R.id.bottom_pause)
        btnPause.setOnClickListener {
            if (paused) {
                // resume the Session with the last category
                resumeSession(sectionBuffer.last().first.category_id)
            } else {
                pauseSession()
            }
        }
    }

    private fun initCategoryList() {
        val categories = ArrayList<Category>()
        val categoryAdapter = CategoryAdapter(categories, ::categoryPressed)

        val categoryList = findViewById<RecyclerView>(R.id.categoryList)
        categoryList.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false)
        categoryList.adapter = categoryAdapter

        lifecycleScope.launch {
            activeCategories = dao?.getActiveCategories()
            if (activeCategories != null) {
                categories.addAll(activeCategories!!)
            }
            // notifyDataSetChanged necessary here since all items might have changed
            categoryAdapter.notifyDataSetChanged()
        }
    }

    // the routine for handling presses to category buttons
    private fun categoryPressed(categoryView: View) {
        // get the category id from the view tag and calculate current timestamp
        val categoryId = categoryView.tag as Int

        if (!sessionActive) {   // session starts now
            sessionActive = true
            fillSectionListView(true)
            findViewById<ImageButton>(R.id.bottom_pause).visibility = View.VISIBLE
            findViewById<ImageButton>(R.id.bottom_stop).visibility = View.VISIBLE
        } else {
            endSection()
        }
        // start a new section for the chosen category
        startNewSection(categoryId)
    }

    private fun startNewSection(categoryId: Int) {
        val now = Date().time / 1000L
        sectionBuffer.add(
            Pair(
                PracticeSection(
                    0,  // 0 means auto-increment
                    null,
                    categoryId,
                    null,
                    now,
                ),
                0
            )
        )
    }

    private fun endSection() {
        // save duration of last section
        sectionBuffer.last().first.apply {
            duration = getDuration(this)
        }
        fillSectionListView()
    }

    private fun pauseSession() {
        paused = true
        // swap pause icon with play icon
        findViewById<ImageButton>(R.id.bottom_pause).apply {
            setImageResource(R.drawable.ic_play)
        }
        // show the fab
        findViewById<ExtendedFloatingActionButton>(R.id.fab_info_popup).apply {
            show()
        }
    }

    private fun resumeSession(categoryId: Int) {
        paused = false
        pauseDuration = 0
        // swap pause icon with play icon
        findViewById<ImageButton>(R.id.bottom_pause).apply {
            setImageResource(R.drawable.ic_pause)
        }
        // show the fab
        findViewById<ExtendedFloatingActionButton>(R.id.fab_info_popup).apply {
            hide()
        }
    }

    private fun endSession(rating: Int, comment: String?) {
        // finish the final section
        endSection()

        // get total break duration
        var totalBreakDuration = 0
        sectionBuffer.forEach {
            totalBreakDuration += it.second
        }

        // TODO: Check if comment is empty -> insert null
        val newSession = PracticeSession(
            0,      // id=0 means not assigned, autoGenerate=true will do it for us
            totalBreakDuration,
            rating.toInt(),
            comment,
            1
        )

        lifecycleScope.launch {
            // create a new session row and save its id
            val sessionId = dao?.insertSession(newSession)

            // add the new sessionId to every section in the section buffer
            for (section in sectionBuffer) {
                section.first.practice_session_id = sessionId?.toInt()
                // update section durations to exclude break durations
                section.first.duration = section.first.duration?.minus(section.second)
                // and insert them into the database
                dao?.insertSection(section.first)
            }

            // reset section buffer and session status
            sectionBuffer.clear()
            sessionActive = false
        }

        // terminate and go back to MainActivity
        finish()
    }

    private fun initEndSessionDialog() {
        // instantiate the builder for the alert dialog
        val endSessionDialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater;
        val dialogView = inflater.inflate(R.layout.dialog_view_end_session, null)

        val dialogRatingBar = dialogView.findViewById<RatingBar>(R.id.dialogRatingBar)
        val dialogComment = dialogView.findViewById<EditText>(R.id.dialogComment)

        // Dialog Setup
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
        val endSessionDialog: AlertDialog = endSessionDialogBuilder.create()

        // stop session button functionality
        findViewById<ImageButton>(R.id.bottom_stop).setOnClickListener {
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


    /**
     * calculates total Duration (including pauses) of a section
     */
    private fun getDuration(section: PracticeSection): Int {
        val now = Date().time / 1000L
        return (now - section.timestamp).toInt()
    }

    // calling practiceTimer will start a handler, which executes the code in the post() method once per second
    private fun practiceTimer() {
        // get the text views.
        val practiceTimeView = findViewById<TextView>(R.id.practiceTimer)
        val fabInfoPause = findViewById<ExtendedFloatingActionButton>(R.id.fab_info_popup)

        // creates a new Handler
        Handler(Looper.getMainLooper()).also {

            // the post() method executes immediately
            it.post(object : Runnable {
                override fun run() {
                    if (sessionActive && sectionBuffer.isNotEmpty()) {
                        // load the current section from the sectionBuffer
                        val firstSection = sectionBuffer.first()
                        val currentSection = sectionBuffer.last()

                        val now = Date().time / 1000

                        if (paused) {
                            // increment pause time. Since Pairs<> are not mutable (but ArrayList is)
                            // we have to copy the element and replace the whole element in the ArrayList
                            sectionBuffer[sectionBuffer.lastIndex] =
                                sectionBuffer.last().copy(second = sectionBuffer.last().second + 1)

                            pauseDuration++
                            // display pause duration on the fab, but only time after pause was activated
                            fabInfoPause.text = "Pause: %02d:%02d:%02d".format(
                                pauseDuration / 3600,
                                pauseDuration % 3600 / 60,
                                pauseDuration % 60
                            )
                        }

                        // calculate total time of all sections (including pauses)
                        var totalPracticeDuration = (now - firstSection.first.timestamp).toInt()
                        // subtract all pause durations
                        sectionBuffer.forEach { section ->
                            totalPracticeDuration -= section.second
                        }
                        practiceTimeView.text = "%02d:%02d:%02d".format(
                            totalPracticeDuration / 3600,
                            totalPracticeDuration % 3600 / 60,
                            totalPracticeDuration % 60
                        )


                        fillSectionListView()
                    } else {
                        practiceTimeView.text = "00:00:00"
                    }

                    // post the code again with a delay of 1 second
                    it.postDelayed(this, 1000)
                }
            })
        }
    }

    private fun fillSectionListView(init: Boolean = false) {
        // show all sections in listview
        listItems.clear()
        for (n in sectionBuffer.size - 1 downTo 0) {
            var duration = sectionBuffer[n].first.duration?.minus(sectionBuffer[n].second)
            if (duration == null) {
                duration = getDuration(sectionBuffer[n].first).minus(sectionBuffer[n].second)
            }
            listItems.add(
                    "${activeCategories?.get(sectionBuffer[n].first.category_id - 1)?.name} " +
                    "\t\t\t\t\t${duration}s"
            )
        }
        if (init) {
            val sectionsList = findViewById<ListView>(R.id.currentSections)
            sectionsAdapter =
              ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems)
            sectionsList.adapter = sectionsAdapter
        }
        sectionsAdapter.notifyDataSetChanged()
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

            // TODO set right margin for last 3 elements programmatically
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size
    }
}