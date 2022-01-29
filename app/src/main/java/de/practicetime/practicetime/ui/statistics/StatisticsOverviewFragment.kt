package de.practicetime.practicetime.ui.statistics

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.PTDao
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.GoalType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.math.min

class StatisticsOverviewFragment : Fragment(R.layout.fragment_statistics_overview) {

    private lateinit var dao: PTDao
    private lateinit var lastDaysChart: BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        openDatabase()

        val sessionDetailClickListener = View.OnClickListener {
            val i = Intent(requireContext(), SessionStatsActivity::class.java)
            requireActivity().startActivity(i)
        }
        view.findViewById<CardView>(R.id.stats_ov_cardview_last7days).setOnClickListener(sessionDetailClickListener)
        view.findViewById<ImageButton>(R.id.stats_ov_card_last7days_ib_more_details).setOnClickListener(sessionDetailClickListener)

        val goalsDetailClickListener = View.OnClickListener {
            val i = Intent(requireContext(), GoalStatsActivity::class.java)
            requireActivity().startActivity(i)
        }
        view.findViewById<CardView>(R.id.stats_ov_cardview_lastgoals).setOnClickListener(goalsDetailClickListener)
        view.findViewById<ImageButton>(R.id.stats_ov_card_lastgoals_ib_more_details).setOnClickListener(goalsDetailClickListener)


        initHeaderData()

        initLast7DaysChart()
        setLast7DaysChartData()

        initLastGoalsCard()
    }

    private fun initLast7DaysChart() {
        lastDaysChart = requireView().findViewById(R.id.stats_ov_card_last7days_bar_chart)
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
            textColor = getThemeColor(R.attr.colorOnSurface)
        }

        lastDaysChart.axisLeft.isEnabled = false
        lastDaysChart.axisRight.isEnabled = false

        lastDaysChart.notifyDataSetChanged()
        lastDaysChart.invalidate()
    }

    private fun setLast7DaysChartData() {
        lifecycleScope.launch {

            // get all total durations from the last 7 days
            val barChartArray = arrayListOf<BarEntry>()
            for (day in 0 downTo -6) {
                val dur = dao.getSectionsWithCategories(
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
                color = getThemeColor(R.attr.colorPrimary)
            }

            val barData = BarData(dataSetBarChart)
            barData.apply {
                barWidth = 0.4f
                isHighlightEnabled = false

            }
            lastDaysChart.apply {
                data = barData
                animateXY(500, 1000, Easing.EaseOutBack)
                notifyDataSetChanged()
                invalidate()
            }

            requireView()
                .findViewById<TextView>(R.id.stats_ov_card_last7days_tv_avg_time)
                .text = getAvgText(barChartArray)
        }
    }

    private fun getAvgText(barChartArray: ArrayList<BarEntry>): String {
        val totalSec = barChartArray.sumOf { it.y.toInt() }

        val hours = totalSec.div(3600)
        val minutes = (totalSec.rem(3600)).div(60)

        return if (hours > 0) {
            "%dh %dmin".format(hours, minutes)
        } else if (minutes == 0 && hours > 0){
            "<1min"
        } else {
            "%dmin".format(minutes)
        }
    }

    private fun initLastGoalsCard() {
        lifecycleScope.launch {
            val lastGoals = dao.getGoalInstancesWithDescription().takeLast(5)
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

                val gi = lastGoals[i].instance
//                Log.d("TAG", "Goal $i: id=${gi.id} progress=${gi.progress}, target=${gi.target}")

                pBar.progress = min(100, (gi.progress.toFloat() / gi.target.toFloat() * 100).toInt())
                date.text = getDateString(gi.startTimestamp + gi.periodInSeconds)

                if (pBar.progress == 100) {
                    check.visibility = View.VISIBLE
                    achievedGoalsCount++
                }

                // change color according to category
                val gd = lastGoals[i].description
                if (gd.type == GoalType.CATEGORY_SPECIFIC) {
                    // find out category
                    val catId = dao.getGoalDescriptionCategoryCrossRefsWhereDescriptionId(gd.id).first().categoryId
                    val cat = dao.getCategory(catId)
                    val categoryColors =  requireContext().resources.getIntArray(R.array.category_colors)
                    val color = ColorStateList.valueOf(categoryColors[cat.colorIndex])

                    pBar.progressTintList = color
                    check.imageTintList = color
                }
            }

            requireView().findViewById<TextView>(R.id.stats_ov_card_lastgoals_tv_achieved).text =
                "$achievedGoalsCount/5"
        }

    }

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
                        tvDesc.text = getString(R.string.total_time_last_month)
                    }
                    else -> {
                        tvData.text = "TBA"
                        tvDesc.text = "TBA"
                    }
                }
            }
        }
    }

    private suspend fun getTotalTimeLastMonth(): String {
        val beginLastMonth = ZonedDateTime.now()
            .with(ChronoField.DAY_OF_MONTH , 1 )    // jump to first day of this month
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .minusMonths(1) // subtract one month from now
            .toEpochSecond()

        // half-open: end of last month = start of this month
        val endLastMonth = ZonedDateTime.now()
            .with(ChronoField.DAY_OF_MONTH , 1 )
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .toEpochSecond()

        val totalTime = dao.getSectionsWithCategories(beginLastMonth, endLastMonth)
            .sumOf { it.section.duration ?: 0}

        return secondsToTimeString(totalTime)
    }

    private fun secondsToTimeString(seconds: Int?): String {
        // TODO change to string resources with placeholders eventually
        val (h, m) = secondsToHoursMins(seconds)
        var str = ""
        if (h != 0) str += "${h}h "
        if (m != 0) str += "${m}m"
        else
            if (h == 0)
                if (seconds != 0) str = "< 1m"
                else str += "0m"
        return str
    }

    private fun secondsToHoursMins(seconds: Int?): Pair<Int?, Int?> {
        val hours = seconds?.div(3600)
        val minutes = (seconds?.rem(3600))?.div(60)

        return Pair(hours, minutes)
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    /**
     * formats x axis value according to Last 7 days
     * // TODO re-use the Formatter from the Statistics Activities
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(xValue: Float): String {
            return ZonedDateTime.now()
                .plusDays(xValue.toLong())
                .format(DateTimeFormatter.ofPattern("EEEEE"))
        }
    }

    // get the Beginning of daysInPastOffset Days in the past
    private fun getStartOfDay(daysInPastOffset: Long): ZonedDateTime {
        val day = ZonedDateTime.now()
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusDays(daysInPastOffset)
        return day
    }

    // get the End of daysInPastOffset Days in the past. Half-open: Actually get the Start of the Next day
    private fun getEndOfDay(daysInPastOffset: Long): ZonedDateTime {
        return ZonedDateTime.now()
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusDays(daysInPastOffset + 1)
    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }

    private fun getDateString(epochSecs: Long): String {
        return ZonedDateTime
            .ofInstant(Instant.ofEpochSecond(epochSecs), ZoneId.systemDefault())
            .minusHours(1)  // subtract 1 hour to get the day before (because of half-open approach)
            .format(DateTimeFormatter.ofPattern("dd.MM."))
    }
}