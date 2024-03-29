package com.arnyminerz.escalaralcoiaicomtat.core.ui.map

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiLet
import com.bumptech.glide.request.RequestOptions
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import timber.log.Timber

@Composable
fun MapBottomDialog(
    dataClassActivity: Class<*>,
    bottomDialogTitle: String,
    bottomDialogImage: Uri?,
) {
    val context = LocalContext.current

    Card(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            GlideImage(
                imageModel = { bottomDialogImage },
                requestOptions = {
                    RequestOptions
                        .placeholderOf(R.drawable.ic_wide_placeholder)
                        .error(R.drawable.ic_wide_placeholder)
                },
                imageOptions = ImageOptions(
                    contentDescription = bottomDialogTitle,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = bottomDialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    var buttonEnabled by remember { mutableStateOf(true) }
                    Button(
                        enabled = buttonEnabled,
                        onClick = {
                            buttonEnabled = false
                            doAsync {
                                DataClass.getIntent(
                                    context,
                                    dataClassActivity,
                                    bottomDialogTitle,
                                )?.uiLet { intent ->
                                    buttonEnabled = true
                                    context.launch(intent)
                                } ?: uiContext {
                                    Timber.e("Could not find intent for \"$bottomDialogTitle\"")
                                    context.toast(R.string.toast_error_internal)
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Row {
                            Image(
                                Icons.Rounded.Login,
                                contentDescription = stringResource(R.string.action_view),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.CenterVertically)
                            )
                            Text(
                                text = stringResource(R.string.action_view),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
