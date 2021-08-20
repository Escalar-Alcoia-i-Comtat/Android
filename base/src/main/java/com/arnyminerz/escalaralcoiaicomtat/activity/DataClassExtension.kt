package com.arnyminerz.escalaralcoiaicomtat.activity

import android.app.Activity
import android.content.Intent
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import timber.log.Timber

/**
 * Launches the DataClass' [Activity].
 * @author Arnau Mora
 * @since 20210811
 * @param activity The [Activity] that is requesting the launch.
 */
@WorkerThread
@Throws(IllegalArgumentException::class)
suspend fun DataClassImpl.launch(activity: Activity) {
    val activityClass: Class<*> = when (namespace) {
        Area.NAMESPACE -> AreaActivity::class.java
        Zone.NAMESPACE -> ZoneActivity::class.java
        Sector.NAMESPACE -> SectorActivity::class.java
        Path.NAMESPACE -> SectorActivity::class.java
        else -> throw IllegalArgumentException("Cannot launch activity since $namespace is not a valid namespace.")
    }
    val intent = Intent(activity, activityClass)
    val pathPieces = documentPath.split("/")
    Timber.v("Launching activity with path $documentPath")
    when (namespace) {
        Area.NAMESPACE -> {
            intent.putExtra(EXTRA_AREA, pathPieces[1]) // area ID
        }
        Zone.NAMESPACE -> {
            intent.putExtra(EXTRA_AREA, pathPieces[1]) // area ID
            intent.putExtra(EXTRA_ZONE, pathPieces[3]) // zone ID
        }
        Sector.NAMESPACE, Path.NAMESPACE -> {
            intent.putExtra(EXTRA_AREA, pathPieces[1]) // area ID
            intent.putExtra(EXTRA_ZONE, pathPieces[3]) // zone ID
            Timber.v("Getting sectors for zone ${pathPieces[3]}...")
            val app = activity.app
            val areas = app.getAreas()
            val sectors = areas[pathPieces[1]]
                ?.get(app.searchSession, pathPieces[3])
                ?.getChildren(app.searchSession)
            if (sectors == null)
                Timber.e("Could not load sectors from area ${pathPieces[1]}, sector ${pathPieces[3]}")
            intent.putExtra(EXTRA_SECTOR, pathPieces[5])
        }
    }
    uiContext { activity.startActivity(intent) }
}
