package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.color
import me.bytebeats.views.charts.bar.BarChartData

class SectorPageViewModelImpl(application: Application) : AndroidViewModel(application),
    SectorPageViewModel {

    override fun getBarChartData(objectId: String): MutableState<BarChartData?> =
        mutableStateOf<BarChartData?>(null).apply {
            value = BarChartData(
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
        }

    override fun loadSector(zone: Zone, index: Int): MutableState<Sector?> {
        TODO("Not yet implemented")
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