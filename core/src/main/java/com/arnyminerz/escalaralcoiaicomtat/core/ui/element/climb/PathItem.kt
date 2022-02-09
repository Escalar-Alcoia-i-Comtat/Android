package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.drawable
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.text
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast

@Composable
fun PathItem(path: Path) {
    val context = LocalContext.current

    Card(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            var infoVisible by remember { mutableStateOf(false) }
            val infoIconRotation by animateValueAsState(
                targetValue = if (infoVisible) -180f else -90f,
                typeConverter = Float.DegreeConverter,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sketch Number
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp, end = 4.dp)
                ) {
                    Text(
                        text = path.sketchId.toString(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp,
                    )
                }
                // Title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(
                        text = path.displayName,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Grade
                Column(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                ) {
                    val grade = path.grade()
                    Text(
                        text = grade.getAnnotatedString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
                // Height
                path.heights.takeIf { it.size > 0 }?.run {
                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = if (infoVisible && size > 1)
                                subList(1, size - 1).joinToString(separator = "m\n")
                            else
                                get(0).toString() + "m",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 20.sp,
                        )
                    }
                }
                // View button
                Column {
                    IconButton(
                        onClick = { infoVisible = !infoVisible },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = stringResource(R.string.image_desc_path_info),
                            modifier = Modifier.rotate(infoIconRotation)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = infoVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    if (path.endings.size > 0)
                        Chip(
                            text = stringResource(path.endings[0].text),
                            icon = ContextCompat.getDrawable(context, path.endings[0].drawable),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_ending_info) },
                        )

                    val fixedSafesData = path.fixedSafesData
                    for (chip in fixedSafesData.list())
                        if (chip.count > 0)
                            Chip(
                                text = stringResource(chip.displayName, chip.count),
                                icon = ContextCompat.getDrawable(context, chip.image),
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 4.dp),
                                onClick = { context.toast(R.string.toast_material_fixed) }
                            )

                    val requiredSafesData = path.requiredSafesData
                    if (requiredSafesData.crackerRequired)
                        Chip(
                            text = stringResource(R.string.safe_required_cracker),
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_cracker),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_material_required) },
                        )
                    if (requiredSafesData.friendRequired)
                        Chip(
                            text = stringResource(R.string.safe_required_friend),
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_friend),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_material_required) },
                        )
                    if (requiredSafesData.lanyardRequired)
                        Chip(
                            text = stringResource(R.string.safe_required_lanyard),
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_lanyard),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_material_required) },
                        )
                    if (requiredSafesData.nailRequired)
                        Chip(
                            text = stringResource(R.string.safe_required_nail),
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_reunio_clau),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_material_required) },
                        )
                    if (requiredSafesData.pitonRequired)
                        Chip(
                            text = stringResource(R.string.safe_required_piton),
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_reunio_clau),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_material_required) },
                        )
                    if (requiredSafesData.stripsRequired)
                        Chip(
                            text = stringResource(R.string.safe_required_strips),
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_strips),
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp),
                            onClick = { context.toast(R.string.toast_material_required) },
                        )
                }
            }
        }
    }
}

@Preview
@Composable
fun PathItemPreview() {
    PathItem(path = Path.SAMPLE_PATH)
}