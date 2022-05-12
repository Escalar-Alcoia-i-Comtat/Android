package com.arnyminerz.lib.app_intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.lib.app_intro.action.IntroAction
import com.arnyminerz.lib.app_intro.action.IntroActionContext
import com.arnyminerz.lib.app_intro.action.IntroActionType

/**
 * Allows to display a page that can be used in the intro of the app to introduce the app
 * functionality to the user.
 * @author Arnau Mora
 * @since 20220130
 * @param R The return type of [IntroPageData.action] if set. Otherwise can be [Any].
 * @param data The [IntroPageData] to display.
 */
@Composable
@ExperimentalMaterial3Api
fun <R : Any?> IntroPage(data: IntroPageData<R>) {
    Column(
        modifier = Modifier
            .fillMaxHeight(1f)
            .fillMaxWidth(1f)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(start = 50.dp, end = 50.dp, top = 120.dp),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
            text = data.title
        )
        Text(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(top = 16.dp, start = 40.dp, end = 40.dp),
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
            text = data.content
        )

        val action = data.action
        @Suppress("UNCHECKED_CAST")
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (action.type) {
                IntroActionType.BUTTON -> {
                    val context: IntroActionContext<R> =
                        object : IntroActionContext<R>() {
                            override fun setState(state: R) {}
                        }
                    Button(
                        enabled = action.enabled,
                        onClick = {
                            action.callback.invoke(context, null as R)
                        },
                    ) {
                        Text(text = action.text)
                    }
                }
                IntroActionType.SWITCH -> {
                    var checked by remember {
                        mutableStateOf(
                            action.currentValue as? Boolean ?: false
                        )
                    }
                    val context: IntroActionContext<R> =
                        object : IntroActionContext<R>() {
                            override fun setState(state: R) {
                                (state as? Boolean)?.let { checked = it }
                            }
                        }
                    Switch(
                        checked = checked,
                        enabled = action.enabled,
                        onCheckedChange = {
                            checked = it
                            action.callback.invoke(context, it as R)
                        },
                    )
                    Text(
                        text = action.text,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                IntroActionType.CHECKBOX -> {
                    var checked by remember {
                        mutableStateOf(
                            action.currentValue as? Boolean ?: false
                        )
                    }
                    val context: IntroActionContext<R> =
                        object : IntroActionContext<R>() {
                            override fun setState(state: R) {
                                (state as? Boolean)?.let { checked = it }
                            }
                        }
                    Checkbox(
                        checked = checked,
                        enabled = action.enabled,
                        onCheckedChange = {
                            checked = it
                            action.callback.invoke(context, it as R)
                        }
                    )
                    Text(
                        text = action.text,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
@Preview(name = "IntroPage Preview")
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
fun IntroPagePreview() {
    IntroPage<Any>(
        data = IntroPageData(
            "This is title",
            "This is the content of the slide. The text can be modified for each slide."
        )
    )
}

@Composable
@Preview(name = "IntroPage Preview with switch")
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
fun IntroPagePreviewSwitch() {
    var switchStatus by remember { mutableStateOf(false) }

    IntroPage(
        data = IntroPageData(
            "This is title",
            "This is the content of the slide. The text can be modified for each slide.",
            action = IntroAction(
                if (switchStatus)
                    "This can be switched"
                else
                    "This has been switched",
                switchStatus,
                IntroActionType.SWITCH,
                callback = { switched ->
                    switchStatus = switched
                }
            )
        )
    )
}