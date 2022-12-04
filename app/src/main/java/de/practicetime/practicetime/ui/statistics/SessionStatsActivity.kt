/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui.statistics

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.tabs.TabLayout
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

class SessionStatsActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var libraryItemListAdapter: LibraryItemStatsAdapter
    private var daysViewWeekOffset = 0L
    private var weeksViewWeekOffset = 0L
    private var monthsViewMonthOffset = 0L
    private var colorAmount = 0

    private val libraryItems = ArrayList<LibraryItemListElement>()

    data class LibraryItemListElement(
        val libraryItem: LibraryItem,
        var totalDuration: Int = 0,
        var selected: Boolean,
        var visible: Boolean,
    )

    private enum class VIEWS(val barCount: Int) {
        DAYS_VIEW(7),   // be careful to change because 7 means Mon-Sun here!
        WEEKS_VIEW(7),  // current week + last 6 weeks
        MONTHS_VIEW(7), // current month + last 6 months
    }
    private var activeView = VIEWS.DAYS_VIEW

    companion object {
        const val BAR_CHART = 0
        const val PIE_CHART = 1
    }
    private var chartType = BAR_CHART   // current chart type to display


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        setSupportActionBar(findViewById(R.id.stats_session_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.session_statistics)
        }

        initTabs()

        findViewById<ImageButton>(R.id.btn_rwnd).setOnClickListener {
            seekPast()
        }
        findViewById<ImageButton>(R.id.btn_fwd).setOnClickListener {
            seekFuture()
        }

        colorAmount = resources?.getIntArray(R.array.library_item_colors)?.toCollection(mutableListOf())?.size ?: 0
        initBarChart()
        initPieChart()
        initLibraryItemList()

        updateChartData()
        setBtnEnabledState()
    }

    private fun initTabs() {
        findViewById<TabLayout>(R.id.statistics_tablayout).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    0 -> {  // days view
                        activeView = VIEWS.DAYS_VIEW
                        updateChartData()
                        setBtnEnabledState()
                        // reset time ranges for other views
                        weeksViewWeekOffset = 0L
                        monthsViewMonthOffset = 0L
                    }
                    1 -> { // week view
                        activeView = VIEWS.WEEKS_VIEW
                        updateChartData()
                        setBtnEnabledState()
                        // reset time ranges for other views
                        daysViewWeekOffset = 0L
                        monthsViewMonthOffset = 0L
                    }
                    2 -> {  //months view
                        activeView = VIEWS.MONTHS_VIEW
                        updateChartData()
                        setBtnEnabledState()
                        // reset time ranges for other views
                        daysViewWeekOffset = 0L
                        weeksViewWeekOffset = 0L
                    }
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
    }

    // needed because we use setSupportActionBar() https://stackoverflow.com/a/57978912
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.stats_session_toolbar_menu_base, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.statsToolbarChartToggle -> {
                when (chartType) {
                    BAR_CHART -> {
                        chartType = PIE_CHART
                        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_bar_chart)
                    }
                    PIE_CHART -> {
                        chartType = BAR_CHART
                        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_pie_chart)
                    }
                    else -> {
                        chartType = BAR_CHART
                        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_pie_chart)
                    }
                }
                updateChartData(false)
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    /** initialize the checkbox list with the libraryItems */
    private fun initLibraryItemList() {
        lifecycleScope.launch {
            PTDatabase.getInstance(applicationContext).libraryItemDao.getAll().forEach {
                libraryItems.add(
                    LibraryItemListElement(
                        it,
                        selected = true,
                        visible = false
                    )
                )
            }
            libraryItemListAdapter = LibraryItemStatsAdapter()

            val libraryItemRecyclerView = findViewById<RecyclerView>(R.id.recyclerview_statistics)
            libraryItemRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@SessionStatsActivity)
                adapter = libraryItemListAdapter
            }
        }
    }

    /** initialize the bar chart view object */
    private fun initBarChart() {
        // for rounded bars: https://gist.github.com/xanscale/e971cc4f2f0712a8a3bcc35e85325c27
        //      issue: https://github.com/PhilJay/MPAndroidChart/issues/2771#issuecomment-566719474

        barChart = findViewById(R.id.bar_chart)
        barChart.apply {
            isDragEnabled = false
            isDoubleTapToZoomEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setOnChartValueSelectedListener(this@SessionStatsActivity)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)
            isHighlightFullBarEnabled = true
        }

        // x axis
        barChart.xAxis.apply {
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = activeView.barCount
            valueFormatter = XAxisValueFormatter()
            textColor = PracticeTime.getThemeColor(R.attr.colorOnSurface, this@SessionStatsActivity)
        }

        // left axis
        barChart.axisLeft.apply {
            axisMinimum = 0f   // needed for y axis scaling (probably a bug!)
            isEnabled = false
        }

        // right axis
        barChart.axisRight.apply {
            axisMinimum = 0f
            setDrawAxisLine(false)
            textColor = PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, this@SessionStatsActivity)
            valueFormatter = YAxisValueFormatter()
        }

        barChart.notifyDataSetChanged()
        barChart.invalidate()
    }

    /** initialize the (hidden) Pie chart view object */
    private fun initPieChart() {
        pieChart = findViewById(R.id.pie_chart)
        pieChart.apply {
            setDrawEntryLabels(false)
            isHighlightPerTapEnabled = false

            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            isRotationEnabled = false

            maxAngle = 180f // HALF CHART
            rotationAngle = 180f

            isDrawHoleEnabled = true
            holeRadius = 58f
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)    // disable transparent circle

//            setEntryLabelColor(getThemeColor(R.attr.colorOnSurfaceLowerContrast))
//            setEntryLabelTextSize(9f)

//            // add space at the top for the labels
//            setExtraOffsets(0f, 20f, 0f, 0f);

            setDrawCenterText(false)
//            setCenterTextOffset(0f, -20f)

            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        Log.d("TAG", "onValueSelected")
        if (e == null)
            return
    }

    override fun onNothingSelected() {}

    /** sets the heading above the chart for the time range */
    private fun setHeadingTextViews() {
        val tvRange = findViewById<TextView>(R.id.tv_chart_header)
        val tvTotalTimeInRange = findViewById<TextView>(R.id.tv_secondary_chart_header)

        val chartArray = barChart.data.getDataSetByIndex(0) as BarDataSet
        // because we're always counting down in the loops, xVals are chronologically reversed
        val firstBarXVal = chartArray.xMin
        val lastBarXVal = chartArray.xMax
        when(activeView) {

            VIEWS.DAYS_VIEW -> {
                var formatStrStart = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL $DATE_FORMATTER_PATTERN_DAY_OF_MONTH"
                var formatStrEnd = DATE_FORMATTER_PATTERN_DAY_OF_MONTH

                if (getStartOfWeek(daysViewWeekOffset).month != getEndOfWeek(daysViewWeekOffset).minusDays(1).month) {
                    formatStrStart = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV $DATE_FORMATTER_PATTERN_DAY_OF_MONTH"
                    formatStrEnd = formatStrStart    // also show month if it is different from startMonth
                }

                val start = getStartOfWeek(daysViewWeekOffset)
                    .format(DateTimeFormatter.ofPattern(formatStrStart))

                val end = getEndOfWeek(daysViewWeekOffset)
                    .minusDays(1)   // subtract one day because of half-open approach
                    .format(DateTimeFormatter.ofPattern(formatStrEnd))

                tvRange.text = ("$start - $end")
            }

            VIEWS.WEEKS_VIEW -> {
                val formatStr = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV $DATE_FORMATTER_PATTERN_DAY_OF_MONTH"

                val start = getStartOfWeek(firstBarXVal.toLong())
                    .format(DateTimeFormatter.ofPattern(formatStr))

                val end = getEndOfWeek(lastBarXVal.toLong())
                    .minusDays(1)  // subtract one day because of half-open approach
                    .format(DateTimeFormatter.ofPattern(formatStr))

                tvRange.text = ("$start - $end")
            }

            VIEWS.MONTHS_VIEW -> {
                val startDate = getStartOfMonth(firstBarXVal.toLong())

                val endDate = getEndOfMonth(lastBarXVal.toLong())
                    .minusDays(1)   // subtract one day because of half-open approach

                var formatStrStart = DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL
                var formatStrEnd = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL $DATE_FORMATTER_PATTERN_YEAR"
                if(startDate.year != endDate.year) {
                    formatStrStart = "$DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV $DATE_FORMATTER_PATTERN_YEAR"
                    formatStrEnd = formatStrStart
                }

                val start = startDate.format(DateTimeFormatter.ofPattern(formatStrStart))
                val end = endDate.format(DateTimeFormatter.ofPattern(formatStrEnd))
                tvRange.text = ("$start - $end")
            }
        }
        // show sum of visible data
        val durationStr = getDurationString(
            chartArray.values.sumOf {
                it.yVals.sum().toInt()
            },
            TIME_FORMAT_HUMAN_PRETTY
        )
        tvTotalTimeInRange.text = TextUtils.concat(
            getString(R.string.total_time),
            ": ",
            durationStr)
    }

    /** toggles the states of the "days"/"month"/"week" chooser buttons*/
    private fun setBtnEnabledState() {
        val btnFwd = findViewById<ImageButton>(R.id.btn_fwd)

        when(activeView) {
            VIEWS.DAYS_VIEW -> {
                btnFwd.isEnabled = daysViewWeekOffset != 0L
            }
            VIEWS.WEEKS_VIEW -> {
                btnFwd.isEnabled = weeksViewWeekOffset != 0L
            }
            VIEWS.MONTHS_VIEW -> {
                btnFwd.isEnabled = monthsViewMonthOffset != 0L
            }
        }
    }

    /** called whenever the chart has to update since the user requests other data (e.g. time range) */
    private fun updateChartData(recalculateDurs: Boolean = true) {
        lifecycleScope.launch {
            // re-calculate bar data
            val (barValues, pieValues) = when (activeView) {
                VIEWS.DAYS_VIEW -> getMoToFrArray()
                VIEWS.WEEKS_VIEW -> getWeeksArray()
                VIEWS.MONTHS_VIEW -> getMonthsArray()
            }

            val dataSetBarChart: BarDataSet
            val dataSetPieChart: PieDataSet

            if (barChart.data != null && barChart.data.dataSetCount > 0) {
                dataSetBarChart = barChart.data.getDataSetByIndex(0) as BarDataSet
                dataSetBarChart.apply {
                    values = barValues
                }
                barChart.data.notifyDataChanged()
                barChart.notifyDataSetChanged()

                dataSetPieChart = pieChart.data.getDataSetByIndex(0) as PieDataSet
                dataSetPieChart.values = pieValues
                pieChart.data.notifyDataChanged()
                pieChart.notifyDataSetChanged()

            } else {
                // first time drawing chart, create the DataSet from values
                dataSetBarChart = BarDataSet(barValues, "Label")
                dataSetPieChart = PieDataSet(pieValues, "Label")

                val libraryItemColors = resources?.getIntArray(R.array.library_item_colors)
                    ?.toCollection(mutableListOf())
                dataSetBarChart.apply {
                    colors = libraryItemColors
                    setDrawValues(true)
                    highLightColor = PracticeTime.getThemeColor(R.attr.colorOnSurface, this@SessionStatsActivity)
                    highLightAlpha = 80    // 150 out of 255 (0=fully transparent)
                }
                dataSetPieChart.apply {
                    colors = libraryItemColors
                    setDrawValues(true)
                    sliceSpace = 3f

                    // Disabled value lines:
//                    yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
//                    //setSelectionShift(0f);
//                    valueLinePart1OffsetPercentage = 70f    // start of value line in % from center of chart
//                    valueLinePart1Length = 0.6f // lenght of "outgoing" line
//                    valueLinePart2Length = 0.4f // length of horizonal line
                }

                val barData = BarData(dataSetBarChart)
                barData.apply {
                    barWidth = 0.4f
                    setValueFormatter(BarChartValueFormatter())
                    setValueTextColor(PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, this@SessionStatsActivity))
                    setValueTextSize(12f)
                    isHighlightEnabled = true
                }

                barChart.data = barData


                val pieData = PieData(dataSetPieChart)
                pieData.apply {
                    setValueFormatter(PieChartValueFormatter())
                    setValueTextSize(11f)
                    setValueTextColor(PracticeTime.getThemeColor(R.attr.colorOnSurface, this@SessionStatsActivity))
                }
                pieChart.data = pieData
            }

            if(chartType == BAR_CHART) {
                pieChart.visibility = View.GONE
                barChart.visibility = View.VISIBLE
                highlightCurrentInterval()
            } else {
                pieChart.visibility = View.VISIBLE
                barChart.visibility = View.GONE
            }

            val (max, cnt) = calculateAxisValues()
            barChart.apply {
                axisRight.axisMaximum = max
                axisRight.setLabelCount(cnt, true)
                axisLeft.axisMaximum = max
                setFitBars(true)

                // redraw the chart
                animateY(1000, Easing.EaseOutBack)
                notifyDataSetChanged()
                invalidate()
            }

            pieChart.apply {
                animateY(1400, Easing.EaseInOutQuad)
                notifyDataSetChanged()
                invalidate()
            }

            // update the Heading
            setHeadingTextViews()

            // don't recalculate the total durations for each libraryItem if explicitly told so to prevent flashing
            if (recalculateDurs)
                libraryItemListAdapter.notifyItemRangeChanged(0, libraryItems.filter { it.visible }.size)

            delay(50)  // TODO this is needed in order to make movePieChart work (halfway) the first time. Don't know why. Still buggy tho
            movePieChart()
        }
    }

    private fun highlightCurrentInterval() {

        val shouldDisplay = when(activeView) {
            VIEWS.DAYS_VIEW -> daysViewWeekOffset == 0L
            VIEWS.WEEKS_VIEW -> weeksViewWeekOffset == 0L
            VIEWS.MONTHS_VIEW -> monthsViewMonthOffset == 0L
        }

        barChart.highlightValue(-1f, 0, false)   // remove all highlighting
        if (shouldDisplay) {

            val highlightXValue =
                if (activeView == VIEWS.DAYS_VIEW) {
                    getCurrentDayIndexOfWeek().toFloat()
                } else {
                    barChart.barData.xMax
                }
            barChart.highlightValue(highlightXValue, 0, false)
            barChart.invalidate()
        }
    }

    private fun movePieChart() {
        val p = (pieChart.layoutParams as LinearLayout.LayoutParams)
//        Log.d("TAG", "pieChart margins: lef:${p.leftMargin} right:${p.rightMargin} top:${p.topMargin} bot: ${p.bottomMargin}")

        // reset all margins
        p.setMargins(p.leftMargin, 0, p.rightMargin, 0)
        pieChart.layoutParams = p

        // re-set top margin to shift it down
        val height = ((pieChart as View).measuredHeight * 0.45).toInt()

        // we want to enlarge the pie chart (since its vertical space is too small) therefore, enlarge the
        // vertical space by adding `height` pixels at the bottom but only reducing `height*0.3` pixels at the top
        p.setMargins(
            p.leftMargin,
            p.topMargin + (height*0.5).toInt(),
            p.rightMargin,
            p.bottomMargin - height
        )
        pieChart.layoutParams = p
    }

    /**
     * Function to calculate the y Axis label values which should be drawn.
     * makes sure all values are 15min, 30min or full hours intervals
     */
    private fun calculateAxisValues(): Pair<Float, Int> {
        val maximumRequired = barChart.yMax * 1.1f // determine maximum value shown, let 10% margin on top for value

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

    /** "<" button to seek into data more in the past */
    private fun seekPast() {
        when(activeView) {
            VIEWS.DAYS_VIEW ->
                daysViewWeekOffset--    // special treatment for weekview because we always want Mo-Sun range
            VIEWS.WEEKS_VIEW ->
                weeksViewWeekOffset -= activeView.barCount
            VIEWS.MONTHS_VIEW ->
                monthsViewMonthOffset -= activeView.barCount
        }
        updateChartData()
        setBtnEnabledState()
    }

    /** ">" button to seek into data less in the past */
    private fun seekFuture() {
        when(activeView) {
            VIEWS.DAYS_VIEW ->
                daysViewWeekOffset++    // special treatment for weekview because we always want Mo-Sun range
            VIEWS.WEEKS_VIEW ->
                weeksViewWeekOffset += activeView.barCount
            VIEWS.MONTHS_VIEW ->
                monthsViewMonthOffset += activeView.barCount
        }
        updateChartData()
        setBtnEnabledState()
    }

    /**
     * construct array for "week" view -> each bar = 1 day
     */
    private suspend fun getMoToFrArray(): Pair< ArrayList<BarEntry>, ArrayList<PieEntry> > {
        val barChartArray = arrayListOf<BarEntry>()
        val pieChartArray = arrayListOf<PieEntry>()
        val visibleLibraryItems = ArrayList<UUID>()

        libraryItems.forEach { it.totalDuration = 0 }

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}

        for (day in VIEWS.DAYS_VIEW.barCount downTo 1) {
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisDay = PTDatabase.getInstance(applicationContext).sectionDao.getWithLibraryItems(
                getStartOfDayOfWeek(day.toLong(), daysViewWeekOffset).toEpochSecond(),
                getEndOfDayOfWeek(day.toLong(), daysViewWeekOffset).toEpochSecond()
            )
            sectionsThisDay.forEach { (section, libraryItem) ->
                // only show selected entries (checkbox enabled)
                if(libraryItems.any { it.selected && it.libraryItem.id == libraryItem.id }) {
                    // sum all section duration with same color (regardless whether they are actually the same libraryItem)
                    floatArrDurBarChart[libraryItem.colorIndex] += (section.duration ?: 0).toFloat()
                    floatArrDurPieChart[libraryItem.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleLibraryItems.add(libraryItem.id)
                libraryItems.first { it.libraryItem.id == libraryItem.id}.totalDuration += section.duration ?: 0
            }
            // add the entry to the BEGINNING of the array otherwise the bars will not be clickable (probably a bug?)
            barChartArray.add(0, BarEntry(day.toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "LibraryItem $cat_id"))
        }

        updateVisibleLibraryItems(visibleLibraryItems)
        return Pair(barChartArray, pieChartArray)
    }

    /**
     * construct array for "months" view -> each bar = 1 week
     */
    private suspend fun getWeeksArray(): Pair< ArrayList<BarEntry>, ArrayList<PieEntry> > {
        val chartArray = arrayListOf<BarEntry>()
        val pieChartArray = arrayListOf<PieEntry>()
        val visibleLibraryItems = ArrayList<UUID>()

        libraryItems.forEach { it.totalDuration = 0 }

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}

        for (week in 0 downTo -(VIEWS.WEEKS_VIEW.barCount-1)) {     // last 10 weeks
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisWeek = PTDatabase.getInstance(applicationContext).sectionDao.getWithLibraryItems(
                getStartOfWeek(week.toLong() + weeksViewWeekOffset).toEpochSecond(),
                getEndOfWeek(week.toLong() + weeksViewWeekOffset).toEpochSecond()
            )
            sectionsThisWeek.forEach { (section, libraryItem) ->
                // only show selected entries (checkbox enabled)
                if(libraryItems.any { it.selected && it.libraryItem.id == libraryItem.id }) {
                    floatArrDurBarChart[libraryItem.colorIndex] += (section.duration ?: 0).toFloat()
                    floatArrDurPieChart[libraryItem.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleLibraryItems.add(libraryItem.id)
                libraryItems.first { it.libraryItem.id == libraryItem.id}.totalDuration += section.duration ?: 0
            }
            // add the entry to the BEGINNING of the array otherwise the bars will not be clickable (probably a bug?)
            chartArray.add(0, BarEntry((week + weeksViewWeekOffset).toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "LibraryItem $cat_id"))
        }

        updateVisibleLibraryItems(visibleLibraryItems)
        return Pair(chartArray, pieChartArray)
    }

    /**
     * construct array for "year" view -> each bar = 1 month
     */
    private suspend fun getMonthsArray(): Pair< ArrayList<BarEntry>, ArrayList<PieEntry> > {
        val barChartArray = arrayListOf<BarEntry>()
        val pieChartArray = arrayListOf<PieEntry>()
        val visibleLibraryItems = ArrayList<UUID>()

        libraryItems.forEach { it.totalDuration = 0 }

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}
        for (month in 0 downTo -(VIEWS.MONTHS_VIEW.barCount-1)) {
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisMonth = PTDatabase.getInstance(applicationContext).sectionDao.getWithLibraryItems(
                getStartOfMonth(month.toLong() + monthsViewMonthOffset).toEpochSecond(),
                getEndOfMonth(month.toLong() + monthsViewMonthOffset).toEpochSecond()
            )
            sectionsThisMonth.forEach { (section, libraryItem) ->
                // only show selected entries (checkbox enabled)
                if(libraryItems.any { it.selected && it.libraryItem.id == libraryItem.id }) {
                    floatArrDurBarChart[libraryItem.colorIndex] += (section.duration ?: 0).toFloat()
                    floatArrDurPieChart[libraryItem.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleLibraryItems.add(libraryItem.id)
                libraryItems.first { it.libraryItem.id == libraryItem.id}.totalDuration += section.duration ?: 0
            }
            // add the entry to the BEGINNING of the array otherwise the bars will not be clickable (probably a bug?)
            barChartArray.add(0, BarEntry((month + monthsViewMonthOffset).toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "LibraryItem $cat_id"))
        }

        updateVisibleLibraryItems(visibleLibraryItems)
        return Pair(barChartArray, pieChartArray)
    }

    /** updates the shown Elements in the checkbox list according to the data in the chart */
    private fun updateVisibleLibraryItems(visibleLibraryItems: List<UUID>) {
        var elemRemovedOrInserted = false
        // traverse in reverse order so that newly inserted/removed items don't affect list indices
        libraryItems.asReversed().forEach { elem ->
            if (visibleLibraryItems.contains(elem.libraryItem.id)) {
                if (!elem.visible) {   // libraryItem was hidden before, should now be shown
                    elem.visible = true
                    libraryItems.filter { it.visible }    // convert to same list as adapter uses
                        .indexOfFirst { it.libraryItem.id == elem.libraryItem.id }    // find newly "inserted" item position
                        .let { position ->
                            libraryItemListAdapter.notifyItemInserted(position)    // notify adapter about new element
                        }
                    elemRemovedOrInserted = true
                }
            } else {
                if (elem.visible) {     // libraryItem was shown, should now be hidden
                    libraryItems.filter { it.visible }    // convert to same list as adapter uses
                        .indexOfFirst { it.libraryItem.id == elem.libraryItem.id }    // find newly "removed" item position
                        .let { position ->
                            libraryItemListAdapter.notifyItemRemoved(position)    // notify adapter about deleted element
                        }
                    elem.visible = false
                    elemRemovedOrInserted = true
                }
            }
        }
        // scroll to top if elements are removed/inserted to show possibly added items on top
        if(elemRemovedOrInserted) findViewById<RecyclerView>(R.id.recyclerview_statistics).scrollToPosition(0)

        // if list is empty, show shrug
        if (libraryItems.none { it.visible }) {
            findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.VISIBLE
            findViewById<TextView>(R.id.shrug_text_1).text = resources.getString(R.string.no_sessions)
            findViewById<TextView>(R.id.shrug_text_2).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.GONE
        }
    }

    /**
     * formats x axis value according to our time scaling
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(value: Float): String {
            return when (activeView) {
                VIEWS.DAYS_VIEW ->
                    getDayString(value)
                VIEWS.WEEKS_VIEW ->
                    getWeekString(value)
                VIEWS.MONTHS_VIEW ->
                    getMonthString(value)
            }
        }



        private fun getDayString(xValue: Float): String {
            if (xValue < 1 || xValue > 7) {
                // sometimes on activeView change getFormattedValue is called to soon / on wrong xValue
                // so just return nothing to prevent crash because of wrong dayIndex
                return ""
            }
            return getStartOfDayOfWeek(xValue.toLong(), 0)
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_WEEKDAY_ABBREV))
        }

        private fun getWeekString(xValue: Float): String {
            // maybe a solution for multiline: https://stackoverflow.com/a/46676451
            val start = getStartOfWeek(xValue.toLong())
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_OF_MONTH_PADDED))

            val end = getEndOfWeek(xValue.toLong())
                .minusDays(1)   // subtract one day to get the "last" day (because of half-open approach)
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_OF_MONTH_PADDED))

            return ("$start - $end")
        }

        private fun getMonthString(xValue: Float): String {
            return getStartOfMonth(xValue.toLong())
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV))
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


    /**
     * This ValueFormatter shows the sum of all stacked Bars on top of the each bar instead of for every segment
     * It should do the same as StackedValueFormatter but this one doesn't work for our case because we have
     * "invisible" stacked segments with value=0 in our bars which results to no call in getBarStackedLabel() and thus
     * the sum doesn't get drawn. This one only uses non-zero entries to determine the top position
     */
    private inner class BarChartValueFormatter: ValueFormatter() {

        var lastEntryX = 100f            // the x entry (=Bar ID) of last time getBarStackedLabel() was called
        var stackCounterCurrentBar = 0  // counter variable counting the stack we are inside the current bar
        var stackEntriesNotZero = 0     // amount of non-zero entries in this bar

        // getBarStackedLabel is called on every bar for every non-zero segment
        // value: the y value of current segment
        // stackedEntry: the whole BarEntry object for current bar, always the same for each stack on the same Bar
        override fun getBarStackedLabel(value: Float, stackedEntry: BarEntry): String {

            var prefix = ""
            if (stackedEntry.x == getCurrentDayIndexOfWeek().toFloat() && daysViewWeekOffset == 0L && activeView == VIEWS.DAYS_VIEW)
                prefix += "${getString(R.string.today)}: "

            // only display label if Bar is highlighted
            if (barChart.highlighted != null &&
                barChart.highlighted.find { it.x == stackedEntry.x } != null) {

                if (stackedEntry.x != lastEntryX) {
                    lastEntryX = stackedEntry.x
                    // first stack on a new bar
                    stackCounterCurrentBar = 1
                    stackEntriesNotZero = stackedEntry.yVals?.filterNot { it == 0f }?.count() ?: 0

                    // show 0 if there are no stacks in this bar
                    if (stackEntriesNotZero == 0) {
                        // reset (set to value which will never occur on x axis) lastEntry so that un- and then re-selecting same bar works
                        lastEntryX = 100f
                        return prefix + "0"
                    }

                    // show value if there is only 1 stack in this bar
                    if (stackEntriesNotZero == 1) {
                        // reset (set to value which will never occur on x axis) lastEntry so that un- and then re-selecting same bar works
                        lastEntryX = 100f
                        return prefix + getDurationString(stackedEntry.yVals?.sum()?.toInt() ?: 0, TIME_FORMAT_HUMAN_PRETTY)
                    }
                } else {
                    lastEntryX = stackedEntry.x
                    stackCounterCurrentBar++
                    if (stackCounterCurrentBar == stackEntriesNotZero) {
                        // reset (set to value which will never occur on x axis) lastEntry so that un- and then re-selecting same bar works
                        lastEntryX = 100f
                        // we reached the last non-zero stack of the bar, so we're at the top
                        return prefix + getDurationString(stackedEntry.yVals?.sum()?.toInt() ?: 0, TIME_FORMAT_HUMAN_PRETTY)
                    }
                }
            }
            return ""   // return empty string so no value is drawn
        }
    }


    private inner class PieChartValueFormatter : ValueFormatter() {

        override fun getFormattedValue(value: Float): String {

            if (value > 2)
                return "${value.roundToInt()} %"

            return ""
        }
    }

    private inner class LibraryItemStatsAdapter : RecyclerView.Adapter<LibraryItemStatsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val catCheckBox: CheckBox = view.findViewById(R.id.checkbox_libraryItem)
            val catTimeView: TextView = view.findViewById(R.id.total_time_libraryItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_statisitcs_session, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val elem = libraryItems.filter { it.visible }[position]
            val libraryItemColors = resources.getIntArray(R.array.library_item_colors)

            holder.catCheckBox.text = elem.libraryItem.name
            holder.catCheckBox.setOnCheckedChangeListener(null)
            holder.catCheckBox.isChecked = elem.selected
            holder.catCheckBox.buttonTintList = ColorStateList.valueOf(
                libraryItemColors[elem.libraryItem.colorIndex]
            )
            holder.catCheckBox.setOnCheckedChangeListener { _, isChecked ->
                elem.selected = isChecked   // sync list with UI
                updateChartData(recalculateDurs = false)  // notify fragment to change chart
            }

            holder.catTimeView.text = getDurationString(elem.totalDuration, TIME_FORMAT_HUMAN_PRETTY)
        }

        override fun getItemCount(): Int = libraryItems.filter { it.visible }.size

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
