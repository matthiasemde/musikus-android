package de.practicetime.practicetime

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.GoalDescription
import de.practicetime.practicetime.entities.GoalInstance
import de.practicetime.practicetime.entities.GoalPeriodUnit
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class GoalsStatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var dao: PTDao
    private lateinit var barChart: BarChart
    private lateinit var goalListAdapter: GoalStatsAdapter

    private val goals = ArrayList<GoalListElement>()
    private var selectedGoal = -1

    data class GoalListElement(
        val goalInstances: List<GoalInstance>,
        val goalDesc: GoalDescription,
        val category: Category?,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // get the dao object
        openDatabase()

        view.findViewById<ImageButton>(R.id.btn_rwnd).setOnClickListener {
//            seekPast(view)
        }
        view.findViewById<ImageButton>(R.id.btn_fwd).setOnClickListener {
//            seekFuture(view)
        }

//        initBarChart()
//        initPieChart()
//        updateChartData()
//        setBtnEnabledState(view)

        initGoalsList()

        view.findViewById<LinearLayout>(R.id.ll_statistics_toggle_timespan).visibility = View.GONE
    }

    private fun initGoalsList() {
        lifecycleScope.launch {
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
            val layoutManager = LinearLayoutManager(requireContext())

            val categoryRecyclerView = requireView().findViewById<RecyclerView>(R.id.recyclerview_statistics)
            categoryRecyclerView.layoutManager = layoutManager
            categoryRecyclerView.adapter = goalListAdapter
        }
    }

    /*
    private fun initBarChart() {
        // for rounded bars: https://gist.github.com/xanscale/e971cc4f2f0712a8a3bcc35e85325c27
        //      issue: https://github.com/PhilJay/MPAndroidChart/issues/2771#issuecomment-566719474

        barChart = requireView().findViewById(R.id.bar_chart) as BarChart
        barChart.setTouchEnabled(false)
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.invalidate()

        // axis settings
        val leftAxis = barChart.axisLeft
        val xAxis = barChart.xAxis
        val rightAxis = barChart.axisRight

        leftAxis.axisMinimum = 0f   // needed for y axis scaling (probably a bug!)
        leftAxis.isEnabled = false

        rightAxis.axisMinimum = 0f
        rightAxis.setDrawAxisLine(false)
        rightAxis.textColor = getThemeColor(R.attr.colorOnSurfaceLowerContrast)
        rightAxis.valueFormatter = YAxisValueFormatter()

        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.labelCount = activeView.barCount
        xAxis.valueFormatter = XAxisValueFormatter()
        xAxis.textColor = getThemeColor(R.attr.colorOnSurface)

    }

    private fun initPieChart() {
        pieChart = requireView().findViewById(R.id.pie_chart);

        val height = (requireView().measuredHeight * 0.65).toInt()

        Log.d("ZAG", "height: $height")

        val p = (pieChart.layoutParams as LinearLayout.LayoutParams)
        p.setMargins(0, 0, 0, -height)
        pieChart.layoutParams = p

        pieChart.setUsePercentValues(true);
        pieChart.description.isEnabled = false;

        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)

        pieChart.isRotationEnabled = false;

        pieChart.animateY(1400, Easing.EaseInOutQuad);

        pieChart.legend.isEnabled = false

        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(9f)

        pieChart.maxAngle = 180f // HALF CHART
        pieChart.rotationAngle = 180f

        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 61f
        pieChart.setCenterTextOffset(0f, -20f)

//        chart.setHighlightPerTapEnabled(true);
//
//        // chart.setUnit(" €");
//        // chart.setDrawUnitsInChart(true);
//
//        // add a selection listener
//        chart.setOnChartValueSelectedListener(this);
//
//        seekBarX.setProgress(4);
//        seekBarY.setProgress(10);

//        chart.setTransparentCircleColor(Color.WHITE);
//        chart.setTransparentCircleAlpha(110);

//        pieChart.setHoleRadius(58f);
//        chart.setTransparentCircleRadius(61f);
//
//        chart.setDrawCenterText(true);

//        chart.setExtraOffsets(5, 10, 5, 5);

//        chart.setDragDecelerationFrictionCoef(0.95f);

//        chart.setCenterTextTypeface(tfLight);
//        chart.setCenterText(generateCenterSpannableText());
    }

    private fun setHeadingTextViews(view: View) {
        val tvRange = view.findViewById<TextView>(R.id.tv_chart_header)
        val tvTotalTimeInRange = view.findViewById<TextView>(R.id.tv_secondary_chart_header)

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
                    .format(DateTimeFormatter.ofPattern("MMMM d"))
                val end = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusWeeks(lastBarXVal.toLong())
                    .with(ChronoField.DAY_OF_WEEK , 7)  // go to Sunday
                    .format(DateTimeFormatter.ofPattern("MMMM d"))
                tvRange.text = ("$start - $end")
            }

            VIEWS.MONTHS_VIEW -> {
                val startDate = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusMonths(firstBarXVal.toLong())

                val endDate = LocalDate
                    .now(ZoneId.systemDefault())
                    .plusMonths(lastBarXVal.toLong())

                var formatStr = "MMMM"
                if(startDate.year != endDate.year) {
                    formatStr = "MMMM y"    // also show year of beginning if it is different from end
                }

                val start = startDate.format(DateTimeFormatter.ofPattern(formatStr))
                val end = endDate.format(DateTimeFormatter.ofPattern("MMMM y"))
                tvRange.text = ("$start - $end")
            }
        }
        // show sum of visible data
        val durationStr = secondsToTimeString(
            chartArray.values.sumOf {
                it.yVals.sum().toInt()
            }
        )
        tvTotalTimeInRange.text = requireContext().getString(R.string.total_time, durationStr)
    }

    private fun setBtnEnabledState(view: View) {
        val btnWeek = view.findViewById<AppCompatButton>(R.id.btn_days)
        val btnMonth = view.findViewById<AppCompatButton>(R.id.btn_weeks)
        val btnYear = view.findViewById<AppCompatButton>(R.id.btn_months)
        val btnFwd = view.findViewById<ImageButton>(R.id.btn_fwd)

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

                val categoryColors = context?.resources?.getIntArray(R.array.category_colors)
                    ?.toCollection(mutableListOf())
                dataSetBarChart.colors = categoryColors
                dataSetPieChart.colors = categoryColors
                dataSetBarChart.setDrawValues(true)
                dataSetPieChart.setDrawValues(true)

                val barData = BarData(dataSetBarChart)
                barData.barWidth = 0.4f
                barData.setValueFormatter(BarChartValueFormatter())
                barData.setValueTextColor(getThemeColor(R.attr.colorOnSurfaceLowerContrast))
                barData.setValueTextSize(12f)

                barChart.data = barData


                val pieData = PieData(dataSetPieChart)
                pieData.setValueFormatter(PercentFormatter())
                pieData.setValueTextSize(11f)
                pieData.setValueTextColor(Color.WHITE)
                pieChart.data = pieData

            }

            if(chartType == BAR_CHART) {
                pieChart.visibility = View.GONE
                barChart.visibility = View.VISIBLE
            } else {
                pieChart.visibility = View.VISIBLE
                barChart.visibility = View.GONE
            }

            // redraw the chart
            barChart.animateY(1000, Easing.EaseOutBack)
            barChart.invalidate()

            pieChart.animateY(1400, Easing.EaseInOutQuad)
            pieChart.invalidate()

            // update the Heading
            setHeadingTextViews(requireView())

            // don't recalculate the total durations for each category if explicitly told so to prevent flashing
            if (recalculateDurs)
                categoryListAdapter.notifyItemRangeChanged(0, categories.filter { it.visible }.size)
        }
    }

    private fun seekPast(view: View) {
        when(activeView) {
            VIEWS.DAYS_VIEW ->
                daysViewWeekOffset--    // special treatment for weekview because we always want Mo-Sun range
            VIEWS.WEEKS_VIEW ->
                weeksViewWeekOffset -= activeView.barCount
            VIEWS.MONTHS_VIEW ->
                monthsViewMonthOffset -= activeView.barCount
        }
        updateChartData()
        setBtnEnabledState(view)
    }

    private fun seekFuture(view: View) {
        when(activeView) {
            VIEWS.DAYS_VIEW ->
                daysViewWeekOffset++    // special treatment for weekview because we always want Mo-Sun range
            VIEWS.WEEKS_VIEW ->
                weeksViewWeekOffset += activeView.barCount
            VIEWS.MONTHS_VIEW ->
                monthsViewMonthOffset += activeView.barCount
        }
        updateChartData()
        setBtnEnabledState(view)
    }
*/

    /*
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
            val sectionsThisDay = dao.getSectionsWithCategories(
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
            barChartArray.add(BarEntry(day.toFloat(), floatArrDurBarChart))
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

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}

        for (week in 0 downTo -(VIEWS.WEEKS_VIEW.barCount-1)) {     // last 10 weeks
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisWeek = dao.getSectionsWithCategories(
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
            chartArray.add(BarEntry((week + weeksViewWeekOffset).toFloat(), floatArrDurBarChart))
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

        // init pie chart array here because we sum over all days
        val floatArrDurPieChart = FloatArray(colorAmount) {0f}
        for (month in 0 downTo -(VIEWS.MONTHS_VIEW.barCount-1)) {
            val floatArrDurBarChart = FloatArray(colorAmount) {0f}
            val sectionsThisMonth = dao.getSectionsWithCategories(
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
            barChartArray.add(BarEntry((month + monthsViewMonthOffset).toFloat(), floatArrDurBarChart))
        }

        floatArrDurPieChart.forEachIndexed { cat_id, dur ->
            pieChartArray.add(PieEntry(dur, "Category $cat_id"))
        }

        updateVisibleCategories(visibleCategories)
        return Pair(barChartArray, pieChartArray)
    }

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
        if(elemRemovedOrInserted) requireView().findViewById<RecyclerView>(R.id.recyclerview_statistics).scrollToPosition(0)

        // if list is empty, show shrug
        if (categories.none { it.visible }) {
            requireView().findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.VISIBLE
            requireView().findViewById<TextView>(R.id.shrug_text_1).text = resources.getString(R.string.no_sessions)
            requireView().findViewById<TextView>(R.id.shrug_text_2).visibility = View.GONE
        } else {
            requireView().findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.GONE
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
        var weekOffsetAdpated = weekOffset
        if (dayIndex > 6) {
            nextDay = (dayIndex + 1) % 7
            weekOffsetAdpated += 1  // increase weekOffset so that we take the start of the first day of NEXT week as end of day
        }
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_WEEK, nextDay)         // ISO 8601, Monday is first day of week.
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusWeeks(weekOffsetAdpated)
    }

*/

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

