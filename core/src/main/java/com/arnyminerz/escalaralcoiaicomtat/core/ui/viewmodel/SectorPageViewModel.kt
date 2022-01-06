package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import me.bytebeats.views.charts.bar.BarChartData
import kotlin.random.Random

interface SectorPageViewModel {
    companion object {
        val composeViewModel = object : SectorPageViewModel {
            private val colors = mutableListOf(
                Color(0XFFF44336),
                Color(0XFFE91E63),
                Color(0XFF9C27B0),
                Color(0XFF673AB7),
                Color(0XFF3F51B5),
                Color(0XFF03A9F4),
                Color(0XFF009688),
                Color(0XFFCDDC39),
                Color(0XFFFFC107),
                Color(0XFFFF5722),
                Color(0XFF795548),
                Color(0XFF9E9E9E),
                Color(0XFF607D8B)
            )

            private fun randomValue(): Float = Random.Default.nextInt(25, 125).toFloat()
            private fun randomColor(): Color {
                val idx = Random.Default.nextInt(colors.size)
                return colors.removeAt(idx)
            }

            override fun getBarChartData(
                namespace: String,
                objectId: String
            ): MutableState<BarChartData> = mutableStateOf(
                BarChartData(
                    bars = listOf(
                        randomValue().let {
                            BarChartData.Bar(
                                label = it.toString(),
                                value = it,
                                color = randomColor(),
                            )
                        },
                        randomValue().let {
                            BarChartData.Bar(
                                label = it.toString(),
                                value = it,
                                color = randomColor(),
                            )
                        },
                        randomValue().let {
                            BarChartData.Bar(
                                label = it.toString(),
                                value = it,
                                color = randomColor(),
                            )
                        },
                    )
                )
            )
        }
    }

    /**
     * Gets the [BarChartData] of a DataClass.
     * @author Arnau Mora
     * @since 20220106
     * @return A [MutableState] that contains the [BarChartData] and gets updated with the correct
     * data once loaded.
     */
    fun getBarChartData(namespace: String, objectId: String): MutableState<BarChartData>
}