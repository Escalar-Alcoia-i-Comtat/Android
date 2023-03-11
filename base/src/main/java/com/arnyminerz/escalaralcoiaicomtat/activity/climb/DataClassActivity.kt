package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.network.ConnectivityStateListener
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.setContentThemed
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.SectorViewScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

/**
 * An Activity for displaying the contents of a Data Class.
 * Requires [EXTRA_DATACLASS] which will be the DataClass that gets loaded.
 * @author Arnau Mora
 * @since 20220105
 */
class DataClassActivity : NetworkAwareActivity() {
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
    private val sectorPageViewModel by viewModels<SectorPageViewModel>(
        factoryProducer = { SectorPageViewModel.Factory(application) }
    )

    /**
     * Tells whether or not the device is connected to the Internet.
     * @author Arnau Mora
     * @since 20220102
     */
    private val hasInternet = MutableLiveData<Boolean>()

    /**
     * The id of the DataClass to display.
     * @author Arnau Mora
     * @since 20220330
     */
    private var dataClassId: String? = null

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

    private val navStack = mutableStateOf(emptyList<DataClassImpl>())

    /**
     * Stores whether or not the sector image is maximized.
     * @author Arnau Mora
     * @since 20220323
     */
    private var isMaximized = mutableStateOf(false)

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
        // Load the nav stack
        loadNavStack(savedInstanceState)

        setContentThemed {
            // Add back pressed callback
            BackHandler(onBack = ::backHandler)

            val data by sectorPageViewModel.dataClass.observeAsState()
            data?.let { dataClass ->
                Timber.d("DataClass namespace: ${dataClass.namespace}. Index=$index. Children Count=$childrenCount")
                if (dataClass.namespace == Zone.NAMESPACE && index != null && childrenCount != null) {
                    val zone = dataClass as Zone
                    SectorViewScreen(
                        sectorPageViewModel,
                        zone,
                        childrenCount!!,
                        isMaximized,
                        index,
                    ) { index = it }
                } else {
                    Timber.d("Rendering Area/Zone...")
                    // If not, load the DataClassExplorer
                    DataClassExplorer(
                        exploreViewModel,
                        hasInternet,
                        navStack
                    ) { adding, item ->
                        navStack.value = navStack.value
                            .toMutableList()
                            .let { list ->
                                if (adding) {
                                    list.add(item)
                                    list
                                } else
                                    list.filter { it != item }
                            }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        dataClassId?.let { outState.put(EXTRA_DATACLASS_ID, it) }
        sectorPageViewModel.dataClass.value?.let { outState.put(EXTRA_DATACLASS, it) }
        outState.put(EXTRA_INDEX, index)
        childrenCount?.let { outState.put(EXTRA_CHILDREN_COUNT, it) }
        outState.put(EXTRA_NAV_STACK, ArrayList(navStack.value))

        super.onSaveInstanceState(outState)
    }

    override fun onStateChange(state: ConnectivityStateListener.NetworkState) {
        super.onStateChange(state)
        doAsync {
            val internetAvailable = state.isInternetAvailable
            Timber.i("Updated network state. Internet: $internetAvailable")
            hasInternet.postValue(internetAvailable)
        }
    }

    /**
     * Handles what the app should do when pressing the back button.
     * @author Arnau Mora
     * @since 20220714
     */
    fun backHandler() {
        if (navStack.value.size > 1)
            navStack.value = navStack.value.dropLast(1)
        else if (isMaximized.value)
            isMaximized.value = false
        else
            finish()
    }

    /**
     * Loads the value of the DataClass to load from the Activity extras.
     * @author Arnau Mora
     * @since 20220106
     * @param extras The intent extras.
     * @param savedInstanceState The instance state of the Activity, for recovering saved data.
     */
    private fun loadDataClassExtra(
        extras: Bundle?,
        savedInstanceState: Bundle?,
    ): Boolean {
        val dataClassExtra: Parcelable? =
            extras?.getExtra(EXTRA_DATACLASS) ?: savedInstanceState?.getExtra(EXTRA_DATACLASS)

        Timber.v("EXTRA_DATACLASS is present in intent extras. Type: ${dataClassExtra?.let { it::class.java.simpleName }}")

        @Suppress("UNCHECKED_CAST")
        val castDataClass = dataClassExtra as DataClass<DataClassImpl, *>?
        sectorPageViewModel.dataClass.postValue(castDataClass)
        val dataClassId =
            extras?.getExtra(EXTRA_DATACLASS_ID) ?: savedInstanceState?.getExtra(EXTRA_DATACLASS_ID)

        if (castDataClass != null && exploreViewModel.children.isEmpty())
            exploreViewModel.childrenLoader(castDataClass) { it.displayName }

        if (dataClassId != null)
            sectorPageViewModel.loadZone(dataClassId)

        if (dataClassId == null && dataClassExtra == null) {
            Timber.e("Finishing DataClassActivity since no DataClass was passed.")
            finishActivity(REQUEST_CODE_ERROR_NO_DATACLASS)
            return false
        }

        navStack.value =
            if (castDataClass != null) listOf<DataClassImpl>(castDataClass) else emptyList()
        return true
    }

    /**
     * Loads the nav stack from the Activity's saved instance state.
     * @author Arnau Mora
     * @since 20220331
     * @param savedInstanceState The Activity's saved instance state.
     */
    private fun loadNavStack(savedInstanceState: Bundle?) {
        if (savedInstanceState == null)
            return

        val newNavStack = savedInstanceState.getExtra(EXTRA_NAV_STACK)
            ?.map { it as DataClassImpl }
            ?: return

        navStack.value = newNavStack
    }
}
