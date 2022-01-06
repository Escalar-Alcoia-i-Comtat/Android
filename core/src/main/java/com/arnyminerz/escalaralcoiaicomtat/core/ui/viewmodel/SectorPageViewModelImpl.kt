package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.color
import kotlinx.coroutines.launch
import me.bytebeats.views.charts.bar.BarChartData
import timber.log.Timber

class SectorPageViewModelImpl(application: Application) : AndroidViewModel(application),
    SectorPageViewModel {

    override fun getBarChartData(objectId: String): MutableState<BarChartData?> {
        val data = mutableStateOf<BarChartData?>(null)

        data.value = BarChartData(
            bars = listOf(
                BarChartData.Bar(
                    label = "Demo 1",
                    value = 5f,
                    color = ContextCompat.getColor(context, R.color.grade_green).color()
                ),
                BarChartData.Bar(
                    label = "Demo 2",
                    value = 7f,
                    color = ContextCompat.getColor(context, R.color.grade_blue).color()
                )
            )
        )

        return data
    }

    override fun loadSectors(zoneId: String): MutableState<List<Sector>> {
        val state = mutableStateOf<List<Sector>>(emptyList())

        viewModelScope.launch {
            // First get the parent zone
            Timber.d("Loading zone $zoneId...")
            val zone = app.getZone(zoneId) ?: run {
                // If the zone was not found
                Timber.e("Zone with id $zoneId could not be found!")
                return@launch
            }
            // Then get the children sectors
            Timber.d("Loading children sectors of zone $zoneId...")
            val sectors = zone.getChildren(app.searchSession)
            // And pass them to the state
            state.value = sectors
        }

        return state
    }

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