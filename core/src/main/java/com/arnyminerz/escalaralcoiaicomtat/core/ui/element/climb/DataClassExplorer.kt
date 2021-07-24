package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.ui.animation.EnterAnimation
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.DataClassViewModel
import timber.log.Timber

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun <T : DataClass<*, *>, V : DataClassViewModel<T>> Explorer(
    activity: Activity,
    navController: NavController,
    dataClassViewModel: Class<V>,
    viewModelArguments: List<Any> = listOf()
) {
    val types = arrayListOf<Class<*>>()
    for (argument in viewModelArguments)
        types.add(argument::class.java)

    Timber.v("Exploring ViewModel named ${dataClassViewModel.name}")
    Timber.v("ViewModel args: $viewModelArguments ($types)")

    val viewModel = dataClassViewModel
        .getConstructor(*types.toTypedArray())
        .newInstance(*viewModelArguments.toTypedArray())
    val liveData = viewModel.items
    val items: List<T> by liveData.observeAsState(listOf())
    var finishedLoading by remember { mutableStateOf(false) }

    liveData.observe(activity as LifecycleOwner) {
        finishedLoading = it.isNotEmpty()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        EnterAnimation(visible = !finishedLoading, modifier = Modifier.size(52.dp)) {
            CircularProgressIndicator()
        }
    }
    AnimatedVisibility(
        visible = finishedLoading,
        enter = slideInHorizontally(
            initialOffsetX = { -40 }
        ) + fadeIn(initialAlpha = 0.7f),
        exit = slideOutHorizontally() + fadeOut(),
    ) {
        DataClassList(
            activity,
            navController,
            items,
            if (viewModel.columnsPerRow % 2 == 0) R.drawable.ic_tall_placeholder else R.drawable.ic_wide_placeholder,
            viewModel.columnsPerRow,
            viewModel.fixedHeight
        )
    }
}
