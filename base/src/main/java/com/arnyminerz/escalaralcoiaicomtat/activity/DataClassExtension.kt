package com.arnyminerz.escalaralcoiaicomtat.activity

import android.app.Activity
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
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
    val pathPieces = documentPath.split("/")
    Timber.v("Launching activity with path $documentPath")
    // 1 -> Area ID
    // 3 -> Zone ID
    // 5 -> Sector ID
    val intent = when (namespace) {
        Area.NAMESPACE ->
            AreaActivity.intent(activity, pathPieces[1]) // area ID
        Zone.NAMESPACE ->
            ZoneActivity.intent(activity, pathPieces[3]) // zone ID
        Sector.NAMESPACE, Path.NAMESPACE ->
            SectorActivity.intent(activity, pathPieces[3], pathPieces[5])
        else ->
            throw IllegalArgumentException("Cannot launch activity since $namespace is not a valid namespace.")
    }
    uiContext { activity.startActivity(intent) }
}
