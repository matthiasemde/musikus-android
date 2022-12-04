/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.ui.sessionlist

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.SectionWithLibraryItem
import de.practicetime.practicetime.database.SessionWithSectionsWithLibraryItems
import de.practicetime.practicetime.utils.SCALE_FACTOR_FOR_SMALL_TEXT
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY
import de.practicetime.practicetime.utils.getDurationString
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RatingBar(
    rating: Int = 5,
    total: Int = 7,
    image: ImageVector,
    color: Color = Color(0xFFFFB300),
) {
    val size = 16.dp
    Row() {
        for(i in 1..rating) {
            Icon(
                modifier = Modifier.size(size),
                imageVector = image,
                tint = color,
                contentDescription = null
            )
        }
        for(i in rating + 1..total) {
            Icon(
                modifier = Modifier.size(size),
                imageVector = image,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null
            )
        }
    }
}


@Composable
fun SessionCard(
    sessionWithSectionsWithLibraryItems: SessionWithSectionsWithLibraryItems,
) {
    val (session, sectionsWithLibraryItems) = sessionWithSectionsWithLibraryItems

    // compute the total practice time
    var practiceDuration = 0
    sectionsWithLibraryItems.forEach { (section, _) ->
        practiceDuration += section.duration ?: 0
    }

    // define the time and date format
    val timeFormat = SimpleDateFormat("H:mm", Locale.getDefault())

    ElevatedCard(
//        modifier = Modifier.height(300.dp)
    ) {
        /** Card Header */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // read the start duration from the first section and bring it to milliseconds
            val startTimestamp = sectionsWithLibraryItems.first().section.timestamp * 1000L
            Text(
                text = timeFormat.format(Date(startTimestamp))
            )
            RatingBar(
                rating = session.rating,
                total = 5,
                image = Icons.Default.Star
            )
        }
        Divider()

        /** Main Card content */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(4f),
                    text = getDurationString(practiceDuration, TIME_FORMAT_HUMAN_PRETTY).toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(2f),
                    text = getDurationString(session.breakDuration, TIME_FORMAT_HUMAN_PRETTY, SCALE_FACTOR_FOR_SMALL_TEXT).toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Row {
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(4f),
                    text = stringResource(id = R.string.sessionSummaryPracticeTime),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(2f),
                    text = stringResource(id = R.string.sessionSummaryBreakTime),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            sectionsWithLibraryItems.forEach { sectionWithLibraryItem ->
                Row {
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .width(0.dp)
                            .weight(4f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = 8.dp)
                                .width(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Color(
                                        PracticeTime.getLibraryItemColors(LocalContext.current)[sectionWithLibraryItem.libraryItem.colorIndex]
                                    )
                                )
                        )
                        Text(
                            text = sectionWithLibraryItem.libraryItem.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        modifier = Modifier
                            .width(0.dp)
                            .weight(2f),
                        text = getDurationString(
                            sectionWithLibraryItem.section.duration ?: 0,
                            TIME_FORMAT_HUMAN_PRETTY,
                            SCALE_FACTOR_FOR_SMALL_TEXT
                        ).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        /** Optional Comment Field */
        session.comment?.let { comment ->
            if (comment.isBlank()) return@let
            Divider()
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.sessionSummaryComment),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}


@SuppressLint("ViewConstructor")
class SessionCard(
    context: Context,
    sessionWithSectionsWithLibraryItems: SessionWithSectionsWithLibraryItems
) : LinearLayout(context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.listitem_session_list_summary, null)

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

    init {
        val (session, sectionsWithLibraryItems) = sessionWithSectionsWithLibraryItems

        // define the layout and adapter for the section list
        val sectionAdapter = SectionAdapter(sectionsWithLibraryItemsList, context)
        val layoutManager = LinearLayoutManager(context)
        sectionList.layoutManager = layoutManager
        sectionList.adapter = sectionAdapter

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

        super.addView(view)

    }


    fun update() {

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
