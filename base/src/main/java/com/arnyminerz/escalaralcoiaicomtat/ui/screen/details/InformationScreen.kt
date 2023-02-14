package com.arnyminerz.escalaralcoiaicomtat.ui.screen.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.CardWithIcon
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.CardWithIconAndMarkdown
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.CardWithIconAndText

@Composable
@ExperimentalMaterial3Api
fun InformationScreen(
    path: Path,
    onCloseRequested: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(path.displayName) },
                navigationIcon = {
                    IconButton(onClick = onCloseRequested) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // TODO: Blocking

            // Builder information
            if (path.buildPatch != null || path.patches.isNotEmpty())
                CardWithIconAndText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    icon = Icons.Outlined.Construction,
                    title = stringResource(R.string.info_builder_title),
                    text = buildAnnotatedString {
                        path.buildPatch?.let { patch ->
                            append(
                                stringResource(
                                    R.string.info_builder,
                                    (patch.name ?: "¿?") + (patch.date?.let { ", $it" } ?: ""),
                                )
                            )
                        }
                        path.patches
                            // Only append if there are patches
                            .takeIf { it.isNotEmpty() }
                            // Break the previous line
                            .also { appendLine() }
                            // If a patch has been found, add the first line
                            ?.also { append(stringResource(R.string.info_rebuilders)) }
                            // Now add all the enumerating lines
                            ?.forEach { patch ->
                                // Format: <TAB><DOT> {name OR ¿?}{, date}
                                append("\n\t\u0046 ${patch.name ?: "¿?"}${patch.date?.let { ", $it" } ?: ""}")
                            }
                    },
                )

            // General information
            CardWithIconAndText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.info_general_title),
                text = buildAnnotatedString {
                    append(
                        stringResource(
                            R.string.info_general_grade,
                            path.generalGrade,
                            if (path.pitches.isNotEmpty())
                                stringResource(R.string.info_general_grade_more)
                            else "",
                            path.generalHeight ?: "",
                        )
                    )
                },
            )

            // Express count
            path.fixedSafesData.quickdrawCount.takeIf { it > 0 }?.let { quickdrawCount ->
                CardWithIconAndText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    icon = Icons.Outlined.Numbers,
                    title = stringResource(R.string.info_quickdraws_title),
                    text = buildAnnotatedString {
                        append(
                            stringResource(
                                R.string.info_quickdraws,
                                pluralStringResource(
                                    R.plurals.safe_quickdraws_lower,
                                    count = quickdrawCount.toInt(),
                                    quickdrawCount,
                                ),
                            )
                        )
                    },
                )
            }

            // Requires material
            if (path.requiredSafesData.hasSafeCount())
                CardWithIcon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    icon = null,
                    title = stringResource(R.string.info_required_title),
                    content = {
                        Text(stringResource(R.string.info_required))
                        for (safeCountData in path.requiredSafesData.list().filter { it.count > 0 })
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Image(
                                    painter = painterResource(safeCountData.image),
                                    contentDescription = safeCountData.stringResource(),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 4.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        safeCountData.stringResource(),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        stringResource(safeCountData.description),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                    },
                )

            // Fixed material
            if (path.fixedSafesData.hasSafeCount())
                CardWithIcon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    icon = null,
                    title = stringResource(R.string.info_fixed_title),
                    content = {
                        Text(stringResource(R.string.info_fixed))
                        for (safeCountData in path.fixedSafesData.list().filter { it.count > 0 })
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Image(
                                    painter = painterResource(safeCountData.image),
                                    contentDescription = safeCountData.stringResource(),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 4.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        safeCountData.stringResource(),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        stringResource(safeCountData.description),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                    },
                )

            // TODO: Multi-pitch paths

            // Description
            path.description?.let { description ->
                CardWithIconAndMarkdown(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    icon = null,
                    title = stringResource(R.string.info_description),
                    text = description,
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
@ExperimentalMaterial3Api
fun InformationScreenPreview() {
    InformationScreen(path = Path.SAMPLE_PATH) {}
}
