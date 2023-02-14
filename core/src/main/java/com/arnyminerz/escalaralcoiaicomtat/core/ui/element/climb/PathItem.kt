package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Grade
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.getAnnotatedString
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.grades
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
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast

class PathItemSampleProvider :
    PreviewParameterProvider<PathItemSampleProvider.PathItemPreviewData> {
    data class PathItemPreviewData(
        val path: Path,
        val blockingData: BlockingData?,
        val expanded: Boolean,
    )

    override val values: Sequence<PathItemPreviewData> = sequenceOf(
        PathItemPreviewData(
            Path.SAMPLE_PATH,
            BlockingData("1234", Path.SAMPLE_PATH_OBJECT_ID, BlockingType.UNKNOWN.idName, null),
            false,
        ),
        PathItemPreviewData(
            Path.SAMPLE_PATH,
            BlockingData("1234", Path.SAMPLE_PATH_OBJECT_ID, BlockingType.BIRD.idName, null),
            false,
        ),
        PathItemPreviewData(
            Path.SAMPLE_PATH,
            null,
            false,
        ),
        PathItemPreviewData(
            Path.SAMPLE_PATH_MULTIPITCH,
            null,
            false,
        ),
        PathItemPreviewData(
            Path.SAMPLE_PATH_MULTIPITCH,
            null,
            true,
        ),
    )
}

@Composable
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
fun PathItem(
    path: Path,
    informationIntent: (path: Path) -> Intent,
    blockingData: BlockingData? = null,
    expanded: Boolean = false,
) {
    val backgroundColor = if (blockingData != null)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (blockingData != null)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textColor,
        ),
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            var infoVisible by remember { mutableStateOf(expanded) }
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
                        color = textColor,
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
                        color = textColor,
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
                                color = textColor,
                                overflow = TextOverflow.Visible,
                                maxLines = 2,
                            )
                        }
                }
                // Grade
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = if (infoVisible && path.pitches.isNotEmpty())
                            path.pitches
                                .grades()
                                .getAnnotatedString()
                        else
                            Grade(path.generalGrade).getAnnotatedString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                // Height
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        text = if (infoVisible && path.pitches.isNotEmpty())
                            path.pitches
                                .mapNotNull { it.height?.let { h -> "${h}m" } }
                                .joinToString("\n")
                        else
                            path.generalHeight?.let { it.toString() + "m" } ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor,
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
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val blockingType = blockingData?.blockingType
                    if (blockingType != null && blockingType != BlockingType.UNKNOWN)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            modifier = Modifier
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp,
                                )
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Icon(
                                        Icons.Rounded.Warning,
                                        contentDescription = stringResource(blockingType.contentDescription),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .padding(start = 4.dp, top = 4.dp),
                                        tint = MaterialTheme.colorScheme.onError,
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(
                                            horizontal = 4.dp,
                                            vertical = 8.dp,
                                        )
                                ) {
                                    Text(
                                        text = stringResource(blockingType.explanation),
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }

                    BadgesRow(path, informationIntent)
                }
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun SimpleChip(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    SuggestionChip(
        label = {
            Text(text)
        },
        icon = {
            Image(
                painter = painterResource(icon),
                contentDescription = text,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier
            .padding(horizontal = 4.dp),
        onClick = onClick,
    )
}

@Composable
@ExperimentalMaterial3Api
private fun SimpleChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SuggestionChip(
        label = {
            Text(text)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier
            .padding(horizontal = 4.dp),
        onClick = onClick,
    )
}

@Composable
@ExperimentalMaterial3Api
fun BadgesRow(path: Path, informationIntent: (path: Path) -> Intent) {
    val context = LocalContext.current

    val fixedSafesData = path.fixedSafesData
    val requiredSafesData = path.requiredSafesData

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
    ) {
        if (fixedSafesData.quickdrawCount > 0) {
            val toastText =
                stringResource(R.string.toast_material_strings)
                    .format(fixedSafesData.quickdrawCount)
            SimpleChip(
                text = pluralStringResource(
                    R.plurals.safe_quickdraws,
                    count = fixedSafesData.quickdrawCount.toInt(),
                    fixedSafesData.quickdrawCount,
                ),
                icon = R.drawable.ic_icona_express,
                onClick = { context.toast(toastText) }
            )
        }
        SimpleChip(
            text = stringResource(R.string.path_chip_info),
            icon = Icons.Outlined.Info,
            onClick = { context.startActivity(informationIntent(path)) }
        )

        if (requiredSafesData.crackerRequired)
            SimpleChip(
                text = stringResource(R.string.safe_required_cracker),
                icon = R.drawable.ic_cracker,
                onClick = { context.toast(R.string.toast_material_required) },
            )
        if (requiredSafesData.friendRequired)
            SimpleChip(
                text = stringResource(R.string.safe_required_friend),
                icon = R.drawable.ic_friend,
                onClick = { context.toast(R.string.toast_material_required) },
            )
        if (requiredSafesData.lanyardRequired)
            SimpleChip(
                text = stringResource(R.string.safe_required_lanyard),
                icon = R.drawable.ic_lanyard,
                onClick = { context.toast(R.string.toast_material_required) },
            )
        if (requiredSafesData.nailRequired)
            SimpleChip(
                text = stringResource(R.string.safe_required_nail),
                icon = R.drawable.ic_reunio_clau,
                onClick = { context.toast(R.string.toast_material_required) },
            )
        if (requiredSafesData.pitonRequired)
            SimpleChip(
                text = stringResource(R.string.safe_required_piton),
                icon = R.drawable.ic_reunio_clau,
                onClick = { context.toast(R.string.toast_material_required) },
            )
        if (requiredSafesData.stripsRequired)
            SimpleChip(
                text = stringResource(R.string.safe_required_strips),
                icon = R.drawable.ic_strips,
                onClick = { context.toast(R.string.toast_material_required) },
            )
    }
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
    ) {
        if (path.generalEnding != null)
            SimpleChip(
                stringResource(
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
                },
                onClick = { context.toast(R.string.toast_ending_info) },
            )

        for (chip in fixedSafesData.list())
            if (chip.count > 0)
                SimpleChip(
                    text = if (chip.count >= 999 || chip.countableLabelRes == null)
                        stringResource(chip.uncountableLabelRes)
                    else
                        pluralStringResource(
                            chip.countableLabelRes,
                            chip.count.toInt(),
                            chip.count
                        ),
                    icon = chip.image,
                    onClick = { context.toast(R.string.toast_material_fixed) }
                )
    }
}

@Preview(name = "Path Item Preview")
@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
fun PathItemPreview(
    @PreviewParameter(PathItemSampleProvider::class) previewData: PathItemSampleProvider.PathItemPreviewData,
) {
    PathItem(
        path = previewData.path,
        informationIntent = { Intent() },
        blockingData = previewData.blockingData,
        expanded = previewData.expanded,
    )
}
