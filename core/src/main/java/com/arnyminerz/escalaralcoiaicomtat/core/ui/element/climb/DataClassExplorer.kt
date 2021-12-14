package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.ui.animation.EnterAnimation
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.DataClassViewModel

@Composable
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun <T : DataClass<*, *>, V : DataClassViewModel<T, *>> Explorer(
    context: Context,
    navController: NavController,
    viewModel: V,
) {
    val liveData = viewModel.items
    val items: List<T> by liveData.observeAsState(listOf())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        EnterAnimation(visible = items.isEmpty(), modifier = Modifier.size(52.dp)) {
            // TODO: Add progress indicator when implemented in MD3
            // CircularProgressIndicator()
        }
    }
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = slideInHorizontally(
            initialOffsetX = { -40 }
        ) + fadeIn(initialAlpha = 0.7f),
        exit = slideOutHorizontally() + fadeOut(),
    ) {
        if (items.isNotEmpty())
            context.DataClassList(
                navController,
                items,
                if (viewModel.columnsPerRow % 2 == 0)
                    R.drawable.ic_tall_placeholder
                else R.drawable.ic_wide_placeholder,
                viewModel.columnsPerRow,
                viewModel.fixedHeight
            )
    }
}
