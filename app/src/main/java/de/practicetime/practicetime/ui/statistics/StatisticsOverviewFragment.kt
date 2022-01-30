package de.practicetime.practicetime.ui.statistics

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.min

class StatisticsOverviewFragment : Fragment(R.layout.fragment_statistics_overview) {

    private lateinit var lastDaysChart: BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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
            textColor = PracticeTime.ctx.getThemeColor(R.attr.colorOnSurface, requireActivity())
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
                val dur = PracticeTime.dao.getSectionsWithCategories(
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
                color = PracticeTime.ctx.getThemeColor(R.attr.colorPrimary, requireActivity())
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
            val lastGoals = PracticeTime.dao.getGoalInstancesWithDescription().takeLast(5)
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
                date.text = epochSecondsToDate(gi.startTimestamp + gi.periodInSeconds)
                    .minusHours(1)  // subtract 1 hour to get the day before (because of half-open approach)
                    .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH))

                if (pBar.progress == 100) {
                    check.visibility = View.VISIBLE
                    achievedGoalsCount++
                }

                // change color according to category
                val gd = lastGoals[i].description
                if (gd.type == GoalType.CATEGORY_SPECIFIC) {
                    // find out category
                    val catId = PracticeTime.dao.getGoalDescriptionCategoryCrossRefsWhereDescriptionId(gd.id).first().categoryId
                    val cat = PracticeTime.dao.getCategory(catId)
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
        val beginLastMonth = getStartOfMonth(-1).toEpochSecond()
        val endLastMonth = getEndOfMonth(-1).toEpochSecond()

        val totalTime = PracticeTime.dao.getSectionsWithCategories(beginLastMonth, endLastMonth)
            .sumOf { it.section.duration ?: 0}

        return getDurationString(totalTime, TIME_FORMAT_HUMAN_PRETTY_SHORT)
    }

    /**
     * formats x axis value according to Last 7 days
     * // TODO re-use the Formatter from the Statistics Activities
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(xValue: Float): String {
            return getStartOfDay(xValue.toLong())
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_WEEKDAY_SHORT))
        }
    }
}