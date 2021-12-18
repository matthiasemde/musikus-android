package de.practicetime.practicetime

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.collections.ArrayList


class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var dao: PTDao
    private lateinit var chart: BarChart
    private var weekViewWeekOffset = 0L
    private var monthViewWeekOffset = 0L
    private var yearViewMonthOffset = 0L
    private var colorAmount = 0

    private enum class VIEWS(val barCount: Int) {
        WEEK_VIEW(7),   // be careful to change because 7 means Mon-Sun here!
        MONTH_VIEW(7), // current week + last 9 weeks
        YEAR_VIEW(7),  // current month + last 11 months
    }

    private var activeView = VIEWS.WEEK_VIEW

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        colorAmount = context?.resources?.getIntArray(R.array.category_colors)?.toCollection(mutableListOf())?.size ?: 0

        chart = view.findViewById(R.id.chart) as BarChart

        view.findViewById<Button>(R.id.btn_week).setOnClickListener {
            activeView = VIEWS.WEEK_VIEW
            updateChartData()
            setBtnEnabledState(view)
            // reset time ranges for other views
            monthViewWeekOffset = 0L
            yearViewMonthOffset = 0L
        }
        view.findViewById<Button>(R.id.btn_month).setOnClickListener {
            activeView = VIEWS.MONTH_VIEW
            updateChartData()
            setBtnEnabledState(view)
            // reset time ranges for other views
            weekViewWeekOffset = 0L
            yearViewMonthOffset = 0L
        }
        view.findViewById<Button>(R.id.btn_year).setOnClickListener {
            activeView = VIEWS.YEAR_VIEW
            updateChartData()
            setBtnEnabledState(view)
            // reset time ranges for other views
            weekViewWeekOffset = 0L
            monthViewWeekOffset = 0L
        }
        view.findViewById<Button>(R.id.btn_rwnd).setOnClickListener {
            seekPast(view)
        }
        view.findViewById<Button>(R.id.btn_fwd).setOnClickListener {
            seekFuture(view)
        }

        openDatabase()

        initChartView()
        updateChartData()
        setBtnEnabledState(view)
    }

    private fun initChartView() {
        // for rounded bars: https://gist.github.com/xanscale/e971cc4f2f0712a8a3bcc35e85325c27
        //      issue: https://github.com/PhilJay/MPAndroidChart/issues/2771#issuecomment-566719474

        chart.setTouchEnabled(false)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawValueAboveBar(true)
        chart.animateY(1000, Easing.EaseOutBack)
        chart.invalidate()

        // axis settings
        val leftAxis = chart.axisLeft
        val xAxis = chart.xAxis
        val rightAxis = chart.axisRight

        leftAxis.axisMinimum = 0f   // needed for y axis scaling (probably a bug!)
        leftAxis.isEnabled = false
        rightAxis.axisMinimum = 0f
        rightAxis.setDrawAxisLine(false)
        rightAxis.textColor = getThemeColor(R.attr.colorOnSurfaceLowerContrast)

        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.labelCount = activeView.barCount
        xAxis.valueFormatter = XAxisValueFormatter()
        xAxis.textColor = getThemeColor(R.attr.colorOnSurface)

    }

    private fun setHeadingTextViews(view: View) {
        val tvRange = view.findViewById<TextView>(R.id.tv_visible_range)
        val tvTotalTimeInRange = view.findViewById<TextView>(R.id.tv_total_time_visible_range)

        val chartArray = chart.data.getDataSetByIndex(0) as BarDataSet
        // because we're always counting down in the loops, xVals are chronologically reversed
        val firstBarXVal = chartArray.xMin
        val lastBarXVal = chartArray.xMax
        when(activeView) {

            VIEWS.WEEK_VIEW -> {
                val start = getStartOfWeek(weekViewWeekOffset)
                    .format(DateTimeFormatter.ofPattern("MMMM d"))

                var formatStr = "d"
                if (getStartOfWeek(weekViewWeekOffset).month != getEndOfWeek(weekViewWeekOffset).minusDays(1).month) {
                    formatStr = "MMMM d"    // also show month if it is different from startMonth
                }

                val end = getEndOfWeek(weekViewWeekOffset)
                    .minusDays(1)   // subtract one day because of half-open approach
                    .format(DateTimeFormatter.ofPattern(formatStr))
                tvRange.text = ("$start - $end")
            }

            VIEWS.MONTH_VIEW -> {
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

            VIEWS.YEAR_VIEW -> {
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
        tvTotalTimeInRange.text = "${chartArray.values.sumOf { it.yVals.sum().toLong() }.toString()} sec"
    }

    private fun setBtnEnabledState(view: View) {
        val btnWeek = view.findViewById<Button>(R.id.btn_week)
        val btnMonth = view.findViewById<Button>(R.id.btn_month)
        val btnYear = view.findViewById<Button>(R.id.btn_year)
        val btnFwd = view.findViewById<Button>(R.id.btn_fwd)

        btnWeek.isEnabled = true
        btnMonth.isEnabled = true
        btnYear.isEnabled = true

        when(activeView) {
            VIEWS.WEEK_VIEW -> {
                btnFwd.isEnabled = weekViewWeekOffset != 0L
                btnWeek.isEnabled = false
            }
            VIEWS.MONTH_VIEW -> {
                btnFwd.isEnabled = monthViewWeekOffset != 0L
                btnMonth.isEnabled = false
            }
            VIEWS.YEAR_VIEW -> {
                btnFwd.isEnabled = yearViewMonthOffset != 0L
                btnYear.isEnabled = false
            }
        }
    }

    private fun updateChartData() {
        lifecycleScope.launch {
            val values = when (activeView) {
                VIEWS.WEEK_VIEW -> getMoToFrArray()
                VIEWS.MONTH_VIEW -> getWeeksArray()
                VIEWS.YEAR_VIEW -> getMonthsArray()
            }

            var dataSet: BarDataSet

            if (chart.data != null && chart.data.dataSetCount > 0) {
                dataSet = chart.data.getDataSetByIndex(0) as BarDataSet
                dataSet.values = values
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()

            } else {
                // first time drawing chart, create the DataSet from values
                dataSet = BarDataSet(values, "Label")

                val categoryColors = context?.resources?.getIntArray(R.array.category_colors)?.toCollection(mutableListOf())
                dataSet.colors = categoryColors
                dataSet.setDrawValues(true)

                val data = BarData(dataSet)
                data.barWidth = 0.4f
                data.setValueFormatter(BarChartValueFormatter())
                data.setValueTextColor(getThemeColor(R.attr.colorOnSurfaceLowerContrast))
                data.setValueTextSize(12f)

                chart.data = data
            }
            // redraw the chart
            chart.animateY(1000, Easing.EaseOutBack)
            chart.invalidate()

            // update the Heading
            setHeadingTextViews(requireView())
        }
    }

    private fun seekPast(view: View) {
        when(activeView) {
            VIEWS.WEEK_VIEW ->
                weekViewWeekOffset--    // special treatment for weekview because we always want Mo-Sun range
            VIEWS.MONTH_VIEW ->
                monthViewWeekOffset -= activeView.barCount
            VIEWS.YEAR_VIEW ->
                yearViewMonthOffset -= activeView.barCount
        }
        updateChartData()
        setBtnEnabledState(view)
    }

    private fun seekFuture(view: View) {
        when(activeView) {
            VIEWS.WEEK_VIEW ->
                weekViewWeekOffset++    // special treatment for weekview because we always want Mo-Sun range
            VIEWS.MONTH_VIEW ->
                monthViewWeekOffset += activeView.barCount
            VIEWS.YEAR_VIEW ->
                yearViewMonthOffset += activeView.barCount
        }
        updateChartData()
        setBtnEnabledState(view)
    }

    /**
     * construct array for "week" view -> each bar = 1 day; 7 bars per chart
     */
    private suspend fun getMoToFrArray(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()

        for (day in VIEWS.WEEK_VIEW.barCount downTo 1) {
            val floatArrDur = FloatArray(colorAmount) {0f}
            val sectionsThisDay = dao.getSectionsWithCategories(
                getStartOfDayOfWeek(day.toLong(), weekViewWeekOffset).toEpochSecond(),
                getEndOfDayOfWeek(day.toLong(), weekViewWeekOffset).toEpochSecond()
            )
            sectionsThisDay.forEach { (section, category) ->
                floatArrDur[category.colorIndex] += (section.duration ?:0).toFloat()
//                log("Section ${category.name} dur=${section.duration} added on day $day")
            }
            chartArray.add(BarEntry(day.toFloat(), floatArrDur))
        }

        return chartArray
    }

    /**
     * construct array for "months" view -> each bar = 1 week; 10 bars per chart
     */
    private suspend fun getWeeksArray(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()

        for (week in 0 downTo -(VIEWS.MONTH_VIEW.barCount-1)) {     // last 10 weeks

            log("Week $week ( ${getStartOfWeek(week.toLong() + monthViewWeekOffset)} bis ${getEndOfWeek(week.toLong() + monthViewWeekOffset)}:")

            val floatArrDur = FloatArray(colorAmount) {0f}
            val sectionsThisWeek = dao.getSectionsWithCategories(
                getStartOfWeek(week.toLong() + monthViewWeekOffset).toEpochSecond(),
                getEndOfWeek(week.toLong() + monthViewWeekOffset).toEpochSecond()
            )
            sectionsThisWeek.forEach { (section, category) ->
                floatArrDur[category.colorIndex] += (section.duration ?:0).toFloat()
                log("\t${category.name} (${section.duration})")
            }
            chartArray.add(BarEntry((week + monthViewWeekOffset).toFloat(), floatArrDur))
        }
        return chartArray
    }


    /**
     * construct array for "year" view -> each bar = 1 month; 12 bars per chart
     */
    private suspend fun getMonthsArray(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()

        for (month in 0 downTo -(VIEWS.YEAR_VIEW.barCount-1)) {
            val floatArrDur = FloatArray(colorAmount) {0f}
            val sectionsThisMonth = dao.getSectionsWithCategories(
                getStartOfMonth(month.toLong() + yearViewMonthOffset).toEpochSecond(),
                getEndOfMonth(month.toLong() + yearViewMonthOffset).toEpochSecond()
            )
            sectionsThisMonth.forEach { (section, category) ->
                floatArrDur[category.colorIndex] += (section.duration ?:0).toFloat()
                log("\t${category.name} (${section.duration})")
            }
            chartArray.add(BarEntry((month + yearViewMonthOffset).toFloat(), floatArrDur))
        }

        // Logging
        chartArray.forEach {
            log("Month: ${it.x}:")
            it.yVals.forEach {
                log("\t\tCategory with $it seconds")
            }
        }

        return chartArray
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
        if (dayIndex > 6)
            nextDay = (dayIndex + 1) % 7
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_WEEK, nextDay)         // ISO 8601, Monday is first day of week.
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusWeeks(weekOffset)
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private fun log(msg: String) {
        Log.d("STATISTICS", msg)
    }

    inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(value: Float): String {
            return when (activeView) {
                VIEWS.WEEK_VIEW ->
                    getDayString(value)
                VIEWS.MONTH_VIEW ->
                    getWeekString(value)
                VIEWS.YEAR_VIEW ->
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
     * This ValueFormatter shows the sum of all stacked Bars on top of the each bar instead of for every segment
     * It should do the same as StackedValueFormatter but this one doesn't work for our case because we have
     * "invisible" stacked segments with value=0 in our bars which results to no call in getBarStackedLabel() and thus
     * the sum doesn't get drawn. This one only uses non-zero entries to determine the top position
     */
    inner class BarChartValueFormatter: ValueFormatter() {

        var lastEntryX = -1f            // the x entry (=Bar ID) of last time getBarStackedLabel() was called
        var stackCounterCurrentBar = 0  // counter variable counting the stack we are inside the current bar
        var stackEntriesNotZero = 0     // amount of non-zero entries in this bar

        // getBarStackedLabel is called on every bar for every non-zero segment
        // value: the y value of current segment
        // stackedEntry: the whole BarEntry object for current bar, always the same for each stack on the same Bar
        override fun getBarStackedLabel(value: Float, stackedEntry: BarEntry?): String {
            if (stackedEntry?.x != lastEntryX) {
                // first stack on a new bar
                stackCounterCurrentBar = 1
                stackEntriesNotZero = stackedEntry?.yVals?.filterNot { it == 0f }?.count() ?: 0

                // show 0 if there are no stacks in this bar
                if (stackEntriesNotZero == 0)
                    return "0"

                // show value if there is only 1 stack in this bar
                if (stackEntriesNotZero == 1) {
                    return stackedEntry?.yVals?.sum()?.toInt().toString()
                }
            } else {
                stackCounterCurrentBar++
                if (stackCounterCurrentBar == stackEntriesNotZero) {
                    // we reached the last non-zero stack of the bar, so we're at the top
                    return stackedEntry?.yVals?.sum()?.toInt().toString()
                }
            }

            lastEntryX = stackedEntry?.x ?: -1f
            return ""   // return empty string so no value is drawn
        }
    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }
}
