package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.icon
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.textResource
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.ZoomableImage
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.mapsIntent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// TODO: Move this somewhere else
val Float.Companion.DegreeConverter
    get() = TwoWayConverter<Float, AnimationVector2D>({
        val rad = (it * Math.PI / 180f).toFloat()
        AnimationVector2D(sin(rad), cos(rad))
    }, {
        ((atan2(it.v1, it.v2) * 180f / Math.PI).toFloat() + 360) % 360
    })

@Composable
fun SectorPage(
    viewModel: SectorPageViewModel,
    sector: Sector,
) {
    val context = LocalContext.current
    viewModel.loadPaths(sector)

    Column(modifier = Modifier.fillMaxSize()) {
        // Image
        ZoomableImage(
            imageModel = viewModel.loadImage(sector),
            contentDescription = "TODO",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.7f)
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
                                    text = stringResource(sector.sunTime.textResource),
                                    icon = ContextCompat.getDrawable(context, sector.sunTime.icon),
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
                                )
                                if (sector.kidsApt)
                                    Chip(
                                        text = stringResource(R.string.sector_kids_apt),
                                        icon = ContextCompat.getDrawable(
                                            context,
                                            R.drawable.ic_round_child_care_24
                                        ),
                                        modifier = Modifier.padding(start = 4.dp),
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
                                    )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(4.dp)
                                    .clickable(enabled = sector.location != null) {
                                        sector.location
                                            ?.mapsIntent(true, sector.displayName)
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
                                        sector.walkingTime.toString()
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
                            var chartVisible by remember { mutableStateOf(false) }
                            val chartButtonIconRotation by animateValueAsState(
                                targetValue = if (chartVisible) -180f else -90f,
                                typeConverter = Float.DegreeConverter,
                            )

                            // Load the chart data
                            //viewModel.loadBarChartData(sector)

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
                                IconButton(
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
                            AnimatedVisibility(visible = chartVisible) {
                                Text(text = "Hello, this doesn't work, but hey, here's a pig 🐷")
                                /*BarChar(
                                    barChartData = viewModel.barChartData,
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
                                        drawLocation = SimpleLabelDrawer.DrawLocation.XAxis,
                                    )
                                )*/
                            }
                        }
                    }
                }
            }
            items(viewModel.paths) { item ->
                PathItem(item)
            }
        }
    }
}
