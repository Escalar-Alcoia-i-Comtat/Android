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

@ExperimentalUnsignedTypes
class BarChartHelper private constructor(
    private val barData: BarData,
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
            if (grade.isEmpty()) return -1

            var b = 0
            barsList.forEach { bar ->
                bar.first.forEach { gr ->
                    if (grade[0] == gr)
                        return b
                }
                b++
            }

            return -1
        }

        private val yFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry): String =
                barEntry.y.toInt().toString()

            override fun getAxisLabel(value: Float, axis: AxisBase?): String = ""
        }

        fun fromPaths(context: Context, paths: Collection<Path>): BarChartHelper {
            val gradeEntries = arrayListOf<BarEntry>()
            val grades = hashMapOf<Int, Pair<Grade, Int>?>(
                0 to null,
                1 to null,
                2 to null,
                3 to null
            ) //ID, [Grade, count]
            val gradeQuarters = arrayListOf<String>()
            val gradeColors = arrayListOf<Int>()
            paths.forEach { path ->
                try {
                    val grade = path.grade()
                    val id = barIndex(grade.displayName)
                    if (id >= 0)
                        if (grades.containsKey(id))
                            grades[id] = Pair(grade, (grades[id]?.second ?: 0) + 1)
                        else
                            grades[id] = Pair(grade, 1)
                } catch (ex: NoSuchElementException) {
                }
            }
            var c = 0
            grades.forEach { (id, gradec) ->
                val formatting = if (gradec != null) {
                    val grade = gradec.first
                    val count = gradec.second

                    Pair(BarEntry(c.toFloat(), count.toFloat()), grade.color())
                } else {
                    Pair(BarEntry(c.toFloat(), 0.5f), Grade.gradeColor(barsList[id].second))
                }
                gradeEntries.add(formatting.first)

                gradeQuarters.add(barsList[id].second)
                gradeColors.add(formatting.second)
                c++
            }

            val dataSet =
                BarDataSet(gradeEntries, context.getString(R.string.path_stats_chart_title))
            dataSet.setColors(gradeColors.toIntArray(), context)
            dataSet.valueFormatter = yFormatter
            dataSet.valueTextSize = 14f
            return BarChartHelper(BarData(dataSet), gradeQuarters)
        }
    }

    val data: BarData
        get() = barData

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