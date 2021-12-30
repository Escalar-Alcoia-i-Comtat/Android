package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetMarkerCentering
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata.MutableListLiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.maps.android.data.kml.KmlContainer
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val storage: FirebaseStorage = Firebase.storage

    val locations: MutableListLiveData<LatLng> = MutableListLiveData<LatLng>().apply {
        postValue(mutableListOf())
    }

    val centerMarkerOnClick: StateFlow<Boolean> = markerCentering().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    fun loadGoogleMap(googleMap: GoogleMap, dataClass: DataClass<*, *>) {
        @Suppress("BlockingMethodInNonBlockingContext")
        viewModelScope.launch {
            val kmzFile = dataClass.kmzFile(context, storage, false)
            val kmzStream = kmzFile.inputStream()
            val kmzLayer = KmlLayer(googleMap, kmzStream, context)
            kmzLayer.addLayerToMap()

            fun iterateContainers(
                hasContainers: Boolean,
                containers: Iterable<KmlContainer>,
                count: Int = 0
            ) {
                if (count < 10 && hasContainers)
                    for (container in containers) {
                        if (container.hasContainers())
                            iterateContainers(true, container.containers, count + 1)
                        if (container.hasPlacemarks()) {
                            Timber.i("Container has placemarks")
                            for (placemark in container.placemarks) {
                                when {
                                    placemark.markerOptions != null -> {
                                        locations.add(placemark.markerOptions.position)
                                    }
                                    placemark.polygonOptions != null -> {
                                        val points = placemark.polygonOptions.points
                                        locations.addAll(points)
                                    }
                                    placemark.polylineOptions != null -> {
                                        val points = placemark.polygonOptions.points
                                        locations.addAll(points)
                                    }
                                    placemark.hasGeometry() -> {
                                        val geometry = placemark.geometry
                                        val point = geometry.geometryObject as? LatLng
                                        if (point != null) {
                                            Timber.i("Adding point: $point")
                                            locations.add(point)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            iterateContainers(kmzLayer.hasContainers(), kmzLayer.containers)
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