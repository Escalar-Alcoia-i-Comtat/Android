package com.arnyminerz.escalaralcoiaicomtat.view

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.Grade
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import timber.log.Timber

@ExperimentalUnsignedTypes
class BarChartHelper private constructor(
    val barData: BarData,
    private val quarters: ArrayList<String>
) {
    companion object {
        private val barsList = listOf(
            Pair(
                listOf('3', '4', '5'),
                "3º-5+"
            ),
            Pair(
                listOf('6'),
                "6a-6c+"
            ),
            Pair(
                listOf('7'),
                "7a-7c+"
            ),
            Pair(
                listOf('8'),
                "8a-8c+"
            )
        )

        private fun barIndex(grade: String): Int {
            var result = -1
            if (grade.isNotEmpty()) {
                for ((b, bar) in barsList.withIndex())
                    for (gr in bar.first)
                        if (grade[0] == gr) {
                            result = b
                            break
                        }
            }

            return result
        }

        private val yFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry): String =
                barEntry.y.toInt().toString()

            override fun getAxisLabel(value: Float, axis: AxisBase?): String = ""
        }

        fun fromPaths(context: Context, paths: Collection<Path>): BarChartHelper {
            val gradeEntries = arrayListOf<BarEntry>()
            val grades = arrayListOf<Pair<Grade, Int>?>(null, null, null, null)
            val gradeQuarters = arrayListOf<String>()
            val gradeColors = arrayListOf<Int>()
            for (path in paths)
                try {
                    val grade = path.grade()
                    val index = barIndex(grade.displayName)
                    if (index >= 0)
                        if (grades[index] != null)
                            grades[index] = Pair(grade, (grades[index]?.second ?: 0) + 1)
                        else
                            grades[index] = Pair(grade, 1)
                } catch (ex: NoSuchElementException) {
                    Timber.w(ex)
                }

            for ((g, gr) in grades.withIndex()) {
                if (gr == null) continue
                val (grade, count) = gr
                val formatting = Pair(BarEntry(g.toFloat(), count.toFloat()), grade.color())
                gradeEntries.add(formatting.first)

                gradeQuarters.add(barsList[g].second)
                gradeColors.add(formatting.second)
            }

            val dataSet = BarDataSet(gradeEntries, context.getString(R.string.path_stats_chart_title))
            dataSet.setColors(gradeColors.toIntArray(), context)
            dataSet.valueFormatter = yFormatter
            dataSet.valueTextSize = 14f
            return BarChartHelper(BarData(dataSet), gradeQuarters)
        }
    }

    val xFormatter = object : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String =
            try {
                quarters[value.toInt()]
            } catch (_: java.lang.IndexOutOfBoundsException) {
                "N/A"
            }

        override fun getBarLabel(barEntry: BarEntry): String =
            barEntry.x.toInt().toString()
    }

    fun removeStyles(yAxis: YAxis): YAxis =
        with(yAxis) {
            axisMinimum = 0f
            granularity = 1f
            setDrawLabels(false)
            setDrawGridLines(false)
            textSize = 20f
            valueFormatter = yFormatter
            setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
            this
        }
}
