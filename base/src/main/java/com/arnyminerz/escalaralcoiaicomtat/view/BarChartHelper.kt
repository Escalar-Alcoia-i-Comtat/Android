package com.arnyminerz.escalaralcoiaicomtat.view

import android.content.Context
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import timber.log.Timber

class BarChartHelper private constructor(
    val barData: BarData,
    private val quarters: Map<Float, String>
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
            if (grade.isNotEmpty())
                for ((b, bar) in barsList.withIndex())
                    if (bar.first.contains(grade[0])) {
                        result = b
                        break
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
            val grades = arrayMapOf<Int, Pair<Int, Int>>() // Color, Count
            val gradeQuarters = arrayMapOf<Float, String>()
            val gradeColors = arrayListOf<Int>()
            for (path in paths)
                try {
                    val grade = path.grade()
                    val index = barIndex(grade.displayName)
                    if (index >= 0) {
                        Timber.d("Adding grade \"${grade.displayName}\" at $index")
                        grades[index] = Pair(
                            grade.color(),
                            (grades[index]?.second ?: 0) + 1
                        )
                    } else
                        Timber.w("Could not load bar index")
                } catch (ex: NoSuchElementException) {
                    Timber.w(ex)
                }

            for (b in grades.keys) {
                val (gradeColor, count) = grades[b]!!
                val f = b.toFloat()
                Timber.d("Processing bar entry #$f")
                val entry = BarEntry(f, count.toFloat())
                gradeEntries.add(entry)

                gradeQuarters[f] = barsList[b].second
                gradeColors.add(gradeColor)
            }

            val dataSet =
                BarDataSet(gradeEntries, context.getString(R.string.path_stats_chart_title))
            dataSet.setColors(gradeColors.toIntArray(), context)
            dataSet.valueFormatter = yFormatter
            dataSet.valueTextSize = 14f
            return BarChartHelper(BarData(dataSet), gradeQuarters)
        }
    }

    val xFormatter = object : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String =
                quarters.getOrElse(value) {
                    Timber.w("Could not get value at ${value.toInt()} for quarters.")
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
