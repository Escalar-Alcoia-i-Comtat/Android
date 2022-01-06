package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.SunTime
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.icon
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.textResource
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.mapsIntent
import com.google.android.gms.maps.model.LatLng
import me.bytebeats.views.charts.bar.BarChar
import me.bytebeats.views.charts.bar.BarChartData
import me.bytebeats.views.charts.bar.render.bar.SimpleBarDrawer
import me.bytebeats.views.charts.bar.render.label.SimpleLabelDrawer
import me.bytebeats.views.charts.bar.render.xaxis.SimpleXAxisDrawer
import me.bytebeats.views.charts.bar.render.yaxis.SimpleYAxisDrawer
import me.bytebeats.views.charts.simpleChartAnimation

@Composable
fun SectorPage(
    viewModel: SectorPageViewModel,
    objectId: String,
    displayName: String,
    @SunTime sun: Int,
    kidsApt: Boolean,
    walkingTime: Long,
    location: LatLng?,
) {
    val context = LocalContext.current

    var chartVisible by remember { mutableStateOf(true) }
    val chartButtonIconRotation by animateFloatAsState(
        if (chartVisible) -180f else -90f,
        animationSpec = tween(
            durationMillis = 100, // rotation is retrieved with this frequency
            easing = LinearEasing
        ),
    )

    // TODO: Sector image
    LazyColumn {
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Info Card
                Card(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Chips
                            Chip(
                                text = stringResource(sun.textResource),
                                icon = ContextCompat.getDrawable(context, sun.icon),
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                            )
                            if (kidsApt)
                                Chip(
                                    text = stringResource(R.string.sector_kids_apt),
                                    icon = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.ic_round_child_care_24
                                    ),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(4.dp)
                                .clickable(enabled = location != null) {
                                    location
                                        ?.mapsIntent(true, displayName)
                                        ?.let {
                                            context.launch(it)
                                        }
                                }
                        ) {
                            Icon(
                                Icons.Rounded.DirectionsWalk,
                                contentDescription = stringResource(R.string.image_desc_walking_time),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(
                                    R.string.sector_walking_time,
                                    walkingTime.toString()
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Stats card
                Card(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val barChartData by viewModel.getBarChartData(objectId)

                        // Heading
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.sector_info_chart_title),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            AnimatedVisibility(visible = barChartData == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                            IconButton(
                                enabled = barChartData != null,
                                onClick = { chartVisible = !chartVisible }
                            ) {
                                Icon(
                                    Icons.Rounded.ChevronLeft,
                                    contentDescription = stringResource(R.string.sector_info_chart_button_desc),
                                    modifier = Modifier.rotate(chartButtonIconRotation),
                                )
                            }
                        }
                        // Chart
                        AnimatedVisibility(visible = chartVisible && barChartData != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                BarChar(
                                    barChartData = barChartData ?: BarChartData(emptyList()),
                                    modifier = Modifier
                                        .height(120.dp)
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                                    animation = simpleChartAnimation(),
                                    barDrawer = SimpleBarDrawer(),
                                    xAxisDrawer = SimpleXAxisDrawer(),
                                    yAxisDrawer = SimpleYAxisDrawer(
                                        axisLineThickness = 0.dp,
                                        axisLineColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelTextSize = 0.sp,
                                        labelValueFormatter = { "" } // Disables values for the y axis
                                    ),
                                    labelDrawer = SimpleLabelDrawer(
                                        drawLocation = SimpleLabelDrawer.DrawLocation.Inside,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
