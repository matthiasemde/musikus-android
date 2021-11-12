package de.practicetime.practicetime

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.entities.SectionWithCategory
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SessionSummaryAdapter(
    private val context: Context,
    private val sessionsWithSectionsWithCategories: ArrayList<SessionWithSectionsWithCategories>,
) : RecyclerView.Adapter<SessionSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val summaryDayLayout: LinearLayout = view.findViewById(R.id.summaryDayLayout)
        val summaryDate: TextView = view.findViewById(R.id.summaryDate)
        val summaryDayDuration: TextView = view.findViewById(R.id.summaryDayDuration)

        val summaryTime: TextView = view.findViewById(R.id.summaryTime)
        val breakDuration: TextView = view.findViewById(R.id.breakDuration)
        val practiceDuration: TextView = view.findViewById(R.id.practiceDuration)
        val sectionList: RecyclerView = view.findViewById(R.id.sectionList)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val commentField: TextView = view.findViewById(R.id.commentField)

        val sectionsWithCategories = ArrayList<SectionWithCategory>()

        //define the time and date format
        val timeFormat: SimpleDateFormat = SimpleDateFormat("HH:mm")
        val dateFormat: SimpleDateFormat = SimpleDateFormat("E dd.MM.yyyy")

        init {
            // define the layout and adapter for the section list
            val sectionAdapter = SectionAdapter(sectionsWithCategories)
            val layoutManager = LinearLayoutManager(context)
            sectionList.layoutManager = layoutManager
            sectionList.adapter = sectionAdapter
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.view_session_summary, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position
        val (session, sectionsWithCategories) = sessionsWithSectionsWithCategories[position]

        var currentSessionDate: Calendar

        Calendar.getInstance().also { newDate ->
            newDate.timeInMillis = sectionsWithCategories.first().section.timestamp * 1000L
            currentSessionDate = newDate
        }

        // detect, if this session is either the latest session or the last session of a day
        var lastSessionOfTheDay = position == this.itemCount-1

        if(position + 1 < this.itemCount) {
            var nextSessionDate: Calendar

            Calendar.getInstance().also { newDate ->
                newDate.timeInMillis = sessionsWithSectionsWithCategories[position + 1]
                    .sections.first().section.timestamp * 1000L
                nextSessionDate = newDate
            }
            if(currentSessionDate.get(Calendar.DAY_OF_MONTH) !=
                nextSessionDate.get(Calendar.DAY_OF_MONTH)) {
                lastSessionOfTheDay = true
            }
        }

        // if so, calculate the total time practiced that day and display it
        if(lastSessionOfTheDay) {
            var totalPracticeDuration = 0
            for (i in position downTo 0) {
                val (_, sectionsWithCategories) = sessionsWithSectionsWithCategories[i]
                var date: Calendar
                Calendar.getInstance().also { newDate ->
                    newDate.timeInMillis = sectionsWithCategories.first().section.timestamp * 1000L
                    date = newDate
                }
                if(currentSessionDate.get(Calendar.DAY_OF_MONTH) !=
                    date.get(Calendar.DAY_OF_MONTH)) {
                    break;
                } else {
                    sectionsWithCategories.forEach { (section, _) ->
                        totalPracticeDuration += section.duration ?: 0
                    }
                }
            }

            viewHolder.summaryDayLayout.visibility = View.VISIBLE
            viewHolder.summaryDate.text = viewHolder.dateFormat.format(currentSessionDate.timeInMillis)
            viewHolder.summaryDayDuration.text = "%dh %dmin".format(
//      Todo change back eventually      breakDuration / 3600,
                totalPracticeDuration % 3600 / 60,
                totalPracticeDuration % 60
            )
        } else {
            viewHolder.summaryDayLayout.visibility = View.GONE
        }

        // compute the total practice time
        var practiceDuration = 0
        sectionsWithCategories.forEach { (section, _) ->
            practiceDuration += section.duration ?: 0
        }

        val breakDuration = session.break_duration

        // read the start duration from the first section and bring it to milliseconds
        val startTimestamp = sectionsWithCategories.first().section.timestamp * 1000L

        // set the time field accordingly
        (viewHolder.timeFormat.format(Date(startTimestamp)) +
            " - " +
            viewHolder.timeFormat.format(
                Date(startTimestamp + (breakDuration + practiceDuration) * 1000L)
            )).also {
            viewHolder.summaryTime.text = it
        }

        // set the break time text equal to the sessions break duration
        viewHolder.breakDuration.text = "%dh %dmin".format(
//      Todo change back eventually      breakDuration / 3600,
            breakDuration % 3600 / 60,
            breakDuration % 60
        )

        // show the practice duration in the practice duration field
        viewHolder.practiceDuration.text = "%dh %dmin".format(
//      Todo change back eventually      practiceDuration / 3600,
            practiceDuration % 3600 / 60,
            practiceDuration % 60
        )

        // set the sections and update the section adapter about the change
        viewHolder.sectionsWithCategories.clear()
        viewHolder.sectionsWithCategories.addAll(sectionsWithCategories)
        viewHolder.sectionList.adapter?.notifyDataSetChanged()

        //set the rating bar to the correct star rating
        viewHolder.ratingBar.rating = session.rating.toFloat()

        //set content of the comment field
        viewHolder.commentField.text = session.comment
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = sessionsWithSectionsWithCategories.size

    private inner class SectionAdapter(
        private val sectionsWithCategories: ArrayList<SectionWithCategory>,
    ) : RecyclerView.Adapter<SectionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sectionColor: View = view.findViewById(R.id.sectionColor)
            val sectionName: TextView = view.findViewById(R.id.sectionName)
            val sectionDuration: TextView = view.findViewById(R.id.sectionDuration)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.view_session_summary_section, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            // Get element from your dataset at this position
            val (section, category) = sectionsWithCategories[position]

            // set the color to the category color
            viewHolder.sectionColor.setBackgroundColor(category.color)

            val sectionDuration = section.duration ?: 0

            // contents of the view with that element
            viewHolder.sectionName.text = category.name
            viewHolder.sectionDuration.text = "%dh %dmin".format(
//              Todo change back eventually  sectionDuration / 3600,
                sectionDuration % 3600 / 60,
                sectionDuration % 60
            )
        }


        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = sectionsWithCategories.size
    }
}

