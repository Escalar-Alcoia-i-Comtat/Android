package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class ZoneActivity : DataClassListActivity<Sector, Area, Zone>(1, R.dimen.zone_item_height) {
    companion object {
        var errorNotStored: Boolean = false
    }

    private lateinit var areaId: String
    private lateinit var zoneId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        justAttached = true

        val extras = intent.extras
        if (extras == null) {
            Timber.e("Extras is null")
            onBackPressed()
            return
        }

        val areaIdExtra = intent.getExtra(EXTRA_AREA) ?: savedInstanceState?.getExtra(EXTRA_AREA)
        val zoneIdExtra = intent.getExtra(EXTRA_ZONE) ?: savedInstanceState?.getExtra(EXTRA_ZONE)
        if (areaIdExtra == null || zoneIdExtra == null) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        }
        areaId = areaIdExtra
        zoneId = zoneIdExtra
        doAsync {
            val areas = app.getAreas()
            val area = areas[areaId] ?: run {
                Timber.w("Could not find area \"$areaId\" in AREAS.")
                return@doAsync
            }
            val zones = area.getChildren(app)
            dataClass = zones[zoneId] ?: run {
                Timber.w("Could not find zone \"$zoneId\" in \"$areaId\".")
                return@doAsync
            }

            transitionName = intent.getExtra(EXTRA_ZONE_TRANSITION_NAME)
            position = intent.getExtra(EXTRA_POSITION, 0)

            onStateChangeAsync(appNetworkState)
        }
    }

    override fun onResume() {
        super.onResume()
        loaded = false
        justAttached = false

        if (errorNotStored) {
            Timber.w("errorNotStored")
            Snackbar
                .make(binding.root, R.string.toast_error_no_internet, Snackbar.LENGTH_SHORT)
                .show()
            errorNotStored = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.put(EXTRA_AREA, areaId)
        outState.put(EXTRA_ZONE, zoneId)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (getExtra(EXTRA_STATIC, false))
            launch(AreaActivity::class.java) {
                putExtra(EXTRA_AREA, areaId)
            }
        else super.onBackPressed()
    }

    override fun intentExtra(index: Int, transitionName: String?): Intent =
        Intent(this, SectorActivity::class.java)
            .putExtra(EXTRA_AREA, areaId)
            .putExtra(EXTRA_ZONE, zoneId)
            .putExtra(EXTRA_SECTOR_COUNT, items.size)
            .putExtra(EXTRA_SECTOR_INDEX, index)
            .putExtra(EXTRA_SECTOR_TRANSITION_NAME, transitionName)
}
