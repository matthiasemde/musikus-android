package de.practicetime.practicetime

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
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.GoalDescription
import de.practicetime.practicetime.entities.GoalInstance
import de.practicetime.practicetime.entities.GoalPeriodUnit
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.max

private const val X_AXIS_LABEL_COUNT = 5

class GoalStatsActivity : AppCompatActivity() {

    private lateinit var dao: PTDao
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

        // get the dao object
        openDatabase()

        findViewById<ImageButton>(R.id.btn_rwnd).setOnClickListener {
            seekPast()
        }
        findViewById<ImageButton>(R.id.btn_fwd).setOnClickListener {
            seekFuture()
        }

        lifecycleScope.launch {
//            updateGoals(dao, lifecycleScope)  // TODO how to integrate properly to wait for finish?
            initGoalsList()
            initBarChart()
            updateChartData()
            setButtonEnabledState()
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
        dao.getGoalDescriptionsWithCategories().forEach { (desc, cat) ->
            goals.add(
                GoalListElement(
                    goalInstances = dao.getGoalInstances(desc.id, from = 0L),
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
            setTouchEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)
            notifyDataSetChanged()
            invalidate()
        }

        // axis settings
        barChart.axisLeft.apply {
            axisMinimum = 0f   // needed for y axis scaling because chart depends on left axis (even if disabled)
            isEnabled = false
        }

        barChart.axisRight.apply {
            axisMinimum = 0f
            setDrawAxisLine(false)
            textColor = getThemeColor(R.attr.colorOnSurfaceLowerContrast)
            valueFormatter = YAxisValueFormatter()
            setDrawLimitLinesBehindData(true)
        }

        barChart.xAxis.apply {
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = X_AXIS_LABEL_COUNT
            valueFormatter = XAxisValueFormatter()
            textColor = getThemeColor(R.attr.colorOnSurface)
        }

    }

    private fun getLimitLime(limit: Float): LimitLine {
        // TODO string resource
        return LimitLine(limit, secondsToTimeString(limit.toInt())).apply {
            lineWidth = 1f
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 10f
            enableDashedLine(10f, 10f, 0f)
            lineColor = getChartColor()
            textColor = getChartColor()
        }
    }


    private fun updateChartData() {
        // re-calculate bar data
        val barValues = getGoalsArray()

        val dataSetBarChart: BarDataSet
        if (barChart.data != null && barChart.data.dataSetCount > 0) {
            // update chart data
            dataSetBarChart = barChart.data.getDataSetByIndex(0) as BarDataSet
            dataSetBarChart.values = barValues
            dataSetBarChart.color = getChartColor()
            barChart.data.notifyDataChanged()
            barChart.notifyDataSetChanged()

        } else {
            // first time drawing chart, create the DataSet from values
            dataSetBarChart = BarDataSet(barValues, "Label")
            dataSetBarChart.apply {
                setDrawValues(true)
                color = getChartColor()
                dataSetBarChart.highLightColor = getThemeColor(R.attr.colorOnSurface)
                dataSetBarChart.highLightAlpha = 150
            }
            val barData = BarData(dataSetBarChart)
            barData.apply {
                barWidth = 0.4f
                setValueFormatter(BarChartValueFormatter())
                setValueTextColor(getThemeColor(R.attr.colorOnSurfaceLowerContrast))
                setValueTextSize(12f)
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
            highlightValue((goals[selectedGoal].goalInstances.size - 1).toFloat(), 0, false)

            // redraw the chart
            animateY(1000, Easing.EaseOutBack)
            notifyDataSetChanged()
            invalidate()
        }

        // update the Heading
        setHeadingTextViews()

    }

    /**
     * Function to calculate the y Axis label values which should be drawn.
     * makes sure all values are 15min, 30min or full hours intervals
     */
    private fun calculateAxisValues(): Pair<Float, Int> {
        val t = getGoalInstance(lastGoalInstShownIndex).target.toFloat()
        val maximumRequired = max(t * 1.1f, barChart.yMax) // determine maximum value shown

        val interval = when {
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

        when(goals[selectedGoal].goalDesc.periodUnit) {

            GoalPeriodUnit.DAY -> {
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
            }

            GoalPeriodUnit.WEEK -> {
                var formatStr = "w"
                if (unixTimeToZonedDateTime(tmStart).year != unixTimeToZonedDateTime(tmEnd).year)
                    formatStr = "w (y)"

                val start = unixTimeToZonedDateTime(tmStart)
                    .format(DateTimeFormatter.ofPattern(formatStr, Locale.UK))
                val end = unixTimeToZonedDateTime(tmEnd)
                    .format(DateTimeFormatter.ofPattern("w (y)", Locale.UK))

                // TODO string resources
                tvRange.text = "Week $start - $end"
            }

            GoalPeriodUnit.MONTH -> {
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
        }

        // TODO string resources
        if (lastGoalInstShownIndex < 0)
            tvSecondHeader.text = "No records for current goal"
        else {
            val succeeded = goals[selectedGoal].goalInstances
                .subList(max(firstGoalInstShownIndex, 0), lastGoalInstShownIndex+1) // +1 because lastindex is exclusive
                .filter { it.progress >= it.target }
                .size

            val failed = goals[selectedGoal].goalInstances
                .subList(max(firstGoalInstShownIndex, 0), lastGoalInstShownIndex+1)
                .filter { it.progress < it.target }
                .size

            tvSecondHeader.text = "Succeeded $succeeded out of ${failed+succeeded}"
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
                if(i < 0) 0.001f    // all instances exhausted. Make 0.001f to recognize that it is fake in the Valueformatter
                else allInstances[i].progress.toFloat()

            barChartArray.add(BarEntry(i.toFloat(), yVal))
        }

        return barChartArray
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            this,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
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
                    // show calender week index/indices as xTick
                    labelString = unixTimeToWeekOfYear(timeStampStart)
                    if (multiIntervalGoal)
                        labelString += " - ${unixTimeToWeekOfYear(timeStampStart + periodInSecs - 1)}"
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

    private fun unixTimeToWeekOfYear(timestamp: Long): String {
        // Week number for Sundays is determined by week start day: https://stackoverflow.com/a/52950623
        // which is again dependent on Locale passed to DateTimeFormatter.ofPattern()
        // Locale.getDefault() results in week start at Sunday if Phone *Language* (!) is set to English (US)
        // since we always want the week to start on Mondays here in this case, hardcode it to Locale.UK
        return unixTimeToZonedDateTime(timestamp)
            .format(DateTimeFormatter.ofPattern("w", Locale.UK))
    }

    private fun unixTimeToMonth(timestamp: Long): String {
        return unixTimeToZonedDateTime(timestamp)
            .format(DateTimeFormatter.ofPattern("MMM"))
    }

    /** Interface helper method for getting goal instances. Returns a valid instance for index >= 0
     * For index < 0, returns a fake instance which can be used e.g. to calculate the time range of shown interval
     * **/
    // TODO this calculation does NOT consider daylight savings!
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
//            val (h, m) = secondsToHoursMins(seconds.toInt())
//            val str = if (h != 0){
//                "${h}." +
//                "${m?.toFloat()?.div(60f)?.times(10)?.roundToInt()} h"   //TODO this is super ugly
//            } else{
//                "${m} m"
//            }
            return secondsToTimeString(seconds.toInt())
        }
    }

    /**
     * This ValueFormatter shows the sum of all stacked Bars on top of the each bar instead of for every segment
     * It should do the same as StackedValueFormatter but this one doesn't work for our case because we have
     * "invisible" stacked segments with value=0 in our bars which results to no call in getBarStackedLabel() and thus
     * the sum doesn't get drawn. This one only uses non-zero entries to determine the top position
     */
    private inner class BarChartValueFormatter: ValueFormatter() {

        var lastEntryX = -1f            // the x entry (=Bar ID) of last time getBarStackedLabel() was called
        var stackCounterCurrentBar = 0  // counter variable counting the stack we are inside the current bar
        var stackEntriesNotZero = 0     // amount of non-zero entries in this bar

        // getBarStackedLabel is called on every bar for every non-zero segment
        // value: the y value of current segment
        // stackedEntry: the whole BarEntry object for current bar, always the same for each stack on the same Bar
        override fun getBarStackedLabel(value: Float, stackedEntry: BarEntry?): String {
            if (stackedEntry?.x != lastEntryX) {
                lastEntryX = stackedEntry?.x ?: -1f
                // first stack on a new bar
                stackCounterCurrentBar = 1
                stackEntriesNotZero = stackedEntry?.yVals?.filterNot { it == 0f }?.count() ?: 0

                // show 0 if there are no stacks in this bar
                if (stackEntriesNotZero == 0)
                    return "0"

                // show value if there is only 1 stack in this bar
                if (stackEntriesNotZero == 1) {
                    return secondsToTimeString(stackedEntry?.yVals?.sum()?.toInt())
                }
            } else {
                lastEntryX = stackedEntry.x
                stackCounterCurrentBar++
                if (stackCounterCurrentBar == stackEntriesNotZero) {
                    // we reached the last non-zero stack of the bar, so we're at the top
                    return secondsToTimeString(stackedEntry.yVals?.sum()?.toInt())
                }
            }
            return ""   // return empty string so no value is drawn
        }

        override fun getBarLabel(barEntry: BarEntry): String {
            barEntry.y.also {
                return if (it == 0.001f) {  // we've encoded fake values with a value of 0.001f
                    "X"
                } else
                    secondsToTimeString(it.toInt())
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
}