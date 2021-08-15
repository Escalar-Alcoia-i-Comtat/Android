package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.has
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import timber.log.Timber

/**
 * A [DataClassListActivity] for exploring the interior of an [Area].
 * The [Area] to explore must be passed through [EXTRA_AREA].
 * @author Arnau Mora
 * @since 20210719
 * @see Area
 * @see DataClassListActivity
 */
class AreaActivity : DataClassListActivity<Zone, DataClassImpl, Area>(2, R.dimen.area_item_height) {
    /**
     * If the contents of the [Area] are being loaded.
     * @author Arnau Mora
     * @since 20210719
     */
    private var loading = false

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

        this.areaId = areaId
        if (!AREAS.has(areaId)) {
            Timber.e("Area is not loaded in AREAS")
            onBackPressed()
            return
        }
        dataClass = AREAS[areaId]!!
        Timber.d("DataClass id: ${dataClass.objectId}")

        position =
            intent.getExtra(EXTRA_POSITION) ?: savedInstanceState?.getInt(EXTRA_POSITION.key, 0)
                    ?: 0
        Timber.d("Current position: $position")

        binding.titleTextView.text = dataClass.displayName
        binding.titleTextView.transitionName = intent.getExtra(EXTRA_AREA_TRANSITION_NAME)
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

    override fun intentExtra(index: Int, transitionName: String?): Intent =
        Intent(this@AreaActivity, ZoneActivity()::class.java)
            .putExtra(EXTRA_AREA, areaId)
            .putExtra(EXTRA_ZONE, items[position].objectId)
}
