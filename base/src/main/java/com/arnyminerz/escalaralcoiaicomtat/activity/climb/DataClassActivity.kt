package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CHILDREN_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PARENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_ERROR_NO_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_REQUESTED_BACK
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModelImpl
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.sizeInBytes
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
    private lateinit var dataClass: DataClass<*, *>

    /**
     * The parent Data Class of [dataClass].
     * @author Arnau Mora
     * @since 20220105
     */
    private var parentDataClass: DataClass<*, *>? = null

    /**
     * Used for Sector display, to remember which is the position of the currently showing sector.
     * @author Arnau Mora
     * @since 20220106
     */
    private var index: Int = 0

    /**
     * Used for Sector display, to know how many sectors there are inside of the parent Zone.
     * @author Arnau Mora
     * @since 20220106
     */
    private var childrenCount: Int? = null

    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalPagerApi::class,
    )
    @ExperimentalBadgeUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        Timber.v("Loaded DataClassActivity with extras (${extras?.sizeInBytes()}): ${extras?.toMap()}")

        // Load the value of dataClass or return
        loadDataClassExtra(extras, savedInstanceState).takeIf { it } ?: return
        // Load the value of parentDataClass
        loadParentDataClassExtra(extras, savedInstanceState)
        // Load index
        index = savedInstanceState?.getExtra(EXTRA_INDEX) ?: index
        // Load childrenCount
        childrenCount = extras?.getExtra(EXTRA_CHILDREN_COUNT)
            ?: savedInstanceState?.getExtra(EXTRA_CHILDREN_COUNT) ?: -1

        setContent {
            AppTheme {
                if (dataClass is Sector)
                    if (parentDataClass is Zone && childrenCount != null) {
                        Timber.d("Rendering Sector...")
                        // If dataClass is a sector, load the sector view screen
                        SectorViewScreen(
                            sectorPageViewModel,
                            parentDataClass as Zone,
                            dataClass as Sector,
                            childrenCount!!,
                            index
                        )
                    } else if (parentDataClass == null)
                        Timber.e("parentDataClass has not been initialized")
                    else if (childrenCount == null)
                        Timber.e("childrenCount has not been initialized")
                    else
                        Timber.e("dataClass is required to be zone. Type: ${dataClass::class.java.name}")
                else {
                    Timber.d("Rendering Area/Zone...")
                    // If not, load the DataClassExplorer
                    DataClassExplorer(
                        exploreViewModel,
                        storage,
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
        parentDataClass?.let { outState.put(EXTRA_PARENT, it) }

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
        dataClass = dataClassExtra as DataClass<*, *>
        return true
    }

    /**
     * Loads the value of [parentDataClass] from the Activity extras.
     * @author Arnau Mora
     * @since 20220106
     * @param extras The intent extras.
     * @param savedInstanceState The instance state of the Activity, for recovering saved data.
     */
    private fun loadParentDataClassExtra(
        extras: Bundle?,
        savedInstanceState: Bundle?,
    ): Boolean {
        val parentExtra = extras?.getExtra(EXTRA_PARENT)
            ?: savedInstanceState?.getExtra(EXTRA_PARENT)
            ?: return false
        Timber.v("EXTRA_PARENT is present in intent extras. Type: ${parentExtra::class.java}")
        parentDataClass = parentExtra as DataClass<*, *>
        return true
    }
}
