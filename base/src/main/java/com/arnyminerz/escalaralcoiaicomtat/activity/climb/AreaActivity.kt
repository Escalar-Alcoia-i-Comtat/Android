package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.view.model.DataClassListViewModel
import com.arnyminerz.escalaralcoiaicomtat.view.model.DataClassListViewModelFactory
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

/**
 * A [DataClassListActivity] for exploring the interior of an [Area].
 * The [Area] to explore must be passed through [EXTRA_AREA].
 * @author Arnau Mora
 * @since 20210719
 * @see Area
 * @see DataClassListActivity
 */
@ExperimentalMaterial3Api
@ExperimentalBadgeUtils
class AreaActivity : DataClassListActivity<Zone, DataClassImpl, Area>(2, R.dimen.zone_item_height) {
    companion object {
        /**
         * Launches the [AreaActivity] with the specified arguments.
         * @author Arnau Mora
         * @since 20210821
         * @param activity The [Activity] that wants to launch the Intent
         * @param areaId The id of the zone to display.
         * @param position The position to move the list at.
         */
        fun intent(activity: Activity, areaId: String, position: Int = 0): Intent =
            Intent(activity, AreaActivity::class.java).apply {
                putExtra(EXTRA_AREA, areaId)
                putExtra(EXTRA_POSITION, position)
            }
    }

    /**
     * The id of the loaded [Area].
     * @author Arnau Mora
     * @since 20210719
     */
    private lateinit var areaId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        justAttached = true

        val extras = intent.extras
        if (extras == null) {
            Timber.e("Extras is null")
            onBackPressed()
            return
        }

        val areaId = intent.getExtra(EXTRA_AREA) ?: savedInstanceState?.getExtra(EXTRA_AREA)

        if (areaId == null) {
            Timber.e("Area extra is null")
            onBackPressed()
            return
        }

        position = intent.getExtra(EXTRA_POSITION)
            ?: savedInstanceState?.getInt(EXTRA_POSITION.key, 0) ?: 0
        Timber.d("Current position: $position")

        Timber.v("Getting view model...")
        val viewModel by viewModels<DataClassListViewModel> {
            DataClassListViewModelFactory(
                app,
                areaId,
                null,
                null
            )
        }
        this.viewModel = viewModel

        this.areaId = areaId
        val app = application as App

        doAsync {
            // Get the corresponding Area and store it in [dataClass]
            dataClass = app.getArea(areaId) ?: run {
                uiContext {
                    Timber.e("Could not find area A/$areaId!")
                    onBackPressed()
                    finish()
                }
                return@doAsync
            }
            Timber.d("DataClass id: A/${dataClass.objectId}")

            uiContext {
                Timber.v("Refreshing UI with the loaded data...")
                onStateChange(appNetworkState)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_POSITION.key, position)
        outState.put(EXTRA_AREA, areaId)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (getExtra(EXTRA_STATIC, false))
            launch(MainActivity::class.java)
        else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()

        loaded = false
        justAttached = false
    }

    override fun intentExtra(transitionName: String?, objectId: String): Intent =
        Intent(this@AreaActivity, ZoneActivity()::class.java)
            .putExtra(EXTRA_AREA, areaId)
            .putExtra(EXTRA_ZONE, objectId)
}
