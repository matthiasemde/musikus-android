/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger 
 */

package de.practicetime.practicetime.ui.sessionlist

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.SectionWithLibraryItem
import de.practicetime.practicetime.database.SessionWithSectionsWithLibraryItems
import de.practicetime.practicetime.utils.SCALE_FACTOR_FOR_SMALL_TEXT
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY
import de.practicetime.practicetime.utils.getDurationString
import java.text.SimpleDateFormat
import java.util.*

class SessionSummaryAdapter(
    private val context: Context,
    var isExpanded: Boolean,
    private val sessionsWithSectionsWithLibraryItems: List<SessionWithSectionsWithLibraryItems>,
    private val selectedSessions: List<Pair<Int, SessionSummaryAdapter>> = listOf(),
    private val shortClickHandler: (
        layoutPosition: Int,
        adapter: SessionSummaryAdapter
    ) -> Unit = {_, _ ->},
    private val longClickHandler: (
        layoutPosition: Int,
        adapter: SessionSummaryAdapter
    ) -> Boolean = { _, _ -> false },
) : RecyclerView.Adapter<SessionSummaryAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_HEADER = 2
    }

    private val onHeaderClickListener = View.OnClickListener {
        isExpanded = !isExpanded
        if(isExpanded)
            notifyItemRangeInserted(1, sessionsWithSectionsWithLibraryItems.size)
        else
            notifyItemRangeRemoved(1, sessionsWithSectionsWithLibraryItems.size)
    }

    override fun getItemViewType(position: Int) =
        if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    override fun getItemCount() : Int {
        return if (isExpanded) sessionsWithSectionsWithLibraryItems.size + 1 else 1
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> ViewHolder.HeaderViewHolder(
                inflater.inflate(
                    R.layout.listitem_session_list_header,
                    viewGroup,
                    false
                )
            )
            else -> ViewHolder.ItemViewHolder(
                inflater.inflate(
                    R.layout.listitem_session_list_summary,
                    viewGroup,
                    false
                ),
                context,
                sessionsWithSectionsWithLibraryItems,
                selectedSessions,
                shortClickHandler,
                longClickHandler,
            )
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is ViewHolder.ItemViewHolder -> viewHolder.bind(
                dataIndex = position - 1,
            )
            is ViewHolder.HeaderViewHolder -> viewHolder.bind(
                sessionsWithSectionsWithLibraryItems.first().sections.first().section.timestamp,
                isExpanded,
                onHeaderClickListener,
            )
        }
    }

    // the sealed view holder class contains a class for both the list items and headers
    sealed class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        class ItemViewHolder(
            view: View,
            private val context: Context,
            private val sessionsWithSectionsWithLibraryItems: List<SessionWithSectionsWithLibraryItems>,
            private val selectedSessions: List<Pair<Int, SessionSummaryAdapter>>,
            private val shortClickHandler: (
                layoutPosition: Int,
                adapter: SessionSummaryAdapter,
            ) -> Unit,
            private val longClickHandler: (
                layoutPosition: Int,
                adapter: SessionSummaryAdapter,
            ) -> Boolean,
            private val isInAdapter: Boolean = true
        ) : ViewHolder(view) {

//            private val summaryDayLayout: LinearLayout = view.findViewById(R.id.summaryDayLayout)
//            private val summaryDate: TextView = view.findViewById(R.id.summaryDate)
//            private val summaryDayDuration: TextView = view.findViewById(R.id.summaryDayDuration)
//
//            private val summaryCard = view.findViewById(R.id.summaryCardWrapper) as InterceptTouchCardView

            private val summaryTimeView: TextView = view.findViewById(R.id.summaryTime)
            private val breakDurationView: TextView = view.findViewById(R.id.breakDuration)
            private val practiceDurationView: TextView = view.findViewById(R.id.practiceDuration)
            private val sectionList: RecyclerView = view.findViewById(R.id.sectionList)
            private val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
            private val commentField: TextView = view.findViewById(R.id.commentField)
            private val commentSection: View = view.findViewById(R.id.commentSection)

            private val sectionsWithLibraryItemsList = ArrayList<SectionWithLibraryItem>()

            // define the time and date format
            private val timeFormat: SimpleDateFormat = SimpleDateFormat("H:mm", Locale.getDefault())
            private val dateFormat: SimpleDateFormat = SimpleDateFormat("E dd.MM.yyyy", Locale.getDefault())

            private val defaultCardElevation = 11F // default value

            init {
                // define the layout and adapter for the section list
                val sectionAdapter = SectionAdapter(sectionsWithLibraryItemsList, context)
                val layoutManager = LinearLayoutManager(context)
                sectionList.layoutManager = layoutManager
                sectionList.adapter = sectionAdapter
            }

            fun bind(dataIndex: Int) {
                // get the session at given position
                val (session, sectionsWithLibraryItems) = sessionsWithSectionsWithLibraryItems[dataIndex]

//                summaryCard.apply {
//                    if (selectedSessions.map { t -> t.first }.contains(bindingAdapterPosition)) {
//                        isSelected = true // set selected so that background changes
//                        // remove Card Elevation because in Light theme it would look ugly
//                        cardElevation = 0f
//                    } else {
//                        isSelected = false // set selected so that background changes
//                        // restore card Elevation
//                        cardElevation = defaultCardElevation
//                    }
//                }
//
//                // set up short and long click handler for selecting sessions
//                summaryCard.setOnClickListener {
//                    if (isInAdapter) {
//                        shortClickHandler(
//                            bindingAdapterPosition,
//                            bindingAdapter as SessionSummaryAdapter
//                        )
//                    }
//                }
//                summaryCard.setOnLongClickListener {
//                    return@setOnLongClickListener if (isInAdapter) {
//                        // tell the event handler we consumed the event
//                        longClickHandler(
//                            bindingAdapterPosition,
//                            bindingAdapter as SessionSummaryAdapter
//                        )
//                    } else false
//                }

                val currentSessionDate = Calendar.getInstance().apply {
                    timeInMillis = sectionsWithLibraryItems.first().section.timestamp * 1000L
                }

                // detect, if this session is either the last session of a day or the whole month
                val lastSessionOfTheDay = if (dataIndex == 0) true else {
                    val nextSessionDate = Calendar.getInstance().apply {
                        timeInMillis = sessionsWithSectionsWithLibraryItems[dataIndex - 1]
                            .sections.first().section.timestamp * 1000L
                    }
                    currentSessionDate.get(Calendar.DAY_OF_YEAR) !=
                            nextSessionDate.get(Calendar.DAY_OF_YEAR)
                }

                // if so, calculate the total time practiced that day and display it
                if(lastSessionOfTheDay) {
                    var totalPracticeDuration = 0
                    for (i in dataIndex until sessionsWithSectionsWithLibraryItems.size) {
                        val (_, pastSectionsWithLibraryItems) = sessionsWithSectionsWithLibraryItems[i]
                        val date = Calendar.getInstance().apply {
                            timeInMillis = pastSectionsWithLibraryItems.first().section.timestamp * 1000L
                        }
                        if(currentSessionDate.get(Calendar.DAY_OF_YEAR) !=
                            date.get(Calendar.DAY_OF_YEAR)) {
                            break
                        } else {
                            pastSectionsWithLibraryItems.forEach { (section, _) ->
                                totalPracticeDuration += section.duration ?: 0
                            }
                        }
                    }

                    val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                    val yesterday = Calendar.getInstance().let {
                        it.add(Calendar.DAY_OF_YEAR, -1)
                        it.get(Calendar.DAY_OF_YEAR)
                    }

//                    summaryDayLayout.visibility = View.VISIBLE
//                    summaryDate.text = when(currentSessionDate.get(Calendar.DAY_OF_YEAR)) {
//                        today -> context.getString(R.string.today)
//                        yesterday -> context.getString(R.string.yesterday)
//                        else -> dateFormat.format(currentSessionDate.timeInMillis)
//                    }
//                    summaryDayDuration.text = getDurationString(totalPracticeDuration, TIME_FORMAT_HUMAN_PRETTY)

                } else {
//                    summaryDayLayout.visibility = View.GONE
                }

                // compute the total practice time
                var practiceDuration = 0
                sectionsWithLibraryItems.forEach { (section, _) ->
                    practiceDuration += section.duration ?: 0
                }

                val breakDuration = session.breakDuration

                // read the start duration from the first section and bring it to milliseconds
                val startTimestamp = sectionsWithLibraryItems.first().section.timestamp * 1000L

                // set the time field accordingly
                summaryTimeView.text = timeFormat.format(Date(startTimestamp))

                // show the practice duration in the practice duration field
                practiceDurationView.text = getDurationString(practiceDuration, TIME_FORMAT_HUMAN_PRETTY)

                // set the break time text equal to the sessions break duration
                breakDurationView.text = getDurationString(breakDuration, TIME_FORMAT_HUMAN_PRETTY, SCALE_FACTOR_FOR_SMALL_TEXT)

                // set the sections and update the section adapter about the change
                sectionsWithLibraryItemsList.clear()
                sectionsWithLibraryItemsList.addAll(sectionsWithLibraryItems)
                sectionList.adapter?.notifyItemRangeInserted(0,sectionsWithLibraryItems.size)

                //set the rating bar to the correct star rating
                ratingBar.rating = session.rating.toFloat()

                if (session.comment.isNullOrEmpty()) {
                    commentSection.visibility = View.GONE
                } else {
                    //set content of the comment field
                    commentSection.visibility = View.VISIBLE
                    commentField.text = session.comment
                }
            }

            private inner class SectionAdapter(
                private val sectionsWithLibraryItems: ArrayList<SectionWithLibraryItem>,
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
                        .inflate(R.layout.listitem_session_list_summary_section, viewGroup, false)

                    return ViewHolder(view)
                }

                // Replace the contents of a view (invoked by the layout manager)
                override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
                    // Get element from your dataset at this position
                    val (section, libraryItem) = sectionsWithLibraryItems[position]

                    // set the color to the libraryItem color
                    val libraryItemColors =  context.resources.getIntArray(R.array.library_item_colors)
                    viewHolder.sectionColor.backgroundTintList = ColorStateList.valueOf(
                        libraryItemColors[libraryItem.colorIndex]
                    )


                    val sectionDuration = section.duration ?: 0

                    // contents of the view with that element
                    viewHolder.sectionName.text = libraryItem.name
                    viewHolder.sectionDuration.text = getDurationString(sectionDuration, TIME_FORMAT_HUMAN_PRETTY, SCALE_FACTOR_FOR_SMALL_TEXT)
                }

                // Return the size of your dataset (invoked by the layout manager)
                override fun getItemCount() = sectionsWithLibraryItems.size
            }
        }

        class HeaderViewHolder(view: View) : ViewHolder(view) {
            private val sessionHeaderMonth: TextView = view.findViewById(R.id.sessionHeaderMonth)
            // bind a new section header with the timestamp of the first session (in seconds)
            fun bind(
                timestamp: Long,
                expanded: Boolean,
                onClickListener: View.OnClickListener,
            ) {
                // TODO move to TimeUtils
                SimpleDateFormat("MMMM").format(Date(timestamp * 1000L)).also {
                    sessionHeaderMonth.text = it
                }
                sessionHeaderMonth.setOnClickListener(onClickListener)
            }
        }
    }
}

class InterceptTouchCardView(context: Context, attrs: AttributeSet) : CardView(context, attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }
}

