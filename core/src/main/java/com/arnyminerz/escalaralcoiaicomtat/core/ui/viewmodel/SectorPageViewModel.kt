package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import me.bytebeats.views.charts.bar.BarChartData

interface SectorPageViewModel {
    /**
     * A Mutable state delegation of [BarChartData] with the data to display.
     * @author Arnau Mora
     * @since 20220106
     */
    var barChartData: BarChartData

    /**
     * A Mutable State delegation of a list of paths with the paths to display.
     * @author Arnau Mora
     * @since 20220106
     */
    var paths: List<Path>

    /**
     * Loads the [BarChartData] of [sector] into [loadBarChartData].
     * @author Arnau Mora
     * @since 20220106
     */
    fun loadBarChartData(sector: Sector)

    /**
     * Loads the list of [Path] contained in [sector].
     * @author Arnau Mora
     * @since 20220106
     * @param sector The [Sector] to load the [Path] from.
     */
    fun loadPaths(sector: Sector)
}