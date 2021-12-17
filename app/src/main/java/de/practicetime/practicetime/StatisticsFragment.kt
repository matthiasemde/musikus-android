package de.practicetime.practicetime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import de.practicetime.practicetime.entities.SectionWithCategory
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoField
import kotlin.collections.ArrayList


class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var dao: PTDao
    private lateinit var chart: BarChart
    private var weekViewWeekOffset = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        chart = view.findViewById(R.id.chart) as BarChart

        view.findViewById<Button>(R.id.btn_week).setOnClickListener {
            toggleDaysView()
        }
        view.findViewById<Button>(R.id.btn_month).setOnClickListener {
            toggleWeeksView()
        }
        view.findViewById<Button>(R.id.btn_year).setOnClickListener {
            toggleMonthsView()
        }
        view.findViewById<Button>(R.id.btn_rwnd).setOnClickListener {
            weekViewWeekOffset--
            lifecycleScope.launch { initChart(view, getWeekData()) }
        }
        view.findViewById<Button>(R.id.btn_fwd).setOnClickListener {
            weekViewWeekOffset++
            lifecycleScope.launch { initChart(view, getWeekData()) }
        }

        openDatabase()

        lifecycleScope.launch { initChart(view, getWeekData()) }

        val now = ZonedDateTime.now().toEpochSecond()
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
    private fun getEndOfWeek(weeksBack: Long): ZonedDateTime {
        return getEndOfDayOfWeek(1, weeksBack).plusWeeks(1)
    }

    private fun getEndOfMonth(monthOffset: Long): Long {
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_MONTH , 1 )
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusMonths(monthOffset)
            .plusMonths(1)
            .toEpochSecond()
    }

    // get the Beginning of a Day (1=Mo, 7=Sun) of the current week (weeksBack=0) / the weeks before (weeksBack>0)
    private fun getStartOfDayOfWeek(dayIndex: Long, weekOffset: Long): ZonedDateTime {
        return ZonedDateTime.now()
            .with(ChronoField.DAY_OF_WEEK , dayIndex )         // ISO 8601, Monday is first day of week.
            .toLocalDate().atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusWeeks(weekOffset)
    }

    // get the End of a Day (1=Mo, 7=Sun) of the current week (weeksBack=0) / the weeks before (weeksBack>0)
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

    private fun toggleDaysView() {

    }

    private fun toggleWeeksView() {

    }

    private fun toggleMonthsView() {

    }

    private fun initChartView() {

    }

    private suspend fun getWeekData(): ArrayList<BarEntry> {
        val chartArray = arrayListOf<BarEntry>()

        for (day in 1..7) {

            val floatArrDurList = arrayListOf<Float>()

            val start = getStartOfDayOfWeek(day.toLong(), weekViewWeekOffset).toEpochSecond()
            val end = getEndOfDayOfWeek(day.toLong(), weekViewWeekOffset).toEpochSecond()

            Log.d("TAG", "Querying from $start until $end")

            var sectionsThisDay = dao.getSectionsWithCateories(start, end)

            sectionsThisDay = squashDuplicateCategories(sectionsThisDay)

            sectionsThisDay.forEachIndexed { index, element ->
                floatArrDurList.add(element.section.duration!!.toFloat())

                Log.d(
                    "TAG",
                    "Section ${element.category.name} dur=${element.section.duration} added on day $day"
                )
            }

            // the list of values for the stacked bar for that day
            val floatArrDurs = floatArrDurList.toFloatArray()
            chartArray.add(BarEntry(day.toFloat(), floatArrDurs))
        }
        return chartArray
    }

    private fun squashDuplicateCategories(sectionsThisDay: List<SectionWithCategory>): List<SectionWithCategory> {
        // TODO not yet implemented
        return sectionsThisDay
    }

    private fun initChart(view: View, chartArray: ArrayList<BarEntry>) {

        val dataSet = BarDataSet(chartArray, "Label")

        Log.d("TGA", "ChartArray Length: ${chartArray.size}")
        Log.d("TGA", "ChartArray: ${chartArray}")

        val categoryColors = context?.resources?.getIntArray(R.array.category_colors)?.toCollection(mutableListOf())


//        val colors = mutableListOf<Int>()
//        var labels = Array(activeCategories.size){""}
//        // sort the colors and labels according to activeCategories
//        activeCategories.forEachIndexed { i, cat ->
//            // add the respective color of current category to the list
//            colors.add(i, categoryColors!![cat.colorIndex])
//            labels[i] = cat.name
//        }

        dataSet.colors = categoryColors
//        dataSet.stackLabels = arrayOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")


        val data = BarData(dataSet)
        data.barWidth = 0.4f

        chart.data = data
        chart.setTouchEnabled(false)
        chart.invalidate()

        // axis settings
        val leftAxis = chart.axisLeft
        val xAxis = chart.xAxis
        val rightAxis = chart.axisRight
        leftAxis.axisMinimum = 0f   // needed for y axis scaling (probably a bug!)
        leftAxis.isEnabled = false
        rightAxis.axisMinimum = 0f
        rightAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM

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
}