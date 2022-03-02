package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetMarkerCentering
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata.MutableListLiveData
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import timber.log.Timber

class MainMapViewModel(
    application: Application,
    markerCentering: GetMarkerCentering
) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val locations: MutableListLiveData<GeoPoint> = MutableListLiveData<GeoPoint>().apply {
        postValue(mutableListOf())
    }

    val centerMarkerOnClick: StateFlow<Boolean> = markerCentering().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    fun loadAreasIntoMap(
        mapView: MapView,
        areas: List<Area>,
        @UiThread finishListener: () -> Unit
    ) {
        Timber.i("Loading KMZ of ${areas.size} areas...")
        viewModelScope.launch {
            try {
                for (area in areas) {
                    Timber.d("Loading KMZ file of $area...")
                    val kmzFile = area.kmzFile(context, false)
                    Timber.d("Parsing KMZ file of $area...")
                    val kmzDocument = KmlDocument()
                    kmzDocument.parseKMZFile(kmzFile)

                    Timber.d("Building KMZ map overlay of $area...")
                    val kmzOverlay =
                        kmzDocument.mKmlRoot.buildOverlay(mapView, null, null, kmzDocument)

                    Timber.d("Adding KMZ overlay to the map...")
                    mapView.overlays.add(kmzOverlay)
                }

                uiContext {
                    Timber.d("Invalidating map...")
                    mapView.invalidate()
                    Timber.d("Centering into map overlays...")
                    var boundingBox: BoundingBox? = null
                    for (overlay in mapView.overlays)
                        boundingBox = boundingBox?.concat(overlay.bounds) ?: overlay.bounds
                    boundingBox?.let { mapView.zoomToBoundingBox(it, false) }
                    finishListener()
                }
            }catch (e: VolleyError) {
                Timber.e(e, "Could not download KMZ file.")
            }
        }
    }

    class Factory(
        private val application: Application,
        private val centerMarkerOnClick: GetMarkerCentering
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(MainMapViewModel::class.java))
                return MainMapViewModel(application, centerMarkerOnClick) as T
            error("Unknown view model class: $modelClass")
        }
    }
}