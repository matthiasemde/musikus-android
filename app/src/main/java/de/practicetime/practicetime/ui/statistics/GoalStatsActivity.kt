/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.statistics

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.tabs.TabLayout
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.GoalDescription
import de.practicetime.practicetime.database.entities.GoalInstance
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.utils.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

private const val X_AXIS_LABEL_COUNT_NORMAL = 5
private const val X_AXIS_LABEL_COUNT_ONEDAYGOAL = 7

class GoalStatsActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var barChart: BarChart
    private lateinit var goalListAdapter: GoalStatsAdapter

    private val goals = ArrayList<GoalListElement>()
    private var selectedGoal = 0

    private var selectedBar = SelectedBar.SELECTED_BAR_NONE
    private enum class SelectedBar {
        SELECTED_BAR_RIGHT,
        SELECTED_BAR_LEFT,
        SELECTED_BAR_NONE
    }

    private var limitLinePosition = LimitLineAllowedPos.BOTH
    private var limitLinePositionSelectedState = LimitLineAllowedPos.BOTH
    private enum class LimitLineAllowedPos {
        ONLY_RIGHT,
        ONLY_LEFT,
        BOTH,
        NONE
    }

    private var verticalLimitLines = arrayListOf<LimitLine>()

    // the first Instance of the selected goal currently shown in graph
    private var firstGoalInstShownIndex = 0
    // the last Instance of the selected goal currently shown in graph
    // negative if no instances are shown in current time range
    private var lastGoalInstShownIndex = 0

    private var intervalOffset = 0

    data class GoalListElement(
        val goalInstances: List<GoalInstance>,
        val goalDesc: GoalDescription,
        val libraryItem: LibraryItem?,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        setSupportActionBar(findViewById(R.id.stats_session_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.goal_statistics)
        }

        findViewById<ImageButton>(R.id.btn_rwnd).setOnClickListener {
            seekPast()
        }
        findViewById<ImageButton>(R.id.btn_fwd).setOnClickListener {
            seekFuture()
        }

        lifecycleScope.launch {
            initGoalsList()
            // if no goals, do nothing. Should never happen normally because you
            // shouldn't be able to enter activity with zero goals
            if (goals.size > 0) {
                initBarChart()
                updateChartData()
                setButtonEnabledState()
            }
        }

        // disable time range switcher buttons
        findViewById<TabLayout>(R.id.statistics_tablayout).visibility = View.GONE
    }

    private fun setButtonEnabledState() {
        findViewById<ImageButton>(R.id.btn_fwd).isEnabled = intervalOffset != 0
    }

    /** get the goals from the database */
    private suspend fun initGoalsList() {
        PTDatabase.getInstance(applicationContext).goalDescriptionDao.getAllWithLibraryItems().forEach { (desc, cat) ->
            goals.add(
                GoalListElement(
                    goalInstances = PTDatabase.getInstance(applicationContext).goalInstanceDao.get(desc.id, from = 0L),
                    goalDesc = desc,
                    libraryItem = cat.firstOrNull()
                )
            )
        }
        goalListAdapter = GoalStatsAdapter()
        val layoutManager = LinearLayoutManager(this@GoalStatsActivity)

        val libraryItemRecyclerView = findViewById<RecyclerView>(R.id.recyclerview_statistics)
        libraryItemRecyclerView.layoutManager = layoutManager
        libraryItemRecyclerView.adapter = goalListAdapter
    }


    private fun initBarChart() {
        // for rounded bars: https://gist.github.com/xanscale/e971cc4f2f0712a8a3bcc35e85325c27
        //      issue: https://github.com/PhilJay/MPAndroidChart/issues/2771#issuecomment-566719474

        barChart = findViewById(R.id.bar_chart)
        barChart.apply {
            isDragEnabled = false
            isDoubleTapToZoomEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setOnChartValueSelectedListener(this@GoalStatsActivity)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)
        }

        // x axis
        barChart.xAxis.apply {
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
//            labelCount = X_AXIS_LABEL_COUNT_NORMAL
            valueFormatter = XAxisValueFormatter()
            textColor = PracticeTime.getThemeColor(R.attr.colorOnSurface, this@GoalStatsActivity)
        }

        // left axis
        barChart.axisLeft.apply {
            axisMinimum = 0f   // needed for y axis scaling because chart depends on left axis (even if disabled)
            isEnabled = false
        }

        // right axis
        barChart.axisRight.apply {
            axisMinimum = 0f
            setDrawAxisLine(false)
            textColor = PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, this@GoalStatsActivity)
            valueFormatter = YAxisValueFormatter()
            setDrawLimitLinesBehindData(true)
        }

        barChart.notifyDataSetChanged()
        barChart.invalidate()
    }

    private fun updateChartData() {

        // re-calculate bar data
        val barValues = getGoalsArray()

        barChart.xAxis.labelCount =
            if (isOneDayGoal()) {
                X_AXIS_LABEL_COUNT_ONEDAYGOAL
            } else {
                X_AXIS_LABEL_COUNT_NORMAL
            }

        val dataSetBarChart: BarDataSet
        if (barChart.data != null && barChart.data.dataSetCount > 0) {
            // update chart data
            dataSetBarChart = barChart.data.getDataSetByIndex(0) as BarDataSet
            dataSetBarChart.apply {
                values = barValues
                color = getChartColor()
            }
            // update
            barChart.data.notifyDataChanged()

        } else {
            // first time drawing chart, create the DataSet from values
            dataSetBarChart = BarDataSet(barValues, "Label")
            dataSetBarChart.apply {
                setDrawValues(true)
                iconsOffset = MPPointF(0f, 18f)     // checkmarks should draw inside of bars
                color = getChartColor()
                highLightColor = PracticeTime.getThemeColor(R.attr.colorOnSurface, this@GoalStatsActivity)
                highLightAlpha = 100    // 150 out of 255 (0=fully transparent)
            }

            val barData = BarData(dataSetBarChart)
            barData.apply {
                barWidth = 0.4f
                setValueFormatter(BarChartValueFormatter())
                setValueTextColor(PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, this@GoalStatsActivity))
                setValueTextSize(12f)
                isHighlightEnabled = true
            }

            // triggers xAxisValueFormatter with all xValues of barArray each 1x
            barChart.data = barData
        }


        /**  Setup the limitline */
        val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()    // limitline limit
        val rightestBar = barValues.find { it.x == dataSetBarChart.xMax }
        val leftestBar = barValues.find { it.x == dataSetBarChart.xMin }

        // reset allowed positions
        limitLinePosition = LimitLineAllowedPos.BOTH
        limitLinePositionSelectedState = LimitLineAllowedPos.BOTH

        // bar heights which are too much for displaying limit line label
        val tooHighNrml = t * 1.05
        val tooHighSlc = t * 0.8

        if (rightestBar != null && leftestBar != null) {

            /* Normal state limitline Position */
            if (leftestBar.y < tooHighNrml && rightestBar.y > tooHighNrml) {
                // right would cover
                limitLinePosition = LimitLineAllowedPos.ONLY_LEFT
            }
            if (leftestBar.y > tooHighNrml && rightestBar.y < tooHighNrml) {
                // left would cover
                limitLinePosition = LimitLineAllowedPos.ONLY_RIGHT
            }
            if (leftestBar.y > tooHighNrml && rightestBar.y > tooHighNrml) {
                // both would cover
                limitLinePosition = LimitLineAllowedPos.NONE
            }

            /* Selected state limitline Position */
            if (leftestBar.y < tooHighSlc && rightestBar.y > tooHighSlc) {
                // right would cover
                limitLinePositionSelectedState = LimitLineAllowedPos.ONLY_LEFT
            }
            if (leftestBar.y > tooHighSlc && rightestBar.y < tooHighSlc) {
                // left would cover
                limitLinePositionSelectedState = LimitLineAllowedPos.ONLY_RIGHT
            }
            if (leftestBar.y > tooHighSlc && rightestBar.y > tooHighSlc) {
                // both would cover
                limitLinePositionSelectedState = LimitLineAllowedPos.NONE
            }
        }

        barChart.apply {
            val (max, cnt) = calculateAxisValues()

            highlightValue(0f, -1, false)   // remove all highlighting
            if (intervalOffset == 0) {
                // highlight last value in the beginning
                highlightValue((goals[selectedGoal].goalInstances.size - 1).toFloat(), 0, false)
                selectedBar = SelectedBar.SELECTED_BAR_RIGHT
            }

            axisLeft.axisMaximum = max
            axisRight.apply {
                axisMaximum = max
                setLabelCount(cnt, true)
                removeAllLimitLines()
                addLimitLine(getTargetLimitLine(t))
            }
            xAxis.apply {
                verticalLimitLines.forEach {
                    addLimitLine(it)
                }
            }

            // redraw the chart
            animateY(1000, Easing.EaseOutBack)
            notifyDataSetChanged()
            // Does NOT trigger xAxisValueFormatter
            invalidate()
        }
        // update the Heading
        setHeadingTextViews()
    }

    /**
     * returns the LimitLine used to visualize the goal target
     */
    private fun getTargetLimitLine(limit: Float): LimitLine {

        val allowedRightNrm = (limitLinePosition == LimitLineAllowedPos.ONLY_RIGHT ||
                limitLinePosition == LimitLineAllowedPos.BOTH)
        val allowedLeftNrm = (limitLinePosition == LimitLineAllowedPos.ONLY_LEFT ||
                limitLinePosition == LimitLineAllowedPos.BOTH)

        val allowedRightSlc = (limitLinePositionSelectedState == LimitLineAllowedPos.ONLY_RIGHT ||
                limitLinePositionSelectedState == LimitLineAllowedPos.BOTH)
        val allowedLeftSlc = (limitLinePositionSelectedState == LimitLineAllowedPos.ONLY_LEFT ||
                limitLinePositionSelectedState == LimitLineAllowedPos.BOTH)


        val wouldCoverLeft = !allowedLeftNrm ||
                (selectedBar == SelectedBar.SELECTED_BAR_LEFT && !allowedLeftSlc)
        val wouldCoverRight = !allowedRightNrm ||
                (selectedBar == SelectedBar.SELECTED_BAR_RIGHT && !allowedRightSlc)


        // default labelPos is right...
        var labelPos = LimitLine.LimitLabelPosition.RIGHT_TOP
        var drawLabel = true

        if (wouldCoverRight) {      // ...except it would cover...
            if (!wouldCoverLeft) {
                // ...then, it is left, but only if it doesn't cover left either
                labelPos = LimitLine.LimitLabelPosition.LEFT_TOP
            } else {
                // if it does, display no label at all
                drawLabel = false
            }
        }

        val label =
            if (drawLabel) getDurationString(limit.toInt(), TIME_FORMAT_HUMAN_PRETTY).toString()
            else ""

        return LimitLine(limit, label).apply {
            lineWidth = 1f
            enableDashedLine(10f, 10f, 0f)
            lineColor = getChartColor()
            textColor = getChartColor()
            labelPosition = labelPos
            textSize = 10f
        }
    }

    /**
     * gets the (vertical) LimitLine used to visualize gaps in the time history between goal instances
     */
    private fun getVerticalLimitLine(limit: Float, label: String): LimitLine {
        return LimitLine(limit, label).apply {
            lineWidth = 3f
            lineColor = PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, this@GoalStatsActivity)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textColor = lineColor
            textSize = 10f
            enableDashedLine(20f, 10f, 0f)
        }
    }

    /**
     * Function to calculate the y Axis label values which should be drawn.
     * makes sure all values are 15min, 30min or full hours intervals
     */
    private fun calculateAxisValues(): Pair<Float, Int> {
        val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()
        val maximumRequired = max(t * 1.1f, barChart.yMax) // determine maximum value shown

        val interval = when {
            maximumRequired < 30*60 -> 5*60          // max Value <30m, round up to next 5min
            maximumRequired < 60*60 -> 15*60         // max Value <1h, round up to next 15min
            maximumRequired < 2*60*60 -> 20*60       // max Value <2h, round up to next 20min
            maximumRequired < 5*60*60 -> 60*60       // max Value <5h, round up to next hour
            maximumRequired < 10*60*60 -> 2*60*60    // max Value <10h, round up to next 2 hours
            else -> {
                // above 10hours, fix the interval to 1/6 of maximum and then round up to full hours
                val desiredInterval = maximumRequired / 6f
                // round desiredInterval up to full hours
                60*60 * ceil(desiredInterval / (60*60)).toInt()
            }
        }
        val newMax = interval * ceil(maximumRequired / interval.toFloat())
        return Pair(newMax, (newMax / interval).toInt() + 1)
    }

    /** Sets the text content for the BarChart Heading and the subheading */
    private fun setHeadingTextViews() {
        val tvRange = findViewById<TextView>(R.id.tv_chart_header)
        val tvSecondHeader = findViewById<TextView>(R.id.tv_secondary_chart_header)

        val chartDataSet = barChart.data.getDataSetByIndex(0) as BarDataSet

        val xValFirstBar = chartDataSet.xMin
        val xValLastBar = chartDataSet.xMax

        // xVal represents index of goalInstances for selectedGoal
        // timestamp of start of interval of first Bar currently shown
        val tmStart = getGoalInstance(xValFirstBar.toInt()).startTimestamp

        // timestamp of end of interval of last Bar currently shown
        val tmEnd = getGoalInstance(xValLastBar.toInt())
            .let {
                it.startTimestamp + it.periodInSeconds - 1  // -1 to get right day because of half-open approach
            }

        if (goals[selectedGoal].goalDesc.periodUnit != GoalPeriodUnit.MONTH) {
            // Daily and Weekly Goals
            var formatStrStart = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL $DATE_FORMATTER_PATTERN_DAY_OF_MONTH"
            var formatStrEnd = DATE_FORMATTER_PATTERN_DAY_OF_MONTH

            if (epochSecondsToDate(tmStart).month != epochSecondsToDate(tmEnd).month) {
                formatStrEnd = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV $DATE_FORMATTER_PATTERN_DAY_OF_MONTH"    // also show month if it is different from startMonth
                formatStrStart = formatStrEnd
            }
            // START date
            val start = epochSecondsToDate(tmStart)
                .format(DateTimeFormatter.ofPattern(formatStrStart))
            val end = epochSecondsToDate(tmEnd)
                .format(DateTimeFormatter.ofPattern(formatStrEnd))

            // set the textView text as the final string
            tvRange.text = ("$start - $end")

        } else {
            // Monthly goals
            var formatStrStart = DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL
            var formatStrEnd = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL $DATE_FORMATTER_PATTERN_YEAR"

            if (epochSecondsToDate(tmStart).year != epochSecondsToDate(tmEnd).year) {
                formatStrStart = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV $DATE_FORMATTER_PATTERN_YEAR"
                formatStrEnd = formatStrStart
            }

            val start = epochSecondsToDate(tmStart)
                .format(DateTimeFormatter.ofPattern(formatStrStart))
            val end = epochSecondsToDate(tmEnd)
                .format(DateTimeFormatter.ofPattern(formatStrEnd))

            tvRange.text = "$start - $end"
        }

        if (lastGoalInstShownIndex < 0)
            tvSecondHeader.text = getString(R.string.no_records_current_goal)
        else {
            val succeeded = goals[selectedGoal].goalInstances
                .subList(max(firstGoalInstShownIndex, 0), lastGoalInstShownIndex+1) // +1 because lastindex is exclusive
                .filter { it.progress >= it.target }
                .size

            val failed = goals[selectedGoal].goalInstances
                .subList(max(firstGoalInstShownIndex, 0), lastGoalInstShownIndex+1)
                .filter { it.progress < it.target }
                .size

            tvSecondHeader.text = getString(R.string.succeeded_failed, succeeded, failed+succeeded)
        }
    }

    private fun getChartColor(): Int {
        return if(goals[selectedGoal].libraryItem != null) {
                val libraryItemColors = resources.getIntArray(R.array.library_item_colors).toCollection(mutableListOf())
                libraryItemColors[goals[selectedGoal].libraryItem!!.colorIndex]
            } else {
                PracticeTime.getThemeColor(R.attr.colorPrimary, this)
            }
    }

    private fun seekPast() {
        intervalOffset -=
            if (isOneDayGoal()) X_AXIS_LABEL_COUNT_ONEDAYGOAL
            else X_AXIS_LABEL_COUNT_NORMAL
        updateChartData()
        setButtonEnabledState()
    }

    private fun seekFuture() {
        intervalOffset +=
            if (isOneDayGoal()) X_AXIS_LABEL_COUNT_ONEDAYGOAL
            else X_AXIS_LABEL_COUNT_NORMAL
        updateChartData()
        setButtonEnabledState()
    }


    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e == null)
            return

        val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()

        selectedBar = if (e.x == barChart.data.getDataSetByIndex(0).xMax &&
            e.y > 0.8 * t)     // only change if value would actually cover limitline label
            SelectedBar.SELECTED_BAR_RIGHT
        else if (e.x == barChart.data.getDataSetByIndex(0).xMin &&
            e.y > 0.8 * t)     // only change if value would actually cover limitline label
            SelectedBar.SELECTED_BAR_LEFT
        else
            SelectedBar.SELECTED_BAR_NONE

        barChart.axisRight.apply {
            removeAllLimitLines()
            addLimitLine(getTargetLimitLine(t))
        }
    }

    override fun onNothingSelected() {
        selectedBar = SelectedBar.SELECTED_BAR_NONE
        barChart.axisRight.apply {
            val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()
            removeAllLimitLines()
            addLimitLine(getTargetLimitLine(t))
        }
    }

    /** gets the ArrayList for the BarChart object out of goals ArrayList */
    private fun getGoalsArray(): ArrayList<BarEntry> {
        val barChartArray = arrayListOf<BarEntry>()

        val labelCount =
            if (isOneDayGoal()) {
                X_AXIS_LABEL_COUNT_ONEDAYGOAL
            } else {
                X_AXIS_LABEL_COUNT_NORMAL
            }

        val allInstances = goals[selectedGoal].goalInstances
        // get X_AXIS_LABEL_COUNT instances from the current interval.
        // intervalOffset == 0 means last X_AXIS_LABEL_COUNT
        // intervalOffset == -1 means get the X_AXIS_LABEL_COUNT before the last X_AXIS_LABEL_COUNT instances etc.
        lastGoalInstShownIndex = allInstances.size-1 + intervalOffset
        firstGoalInstShownIndex = lastGoalInstShownIndex - labelCount + 1 //add 1 because we want exactly X_AXIS_LABEL_COUNT bars

        verticalLimitLines.clear()
        barChart.xAxis.removeAllLimitLines()

       var endDateOfLastInst = allInstances[max(0,firstGoalInstShownIndex)].startTimestamp

        for (i in firstGoalInstShownIndex..lastGoalInstShownIndex) {
            val yVal =
                if(i < 0) 0.001f    // all instances exhausted. Make 0.001f to recognize in the ValueFormatter that it is fake
                else allInstances[i].progress.toFloat()

            // goal is achieved, set the checkmark icon
            if (i >= 0 && yVal >= allInstances[i].target) {
                val iconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_check_small)!!
                // tint it like this because iconTintList requires API >=26
                DrawableCompat.setTint(iconDrawable,
                    PracticeTime.getThemeColor(R.attr.colorSurface, this))
                barChartArray.add(BarEntry(i.toFloat(), yVal, iconDrawable))
            } else
                barChartArray.add(BarEntry(i.toFloat(), yVal))

            if (i >= 0) {
                if (allInstances[i].startTimestamp != endDateOfLastInst) {
                    val pauseBetween = getDurationString(
                        (allInstances[i].startTimestamp - endDateOfLastInst).toInt(),
                        TIME_FORMAT_PRETTY_APPROX_SHORT
                    )
                    // draw vertical line between this and last bar to indicate break
                    verticalLimitLines.add(getVerticalLimitLine(
                        i - 0.5f,
                        getString(R.string.statistics_paused_practicing_limitline_label, pauseBetween)
                    ))
                }
                endDateOfLastInst = allInstances[i].startTimestamp + allInstances[i].periodInSeconds
            }
        }

        return barChartArray
    }

    private fun isOneDayGoal() = goals[selectedGoal].goalDesc.periodUnit == GoalPeriodUnit.DAY &&
            goals[selectedGoal].goalDesc.periodInPeriodUnits ==  1

    /**
     * formats x axis value according to our time scaling
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(value: Float): String {
            // the library calls getFormattedValue very often, also with outdated indices from former
            // datasets, if the amount of entries has changed. This is strange behavior, so just ignore it
            if (value.toInt() >= goals[selectedGoal].goalInstances.size)
                return value.toString()

            // value is index of goalInstance, as set in BarEntry
            val inst = getGoalInstance(value.toInt())
//            return inst.id.toString()
            val timeStampStart = inst.startTimestamp
            val periodInSecs = inst.periodInSeconds
            // true if goal interval covers more than one [timeUnit]
            val multiIntervalGoal = goals[selectedGoal].goalDesc.periodInPeriodUnits > 1

            var labelString = ""

            when (goals[selectedGoal].goalDesc.periodUnit) {

                GoalPeriodUnit.DAY -> {
                    if (multiIntervalGoal) {
                        // show day of month or range of days in month as xTick
                        // always subtract 1 second to get the right day because of half-open approach
                        labelString ="${unixTimeToDayOfMonth(timeStampStart)} -" +
                                " ${unixTimeToDayOfMonth(timeStampStart + periodInSecs - 1)}"
                    } else {
                        labelString = unixTimeToWeekDay(timeStampStart)
                        if (value.toInt() == firstGoalInstShownIndex)
                            labelString += " (${unixTimeToDayOfMonth(timeStampStart)})"
                    }
                }

                GoalPeriodUnit.WEEK -> {
                    // always subtract 1 second to get the right day because of half-open approach
                    labelString = "${unixTimeToDayOfMonth(timeStampStart)} -" +
                            " ${unixTimeToDayOfMonth(timeStampStart + periodInSecs - 1)}"
                }

                GoalPeriodUnit.MONTH -> {
                    labelString = unixTimeToMonth(timeStampStart)
                    if (multiIntervalGoal)
                        labelString += " - ${unixTimeToMonth(timeStampStart + periodInSecs - 1)}"
                }
            }
            return labelString
        }

    }
    /**
     * For DateFormatter patterns see: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
     */

    private fun unixTimeToDayOfMonth(timestamp: Long): String {
        return epochSecondsToDate(timestamp)
            .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_OF_MONTH_PADDED))
    }

    private fun unixTimeToMonth(timestamp: Long): String {
        return epochSecondsToDate(timestamp)
            .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV))
    }

    private fun unixTimeToWeekDay(timestamp: Long): String {
        return epochSecondsToDate(timestamp)
            .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_WEEKDAY_ABBREV))
    }

    /** Interface helper method for getting goal instances. Returns a valid instance for index >= 0
     * For index < 0, returns a fake instance which can be used e.g. to calculate the time range of shown interval
     * **/
    private fun getGoalInstance(index: Int): GoalInstance {

        if (index >= 0) {
            return goals[selectedGoal].goalInstances[index]

        } else {    // return a fake GoalInstance with the correct time range / period

            // borrow some values from the first real instance
            val goalDesc = goals[selectedGoal].goalDesc
            val firstRealInstance = goals[selectedGoal].goalInstances[0]
            var periodInSeconds = firstRealInstance.periodInSeconds
            var startTime = 0L

            when (goals[selectedGoal].goalDesc.periodUnit) {
                GoalPeriodUnit.DAY -> {
                    // calculate plusDays with the API instead of adding periodInSeconds to respect daylight savings
                    startTime = epochSecondsToDate(firstRealInstance.startTimestamp)
                        .plusDays((index * goalDesc.periodInPeriodUnits).toLong())  // index is <0
                        .toEpochSecond()
                    // periodInSeconds doesn't change since it is always UTC

                } GoalPeriodUnit.WEEK -> {
                    startTime = epochSecondsToDate(firstRealInstance.startTimestamp)
                        .plusWeeks((index * goalDesc.periodInPeriodUnits).toLong())  // index is <0
                        .toEpochSecond()
                    // periodInSeconds doesn't change since it is always UTC

                } GoalPeriodUnit.MONTH -> {
                    startTime = epochSecondsToDate(firstRealInstance.startTimestamp)
                        .plusMonths((index * goalDesc.periodInPeriodUnits).toLong())  // index is <0
                        .toEpochSecond()

                    val endTime = epochSecondsToDate(firstRealInstance.startTimestamp)
                        .plusMonths(index.toLong() * goalDesc.periodInPeriodUnits + goalDesc.periodInPeriodUnits)
                        .toEpochSecond()
                    // adjust periodInSeconds for month length
                    periodInSeconds = (endTime - startTime).toInt()
                }
            }

            return GoalInstance(
                goalDescriptionId = firstRealInstance.goalDescriptionId,
                startTimestamp = startTime,
                periodInSeconds = periodInSeconds,
                target = firstRealInstance.target,
                progress = 0,
                renewed = true
            )
        }
    }


    /**
     * formats y axis value according to our time scaling
     */
    private inner class YAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(seconds: Float): String {
            return getDurationString(seconds.toInt(), TIME_FORMAT_HUMAN_PRETTY).toString()
        }
    }


    private inner class BarChartValueFormatter: ValueFormatter() {

        override fun getBarLabel(barEntry: BarEntry): String {
            barEntry.y.also { yVal ->
                return if (yVal == 0.001f) {  // we've encoded fake values with a value of 0.001f
                    "X"
                } else if (
                    barChart.highlighted != null &&                                 // there are highlighted values
                    barChart.highlighted.find { it.x == barEntry.x } != null) {     // barEntry is among the highlighted Values
                    // draw the time
                    getDurationString(yVal.toInt(), TIME_FORMAT_HUMAN_PRETTY).toString()
                } else {
                    // hide total time
                    ""
                }
            }
        }
    }


    private inner class GoalStatsAdapter : RecyclerView.Adapter<GoalStatsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val goalRadioButton: RadioButton = view.findViewById(R.id.radiobutton_goal)
            val goalTitleTv: TextView = view.findViewById(R.id.tv_goal_title_statistics)
            val goalDescTv: TextView = view.findViewById(R.id.tv_goal_desc_statistics)
            val tvNumSuccess: TextView = view.findViewById(R.id.tv_success_count)
            val tvNumFail: TextView = view.findViewById(R.id.tv_fail_count)
            val progressGoal: ProgressBar = view.findViewById(R.id.progressbar_goal_element)
            val llContainer: LinearLayout = view.findViewById(R.id.listitem_stats_goal_ll_container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalStatsAdapter.ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_statistics_goal, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: GoalStatsAdapter.ViewHolder, position: Int) {
            val elem = goals[position]

            val libraryItemColors = resources.getIntArray(R.array.library_item_colors)

            // adapt entry to selected state
            setGoalItemBackgroundColor(holder.llContainer, position, libraryItemColors)

            /** goal entry (radiobutton + progressbar) and set name depending on libraryItem */
            if (elem.libraryItem != null) {
                val catColor = ColorStateList.valueOf(libraryItemColors[elem.libraryItem.colorIndex])
                holder.goalTitleTv.text = elem.libraryItem.name
                holder.goalRadioButton.buttonTintList = catColor
                holder.progressGoal.progressTintList = catColor

            } else {
                holder.goalTitleTv.text = getString(R.string.goal_name_non_specific)
                holder.goalRadioButton.buttonTintList = ColorStateList.valueOf(PracticeTime.getThemeColor(R.attr.colorPrimary, this@GoalStatsActivity))
                holder.progressGoal.progressTintList = null
            }

            /** description of goal */
            val count = elem.goalDesc.periodInPeriodUnits
            val periodFormatted =
                when (elem.goalDesc.periodUnit) {
                    GoalPeriodUnit.DAY -> resources.getQuantityString(R.plurals.time_period_day, count, count)
                    GoalPeriodUnit.WEEK -> resources.getQuantityString(R.plurals.time_period_week, count, count)
                    GoalPeriodUnit.MONTH -> resources.getQuantityString(R.plurals.time_period_month, count, count)
                }

            /** ProgressBar + success/failure count */
            // TODO take target data from most recent Instance enough?
            holder.goalDescTv.text = TextUtils.concat(
                getDurationString(elem.goalInstances.last().target, TIME_FORMAT_HUMAN_PRETTY, SCALE_FACTOR_FOR_SMALL_TEXT),
                " ",
                periodFormatted
            )
            val succeeded = elem.goalInstances.filter { it.progress >= it.target }.size
            val failed = elem.goalInstances.filter { it.progress < it.target }.size

            holder.tvNumSuccess.text = succeeded.toString()
            holder.tvNumFail.text = failed.toString()

            holder.progressGoal.max = succeeded + failed
            holder.progressGoal.progress = succeeded

            /** onclicklistener for elements */
            val radioSelectListener = View.OnClickListener {
                val oldSel = selectedGoal
                intervalOffset = 0  // reset time range
                selectedGoal = holder.bindingAdapterPosition
                notifyItemChanged(oldSel)
                holder.goalRadioButton.isChecked = true

                setGoalItemBackgroundColor(holder.llContainer, position, libraryItemColors)

                val sv = findViewById<NestedScrollView>(R.id.scrollview_statistics)
                // only scroll to top if already more than 100px scrolled to prevent blocking the UI for scroll duration
                if (sv.scrollY > 100)
                    sv.smoothScrollTo(0,0, 600)
                updateChartData()
                setButtonEnabledState()
            }
            holder.llContainer.setOnClickListener(radioSelectListener)
            holder.goalRadioButton.setOnClickListener(radioSelectListener)

            holder.goalRadioButton.isChecked = position == selectedGoal
        }

        override fun getItemCount(): Int = goals.size

        /** set background tint of goal list item. Color will change depending on "selected" state of
         * element since we use a custom selector drawable for foreground
         * */
        private fun setGoalItemBackgroundColor(ll: LinearLayout, position: Int, catColors: IntArray){
            ll.apply {
                if (position != selectedGoal) {
                    isSelected = false
                    return
                }

                var color = ColorStateList.valueOf(
                    PracticeTime.getThemeColor(R.attr.colorPrimary, this@GoalStatsActivity))
                if (goals[position].libraryItem != null)
                    color = ColorStateList.valueOf(catColors[goals[position].libraryItem!!.colorIndex])

                backgroundTintList = color
                isSelected = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
