package com.arnyminerz.escalaralcoiaicomtat.instant.ui.climb

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.theme.ItemTextBackground
import timber.log.Timber

@Composable
@ExperimentalCoilApi
fun <D : DataClass<*, *>> DataClassList(
    items: List<D>,
    @DrawableRes placeholder: Int,
    navController: NavController
) {
    Timber.v("Loading DataClass list (${items.size} items)...")
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App contents
        items(items) { dataClass ->
            Timber.v("$dataClass > Iterating...")
            val downloadUrl = dataClass.downloadUrl
            if (downloadUrl == null)
                Timber.i("$dataClass > Could not load image since downloadUrl is null")
            else
                dataClass.AreaItem(navController, placeholder, downloadUrl)
        }
    }
}

@Composable
@ExperimentalCoilApi
fun <A : DataClassImpl, B : DataClassImpl> DataClass<A, B>.AreaItem(
    navController: NavController,
    @DrawableRes placeholder: Int,
    image: Uri
) {
    val context = LocalContext.current
    var boxSize by remember { mutableStateOf(Size.Zero) }
    var height: Float by remember { mutableStateOf(0f) }

    Timber.v("$this > Composing $displayName...")
    Timber.v("$this > Url: $image")
    Card(
        modifier = Modifier
            .clickable {
                toast(context, "Clicked $displayName")
                val path = this.metadata.documentPath
                Timber.v("$this > Navigating to $path")
                navController.navigate(path)
            }
    ) {
        Box {
            Image(
                painter = rememberImagePainter(
                    data = image.toString(),
                    onExecute = { _, current ->
                        val size = current.size
                        val scale = boxSize.height / boxSize.width
                        height = size.height * scale
                        true
                    },
                    builder = {
                        placeholder(placeholder)
                    }
                ),
                contentScale = ContentScale.Crop,
                contentDescription = "$displayName image",
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .height(150.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        boxSize = layoutCoordinates.size.toSize()
                    }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItemTextBackground)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
