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
     * Loads a [Sector] contained in [Zone].
     * @author Arnau Mora
     * @since 20220106
     * @param zone The [Zone] to load the [Sector] from.
     * @param index The position of the [Sector] inside of the [Zone].
     * @return A [MutableState] that contains a [Sector]. Is null while loading.
     */
    fun loadSector(zone: Zone, index: Int): MutableState<Sector?>
}