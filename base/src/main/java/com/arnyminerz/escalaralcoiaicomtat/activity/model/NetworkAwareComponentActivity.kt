package com.arnyminerz.escalaralcoiaicomtat.activity.model

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkProvider
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync

/**
 * A Component Activity that adapts to the user chosen language, and listens for device connectivity
 * changes. Override [onStateChange] and [onStateChangeAsync] for adding listeners.
 * @author Arnau Mora
 * @since 20220102
 */
abstract class NetworkAwareComponentActivity : LanguageComponentActivity(),
    ConnectivityProvider.ConnectivityStateListener {

    override fun onResume() {
        super.onResume()
        appNetworkProvider.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        appNetworkProvider.removeListener(this)
    }

    /**
     * This will get called when the connectivity state of the device is updated.
     * @author Arnau Mora
     * @since 20210818
     * @param state The new [ConnectivityProvider.NetworkState].
     */
    @MainThread
    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
    }

    /**
     * This will get called when the connectivity state of the device is updated.
     * Gets called asynchronously with [doAsync].
     * @author Arnau Mora
     * @since 20210818
     * @param state The new [ConnectivityProvider.NetworkState].
     * @see doAsync
     * @see asyncCoroutineScope
     */
    @WorkerThread
    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
    }
}
