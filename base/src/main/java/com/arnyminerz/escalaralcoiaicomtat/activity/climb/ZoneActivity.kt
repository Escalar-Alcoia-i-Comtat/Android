package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.view.model.DataClassListViewModel
import com.arnyminerz.escalaralcoiaicomtat.view.model.DataClassListViewModelFactory
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class ZoneActivity : DataClassListActivity<Sector, Area, Zone>(1, R.dimen.sector_item_height) {
    companion object {
        var errorNotStored: Boolean = false

        /**
         * Launches the [SectorActivity] with the specified arguments.
         * @author Arnau Mora
         * @since 20210820
         * @param activity The [Activity] that wants to launch the Intent
         * @param zoneId The id of the zone to display.
         */
        fun intent(activity: Activity, zoneId: String): Intent =
            Intent(activity, ZoneActivity::class.java).apply {
                putExtra(EXTRA_ZONE, zoneId)
            }
    }

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

        val zoneIdExtra = intent.getExtra(EXTRA_ZONE) ?: savedInstanceState?.getExtra(EXTRA_ZONE)
        if (zoneIdExtra == null) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        }
        zoneId = zoneIdExtra

        Timber.v("Getting view model...")
        val viewModel by viewModels<DataClassListViewModel> {
            DataClassListViewModelFactory(
                app,
                null,
                zoneId,
                null
            )
        }
        this.viewModel = viewModel

        doAsync {
            dataClass = app.getZone(zoneId) ?: run {
                Timber.w("Could not find zone \"$zoneId\".")
                return@doAsync
            }

            transitionName = intent.getExtra(EXTRA_ZONE_TRANSITION_NAME)
            position = intent.getExtra(EXTRA_POSITION, 0)

            uiContext { onStateChange(appNetworkState) }
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
        outState.put(EXTRA_ZONE, zoneId)
        super.onSaveInstanceState(outState)
    }

    override fun intentExtra(transitionName: String?, objectId: String): Intent =
        SectorActivity.intent(this, zoneId, objectId)
}
