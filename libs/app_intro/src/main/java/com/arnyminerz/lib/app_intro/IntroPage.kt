package com.arnyminerz.lib.app_intro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.lib.app_intro.action.IntroAction
import com.arnyminerz.lib.app_intro.action.IntroActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val state by action.value.value

        @Suppress("UNCHECKED_CAST")
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (action.type) {
                IntroActionType.SWITCH -> {
                    fun setValue(value: Boolean) = CoroutineScope(Dispatchers.IO).launch {
                        action.value.setValue(value as R)
                    }

                    Switch(
                        checked = state as Boolean,
                        enabled = action.enabled,
                        modifier = Modifier.testTag("intro_switch"),
                        onCheckedChange = { setValue(it) },
                    )
                    Text(
                        text = action.text,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { setValue(!(state as Boolean)) },
                    )
                }
                IntroActionType.CHECKBOX -> {
                    fun setValue(value: Boolean) = CoroutineScope(Dispatchers.IO).launch {
                        action.value.setValue(value as R)
                    }

                    Checkbox(
                        checked = state as Boolean,
                        enabled = action.enabled,
                        modifier = Modifier.testTag("intro_checkbox"),
                        onCheckedChange = { setValue(it) }
                    )
                    Text(
                        text = action.text,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .clickable { setValue(!(state as Boolean)) },
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
    val mutableState = remember { mutableStateOf(false) }

    IntroPage(
        data = IntroPageData(
            "This is title",
            "This is the content of the slide. The text can be modified for each slide.",
            action = IntroAction(
                if (mutableState.value)
                    "This can be switched"
                else
                    "This has been switched",
                object : Value<Boolean> {
                    override val value: State<Boolean>
                        @Composable
                        get() = mutableState

                    override suspend fun setValue(value: Boolean) {
                        mutableState.value = value
                    }
                },
                IntroActionType.SWITCH,
            )
        )
    )
}

@Composable
@Preview(name = "IntroPage Preview with checkbox")
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
fun IntroPagePreviewCheckbox() {
    val value = object : Value<Boolean> {
        val mutableState = remember { mutableStateOf(false) }

        override val value: State<Boolean>
            @Composable
            get() = mutableState

        override suspend fun setValue(value: Boolean) {
            mutableState.value = value
        }
    }

    IntroPage(
        data = IntroPageData(
            "This is title",
            "This is the content of the slide. The text can be modified for each slide.",
            action = IntroAction(
                if (value.value.value)
                    "This can be switched"
                else
                    "This has been switched",
                value,
                IntroActionType.CHECKBOX,
            )
        )
    )
}
