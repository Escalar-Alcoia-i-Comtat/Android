package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun LoadingWindow(
    padding: PaddingValues,
    mainActivity: Class<*>,
    progressMessage: String,
    shouldShowErrorMessage: Boolean,
    errorMessage: String? = null,
    errorCode: Int? = null,
    showLaunchButton: Boolean = false,
) {
    val context = LocalContext.current

    val bottomPadding: Int by animateIntAsState(
        targetValue = if (shouldShowErrorMessage && errorMessage != null) 500 else 0,
        animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
    )

    val drawable: Drawable? =
        try {
            context
                .applicationInfo
                .icon
                .takeIf { it > 0x00 }
                ?.let { icon -> ContextCompat.getDrawable(context, icon) } ?: context
                .applicationInfo
                .takeIf { it != null }
                ?.let { appInfo -> context.packageManager.getApplicationIcon(appInfo) }
            ?: context.packageManager.defaultActivityIcon
        } catch (e: NullPointerException) {
            null
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = bottomPadding.dp)
        ) {
            Image(
                painter = rememberDrawablePainter(drawable),
                contentDescription = "",
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.TopCenter)
            )
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(.7f),
            visible = shouldShowErrorMessage && errorMessage != null
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = errorMessage ?: "",
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (showLaunchButton)
                    OutlinedButton(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            context.launch(mainActivity) {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        }
                    ) {
                        Text(text = "Enter anyway")
                    }
            }
        }

        AnimatedVisibility(
            visible = !shouldShowErrorMessage,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier
                        .padding(bottom = 9.dp, end = 4.dp),
                    text = progressMessage,
                    textAlign = TextAlign.End
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = errorCode != null && shouldShowErrorMessage,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = "Error code: $errorCode", // TODO: Localize
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(name = "LoadingWindow with error", showBackground = true)
@Composable
fun LoadingWindowErrorPreview() {
    LoadingWindow(
        padding = PaddingValues(0.dp),
        mainActivity = Void::class.java,
        progressMessage = "Loading...",
        shouldShowErrorMessage = true,
        errorMessage = "Could not load app",
        errorCode = 10,
    )
}
