package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
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
@ExperimentalMaterial3Api
fun NearbyZonesRow(
    @PreviewParameter(NearbyZoneRowProvider::class) marker: Marker,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
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
    }
}
