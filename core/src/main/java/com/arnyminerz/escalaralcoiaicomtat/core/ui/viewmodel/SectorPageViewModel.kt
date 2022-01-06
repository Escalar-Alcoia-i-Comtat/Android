package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.compose.runtime.MutableState
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import me.bytebeats.views.charts.bar.BarChartData

interface SectorPageViewModel {
    /**
     * Gets the [BarChartData] of a DataClass.
     * @author Arnau Mora
     * @since 20220106
     * @return A [MutableState] that contains the [BarChartData] and gets updated with the correct
     * data once loaded. May be null when still not loaded.
     */
    fun getBarChartData(objectId: String): MutableState<BarChartData?>

    /**
     * Gets the parent zone of a [sector].
     * @author Arnau Mora
     * @since 20220106
     * @return A [MutableState] that contains the parent [Zone] of [sector] or null if it's still
     * loading.
     */
    fun loadZone(sector: Sector): MutableState<Zone?>

    /**
     * Gets the sectors that are contained inside a [Zone].
     * @author Arnau Mora
     * @since 20220106
     * @return A [MutableState] that contains the [List] of children [Sector]s. May be empty while
     * loading.
     */
    fun loadSectors(zone: Zone): MutableState<List<Sector>>
}