package com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * A generic button template for clearing something.
 * @author Arnau Mora
 * @since 20211214
 * @param buttonText The text of the button.
 * @param valueLoad A function that should load the current enabled value of the button.
 * @param deleteAction What to call when the user requests to remove whatever the button removes.
 */
@Composable
private fun ClearButton(
    @StringRes buttonText: Int,
    valueLoad: suspend () -> Boolean,
    deleteAction: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    var enabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val successToastString = stringResource(R.string.toast_clear_ok)
    val failureToastString = stringResource(R.string.toast_error_internal)

    doAsync {
        val value = valueLoad()
        scope.launch { enabled = value }
    }

    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = {
            doAsync {
                try {
                    scope.launch { enabled = false }
                    deleteAction()
                    scope.launch { toast(context, successToastString) }
                } catch (e: IOException) {
                    Timber.e(e, "Could not delete directory.")
                    scope.launch { toast(context, failureToastString) }
                } finally {
                    val value = valueLoad()
                    scope.launch { enabled = value }
                }
            }
        }
    ) {
        Text(stringResource(buttonText))
    }
}

/**
 * A button for controlling the clearing of a directory, for example, the cache.
 * @author Arnau Mora
 * @since 20211214
 * @param buttonText The text of the button that clears the [directory].
 * @param directory The directory to control.
 */
@Composable
private fun ClearDirectoryButton(
    @StringRes buttonText: Int,
    directory: File?,
    modifier: Modifier = Modifier
) {
    ClearButton(
        buttonText = buttonText,
        valueLoad = { directory?.exists() ?: false },
        deleteAction = { deleteDir(directory) },
        modifier = modifier
    )
}

/**
 * A button that clears all the contents from a shared prefs.
 * @author Arnau Mora
 * @since 20211214
 * @param buttonText The text of the button.
 */
@Composable
private fun ClearSettingsButton(
    @StringRes buttonText: Int,
    modifier: Modifier = Modifier
) {
    ClearButton(
        buttonText,
        valueLoad = { true },
        deleteAction = { PreferencesModule.clear() },
        modifier = modifier
    )
}

/**
 * A button that clears all the cdata stored in the search session.
 * @author Arnau Mora
 * @since 20211214
 * @param buttonText The text of the button.
 */
@Composable
private fun ClearSearchSessionButton(
    @StringRes buttonText: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ClearButton(
        buttonText,
        valueLoad = { true },
        deleteAction = {
            DataSingleton.getInstance(context)
                .repository
                .apply {
                    clearAreas()
                    clearZones()
                    clearSectors()
                    clearPaths()
                }

            PreferencesModule
                .systemPreferencesRepository
                .voidData()
        },
        modifier = modifier
    )
}

/**
 * The Window displayed in the storage activity which allows the user to clear different parts
 * of the app's contents.
 * @author Arnau Mora
 * @since 20211214
 * @param launchApp When the user requests to launch the app, this will get called.
 * @param sendFeedback When the user selects the option to send feedback.
 */
@Composable
fun StorageManagerWindow(launchApp: () -> Unit, sendFeedback: () -> Unit) {
    val context = LocalContext.current
    val cacheDir = context.cacheDir
    val storageDir = cacheDir.parentFile

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            ClearDirectoryButton(
                R.string.action_clear_cache,
                cacheDir,
                modifier = Modifier
                    .fillMaxWidth(.5f)
                    .padding(end = 4.dp)
            )
            ClearDirectoryButton(
                R.string.action_clear_storage,
                storageDir,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(start = 4.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(1f)) {
            ClearDirectoryButton(
                R.string.action_clear_downloads,
                context.filesDir,
                modifier = Modifier
                    .fillMaxWidth(.5f)
                    .padding(end = 4.dp)
            )
            ClearSettingsButton(
                R.string.action_clear_settings,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(start = 4.dp)
            )
        }

        ClearSearchSessionButton(
            buttonText = R.string.action_clear_data,
            modifier = Modifier
                .fillMaxWidth(1f)
        )

        Button(
            onClick = launchApp,
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            Text(stringResource(R.string.action_launch_app))
        }

        Button(
            onClick = sendFeedback,
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            Text(stringResource(R.string.action_send_feedback))
        }
    }
}
