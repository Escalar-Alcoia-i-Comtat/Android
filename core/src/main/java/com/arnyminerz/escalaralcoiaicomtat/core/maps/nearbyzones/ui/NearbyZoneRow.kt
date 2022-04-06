package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.dataClassExploreActivity
import com.arnyminerz.escalaralcoiaicomtat.core.utils.async
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launchAsync
import org.osmdroid.views.overlay.Marker

private class NearbyZoneRowProvider : PreviewParameterProvider<Marker> {
    override val values: Sequence<Marker> = sequenceOf(
        Marker(null)
            .apply {
                title = "Nearby Zone"
            }
    )
}

@Preview
@Composable
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
fun NearbyZonesRow(
    @PreviewParameter(NearbyZoneRowProvider::class) marker: Marker,
) {
    val context = LocalContext.current
    ListItem(
        text = {
            Text(
                text = marker.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 4.dp,
                    )
            )
        },
        trailing = {
            IconButton(
                onClick = async {
                    DataClass
                        .getIntent(context, dataClassExploreActivity, marker.title)
                        ?.launchAsync(context)
                },
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    stringResource(R.string.action_view),
                )
            }
        },
    )
}
