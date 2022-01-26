package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import androidx.appsearch.app.AppSearchSession
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import kotlinx.coroutines.launch

@Composable
fun CompressedDownloadedDataItem(
    displayName: String,
    objectId: String,
    searchSession: AppSearchSession,
    dataClassActivity: Class<*>
) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    var viewButtonEnabled by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            ) {
                Text(
                    text = displayName,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Column {
                Button(
                    enabled = viewButtonEnabled,
                    onClick = {
                        viewButtonEnabled = false
                        doAsync {
                            val intent = DataClass.getIntent(
                                context,
                                dataClassActivity,
                                searchSession,
                                objectId
                            )
                            uiScope.launch {
                                viewButtonEnabled = true
                                if (intent != null)
                                    context.launch(intent)
                                else
                                    toast(context, R.string.toast_error_internal)
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Image(
                        Icons.Rounded.RemoveRedEye,
                        contentDescription = stringResource(R.string.action_view),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}
