package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getZones
import com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata.MutableListLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Used by the Nearby Zones module, for doing async tasks.
 * @author Arnau Mora
 * @since 20220130
 * @param application The application that requests the view model initialization.
 */
class NearbyZonesViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * The loaded zones from [loadZones].
     * @author Arnau Mora
     * @since 20220130
     */
    val zones = MutableListLiveData<Zone>()

    /**
     * Starts loading the zones that are indexed into [loadZones]. Updates the result at [zones].
     * @author Arnau Mora
     * @since 20220130
     * @see zones
     */
    fun loadZones() {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                val foundZones = app.searchSession.getZones()
                zones.clear()
                zones.addAll(foundZones)
            }
        }
    }
}