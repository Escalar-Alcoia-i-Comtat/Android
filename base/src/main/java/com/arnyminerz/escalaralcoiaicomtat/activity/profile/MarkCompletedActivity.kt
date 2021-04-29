package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.get
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMarkCompletedBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_PATH
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.toCollection
import timber.log.Timber

class MarkCompletedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarkCompletedBinding

    private var areaId: String? = null
    private var zoneId: String? = null
    private var sectorIndex: Int? = null
    private var pathId: String? = null

    private var area: Area? = null
    private var zone: Zone? = null
    private var sector: Sector? = null
    private var path: Path? = null

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkCompletedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        areaId = intent.getExtra(EXTRA_AREA)
        zoneId = intent.getExtra(EXTRA_ZONE)
        sectorIndex = intent.getExtra(EXTRA_SECTOR_INDEX)
        pathId = intent.getExtra(EXTRA_PATH)

        val extrasInvalid =
            areaId == null || zoneId == null || sectorIndex == null || pathId == null
        if (extrasInvalid) {
            // Any extra missing
            Timber.e("Going back, one or more Intent's extra were missing")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        firestore = Firebase.firestore

        doAsync {
            loadPath()
        }
    }

    /**
     * Loads the [Path] ([path]) data from the specified [areaId], [zoneId], [sectorIndex] and [pathId].
     * It is required that all the parameters are checked to be non-null, or [NullPointerException]
     * will be thrown.
     * @author Arnau Mora
     * @since 20210429
     * @throws NullPointerException When any of the parameters ([areaId], [zoneId], [sectorIndex] or
     * [pathId]) are null.
     */
    @Throws(NullPointerException::class)
    private suspend fun loadPath() {
        Timber.v("Loading area $areaId...")
        area = AREAS[areaId!!]
        if (area == null) {
            // Could not find valid Area
            Timber.e("Could not find Area $areaId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading zone $zoneId...")
        val zones = arrayListOf<Zone>()
        area!!.getChildren(firestore).toCollection(zones)
        try {
            zone = area!![zoneId!!]
        } catch (_: IndexOutOfBoundsException) {
            // Could not find valid Zone
            Timber.e("Could not find Zone $zoneId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading sector #$sectorIndex...")
        val sectors = arrayListOf<Sector>()
        zone!!.getChildren(firestore).toCollection(sectors)
        try {
            sector = zone!![sectorIndex!!]
        } catch (_: IndexOutOfBoundsException) {
            // Could not find valid Zone
            Timber.e("Could not find Sector #$sectorIndex")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading path $pathId...")
        val paths = arrayListOf<Path>()
        sector!!.getChildren(firestore).toCollection(paths)
        try {
            path = sector!![pathId!!]
        } catch (_: IndexOutOfBoundsException) {
            // Could not find valid Zone
            Timber.e("Could not find Path $pathId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }
    }
}
