package de.practicetime.practicetime

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.entities.SectionWithCategory
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SessionSummaryAdapter(
    private val context: Context,
    private var isExpanded: Boolean,
    private val sessionsWithSectionsWithCategories: List<SessionWithSectionsWithCategories>,
) : RecyclerView.Adapter<SessionSummaryAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_HEADER = 2
    }

    private val onHeaderClickListener = View.OnClickListener {
        isExpanded = !isExpanded
        if(isExpanded)
            notifyItemRangeInserted(1, sessionsWithSectionsWithCategories.size)
        else
            notifyItemRangeRemoved(1, sessionsWithSectionsWithCategories.size)

        // notify Adapter that button text has changed
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun getItemCount(): Int =
        if (isExpanded)
            sessionsWithSectionsWithCategories.size + 1
        else
            1

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> ViewHolder.HeaderViewHolder(
                inflater.inflate(
                    R.layout.view_session_summary_header,
                    viewGroup,
                    false
                )
            )
            else -> ViewHolder.ItemViewHolder(
                inflater.inflate(
                    R.layout.view_session_summary,
                    viewGroup,
                    false
                ),
                context
            )
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is ViewHolder.ItemViewHolder -> viewHolder.bind(
                sessionsWithSectionsWithCategories,
                sessionsWithSectionsWithCategories.size - position,
            )
            is ViewHolder.HeaderViewHolder -> viewHolder.bind(
                sessionsWithSectionsWithCategories.first().sections.first().section.timestamp,
                isExpanded,
                onHeaderClickListener,
            )
        }
    }

    // the sealed view holder class contains a class for both the list items and headers
    sealed class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        class ItemViewHolder(view: View, context: Context) : ViewHolder(view) {
            private val summaryDayLayout: LinearLayout = view.findViewById(R.id.summaryDayLayout)
            private val summaryDate: TextView = view.findViewById(R.id.summaryDate)
            private val summaryDayDuration: TextView = view.findViewById(R.id.summaryDayDuration)
//            private val divider : View = view.findViewById(R.id.divider)

            private val summaryTimeView: TextView = view.findViewById(R.id.summaryTime)
            private val breakDurationView: TextView = view.findViewById(R.id.breakDuration)
            private val practiceDurationView: TextView = view.findViewById(R.id.practiceDuration)
            private val sectionList: RecyclerView = view.findViewById(R.id.sectionList)
            private val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
            private val commentField: TextView = view.findViewById(R.id.commentField)
            private val commentLabel: TextView = view.findViewById(R.id.commentLabel)
            private val commentDivider: View = view.findViewById(R.id.commentDivider)

            private val sectionsWithCategoriesList = ArrayList<SectionWithCategory>()

            // define the time and date format
            private val timeFormat: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            private val dateFormat: SimpleDateFormat = SimpleDateFormat("E dd.MM.yyyy", Locale.getDefault())

            init {
                // define the layout and adapter for the section list
                val sectionAdapter = SectionAdapter(sectionsWithCategoriesList, context)
                val layoutManager = LinearLayoutManager(context)
                sectionList.layoutManager = layoutManager
                sectionList.adapter = sectionAdapter
            }

            fun bind(
                sessionsWithSectionsWithCategories: List<SessionWithSectionsWithCategories>,
                position: Int,
            ) {
                // TODO please cleanup code and extract code blocks in separate functions,
                //  100 lines of code in one function is not readable!

                // get the session at given position
                val (session, sectionsWithCategories) = sessionsWithSectionsWithCategories[position]

                var currentSessionDate: Calendar

                Calendar.getInstance().also { newDate ->
                    newDate.timeInMillis = sectionsWithCategories.first().section.timestamp * 1000L
                    currentSessionDate = newDate
                }

                // detect, if this session is either the last session of a day or the whole month
                var lastSessionOfTheDay = position == sessionsWithSectionsWithCategories.size-1

                if(position + 1 < sessionsWithSectionsWithCategories.size) {
                    var nextSessionDate: Calendar

                    Calendar.getInstance().also { newDate ->
                        newDate.timeInMillis = sessionsWithSectionsWithCategories[position + 1]
                            .sections.first().section.timestamp * 1000L
                        nextSessionDate = newDate
                    }
                    if(currentSessionDate.get(Calendar.DAY_OF_YEAR) !=
                        nextSessionDate.get(Calendar.DAY_OF_YEAR)) {
                        lastSessionOfTheDay = true
                    }
                }

                // if so, calculate the total time practiced that day and display it
                if(lastSessionOfTheDay) {
                    var totalPracticeDuration = 0
                    for (i in position downTo 0) {
                        val (_, pastSectionsWithCategories) = sessionsWithSectionsWithCategories[i]
                        var date: Calendar
                        Calendar.getInstance().also { newDate ->
                            newDate.timeInMillis = pastSectionsWithCategories.first().section.timestamp * 1000L
                            date = newDate
                        }
                        if(currentSessionDate.get(Calendar.DAY_OF_YEAR) !=
                            date.get(Calendar.DAY_OF_YEAR)) {
                            break;
                        } else {
                            pastSectionsWithCategories.forEach { (section, _) ->
                                totalPracticeDuration += section.duration ?: 0
                            }
                        }
                    }

                    summaryDayLayout.visibility = View.VISIBLE
//                    divider.visibility = View.VISIBLE
                    summaryDate.text = dateFormat.format(currentSessionDate.timeInMillis)
                    summaryDayDuration.text = getTimeString(totalPracticeDuration)
                } else {
                    summaryDayLayout.visibility = View.GONE
//                    divider.visibility = View.GONE
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
                (timeFormat.format(Date(startTimestamp)) +
                        " - " +
                        timeFormat.format(
                            Date(startTimestamp + (breakDuration + practiceDuration) * 1000L)
                        )).also {
                    summaryTimeView.text = it
                }

                // show the practice duration in the practice duration field
                practiceDurationView.text = getTimeString(practiceDuration)

                // set the break time text equal to the sessions break duration
                breakDurationView.text = getTimeString(breakDuration)

                // set the sections and update the section adapter about the change
                sectionsWithCategoriesList.clear()
                sectionsWithCategoriesList.addAll(sectionsWithCategories)
                sectionList.adapter?.notifyDataSetChanged()

                //set the rating bar to the correct star rating
                ratingBar.rating = session.rating.toFloat()

                // TODO Bug: currently this check returns true although session.comment is not empty
                //  when RecyclerView recycles the views (when scrolling outside and then back)
                if (session.comment.isNullOrEmpty()) {
                    commentField.visibility = View.GONE
                    commentLabel.visibility = View.GONE
                    commentDivider.visibility = View.GONE
                } else {
                    //set content of the comment field
                    commentField.text = session.comment
                }
            }

            private fun getTimeString(durationMs: Int) : String {
                val hoursDur =  durationMs % 3600 / 60     // TODO change back eventually
                val minutesDur = durationMs % 60           // TODO change back eventually

                return if (hoursDur > 0) {
                    "%dh %dmin".format(hoursDur, minutesDur)
                } else {
                    "%dmin".format(minutesDur)
                }
            }

            private inner class SectionAdapter(
                private val sectionsWithCategories: ArrayList<SectionWithCategory>,
                private val context: Context
            ) : RecyclerView.Adapter<SectionAdapter.ViewHolder>() {

                inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                    val sectionColor: ImageView = view.findViewById(R.id.sectionColor)
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
                    val categoryColors =  context.resources.getIntArray(R.array.category_colors)
                    viewHolder.sectionColor.backgroundTintList = ColorStateList.valueOf(
                        categoryColors[category.colorIndex]
                    );


                    val sectionDuration = section.duration ?: 0

                    // contents of the view with that element
                    viewHolder.sectionName.text = category.name
                    viewHolder.sectionDuration.text = getTimeString(sectionDuration)
                }

                // Return the size of your dataset (invoked by the layout manager)
                override fun getItemCount() = sectionsWithCategories.size
            }
        }

        class HeaderViewHolder(private val view: View) : ViewHolder(view) {
            private val sessionHeaderMonth: TextView = view.findViewById(R.id.sessionHeaderMonth)
            private val sessionHeaderToggleButton: Button = view.findViewById(R.id.btn_headermonth_toggle)
            // bind a new section header with the timestamp of the first session (in seconds)
            fun bind(
                timestamp: Long,
                expanded: Boolean,
                onClickListener: View.OnClickListener,
            ) {
                SimpleDateFormat("MMMM").format(Date(timestamp * 1000L)).also {
                    sessionHeaderMonth.text = it
                }
                sessionHeaderToggleButton.setOnClickListener(onClickListener)
                if (expanded) {
                    sessionHeaderToggleButton.text = view.context.getString(R.string.hide_month);
                } else {
                    sessionHeaderToggleButton.text = view.context.getString(R.string.show_month);
                }
            }
        }
    }
}

