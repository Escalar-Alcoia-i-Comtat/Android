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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Grade
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_CHAIN_CARABINER
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_CHAIN_RING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_LANYARD
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_NONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PITON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PLATE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PLATE_LANYARD
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PLATE_RING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_RAPPEL
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_WALKING
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast

@Composable
fun PathItem(path: Path, infoVisibleStart: Boolean = false) {
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
            var infoVisible by remember { mutableStateOf(infoVisibleStart) }
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
                    val builderName = path.buildPatch?.name
                    val builderDate = path.buildPatch?.date
                    if (builderName != null || builderDate != null)
                        AnimatedVisibility(visible = infoVisible) {
                            Text(
                                text = (builderName ?: "") + (builderDate?.let { ", $it" } ?: ""),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                overflow = TextOverflow.Visible,
                                maxLines = 2,
                            )
                        }
                }
                // Grade
                Column(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                ) {
                    val grade = Grade(path.generalGrade)
                    Text(
                        text = grade.getAnnotatedString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
                // Height
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        text = if (infoVisible && path.pitches.isNotEmpty())
                            path.pitches.map { it.height }.joinToString(separator = "m\n")
                        else
                            path.generalHeight?.let { it.toString() + "m" } ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                    )
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
                    if (path.generalEnding != null)
                        Chip(
                            text = stringResource(
                                when (path.generalEnding) {
                                    ENDING_TYPE_PLATE -> R.string.path_ending_plate
                                    ENDING_TYPE_PLATE_RING -> R.string.path_ending_plate_ring
                                    ENDING_TYPE_PLATE_LANYARD -> R.string.path_ending_plate_lanyard
                                    ENDING_TYPE_CHAIN_RING -> R.string.path_ending_chain_ring
                                    ENDING_TYPE_CHAIN_CARABINER -> R.string.path_ending_chain_carabiner
                                    ENDING_TYPE_PITON -> R.string.path_ending_piton
                                    ENDING_TYPE_WALKING -> R.string.path_ending_walking
                                    ENDING_TYPE_RAPPEL -> R.string.path_ending_rappel
                                    ENDING_TYPE_LANYARD -> R.string.path_ending_lanyard
                                    ENDING_TYPE_NONE -> R.string.path_ending_none
                                    else -> R.string.path_ending_unknown
                                }
                            ),
                            icon = ContextCompat.getDrawable(
                                context,
                                when (path.generalEnding) {
                                    ENDING_TYPE_PLATE -> R.drawable.ic_reunio_xapes_24
                                    ENDING_TYPE_PLATE_RING -> R.drawable.ic_reunio_xapesargolla
                                    ENDING_TYPE_PLATE_LANYARD -> R.drawable.ic_lanyard
                                    ENDING_TYPE_CHAIN_RING -> R.drawable.ic_reunio_cadenaargolla
                                    ENDING_TYPE_CHAIN_CARABINER -> R.drawable.ic_reunio_cadenamosqueto
                                    ENDING_TYPE_PITON -> R.drawable.ic_reunio_clau
                                    ENDING_TYPE_WALKING -> R.drawable.ic_descens_caminant
                                    ENDING_TYPE_RAPPEL -> R.drawable.ic_via_rappelable
                                    ENDING_TYPE_LANYARD -> R.drawable.ic_lanyard
                                    ENDING_TYPE_NONE -> R.drawable.round_close_24
                                    else -> R.drawable.round_close_24
                                }
                            ),
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

@Preview(name = "Path with builder")
@Composable
fun PathItemBuilderPreview() {
    PathItem(
        path = Path(
            "1234",
            0,
            12,
            "Testing path",
            ">6c+\n1>6c\n2>6c+\n3>7a",
            ">80\n1>30\n2>30\n3>20",
            ">chain_carabiner\n1>chain_carabiner\n2>chain_carabiner\n3>chain_ring",
            "1>horizontal equipped\n2>diagonal equipped\n3>vertical clear",
            FixedSafesData(10, 0, 0, 0, 0, 0),
            RequiredSafesData(
                lanyardRequired = true,
                crackerRequired = false,
                friendRequired = false,
                stripsRequired = true,
                pitonRequired = false,
                nailRequired = false
            ),
            null,
            "Testing builder;2019",
            null,
            parentSectorId = "123"
        ), true
    )
}
