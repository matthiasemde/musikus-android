/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.statistics

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.SessionWithSectionsWithLibraryItems
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.databinding.FragmentContainerStatisticsBinding
import de.practicetime.practicetime.shared.setCommonToolbar
import de.practicetime.practicetime.ui.goals.updateGoals
import de.practicetime.practicetime.utils.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.min

@Composable
fun StatisticsFragmentHolder() {
    AndroidViewBinding(FragmentContainerStatisticsBinding::inflate)
}

class StatisticsOverviewFragment : Fragment(R.layout.fragment_statistics_overview) {

    private lateinit var allSessions: List<SessionWithSectionsWithLibraryItems>
    private var totalPracticeTime: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            updateGoals(requireContext())    // update the goalInstances if they are outdated

            if (getAllSessions().isNotEmpty()) {
                view.findViewById<NestedScrollView>(R.id.statistics_overview_scrollview).visibility = View.VISIBLE

                val sessionDetailClickListener = View.OnClickListener {
                    val i = Intent(requireContext(), SessionStatsActivity::class.java)
                    requireActivity().startActivity(i)
                }
                view.findViewById<CardView>(R.id.stats_ov_cardview_last7days).setOnClickListener(sessionDetailClickListener)
                view.findViewById<ImageButton>(R.id.stats_ov_card_last7days_ib_more_details).setOnClickListener(sessionDetailClickListener)

                /** last 5 goals overview */
                if (PTDatabase.getInstance(requireContext()).goalInstanceDao.getWithDescription().isNotEmpty()) {
                    view.findViewById<CardView>(R.id.stats_ov_cardview_lastgoals).visibility = View.VISIBLE

                    val goalsDetailClickListener = View.OnClickListener {
                        val i = Intent(requireContext(), GoalStatsActivity::class.java)
                        requireActivity().startActivity(i)
                    }
                    view.findViewById<CardView>(R.id.stats_ov_cardview_lastgoals).setOnClickListener(goalsDetailClickListener)
                    view.findViewById<ImageButton>(R.id.stats_ov_card_lastgoals_ib_more_details).setOnClickListener(goalsDetailClickListener)

                    initLastGoalsCard()
                }

                initHeaderData()
                initLast7DaysCard()
                initRatingsCard()

            } else {
                // show the hint
                requireView().findViewById<TextView>(R.id.statisticsHint).visibility = View.VISIBLE
            }
        }

        setCommonToolbar(requireActivity(), view.findViewById(R.id.statsToolbar)) {
//                Place menu item click handler here
//                when(it) {
//                }
        }
    }

    /**
     * Header with Textviews showing statistics
     */
    private fun initHeaderData() {
        lifecycleScope.launch {
            arrayListOf<LinearLayout>(
                requireView().findViewById(R.id.heading_data_1),
                requireView().findViewById(R.id.heading_data_2),
                requireView().findViewById(R.id.heading_data_3),
                requireView().findViewById(R.id.heading_data_4),

                ).forEachIndexed { i, ll ->
                val tvData = ll.findViewById<TextView>(R.id.stats_ov_heading_tv_data)
                val tvDesc = ll.findViewById<TextView>(R.id.stats_ov_heading_tv_desc)

                when (i) {
                    0 -> {
                        tvData.text = getTotalTimeLastMonth()
                        tvDesc.text = getString(R.string.in_month,
                                    getStartOfMonth(-1)
                                        .format(DateTimeFormatter.ofPattern(
                                        DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL)))
                    }
                    1 -> {
                        tvData.text = getAvgTimePerSession()
                        tvDesc.text = getString(R.string.average)
                    }
                    2 -> {
                        tvData.text = getAvgBreakTimePerHour()
                        tvDesc.text = getString(R.string.avg_break_time)
                    }
                    3 -> {
                        tvData.text = getAvgRating()
                        tvDesc.text = getString(R.string.avg_rating)
                    }
                    else -> {
                        tvData.text = "TBA"
                        tvDesc.text = "TBA"
                    }
                }
            }
        }
    }

    /**
     * PracticeTime "last 7 days" quick glimpse chart
     */
    private fun initLast7DaysCard() {
        val lastDaysChart: BarChart = requireView().findViewById(R.id.stats_ov_card_last7days_bar_chart)
        lastDaysChart.apply {
            setTouchEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false
        }

        // x axis
        lastDaysChart.xAxis.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = 7
            valueFormatter = XAxisValueFormatter()
            textColor = PracticeTime.getThemeColor(R.attr.colorOnSurface, requireActivity())
        }

        lastDaysChart.axisLeft.isEnabled = false
        lastDaysChart.axisRight.isEnabled = false

        lastDaysChart.notifyDataSetChanged()
        lastDaysChart.invalidate()

        setLast7DaysChartData(lastDaysChart)
    }

    private fun setLast7DaysChartData(lastDaysChart: BarChart) {
        lifecycleScope.launch {

            // get all total durations from the last 7 days
            val barChartArray = arrayListOf<BarEntry>()
            for (day in 0 downTo -6) {
                val dur = PTDatabase.getInstance(requireContext()).sectionDao.getWithLibraryItems(
                        getStartOfDay(day.toLong()).toEpochSecond(),
                        getEndOfDay(day.toLong()).toEpochSecond()
                    ).sumOf {
                        it.section.duration ?: 0
                    }
                barChartArray.add(0, BarEntry(day.toFloat(), dur.toFloat()))
            }

            val dataSetBarChart = BarDataSet(barChartArray, "Label")
            dataSetBarChart.apply {
                setDrawValues(false)
                color = PracticeTime.getThemeColor(R.attr.colorPrimary, requireActivity())
            }

            val barData = BarData(dataSetBarChart)
            barData.apply {
                barWidth = 0.4f
                isHighlightEnabled = false

            }
            lastDaysChart.apply {
                data = barData
                animateXY(1000, 1000, Easing.EaseOutBack)
                notifyDataSetChanged()
                invalidate()
            }

            requireView()
                .findViewById<TextView>(R.id.stats_ov_card_last7days_tv_total_time)
                .text = getDurationString(
                        barChartArray.sumOf { it.y.toInt() },
                        TIME_FORMAT_HUMAN_PRETTY
                    )
        }
    }

    private fun initLastGoalsCard() {
        lifecycleScope.launch {
            val lastGoals = PTDatabase.getInstance(requireContext()).goalInstanceDao.getWithDescription(
                from = 0,
                to = getCurrTimestamp()
            )   .sortedBy { it.instance.startTimestamp +  it.instance.periodInSeconds}  // sort by end date
                .takeLast(5)
            var achievedGoalsCount = 0
            arrayListOf<LinearLayout>(
                requireView().findViewById(R.id.progressbarlayout_1),
                requireView().findViewById(R.id.progressbarlayout_2),
                requireView().findViewById(R.id.progressbarlayout_3),
                requireView().findViewById(R.id.progressbarlayout_4),
                requireView().findViewById(R.id.progressbarlayout_5),

            ).forEachIndexed { i, ll ->
                val pBar = ll.findViewById<ProgressBar>(R.id.stats_ov_card_lastgoals_progressbarlayout_pg)
                val check = ll.findViewById<ImageView>(R.id.stats_ov_card_lastgoals_progressbarlayout_iv)
                val date = ll.findViewById<TextView>(R.id.stats_ov_card_lastgoals_progressbarlayout_tv_date)

                if (i < lastGoals.size) {
                    val gi = lastGoals[i].instance

                    pBar.progress =
                        min(100, (gi.progress.toFloat() / gi.target.toFloat() * 100).toInt())
                    date.text = epochSecondsToDate(gi.startTimestamp + gi.periodInSeconds)
                        .minusHours(1)  // subtract 1 hour to get the day before (because of half-open approach)
                        .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH))

                    if (pBar.progress == 100) {
                        check.visibility = View.VISIBLE
                        achievedGoalsCount++
                    }

                    // change color according to libraryItem
                    val gd = lastGoals[i].description
                    if (gd.type == GoalType.ITEM_SPECIFIC) {
                        // find out libraryItem
                        val catId =
                            PTDatabase.getInstance(requireContext()).goalDescriptionDao.getGoalDescriptionLibraryItemCrossRefs(
                                goalDescriptionId = gd.id
                            ).first().libraryItemId
                        val cat = PTDatabase.getInstance(requireContext()).libraryItemDao.get(catId)
                        val libraryItemColors =
                            requireContext().resources.getIntArray(R.array.library_item_colors)
                        val color = ColorStateList.valueOf(libraryItemColors[cat?.colorIndex ?: 0])

                        pBar.progressTintList = color
                        check.imageTintList = color
                    }
                } else {
                    // not enough goals, hide the elements
                    pBar.visibility = View.INVISIBLE
                    check.visibility = View.INVISIBLE
                    date.visibility = View.INVISIBLE
                }
            }

            requireView().findViewById<TextView>(R.id.stats_ov_card_lastgoals_tv_achieved).text =
                "$achievedGoalsCount/${lastGoals.size}"

        }
    }

    private fun initRatingsCard() {
        val ratingsChart: PieChart = requireView().findViewById(R.id.stats_ov_card_session_ratings_pie_chart)

        /** CHART */
        ratingsChart.apply {
            setDrawEntryLabels(true)
            isDrawHoleEnabled = false
            isHighlightPerTapEnabled = false
            setUsePercentValues(false)
            description.isEnabled = false
            legend.apply {
                isEnabled = false
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                orientation = Legend.LegendOrientation.VERTICAL
                isWordWrapEnabled = true

            }
            isRotationEnabled = false
            setExtraOffsets(0f, 10f, 0f, 6f)
            setEntryLabelColor(PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, requireContext()))
            setEntryLabelTextSize(11f)
        }

        lifecycleScope.launch {

            /** DATASET */
            val dataset = PieDataSet(getRatingsPieArray(), "LABEL")
            dataset.apply {
                colors = PracticeTime.getLibraryItemColors(requireContext())
                setDrawValues(false)
                isUsingSliceColorAsValueLineColor = true
                sliceSpace = 2f
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1OffsetPercentage = 100f   // start of value line in % from center of chart
                valueLinePart1Length = 1f             // length of "outgoing" line
                valueLinePart2Length = 2f             // length of horizontal line
            }

            /** DATA */
            val rData = PieData(dataset)
            rData.apply {
                setValueTextSize(10f)
            }

            // render the chart
            ratingsChart.apply {
                data = rData
                animateY(1000, Easing.EaseInOutQuad)
                notifyDataSetChanged()
                invalidate()
            }
        }
    }

    private suspend fun getTotalTimeLastMonth(): CharSequence {
        val beginLastMonth = getStartOfMonth(-1).toEpochSecond()
        val endLastMonth = getEndOfMonth(-1).toEpochSecond()

        val totalTime = PTDatabase.getInstance(requireContext()).sectionDao.getWithLibraryItems(beginLastMonth, endLastMonth)
            .sumOf { it.section.duration ?: 0}

        return getDurationString(totalTime, TIME_FORMAT_HUMAN_PRETTY_SHORT)
    }

    private suspend fun getAvgTimePerSession(): CharSequence {
        return TextUtils.concat(getString(R.string.average_sign) + " ",
                getDurationString(
                    (getTotalPracticeTime().toFloat() / getAllSessions().size.toFloat()).toInt(),
                    TIME_FORMAT_HUMAN_PRETTY_SHORT
                ))
    }

    private suspend fun getAvgBreakTimePerHour(): CharSequence {
        val totalBreakTime = getAllSessions()
            .sumOf { it.session.breakDuration }
        val totalPracticeHours = getTotalPracticeTime().toFloat() / SECONDS_PER_HOUR.toFloat()

        return getDurationString(
            (totalBreakTime.toFloat() / totalPracticeHours).toInt(),
            TIME_FORMAT_HUMAN_PRETTY_SHORT
        )
    }

    private suspend fun getAvgRating(): CharSequence {
        val avg = getAllSessions().sumOf { it.session.rating }.toFloat() /
                getAllSessions().size.toFloat()

        return getString(R.string.average_sign) + " " +
                "%.1f".format(avg)
    }

    private suspend fun getTotalPracticeTime(): Int {
        if (totalPracticeTime < 0) {
            totalPracticeTime = getAllSessions().sumOf { prSess ->
                    prSess.sections.sumOf { it.section.duration ?: 0 }
                }
        }
        return totalPracticeTime
    }

    private suspend fun getAllSessions(): List<SessionWithSectionsWithLibraryItems> {
        if (!this::allSessions.isInitialized)
//            allSessions = PTDatabase.getInstance(requireContext()).sessionDao.getAllWithSectionsWithLibraryItems()
            allSessions = emptyList()

        return allSessions
    }

    private suspend fun getRatingsPieArray(): ArrayList<PieEntry> {
        val pieChartArray = arrayListOf<PieEntry>()
        val ratingsData = IntArray(6) {0}
        getAllSessions().forEach {
            ratingsData[it.session.rating]++
        }
        ratingsData.forEachIndexed { i, ratingValue ->
            if (i != 0)
                pieChartArray.add(PieEntry(ratingValue.toFloat(), "${getStarsString(i)} · $ratingValue"))
        }

        // to make labels not overlap, re-sort the entries based on their size in the chart
        // sort ascending
        pieChartArray.sortBy { it.value }
        // new order will be small-large-small-large
        val a = arrayListOf(1,4,2,3,0)
        val pieChartArrSorted = arrayListOf<PieEntry>()
        a.forEach {index ->
            pieChartArrSorted.add(pieChartArray[index])
        }
        return pieChartArrSorted
    }

    private fun getStarsString(num: Int): String {
        if (num == 0) return "0"
        var str = ""
        for (i in 0 until num) {
            str += getString(R.string.star_sign)
        }
        return str
    }

    /**
     * shows weekday abbreviations under the bars in last7days card
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(xValue: Float): String {
            return getStartOfDay(xValue.toLong())
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_WEEKDAY_SHORT))
        }
    }
}
