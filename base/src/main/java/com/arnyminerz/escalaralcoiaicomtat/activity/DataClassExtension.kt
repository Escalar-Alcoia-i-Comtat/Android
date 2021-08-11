package com.arnyminerz.escalaralcoiaicomtat.activity

import android.app.Activity
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

/**
 * Launches the DataClass' [Activity].
 * @author Arnau Mora
 * @since 20210811
 * @param activity The [Activity] that is requesting the launch.
 */
@UiThread
@Throws(IllegalArgumentException::class)
fun DataClassImpl.launch(activity: Activity) {
    val activityClass: Class<*> = when (namespace) {
        Area.NAMESPACE -> AreaActivity::class.java
        Zone.NAMESPACE -> ZoneActivity::class.java
        Sector.NAMESPACE -> SectorActivity::class.java
        Path.NAMESPACE -> SectorActivity::class.java
        else -> throw IllegalArgumentException("Cannot launch activity since $namespace is not a valid namespace.")
    }
    activity.launch(activityClass) {
        val pathPieces = documentPath.split("/")
        Timber.v("Launching activity with path $documentPath")
        when (namespace) {
            Area.NAMESPACE -> {
                putExtra(EXTRA_AREA, pathPieces[0]) // area ID
            }
            Zone.NAMESPACE -> {
                putExtra(EXTRA_AREA, pathPieces[0]) // area ID
                putExtra(EXTRA_ZONE, pathPieces[2]) // zone ID
            }
            Sector.NAMESPACE -> {
                putExtra(EXTRA_AREA, pathPieces[0]) // area ID
                putExtra(EXTRA_ZONE, pathPieces[2]) // zone ID
            }
            Path.NAMESPACE -> {
                putExtra(EXTRA_AREA, pathPieces[0]) // area ID
                putExtra(EXTRA_ZONE, pathPieces[2]) // zone ID
                val sectors = AREAS[pathPieces[0]]?.get(pathPieces[2])
                    ?.getChildren(activity, Firebase.storage)
                val sectorIndex = sectors?.let {
                    val i = it.indexOfFirst { sector -> sector.objectId == pathPieces[4] }
                    if (i < 0) 0 // If sector was not found, select the first one
                    else i
                } ?: 0
                putExtra(EXTRA_SECTOR_INDEX, sectorIndex)
            }
        }
    }
}
