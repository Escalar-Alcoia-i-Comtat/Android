package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CHILDREN_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_ERROR_NO_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModelImpl
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtraOrSavedInstanceState
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.sizeInBytes
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toMap
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.SectorViewScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
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
    private val exploreViewModel by viewModels<ExploreViewModel>(
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
    private val hasInternet = MutableLiveData<Boolean>()

    /**
     * The Data Class to display.
     * @author Arnau Mora
     * @since 20220105
     */
    private lateinit var dataClass: DataClass<*, *, *>

    /**
     * Used for Sector display, to remember which is the position of the currently showing sector.
     * @author Arnau Mora
     * @since 20220106
     */
    private var index: Int? = null

    /**
     * Used for Sector display, to know how many sectors there are inside of the parent Zone.
     * @author Arnau Mora
     * @since 20220106
     */
    private var childrenCount: Int? = null

    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalPagerApi::class,
    )
    @ExperimentalBadgeUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        Timber.v("Loaded DataClassActivity with extras (${extras?.sizeInBytes()}): ${extras?.toMap()}")

        // Load the value of dataClass or return
        loadDataClassExtra(extras, savedInstanceState).takeIf { it } ?: return
        // Load index
        index = getExtraOrSavedInstanceState(EXTRA_INDEX, savedInstanceState)
        // Load childrenCount
        childrenCount = getExtraOrSavedInstanceState(EXTRA_CHILDREN_COUNT, savedInstanceState)

        Firebase.analytics
            .logEvent(
                FirebaseAnalytics.Event.SELECT_CONTENT,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, dataClass.objectId)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, dataClass.displayName)
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, dataClass.namespace)
                },
            )

        setContent {
            AppTheme {
                Timber.d("DataClass namespace: ${dataClass.namespace}. Index=$index. Children Count=$childrenCount")
                if (dataClass.namespace == Zone.NAMESPACE && index != null && childrenCount != null) {
                    val zone = dataClass as Zone
                    SectorViewScreen(
                        sectorPageViewModel,
                        zone,
                        childrenCount!!,
                        index,
                    ) { index = it }
                } else {
                    Timber.d("Rendering Area/Zone...")
                    // If not, load the DataClassExplorer
                    DataClassExplorer(
                        exploreViewModel,
                        dataClass,
                        hasInternet
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.put(EXTRA_DATACLASS, dataClass)
        outState.put(EXTRA_INDEX, index)
        childrenCount?.let { outState.put(EXTRA_CHILDREN_COUNT, it) }

        super.onSaveInstanceState(outState)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        Timber.i("Updated network state. Internet: ${state.hasInternet}")
        hasInternet.postValue(state.hasInternet)
    }

    /**
     * Loads the value of [dataClass] from the Activity extras.
     * @author Arnau Mora
     * @since 20220106
     * @param extras The intent extras.
     * @param savedInstanceState The instance state of the Activity, for recovering saved data.
     */
    private fun loadDataClassExtra(
        extras: Bundle?,
        savedInstanceState: Bundle?,
    ): Boolean {
        val dataClassExtra: Parcelable = (extras?.getExtra(EXTRA_DATACLASS)
            ?: savedInstanceState?.getExtra(EXTRA_DATACLASS) ?: run {
                Timber.e("Finishing DataClassActivity since no DataClass was passed.")
                finishActivity(REQUEST_CODE_ERROR_NO_DATACLASS)
                return false
            })
        Timber.v("EXTRA_DATACLASS is present in intent extras. Type: ${dataClassExtra::class.java}")
        dataClass = dataClassExtra as DataClass<*, *, *>
        return true
    }
}
