package de.practicetime.practicetime.ui.statistics

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.database.entities.GoalDescription
import de.practicetime.practicetime.database.entities.GoalInstance
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.updateGoals
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

private const val X_AXIS_LABEL_COUNT = 5

class GoalStatsActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var barChart: BarChart
    private lateinit var goalListAdapter: GoalStatsAdapter

    private val goals = ArrayList<GoalListElement>()
    private var selectedGoal = 0

    // the first Instance of the selected goal currently shown in graph
    private var firstGoalInstShownIndex = 0
    // the last Instance of the selected goal currently shown in graph
    // negative if no instances are shown in current time range
    private var lastGoalInstShownIndex = 0

    private var intervalOffset = 0

    data class GoalListElement(
        val goalInstances: List<GoalInstance>,
        val goalDesc: GoalDescription,
        val category: Category?,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        setSupportActionBar(findViewById(R.id.my_toolbar))
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
            updateGoals(PracticeTime.dao)    // update the goalInstances if they are outdated
            initGoalsList()
            // if no goals, do nothing. Should never happen normally because you
            // shouldn't be able to enter activity with zero goals
            if (goals.size > 0) {
                initBarChart()
                updateChartData()
                setButtonEnabledState()
            }
        }

        // disable pie chart button for goals
        findViewById<ImageButton>(R.id.btn_toggle_chart_type).visibility = View.GONE
        // disable time range switcher buttons
        findViewById<LinearLayout>(R.id.ll_statistics_toggle_timespan).visibility = View.GONE
    }

    private fun setButtonEnabledState() {
        findViewById<ImageButton>(R.id.btn_fwd).isEnabled = intervalOffset != 0
    }

    /** get the goals from the database */
    private suspend fun initGoalsList() {
        PracticeTime.dao.getGoalDescriptionsWithCategories().forEach { (desc, cat) ->
            goals.add(
                GoalStatsActivity.GoalListElement(
                    goalInstances = PracticeTime.dao.getGoalInstances(desc.id, from = 0L),
                    goalDesc = desc,
                    category = cat.firstOrNull()
                )
            )
        }
        goalListAdapter = GoalStatsAdapter()
        val layoutManager = LinearLayoutManager(this@GoalStatsActivity)

        val categoryRecyclerView = findViewById<RecyclerView>(R.id.recyclerview_statistics)
        categoryRecyclerView.layoutManager = layoutManager
        categoryRecyclerView.adapter = goalListAdapter
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
            labelCount = X_AXIS_LABEL_COUNT
            valueFormatter = XAxisValueFormatter()
            textColor = getThemeColor(R.attr.colorOnSurface)
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
            textColor = getThemeColor(R.attr.colorOnSurfaceLowerContrast)
            valueFormatter = YAxisValueFormatter()
            setDrawLimitLinesBehindData(true)
        }

        barChart.notifyDataSetChanged()
        barChart.invalidate()
    }

    private fun updateChartData() {
        // re-calculate bar data
        val barValues = getGoalsArray()

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
                highLightColor = getThemeColor(R.attr.colorOnSurface)
                highLightAlpha = 150    // 150 out of 255 (0=fully transparent)
            }

            val barData = BarData(dataSetBarChart)
            barData.apply {
                barWidth = 0.4f
                setValueFormatter(BarChartValueFormatter())
                setValueTextColor(getThemeColor(R.attr.colorOnSurfaceLowerContrast))
                setValueTextSize(12f)
                isHighlightEnabled = true
            }

            barChart.data = barData
        }

        barChart.apply {
            val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()
            val (max, cnt) = calculateAxisValues()

            axisLeft.axisMaximum = max
            axisRight.axisMaximum = max
            axisRight.setLabelCount(cnt, true)
            axisRight.removeAllLimitLines()
            axisRight.addLimitLine(getLimitLime(t))

            highlightValue(-1f, 0, false)   // remove all highlighting
            // highlight last value in the beginning
            highlightValue((goals[selectedGoal].goalInstances.size - 1).toFloat(), 0, false)

            // redraw the chart
            animateY(1000, Easing.EaseOutBack)
            notifyDataSetChanged()
            invalidate()
        }

        // update the Heading
        setHeadingTextViews()
    }

    private fun getLimitLime(
        limit: Float,
        labelPos: LimitLine.LimitLabelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
    ): LimitLine {
        return LimitLine(limit, secondsToTimeString(limit.toInt())).apply {
            lineWidth = 1f
            labelPosition = labelPos
            textSize = 10f
            enableDashedLine(10f, 10f, 0f)
            lineColor = getChartColor()
            textColor = getChartColor()
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
            var formatStrStart = "MMMM d"
            var formatStrEnd = "d"
            if (unixTimeToZonedDateTime(tmStart).month != unixTimeToZonedDateTime(tmEnd).month) {
                formatStrEnd = "MMM d"    // also show month if it is different from startMonth
                formatStrStart = "MMM d"
            }
            // START date
            val start = unixTimeToZonedDateTime(tmStart)
                .format(DateTimeFormatter.ofPattern(formatStrStart))
            val end = unixTimeToZonedDateTime(tmEnd)
                .format(DateTimeFormatter.ofPattern(formatStrEnd))

            // set the textView text as the final string
            tvRange.text = ("$start - $end")

        } else {
            // Monthly goals
            var formatStrStart = "MMMM"
            var formatStrEnd = "MMMM y"
            if (unixTimeToZonedDateTime(tmStart).year != unixTimeToZonedDateTime(tmEnd).year) {
                formatStrStart = "MMM y"
                formatStrEnd = "MMM y"
            }

            val start = unixTimeToZonedDateTime(tmStart)
                .format(DateTimeFormatter.ofPattern(formatStrStart))
            val end = unixTimeToZonedDateTime(tmEnd)
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
        return if(goals[selectedGoal].category != null) {
                val categoryColors = resources.getIntArray(R.array.category_colors).toCollection(mutableListOf())
                categoryColors[goals[selectedGoal].category!!.colorIndex]
            } else {
                getThemeColor(R.attr.colorPrimary)
            }
    }

    private fun seekPast() {
        intervalOffset -= X_AXIS_LABEL_COUNT
        updateChartData()
        setButtonEnabledState()
    }

    private fun seekFuture() {
        intervalOffset += X_AXIS_LABEL_COUNT
        updateChartData()
        setButtonEnabledState()
    }


    override fun onValueSelected(e: Entry?, h: Highlight?) {

        if (e == null)
            return

        barChart.axisRight.apply {
            val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()
            removeAllLimitLines()
            if (e.x == barChart.data.getDataSetByIndex(0).xMax &&
                    e.y > 0.8 * t && e.y < 1.2 * t)     // only change if bar is ca. same height as LimitLine
                addLimitLine(getLimitLime(t, LimitLine.LimitLabelPosition.LEFT_TOP))
            else
                addLimitLine(getLimitLime(t))
        }
    }

    override fun onNothingSelected() {
        barChart.axisRight.apply {
            val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()
            removeAllLimitLines()
            addLimitLine(getLimitLime(t))
        }
    }

    /** gets the ArrayList for the BarChart object out of goals ArrayList */
    private fun getGoalsArray(): ArrayList<BarEntry> {
        val barChartArray = arrayListOf<BarEntry>()

        val allInstances = goals[selectedGoal].goalInstances
        // get X_AXIS_LABEL_COUNT instances from the current interval.
        // intervalOffset == 0 means last X_AXIS_LABEL_COUNT
        // intervalOffset == -1 means get the X_AXIS_LABEL_COUNT before the last X_AXIS_LABEL_COUNT instances etc.
        lastGoalInstShownIndex = allInstances.size-1 + intervalOffset
        firstGoalInstShownIndex = lastGoalInstShownIndex - X_AXIS_LABEL_COUNT + 1 //add 1 because we want exactly X_AXIS_LABEL_COUNT bars

        for (i in firstGoalInstShownIndex..lastGoalInstShownIndex) {
            val yVal =
                if(i < 0) 0.001f    // all instances exhausted. Make 0.001f to recognize in the ValueFormatter that it is fake
                else allInstances[i].progress.toFloat()

            if (i >= 0 && yVal >= allInstances[i].target)
                barChartArray.add(BarEntry(i.toFloat(), yVal, ContextCompat.getDrawable(this, R.drawable.ic_check_small)))
            else
                barChartArray.add(BarEntry(i.toFloat(), yVal))
        }

        return barChartArray
    }

    /**
     * formats x axis value according to our time scaling
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(value: Float): String {

            // value is index of goalInstance, as set in BarEntry
            val inst = getGoalInstance(value.toInt())
            val timeStampStart = inst.startTimestamp
            val periodInSecs = inst.periodInSeconds
            // true if goal interval covers more than one [timeUnit]
            val multiIntervalGoal = goals[selectedGoal].goalDesc.periodInPeriodUnits > 1

            var labelString = ""

            when (goals[selectedGoal].goalDesc.periodUnit) {

                GoalPeriodUnit.DAY -> {
                    // show day of month or range of days in month as xTick
                    labelString = unixTimeToDayOfMonth(timeStampStart)
                    if (multiIntervalGoal)
                        // always subtract 1 second to get the right day because of half-open approach
                        labelString += " - ${unixTimeToDayOfMonth(timeStampStart + periodInSecs - 1)}"
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

    private fun unixTimeToZonedDateTime(timestamp: Long): ZonedDateTime {
        return ZonedDateTime
            .ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
    }

    private fun unixTimeToDayOfMonth(timestamp: Long): String {
        return unixTimeToZonedDateTime(timestamp)
            .format(DateTimeFormatter.ofPattern("dd"))
    }

    private fun unixTimeToMonth(timestamp: Long): String {
        return unixTimeToZonedDateTime(timestamp)
            .format(DateTimeFormatter.ofPattern("MMM"))
    }

    /** Interface helper method for getting goal instances. Returns a valid instance for index >= 0
     * For index < 0, returns a fake instance which can be used e.g. to calculate the time range of shown interval
     * **/
    private fun getGoalInstance(index: Int): GoalInstance {

        if (index >= 0) {
            // return the real instance
            return goals[selectedGoal].goalInstances[index]

        } else {    // return a fake GoalInstance with the correct time range / period

            // borrow some values from the first instance
            val goalDesc = goals[selectedGoal].goalDesc
            val firstInstance = goals[selectedGoal].goalInstances[0]
            var periodInSeconds = firstInstance.periodInSeconds
            var startTime = 0L

            when (goals[selectedGoal].goalDesc.periodUnit) {
                GoalPeriodUnit.DAY -> {
                    // calculate plusDays with the API instead of adding periodInSeconds to respect daylight savings
                    startTime = unixTimeToZonedDateTime(firstInstance.startTimestamp)
                        .plusDays((index * goalDesc.periodInPeriodUnits).toLong())  // index is <0
                        .toEpochSecond()
                    // periodInSeconds doesn't change since it is always UTC

                } GoalPeriodUnit.WEEK -> {
                    startTime = unixTimeToZonedDateTime(firstInstance.startTimestamp)
                        .plusWeeks((index * goalDesc.periodInPeriodUnits).toLong())  // index is <0
                        .toEpochSecond()
                    // periodInSeconds doesn't change since it is always UTC

                } GoalPeriodUnit.MONTH -> {
                    startTime = unixTimeToZonedDateTime(firstInstance.startTimestamp)
                        .plusMonths((index * goalDesc.periodInPeriodUnits).toLong())  // index is <0
                        .toEpochSecond()

                    val endTime = unixTimeToZonedDateTime(firstInstance.startTimestamp)
                        .plusMonths(index.toLong() * goalDesc.periodInPeriodUnits + goalDesc.periodInPeriodUnits)
                        .toEpochSecond()
                    // adjust periodInSeconds for month length
                    periodInSeconds = (endTime - startTime).toInt()
                }
            }

            return GoalInstance(
                goalDescriptionId = firstInstance.goalDescriptionId,
                startTimestamp = startTime,
                periodInSeconds = periodInSeconds,
                target = firstInstance.target,
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
            return secondsToTimeString(seconds.toInt())
        }
    }


    private inner class BarChartValueFormatter: ValueFormatter() {

        override fun getBarLabel(barEntry: BarEntry): String {
            barEntry.y.also { yVal ->
                if (yVal == 0.001f) {  // we've encoded fake values with a value of 0.001f
                    return "X"
                } else if (
                    barChart.highlighted != null &&                                 // there are highlighted values
                    barChart.highlighted.find { it.x == barEntry.x } != null) {     // barEntry is among the highlighted Values
                        // draw the time
                        return secondsToTimeString(yVal.toInt())
                } else {
                    // hide total time
                    return ""
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
            val cardViewWrapper: CardView = view.findViewById(R.id.cardview_goal_statistics)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalStatsAdapter.ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_statistics_goal_list_item, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: GoalStatsAdapter.ViewHolder, position: Int) {
            val elem = goals[position]

            val categoryColors = resources.getIntArray(R.array.category_colors)
            if (elem.category != null) {
                val catColor = ColorStateList.valueOf(categoryColors[elem.category.colorIndex])
                holder.goalTitleTv.text = elem.category.name
                holder.goalRadioButton.buttonTintList = catColor
                holder.progressGoal.progressTintList = catColor
            } else {
                holder.goalTitleTv.text = getString(R.string.goal_name_non_specific)
                holder.goalRadioButton.buttonTintList = ColorStateList.valueOf(getThemeColor(R.attr.colorPrimary))
                holder.progressGoal.progressTintList = null
            }

            // TODO take data from most recent Instance enough?
            val targetHours = elem.goalInstances.last().target / 3600
            val targetMinutes = elem.goalInstances.last().target % 3600 / 60

            // Following copy pasted from GoalAdapter:
            var targetHoursString = ""
            var targetMinutesString = ""
            if (targetHours > 0) targetHoursString = "${targetHours}h "
            if (targetMinutes > 0) targetMinutesString = "${targetMinutes}min "

            val periodFormatted =
                if (elem.goalDesc.periodInPeriodUnits > 1) {  // plural
                    when (elem.goalDesc.periodUnit) {
                        GoalPeriodUnit.DAY -> getString(R.string.goal_description_days, elem.goalDesc.periodInPeriodUnits)
                        GoalPeriodUnit.WEEK -> getString(R.string.goal_description_weeks, elem.goalDesc.periodInPeriodUnits)
                        GoalPeriodUnit.MONTH -> getString(R.string.goal_description_months, elem.goalDesc.periodInPeriodUnits)
                    }
                } else {    // singular
                    when (elem.goalDesc.periodUnit) {
                        GoalPeriodUnit.DAY -> getString(R.string.goal_description_day)
                        GoalPeriodUnit.WEEK -> getString(R.string.goal_description_week)
                        GoalPeriodUnit.MONTH -> getString(R.string.goal_description_month)
                    }
                }

            holder.goalDescTv.text = getString(
                R.string.goal_description_complete,
                targetHoursString,
                targetMinutesString,
                periodFormatted
            )

            val succeeded = elem.goalInstances.filter { it.progress >= it.target }.size
            val failed = elem.goalInstances.filter { it.progress < it.target }.size

            holder.tvNumSuccess.text = succeeded.toString()
            holder.tvNumFail.text = failed.toString()

            holder.progressGoal.max = succeeded + failed
            holder.progressGoal.progress = succeeded

            val radioSelectListener = View.OnClickListener {
                val oldSel = selectedGoal
                intervalOffset = 0  // reset time range
                selectedGoal = holder.bindingAdapterPosition
                notifyItemChanged(oldSel)
                holder.goalRadioButton.isChecked = true
                val sv = findViewById<NestedScrollView>(R.id.scrollview_statistics)
                // only scroll to top if already more than 100px scrolled to prevent blocking the UI for scroll duration
                if (sv.scrollY > 100)
                    sv.smoothScrollTo(0,0, 600)
                updateChartData()
                setButtonEnabledState()
            }
            holder.cardViewWrapper.setOnClickListener(radioSelectListener)
            holder.goalRadioButton.setOnClickListener(radioSelectListener)

            holder.goalRadioButton.isChecked = position == selectedGoal
        }

        override fun getItemCount(): Int = goals.size
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
        // TODO uncomment for production
        val hours = seconds?.div(3600)
        val minutes = (seconds?.rem(3600))?.div(60)

        // FAKE values:
//        val hours = (seconds?.rem(3600))?.div(60)
//        val minutes = seconds?.rem(60)

        return Pair(hours, minutes)
    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }

    private fun log(msg: String) {
        Log.d("GOALS", msg)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}