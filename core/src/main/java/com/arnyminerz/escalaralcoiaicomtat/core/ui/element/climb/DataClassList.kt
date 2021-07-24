package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.ui.ItemTextBackground
import com.arnyminerz.escalaralcoiaicomtat.core.ui.ItemTextColor
import timber.log.Timber

/**
 * A list of items that show the image of a [DataClass].
 * @author Arnau Mora
 * @since 20210724
 * @param navController The controller for managing the current navigation state in the app`.
 * @param items The items to show.
 * @param placeholder The image resource to show while loading the [DataClass]' image.
 * @param columnsPerRow How much columns to show in the list.
 * @param fixedHeight If not null, all the items will have this height.
 */
@Composable
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun <D : DataClass<*, *>> DataClassList(
    navController: NavController,
    items: List<D>,
    @DrawableRes placeholder: Int,
    columnsPerRow: Int = 1,
    fixedHeight: Dp? = null,
) {
    Timber.v("Loading DataClass list (${items.size} items)...")
    val state = rememberLazyListState()
    LazyVerticalGrid(
        state = state,
        cells = GridCells.Fixed(columnsPerRow)
    ) {
        items(items) { dataClass ->
            Timber.v("$dataClass > Iterating...")
            val downloadUrl = dataClass.downloadUrl
            if (downloadUrl == null)
                Timber.i("$dataClass > Could not load image since downloadUrl is null")
            else
                dataClass.DataClassItem(navController, placeholder, downloadUrl, fixedHeight)
        }
    }
}

private const val CARD_CORNER_RADIUS = 16

@Composable
@ExperimentalCoilApi
fun <A : DataClassImpl, B : DataClassImpl> DataClass<A, B>.DataClassItem(
    navController: NavController,
    @DrawableRes placeholder: Int,
    image: Uri,
    fixedHeight: Dp? = null
) {
    var imageRatio by remember { mutableStateOf(1f) }

    val imageModifiers = if (fixedHeight != null) {
        Modifier.requiredHeight(fixedHeight)
    } else {
        Modifier.aspectRatio(imageRatio)
    }
        .fillMaxWidth()

    Timber.v("$this > Composing $displayName...")
    Timber.v("$this > Url: $image")
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(CARD_CORNER_RADIUS.dp))
            .clickable {
                val path = this.metadata.documentPath
                Timber.v("$this > Navigating to $path")
                navController.navigate(path)
            }
    ) {
        Box {
            Image(
                painter = rememberImagePainter(
                    data = image,
                    onExecute = { _, current ->
                        val imageSize = current.size
                        imageRatio = imageSize.width / imageSize.height

                        true
                    },
                    builder = {
                        placeholder(placeholder)
                    }
                ),
                contentScale = ContentScale.Crop,
                contentDescription = "$displayName image",
                modifier = imageModifiers
                    .clip(RoundedCornerShape(CARD_CORNER_RADIUS.dp)),
            )
            Row(
                modifier = imageModifiers,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItemTextBackground)
                        .clip(RoundedCornerShape(CARD_CORNER_RADIUS.dp))
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        color = ItemTextColor,
                    )
                }
            }
        }
    }
}
