package com.arnyminerz.escalaralcoiaicomtat.instant.ui.climb

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.instant.R
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel.DataClassViewModel

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
fun <T : DataClass<*, *>, V : DataClassViewModel<T>> Explorer(
    activity: Activity,
    navController: NavController,
    dataClassViewModel: Class<V>
) {
    val viewModel = dataClassViewModel.getConstructor().newInstance()
    val liveData = viewModel.items
    val items: List<T> by liveData.observeAsState(listOf())
    var isLoading by remember { mutableStateOf(true) }

    liveData.observe(activity as LifecycleOwner) { isLoading = it.isEmpty() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        AnimatedVisibility(visible = isLoading, modifier = Modifier.size(52.dp)) {
            CircularProgressIndicator()
        }
    }
    DataClassList(items, R.drawable.ic_wide_placeholder, navController)
}
