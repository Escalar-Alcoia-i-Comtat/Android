package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.DataClassItemViewModel

@Composable
@ExperimentalCoilApi
fun DataClassItem(item: DataClass<*, *>, onClick: (() -> Unit)? = null) {
    if (item.displayOptions.downloadable)
        DownloadableDataClassItem(item)
    else
        NonDownloadableDataClassItem(item, onClick)
}

/**
 * Displays a data class object that can be downloaded. The UI is a little more complex.
 * @author Arnau Mora
 * @since 20211229
 * @param dataClass The data class to display.
 */
@Composable
private fun DownloadableDataClassItem(dataClass: DataClass<*, *>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        // TODO: Design the layout
    }
}

/**
 * Displays a data class object that can't be downloaded. The UI is simpler, just image and name.
 * @author Arnau Mora
 * @since 20211229
 * @param dataClass The data class to display.
 * @param onClick What to do when clicked.
 */
@ExperimentalCoilApi
@Composable
private fun NonDownloadableDataClassItem(
    dataClass: DataClass<*, *>,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel: DataClassItemViewModel = viewModel(
        factory = DataClassItemViewModel.Factory(
            context.applicationContext as Application
        )
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        val imagePainter = if (viewModel.imageUrls.containsKey(dataClass.pin))
            rememberImagePainter(
                request = ImageRequest.Builder(context)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .data(viewModel.imageUrls[dataClass.pin])
                    .placeholder(R.drawable.ic_wide_placeholder)
                    .error(R.drawable.ic_wide_placeholder)
                    .build(),
                onExecute = { _, _ -> true }
            )
        else painterResource(R.drawable.ic_wide_placeholder)
        Column {
            Image(
                painter = imagePainter,
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable {
                        onClick?.let { it() }
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = dataClass.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = dataClass.displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                        .fillMaxWidth()
                )
            }
        }
        viewModel.loadImage(dataClass)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
fun AreaPreview() {
    DataClassItem(Area.SAMPLE_AREA)
}
