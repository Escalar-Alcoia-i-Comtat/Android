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
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class DataClassActivity : NetworkAwareComponentActivity() {
    internal val exploreViewModel by viewModels<ExploreViewModel>(factoryProducer = {
        ExploreViewModel.Factory(application)
    })

    /**
     * Tells whether or not the device is connected to the Internet.
     * @author Arnau Mora
     * @since 20220102
     */
    internal val hasInternet = MutableLiveData<Boolean>()

    val storage: FirebaseStorage = Firebase.storage

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @ExperimentalBadgeUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val namespace = intent.getExtra(EXTRA_NAMESPACE) ?: run {
            finishActivity(REQUEST_CODE_ERROR_NO_NAMESPACE)
            return
        }
        val objectId = intent.getExtra(EXTRA_OBJECT_ID) ?: run {
            finishActivity(REQUEST_CODE_ERROR_NO_OBJECT_ID)
            return
        }

        setContent {
            AppTheme {
                DataClassExplorer(storage, namespace, objectId)
            }
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        Timber.i("Updated network state. Internet: ${state.hasInternet}")
        hasInternet.postValue(state.hasInternet)
    }
}