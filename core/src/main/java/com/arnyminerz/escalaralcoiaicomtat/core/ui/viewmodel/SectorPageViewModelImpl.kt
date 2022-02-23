package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.grade_black
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.grade_blue
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.grade_green
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.grade_red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bytebeats.views.charts.bar.BarChartData
import timber.log.Timber

class SectorPageViewModelImpl(application: Application) : AndroidViewModel(application),
    SectorPageViewModel {

    override var paths: List<Path> by mutableStateOf(emptyList())

    override var barChartData: BarChartData by mutableStateOf(
        BarChartData(
            bars = listOf(
                BarChartData.Bar(
                    label = "3º-5+",
                    value = 0f,
                    color = grade_green,
                ),
                BarChartData.Bar(
                    label = "6a-6c+",
                    value = 0f,
                    color = grade_blue,
                ),
                BarChartData.Bar(
                    label = "7a-7c+",
                    value = 0f,
                    color = grade_red,
                ),
                BarChartData.Bar(
                    label = "8a-8c+",
                    value = 0f,
                    color = grade_black,
                ),
            )
        )
    )

    override fun loadBarChartData(sector: Sector) {
        viewModelScope.launch {
            val bars = withContext(Dispatchers.IO) {
                Timber.d("Loading path grades...")
                val paths = sector.getChildren(app.searchSession) { it.objectId }
                var grades1Count = 0 // 3º-5+
                var grades2Count = 0 // 6a-6c+
                var grades3Count = 0 // 7a-7c+
                var grades4Count = 0 // 8a-8c+
                Timber.d("Got ${paths.size} paths. Getting grades...")
                for (path in paths)
                    path.generalGrade.let {
                        Timber.v("- Grade: $it")
                        when {
                            it.matches("^[3-5]".toRegex()) -> grades1Count++
                            it.matches("^6".toRegex()) -> grades2Count++
                            it.matches("^7".toRegex()) -> grades3Count++
                            else -> grades4Count++
                        }
                    }

                Timber.d("Grades processed: $grades1Count, $grades2Count, $grades3Count, $grades4Count.")
                listOf(
                    BarChartData.Bar(
                        label = "3º-5+",
                        value = grades1Count.toFloat(),
                        color = grade_green,
                    ),
                    BarChartData.Bar(
                        label = "6a-6c+",
                        value = grades2Count.toFloat(),
                        color = grade_blue,
                    ),
                    BarChartData.Bar(
                        label = "7a-7c+",
                        value = grades3Count.toFloat(),
                        color = grade_red,
                    ),
                    BarChartData.Bar(
                        label = "8a-8c+",
                        value = grades4Count.toFloat(),
                        color = grade_black,
                    ),
                )
            }

            Timber.d("BarChartData built. Updating value...")
            barChartData = barChartData.copy(bars = bars)
        }
    }

    override fun loadPaths(sector: Sector) {
        viewModelScope.launch {
            val pathsList = withContext(Dispatchers.IO) {
                Timber.d("Loading paths from $sector...")
                sector.getChildren(app.searchSession) { it.sketchId }
            }
            paths = pathsList
        }
    }

    override fun loadImage(sector: Sector) = sector.imageData(context)

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(SectorPageViewModelImpl::class.java))
                return SectorPageViewModelImpl(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}