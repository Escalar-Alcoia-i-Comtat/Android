package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
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

            override fun getBarChartData(objectId: String): MutableState<BarChartData?> =
                mutableStateOf(
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

            override fun loadSectors(zoneId: String): MutableState<List<Sector>> =
                mutableStateOf(listOf(Sector.SAMPLE_SECTOR))
        }
    }

    /**
     * Gets the [BarChartData] of a DataClass.
     * @author Arnau Mora
     * @since 20220106
     * @return A [MutableState] that contains the [BarChartData] and gets updated with the correct
     * data once loaded. May be null when still not loaded.
     */
    fun getBarChartData(objectId: String): MutableState<BarChartData?>

    /**
     * Loads the [Sector]s contained inside the [Zone] with [Zone.objectId] [zoneId].
     * @author Arnau Mora
     * @since 20220106
     * @param zoneId The [Zone.objectId] of the [Zone] to get the [Sector]s from.
     */
    fun loadSectors(zoneId: String): MutableState<List<Sector>>

}