package com.arnyminerz.escalaralcoiaicomtat.activity

import android.app.Activity
import androidx.annotation.WorkerThread
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

/**
 * Launches the DataClass' [Activity].
 * @author Arnau Mora
 * @since 20210811
 * @param activity The [Activity] that is requesting the launch.
 */
@WorkerThread
@ExperimentalBadgeUtils
@ExperimentalMaterial3Api
@Throws(IllegalArgumentException::class)
suspend fun DataClassImpl.launch(activity: Activity) {
    val pathPieces = documentPath.split("/")
    Timber.v("Launching activity with path $documentPath")
    // 1 -> Area ID
    // 3 -> Zone ID
    // 5 -> Sector ID
    /*val intent = when (namespace) {
        Area.NAMESPACE ->
            AreaActivity.intent(activity, pathPieces[1]) // area ID
        Zone.NAMESPACE ->
            ZoneActivity.intent(activity, pathPieces[3]) // zone ID
        Sector.NAMESPACE, Path.NAMESPACE ->
            SectorActivity.intent(activity, pathPieces[3], pathPieces[5])
        else ->
            throw IllegalArgumentException("Cannot launch activity since $namespace is not a valid namespace.")
    }*/
    TODO("Should be using Jetpack compose navigation")
    // uiContext { activity.startActivity(intent) }
}
