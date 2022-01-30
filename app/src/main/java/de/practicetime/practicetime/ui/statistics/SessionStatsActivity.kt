package de.practicetime.practicetime.ui.statistics

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY_SHORT
import de.practicetime.practicetime.utils.getDurationString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.math.ceil
import kotlin.math.roundToInt

class SessionStatsActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var categoryListAdapter: CategoryStatsAdapter
    private var daysViewWeekOffset = 0L
    private var weeksViewWeekOffset = 0L
    private var monthsViewMonthOffset = 0L
    private var colorAmount = 0

    private val categories = ArrayList<CategoryListElement>()

    data class CategoryListElement(
        val category: Category,
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

        setSupportActionBar(findViewById(R.id.my_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.session_statistics)
        }

        findViewById<AppCompatButton>(R.id.btn_days).setOnClickListener {
            activeView = VIEWS.DAYS_VIEW
            updateChartData()
            setBtnEnabledState()
            // reset time ranges for other views
            weeksViewWeekOffset = 0L
            monthsViewMonthOffset = 0L
        }
        findViewById<AppCompatButton>(R.id.btn_weeks).setOnClickListener {
            activeView = VIEWS.WEEKS_VIEW
            updateChartData()
            setBtnEnabledState()
            // reset time ranges for other views
            daysViewWeekOffset = 0L
            monthsViewMonthOffset = 0L
        }
        findViewById<AppCompatButton>(R.id.btn_months).setOnClickListener {
            activeView = VIEWS.MONTHS_VIEW
            updateChartData()
            setBtnEnabledState()
            // reset time ranges for other views
            daysViewWeekOffset = 0L
            weeksViewWeekOffset = 0L
        }
        findViewById<ImageButton>(R.id.btn_rwnd).setOnClickListener {
            seekPast()
        }
        findViewById<ImageButton>(R.id.btn_fwd).setOnClickListener {
            seekFuture()
        }
        val btnChart = findViewById<ImageButton>(R.id.btn_toggle_chart_type)
        btnChart.setOnClickListener {
            when (chartType) {
                BAR_CHART -> {
                    chartType = PIE_CHART
                    btnChart.setImageResource(R.drawable.ic_bar_chart)
                }
                PIE_CHART -> {
                    chartType = BAR_CHART
                    btnChart.setImageResource(R.drawable.ic_pie_chart)
                }
                else -> {
                    chartType = BAR_CHART
                    btnChart.setImageResource(R.drawable.ic_pie_chart)
                }
            }
            updateChartData()
        }

        colorAmount = resources?.getIntArray(R.array.category_colors)?.toCollection(mutableListOf())?.size ?: 0
        initBarChart()
        initPieChart()
        initCategoryList()

        updateChartData()
        setBtnEnabledState()
    }


    /** initialize the checkbox list with the categories */
    private fun initCategoryList() {
        lifecycleScope.launch {
            PracticeTime.dao.getAllCategories().forEach {
                Log.d("ZAS", "CATEGORY: ${it.name}")
                categories.add(
                    CategoryListElement(
                        it,
                        selected = true,
                        visible = false
                    )
                )
            }
            categoryListAdapter = CategoryStatsAdapter()
            val layoutManager = LinearLayoutManager(this@SessionStatsActivity)

            val categoryRecyclerView = findViewById<RecyclerView>(R.id.recyclerview_statistics)
            categoryRecyclerView.layoutManager = layoutManager
            categoryRecyclerView.adapter = categoryListAdapter
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
            textColor = getThemeColor(R.attr.colorOnSurface)
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
            textColor = getThemeColor(R.attr.colorOnSurfaceLowerContrast)
            valueFormatter = YAxisValueFormatter()
        }

        barChart.notifyDataSetChanged()
        barChart.invalidate()
    }

    /** initialize the (hidden) Pie chart view object */
    private fun initPieChart() {
        pieChart = findViewById(R.id.pie_chart);
        pieChart.apply {
            setDrawEntryLabels(false)
            isHighlightPerTapEnabled = false

            setUsePercentValues(true);
            description.isEnabled = false;
            legend.isEnabled = false
            isRotationEnabled = false;

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

            animateY(1400, Easing.EaseInOutQuad);
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
                val start = getStartOfWeek(daysViewWeekOffset)
                    .format(DateTimeFormatter.ofPattern("MMMM d"))

                var formatStr = "d"
                if (getStartOfWeek(daysViewWeekOffset).month != getEndOfWeek(daysViewWeekOffset).minusDays(1).month) {
                    formatStr = "MMMM d"    // also show month if it is different from startMonth
                }

                val end = getEndOfWeek(daysViewWeekOffset)
                    .minusDays(1)   // subtract one day because of half-open approach
                    .format(DateTimeFormatter.ofPattern(formatStr))
                tvRange.text = ("$start - $end")
            }

            VIEWS.WEEKS_VIEW -> {
                val start = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusWeeks(firstBarXVal.toLong())
                    .with(ChronoField.DAY_OF_WEEK , 1)  // go to Monday
                    .format(DateTimeFormatter.ofPattern("MMM d"))
                val end = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusWeeks(lastBarXVal.toLong())
                    .with(ChronoField.DAY_OF_WEEK , 7)  // go to Sunday
                    .format(DateTimeFormatter.ofPattern("MMM d"))
                tvRange.text = ("$start - $end")
            }

            VIEWS.MONTHS_VIEW -> {
                val startDate = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusMonths(firstBarXVal.toLong())

                val endDate = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusMonths(lastBarXVal.toLong())

                var formatStrStart = "MMMM"
                var formatStrEnd = "MMMM y"
                if(startDate.year != endDate.year) {
                    formatStrStart = "MMM y"    // also show year of beginning if it is different from end
                    formatStrEnd = "MMM y"
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
            TIME_FORMAT_HUMAN_PRETTY_SHORT
        )
        tvTotalTimeInRange.text = getString(R.string.total_time, durationStr)
    }

    /** toggles the states of the "days"/"month"/"week" chooser buttons*/
    private fun setBtnEnabledState() {
        val btnWeek = findViewById<AppCompatButton>(R.id.btn_days)
        val btnMonth = findViewById<AppCompatButton>(R.id.btn_weeks)
        val btnYear = findViewById<AppCompatButton>(R.id.btn_months)
        val btnFwd = findViewById<ImageButton>(R.id.btn_fwd)

        btnWeek.isSelected = false
        btnMonth.isSelected = false
        btnYear.isSelected = false
        btnWeek.isEnabled = true
        btnMonth.isEnabled = true
        btnYear.isEnabled = true

        when(activeView) {
            VIEWS.DAYS_VIEW -> {
                btnFwd.isEnabled = daysViewWeekOffset != 0L
                btnWeek.isSelected = true
                btnWeek.isEnabled = false
            }
            VIEWS.WEEKS_VIEW -> {
                btnFwd.isEnabled = weeksViewWeekOffset != 0L
                btnMonth.isSelected = true
                btnMonth.isEnabled = false
            }
            VIEWS.MONTHS_VIEW -> {
                btnFwd.isEnabled = monthsViewMonthOffset != 0L
                btnYear.isSelected = true
                btnYear.isEnabled = false
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
                dataSetBarChart.values = barValues
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

                val categoryColors = resources?.getIntArray(R.array.category_colors)
                    ?.toCollection(mutableListOf())
                dataSetBarChart.apply {
                    colors = categoryColors
                    setDrawValues(true)
                }
                dataSetPieChart.apply {
                    colors = categoryColors
                    setDrawValues(true)

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
                    setValueTextColor(getThemeColor(R.attr.colorOnSurfaceLowerContrast))
                    setValueTextSize(12f)
                    isHighlightEnabled = true
                }

                barChart.data = barData


                val pieData = PieData(dataSetPieChart)
                pieData.apply {
                    setValueFormatter(PieChartValueFormatter())
                    setValueTextSize(11f)
                    setValueTextColor(Color.BLACK)
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

            // don't recalculate the total durations for each category if explicitly told so to prevent flashing
            if (recalculateDurs)
                categoryListAdapter.notifyItemRangeChanged(0, categories.filter { it.visible }.size)

            delay(100)  // TODO this is needed in order to make movePieChart work the first time. Don't know why
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
                    getCurrentDayOfWeek().toFloat()
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
        val height = ((pieChart as View).measuredHeight * 0.3).toInt()

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
        val visibleCategories = ArrayList<Int>()

        categories.forEach { it.totalDuration = 0 }

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}

        for (day in VIEWS.DAYS_VIEW.barCount downTo 1) {
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisDay = PracticeTime.dao.getSectionsWithCategories(
                getStartOfDayOfWeek(day.toLong(), daysViewWeekOffset).toEpochSecond(),
                getEndOfDayOfWeek(day.toLong(), daysViewWeekOffset).toEpochSecond()
            )
            sectionsThisDay.forEach { (section, category) ->
                // only show selected entries (checkbox enabled)
                if(categories.any { it.selected && it.category.id == category.id }) {
                    // sum all section duration with same color (regardless whether they are actually the same category)
                    floatArrDurBarChart[category.colorIndex] += (section.duration ?: 0).toFloat()
                    floatArrDurPieChart[category.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleCategories.add(category.id)
                categories.first { it.category.id == category.id}.totalDuration += section.duration ?: 0
            }
            // add the entry to the BEGINNING of the array otherwise the bars will not be clickable (probably a bug?)
            barChartArray.add(0, BarEntry(day.toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "Category $cat_id"))
        }

        updateVisibleCategories(visibleCategories)
        return Pair(barChartArray, pieChartArray)
    }

    /**
     * construct array for "months" view -> each bar = 1 week
     */
    private suspend fun getWeeksArray(): Pair< ArrayList<BarEntry>, ArrayList<PieEntry> > {
        val chartArray = arrayListOf<BarEntry>()
        val pieChartArray = arrayListOf<PieEntry>()
        val visibleCategories = ArrayList<Int>()

        categories.forEach { it.totalDuration = 0 }

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}

        for (week in 0 downTo -(VIEWS.WEEKS_VIEW.barCount-1)) {     // last 10 weeks
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisWeek = PracticeTime.dao.getSectionsWithCategories(
                getStartOfWeek(week.toLong() + weeksViewWeekOffset).toEpochSecond(),
                getEndOfWeek(week.toLong() + weeksViewWeekOffset).toEpochSecond()
            )
            sectionsThisWeek.forEach { (section, category) ->
                // only show selected entries (checkbox enabled)
                if(categories.any { it.selected && it.category.id == category.id }) {
                    floatArrDurBarChart[category.colorIndex] += (section.duration ?: 0).toFloat()
                    floatArrDurPieChart[category.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleCategories.add(category.id)
                categories.first { it.category.id == category.id}.totalDuration += section.duration ?: 0
            }
            // add the entry to the BEGINNING of the array otherwise the bars will not be clickable (probably a bug?)
            chartArray.add(0, BarEntry((week + weeksViewWeekOffset).toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "Category $cat_id"))
        }

        updateVisibleCategories(visibleCategories)
        return Pair(chartArray, pieChartArray)
    }

    /**
     * construct array for "year" view -> each bar = 1 month
     */
    private suspend fun getMonthsArray(): Pair< ArrayList<BarEntry>, ArrayList<PieEntry> > {
        val barChartArray = arrayListOf<BarEntry>()
        val pieChartArray = arrayListOf<PieEntry>()
        val visibleCategories = ArrayList<Int>()

        categories.forEach { it.totalDuration = 0 }

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}
        for (month in 0 downTo -(VIEWS.MONTHS_VIEW.barCount-1)) {
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisMonth = PracticeTime.dao.getSectionsWithCategories(
                getStartOfMonth(month.toLong() + monthsViewMonthOffset).toEpochSecond(),
                getEndOfMonth(month.toLong() + monthsViewMonthOffset).toEpochSecond()
            )
            sectionsThisMonth.forEach { (section, category) ->
                // only show selected entries (checkbox enabled)
                if(categories.any { it.selected && it.category.id == category.id }) {
                    floatArrDurBarChart[category.colorIndex] += (section.duration ?: 0).toFloat()
                    floatArrDurPieChart[category.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleCategories.add(category.id)
                categories.first { it.category.id == category.id}.totalDuration += section.duration ?: 0
            }
            // add the entry to the BEGINNING of the array otherwise the bars will not be clickable (probably a bug?)
            barChartArray.add(0, BarEntry((month + monthsViewMonthOffset).toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "Category $cat_id"))
        }

        updateVisibleCategories(visibleCategories)
        return Pair(barChartArray, pieChartArray)
    }

    /** updates the shown Elements in the checkbox list according to the data in the chart */
    private fun updateVisibleCategories(visibleCategories: List<Int>) {
        var elemRemovedOrInserted = false
        // traverse in reverse order so that newly inserted/removed items don't affect list indices
        categories.asReversed().forEach { elem ->
            if (visibleCategories.contains(elem.category.id)) {
                if (!elem.visible) {   // category was hidden before, should now be shown
                    elem.visible = true
                    categories.filter { it.visible }    // convert to same list as adapter uses
                        .indexOfFirst { it.category.id == elem.category.id }    // find newly "inserted" item position
                        .let { position ->
                            categoryListAdapter.notifyItemInserted(position)    // notify adapter about new element
                        }
                    elemRemovedOrInserted = true
                }
            } else {
                if (elem.visible) {     // category was shown, should now be hidden
                    categories.filter { it.visible }    // convert to same list as adapter uses
                        .indexOfFirst { it.category.id == elem.category.id }    // find newly "removed" item position
                        .let { position ->
                            categoryListAdapter.notifyItemRemoved(position)    // notify adapter about deleted element
                        }
                    elem.visible = false
                    elemRemovedOrInserted = true
                }
            }
        }
        // scroll to top if elements are removed/inserted to show possibly added items on top
        if(elemRemovedOrInserted) findViewById<RecyclerView>(R.id.recyclerview_statistics).scrollToPosition(0)

        // if list is empty, show shrug
        if (categories.none { it.visible }) {
            findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.VISIBLE
            findViewById<TextView>(R.id.shrug_text_1).text = resources.getString(R.string.no_sessions)
            findViewById<TextView>(R.id.shrug_text_2).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.GONE
        }
    }

    // gets the timestamp for start of day
    // dayBack: 0=today, 1=yesterday, 2=day before yesterday
    private fun getStartOfDay(dayOffset: Long): ZonedDateTime {
        // use local timezone here
        return LocalDate
            .now(ZoneId.systemDefault())
            .plusDays(dayOffset)
            .atStartOfDay(ZoneId.systemDefault())
    }

    private fun getStartOfWeek(weekOffset: Long): ZonedDateTime {
        return getStartOfDayOfWeek(1, weekOffset)
    }

    private fun getStartOfMonth(monthOffset: Long): ZonedDateTime {
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_MONTH , 1 )
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusMonths(monthOffset)
    }

    private fun getEndOfDay(dayOffset: Long): ZonedDateTime {
        return LocalDate
            .now(ZoneId.systemDefault())
            .plusDays(dayOffset)
            .plusDays(1)    // begin of next day is end of this day (half-open)
            .atStartOfDay(ZoneId.systemDefault())
    }

    // gets end date of current Week
    private fun getEndOfWeek(weekOffset: Long): ZonedDateTime {
        return getStartOfDayOfWeek(1, weekOffset).plusWeeks(1)
    }

    private fun getEndOfMonth(monthOffset: Long): ZonedDateTime {
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_MONTH , 1 )
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusMonths(monthOffset)
            .plusMonths(1)
    }

    // get the Beginning of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
    private fun getStartOfDayOfWeek(dayIndex: Long, weekOffset: Long): ZonedDateTime {
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_WEEK , dayIndex )         // ISO 8601, Monday is first day of week.
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusWeeks(weekOffset)
    }

    // get the End of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
    private fun getEndOfDayOfWeek(dayIndex: Long, weekOffset: Long): ZonedDateTime {
        // because of half-open approach we have to get the "start of the _next_ day" instead of the end of the current day
        // e.g. end of Tuesday = Start of Wednesday, so make dayIndex 2 -> 3
        var nextDay = dayIndex + 1
        var weekOffsetAdapted = weekOffset
        if (dayIndex > 6) {
            nextDay = (dayIndex + 1) % 7
            weekOffsetAdapted += 1  // increase weekOffset so that we take the start of the first day of NEXT week as end of day
        }
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_WEEK, nextDay)         // ISO 8601, Monday is first day of week.
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusWeeks(weekOffsetAdapted)
    }

    // returns the weekDay of today from index 1=Mo until 7=Sun
    private fun getCurrentDayOfWeek(): Int {
        return ZonedDateTime.now()
            .toLocalDate().dayOfWeek.value
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

        /**
         * For DateFormatter patterns see: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
         */

        private fun getDayString(xValue: Float): String {
            if (xValue < 1 || xValue > 7) {
                // sometimes on activeView change getFormattedValue is called to soon / on wrong xValue
                // so just return nothing to prevent crash because of wrong dayIndex
                return ""
            }
            return ZonedDateTime.now()
                .with(ChronoField.DAY_OF_WEEK , xValue.toLong())
                .format(DateTimeFormatter.ofPattern("E"))
        }

        private fun getWeekString(xValue: Float): String {
            // maybe a solution for multiline: https://stackoverflow.com/a/46676451
            val start = getStartOfWeek(xValue.toLong())
                .format(DateTimeFormatter.ofPattern("dd"))

            val end = getEndOfWeek(xValue.toLong())
                .minusDays(1)   // subtract one day to get the "last" day (because of half-open approach)
                .format(DateTimeFormatter.ofPattern("dd"))

            return ("$start - $end")
        }

        private fun getMonthString(xValue: Float): String {
            return LocalDate
                .now(ZoneId.systemDefault())
                .plusMonths(xValue.toLong())
                .format(DateTimeFormatter.ofPattern("MMM"))
        }
    }

    /**
     * formats y axis value according to our time scaling
     */
    private inner class YAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(seconds: Float): String {
            return getDurationString(seconds.toInt(), TIME_FORMAT_HUMAN_PRETTY_SHORT)
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
            if (stackedEntry.x == getCurrentDayOfWeek().toFloat() && daysViewWeekOffset == 0L && activeView == VIEWS.DAYS_VIEW)
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
                        return prefix + getDurationString(stackedEntry.yVals?.sum()?.toInt() ?: 0, TIME_FORMAT_HUMAN_PRETTY_SHORT)
                    }
                } else {
                    lastEntryX = stackedEntry.x
                    stackCounterCurrentBar++
                    if (stackCounterCurrentBar == stackEntriesNotZero) {
                        // reset (set to value which will never occur on x axis) lastEntry so that un- and then re-selecting same bar works
                        lastEntryX = 100f
                        // we reached the last non-zero stack of the bar, so we're at the top
                        return prefix + getDurationString(stackedEntry.yVals?.sum()?.toInt() ?: 0, TIME_FORMAT_HUMAN_PRETTY_SHORT)
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

    private inner class CategoryStatsAdapter : RecyclerView.Adapter<CategoryStatsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val catCheckBox: CheckBox = view.findViewById(R.id.checkbox_category)
            val catTimeView: TextView = view.findViewById(R.id.total_time_category)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_statistics_category_list_item, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val elem = categories.filter { it.visible }[position]
            val categoryColors = resources.getIntArray(R.array.category_colors)

            holder.catCheckBox.text = elem.category.name
            holder.catCheckBox.setOnCheckedChangeListener(null)
            holder.catCheckBox.isChecked = elem.selected
            holder.catCheckBox.buttonTintList = ColorStateList.valueOf(
                categoryColors[elem.category.colorIndex]
            )
            holder.catCheckBox.setOnCheckedChangeListener { _, isChecked ->
                elem.selected = isChecked   // sync list with UI
                updateChartData(recalculateDurs = false)  // notify fragment to change chart
            }

            holder.catTimeView.text = getDurationString(elem.totalDuration, TIME_FORMAT_HUMAN_PRETTY_SHORT)
        }

        override fun getItemCount(): Int = categories.filter { it.visible }.size

    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}