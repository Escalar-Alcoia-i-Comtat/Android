package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_ERROR_NO_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_REQUESTED_BACK
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModelImpl
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toMap
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.SectorViewScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

/**
 * An Activity for displaying the contents of a Data Class.
 * Requires [EXTRA_DATACLASS] which will be the DataClass that gets loaded.
 * @author Arnau Mora
 * @since 20220105
 */
class DataClassActivity : NetworkAwareComponentActivity() {
    /**
     * The View Model for doing async tasks in DataClassExplorer.
     * @author Arnau Mora
     * @since 20220105
     */
    internal val exploreViewModel by viewModels<ExploreViewModel>(
        factoryProducer = { ExploreViewModel.Factory(application) }
    )

    /**
     * The View Model for doing async tasks.
     * @author Arnau Mora
     * @since 20220105
     */
    private val sectorPageViewModel by viewModels<SectorPageViewModelImpl>(
        factoryProducer = { SectorPageViewModelImpl.Factory(application) }
    )

    /**
     * Tells whether or not the device is connected to the Internet.
     * @author Arnau Mora
     * @since 20220102
     */
    internal val hasInternet = MutableLiveData<Boolean>()

    /**
     * The Firebase Storage instance to fetch files from the server.
     * @author Arnau Mora
     * @since 20220105
     */
    val storage: FirebaseStorage = Firebase.storage

    /**
     * The Data Class to display.
     * @author Arnau Mora
     * @since 20220105
     */
    internal lateinit var dataClass: DataClass<*, *>

    /**
     * Used for Sector display, to remember which is the position of the currently showing sector.
     * @author Arnau Mora
     * @since 20220106
     */
    private var index: Int = 0

    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalPagerApi::class,
    )
    @ExperimentalBadgeUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.v("Loaded DataClassActivity with extras: ${intent.extras?.toMap()}")

        dataClass = (intent.getExtra(EXTRA_DATACLASS)
            ?: savedInstanceState?.getExtra(EXTRA_DATACLASS) ?: run {
                Timber.e("Finishing DataClassActivity since no DataClass was passed.")
                finishActivity(REQUEST_CODE_ERROR_NO_DATACLASS)
                return
            }) as DataClass<*, *>
        index = savedInstanceState?.getExtra(EXTRA_INDEX) ?: index

        setContent {
            AppTheme {
                (dataClass as? Sector)?.let { sector ->
                    // If dataClass is a sector, load the sector view screen
                    SectorViewScreen(sectorPageViewModel, sector)
                } ?:
                // If not, load the DataClassExplorer
                DataClassExplorer(exploreViewModel, storage, dataClass, hasInternet)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.put(EXTRA_DATACLASS, dataClass)
        outState.put(EXTRA_INDEX, index)

        super.onSaveInstanceState(outState)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        Timber.i("Updated network state. Internet: ${state.hasInternet}")
        hasInternet.postValue(state.hasInternet)
    }

    override fun onBackPressed() {
        finishActivity(REQUEST_CODE_REQUESTED_BACK)
    }
}