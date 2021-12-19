package de.practicetime.practicetime

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var dao: PTDao
    private lateinit var chart: BarChart
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
        var visible: Boolean
    )

    private enum class VIEWS(val barCount: Int) {
        DAYS_VIEW(7),   // be careful to change because 7 means Mon-Sun here!
        WEEKS_VIEW(7), // current week + last 9 weeks
        MONTHS_VIEW(7),  // current month + last 11 months
    }
    private var activeView = VIEWS.DAYS_VIEW

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // get the dao object
        openDatabase()

        view.findViewById<AppCompatButton>(R.id.btn_days).setOnClickListener {
            activeView = VIEWS.DAYS_VIEW
            updateChartData()
            setBtnEnabledState(view)
            // reset time ranges for other views
            weeksViewWeekOffset = 0L
            monthsViewMonthOffset = 0L
        }
        view.findViewById<AppCompatButton>(R.id.btn_weeks).setOnClickListener {
            activeView = VIEWS.WEEKS_VIEW
            updateChartData()
            setBtnEnabledState(view)
            // reset time ranges for other views
            daysViewWeekOffset = 0L
            monthsViewMonthOffset = 0L
        }
        view.findViewById<AppCompatButton>(R.id.btn_months).setOnClickListener {
            activeView = VIEWS.MONTHS_VIEW
            updateChartData()
            setBtnEnabledState(view)
            // reset time ranges for other views
            daysViewWeekOffset = 0L
            weeksViewWeekOffset = 0L
        }
        view.findViewById<ImageButton>(R.id.btn_rwnd).setOnClickListener {
            seekPast(view)
        }
        view.findViewById<ImageButton>(R.id.btn_fwd).setOnClickListener {
            seekFuture(view)
        }

        colorAmount = context?.resources?.getIntArray(R.array.category_colors)?.toCollection(mutableListOf())?.size ?: 0
        chart = view.findViewById(R.id.chart) as BarChart
        initChartView()
        updateChartData()
        setBtnEnabledState(view)

        initCategoryList()
    }

    private fun initCategoryList() {
        lifecycleScope.launch {
            dao.getAllCategories().forEach {
                categories.add(CategoryListElement(it, selected = true, visible = false))
            }
            categoryListAdapter = CategoryStatsAdapter()
            val layoutManager = LinearLayoutManager(requireContext())

            val categoryRecyclerView = requireView().findViewById<RecyclerView>(R.id.recyclerview_categories_statistics)
            categoryRecyclerView.layoutManager = layoutManager
            categoryRecyclerView.adapter = categoryListAdapter
        }
    }

    private fun initChartView() {
        // for rounded bars: https://gist.github.com/xanscale/e971cc4f2f0712a8a3bcc35e85325c27
        //      issue: https://github.com/PhilJay/MPAndroidChart/issues/2771#issuecomment-566719474

        chart.setTouchEnabled(false)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawValueAboveBar(true)
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
        rightAxis.valueFormatter = YAxisValueFormatter()

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
            val values = when (activeView) {
                VIEWS.DAYS_VIEW -> getMoToFrArray()
                VIEWS.WEEKS_VIEW -> getWeeksArray()
                VIEWS.MONTHS_VIEW -> getMonthsArray()
            }

            val dataSet: BarDataSet

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

    /**
     * construct array for "week" view -> each bar = 1 day
     */
    private suspend fun getMoToFrArray(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()
        val visibleCategories = ArrayList<Int>()

        categories.forEach { it.totalDuration = 0 }

        for (day in VIEWS.DAYS_VIEW.barCount downTo 1) {
            val floatArrDur = FloatArray(colorAmount) {0f}
            val sectionsThisDay = dao.getSectionsWithCategories(
                getStartOfDayOfWeek(day.toLong(), daysViewWeekOffset).toEpochSecond(),
                getEndOfDayOfWeek(day.toLong(), daysViewWeekOffset).toEpochSecond()
            )
            sectionsThisDay.forEach { (section, category) ->
                // only show selected entries (checkbox enabled)
                if(categories.any { it.selected && it.category.id == category.id }) {
                    // sum all section duration with same color (regardless whether they are actually the same category)
                    floatArrDur[category.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleCategories.add(category.id)
                categories.first { it.category.id == category.id}.totalDuration += section.duration ?: 0
            }
            chartArray.add(BarEntry(day.toFloat(), floatArrDur))
        }

        updateVisibleCategories(visibleCategories)
        return chartArray
    }

    /**
     * construct array for "months" view -> each bar = 1 week
     */
    private suspend fun getWeeksArray(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()
        val visibleCategories = ArrayList<Int>()

        for (week in 0 downTo -(VIEWS.WEEKS_VIEW.barCount-1)) {     // last 10 weeks

            val floatArrDur = FloatArray(colorAmount) {0f}
            val sectionsThisWeek = dao.getSectionsWithCategories(
                getStartOfWeek(week.toLong() + weeksViewWeekOffset).toEpochSecond(),
                getEndOfWeek(week.toLong() + weeksViewWeekOffset).toEpochSecond()
            )
            sectionsThisWeek.forEach { (section, category) ->
                // only show selected entries (checkbox enabled)
                if(categories.any { it.selected && it.category.id == category.id }) {
                    floatArrDur[category.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleCategories.add(category.id)
                categories.first { it.category.id == category.id}.totalDuration += section.duration ?: 0
            }
            chartArray.add(BarEntry((week + weeksViewWeekOffset).toFloat(), floatArrDur))
        }

        updateVisibleCategories(visibleCategories)
        return chartArray
    }


    /**
     * construct array for "year" view -> each bar = 1 month
     */
    private suspend fun getMonthsArray(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()
        val visibleCategories = ArrayList<Int>()

        for (month in 0 downTo -(VIEWS.MONTHS_VIEW.barCount-1)) {
            val floatArrDur = FloatArray(colorAmount) {0f}
            val sectionsThisMonth = dao.getSectionsWithCategories(
                getStartOfMonth(month.toLong() + monthsViewMonthOffset).toEpochSecond(),
                getEndOfMonth(month.toLong() + monthsViewMonthOffset).toEpochSecond()
            )
            sectionsThisMonth.forEach { (section, category) ->
                // only show selected entries (checkbox enabled)
                if(categories.any { it.selected && it.category.id == category.id }) {
                    floatArrDur[category.colorIndex] += (section.duration ?: 0).toFloat()
                }
                visibleCategories.add(category.id)
                categories.first { it.category.id == category.id}.totalDuration += section.duration ?: 0
            }
            chartArray.add(BarEntry((month + monthsViewMonthOffset).toFloat(), floatArrDur))
        }

        updateVisibleCategories(visibleCategories)
        return chartArray
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
        if(elemRemovedOrInserted) requireView().findViewById<RecyclerView>(R.id.recyclerview_categories_statistics).scrollToPosition(0)

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

    private inner class CategoryStatsAdapter : RecyclerView.Adapter<CategoryStatsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val catCheckBox: CheckBox = view.findViewById(R.id.checkbox_category)
            val catTimeView: TextView = view.findViewById(R.id.total_time_category)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryStatsAdapter.ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_statistics_category_list_item, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryStatsAdapter.ViewHolder, position: Int) {

            val elem = categories.filter { it.visible }[position]
            val categoryColors = requireContext().resources.getIntArray(R.array.category_colors)

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

            holder.catTimeView.text = secondsToTimeString(elem.totalDuration)
        }

        override fun getItemCount(): Int = categories.filter { it.visible }.size

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