/*
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
            val (h, m) = secondsToHoursMins(seconds.toInt())
            val str = if (h != 0){
                "${h}." +
                "${m?.toFloat()?.div(60f)?.times(10)?.roundToInt()} h"   //TODO this is super ugly
            } else{
                "${m} m"
            }
            return str
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
    }


     */
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

            val categoryColors = requireContext().resources.getIntArray(R.array.category_colors)
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

            // TODO take data from last Instance?
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
                        GoalPeriodUnit.DAY -> requireContext().getString(R.string.goal_description_days, elem.goalDesc.periodInPeriodUnits)
                        GoalPeriodUnit.WEEK -> requireContext().getString(R.string.goal_description_weeks, elem.goalDesc.periodInPeriodUnits)
                        GoalPeriodUnit.MONTH -> requireContext().getString(R.string.goal_description_months, elem.goalDesc.periodInPeriodUnits)
                    }
                } else {    // singular
                    when (elem.goalDesc.periodUnit) {
                        GoalPeriodUnit.DAY -> requireContext().getString(R.string.goal_description_day)
                        GoalPeriodUnit.WEEK -> requireContext().getString(R.string.goal_description_week)
                        GoalPeriodUnit.MONTH -> requireContext().getString(R.string.goal_description_month)
                    }
                }

            holder.goalDescTv.text = requireContext().getString(
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
                selectedGoal = holder.bindingAdapterPosition
                notifyItemChanged(oldSel)
                holder.goalRadioButton.isChecked = true
                val sv = requireView().findViewById<NestedScrollView>(R.id.scrollview_statistics)
                // only scroll to top if already more than 100px scrolled to prevent blocking the UI for scroll duration
                if (sv.scrollY > 100)
                    sv.smoothScrollTo(0,0, 600)
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
        if (h != 0) str += "$h h "
        if (m != 0) str += "$m min"
        else
            if (h == 0)
                if (seconds != 0) str = "< 1 min"
                else str += "0 min"
        return str
    }

    private fun secondsToHoursMins(seconds: Int?): Pair<Int?, Int?> {
        // TODO uncomment for production
//        val hours = seconds?.div(3600)
//        val minutes = (seconds?.rem(3600))?.div(60)

        // FAKE values:
        val hours = (seconds?.rem(3600))?.div(60)
        val minutes = seconds?.rem(60)

        return Pair(hours, minutes)
    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }
}
