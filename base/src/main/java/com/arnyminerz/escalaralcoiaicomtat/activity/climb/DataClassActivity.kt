package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_OBJECT_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_ERROR_NO_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_ERROR_NO_OBJECT_ID
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

/**
 * An Activity for displaying the contents of a Data Class.
 * Requires [EXTRA_NAMESPACE] and [EXTRA_OBJECT_ID].
 * @author Arnau Mora
 * @since 20220105
 */
class DataClassActivity : NetworkAwareComponentActivity() {
    /**
     * The View Model for doing async tasks.
     * @author Arnau Mora
     * @since 20220105
     */
    internal val exploreViewModel by viewModels<ExploreViewModel>(
        factoryProducer = { ExploreViewModel.Factory(application) }
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
     * The namespace of the displaying Data Class.
     * @author Arnau Mora
     * @since 20220105
     */
    internal lateinit var namespace: String

    /**
     * The object id of the displaying Data Class.
     * @author Arnau Mora
     * @since 20220105
     */
    internal lateinit var objectId: String

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @ExperimentalBadgeUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        namespace = intent.getExtra(EXTRA_NAMESPACE)
            ?: savedInstanceState?.getExtra(EXTRA_NAMESPACE) ?: run {
                finishActivity(REQUEST_CODE_ERROR_NO_NAMESPACE)
                return
            }
        objectId = intent.getExtra(EXTRA_OBJECT_ID)
            ?: savedInstanceState?.getExtra(EXTRA_OBJECT_ID) ?: run {
                finishActivity(REQUEST_CODE_ERROR_NO_OBJECT_ID)
                return
            }

        setContent {
            AppTheme {
                DataClassExplorer(storage)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.put(EXTRA_NAMESPACE, namespace)
        outState.put(EXTRA_OBJECT_ID, objectId)

        super.onSaveInstanceState(outState)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        Timber.i("Updated network state. Internet: ${state.hasInternet}")
        hasInternet.postValue(state.hasInternet)
    }
}