package com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DATA_MODULE_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_INDEXED_SEARCH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
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
 * @param sharedPreferences The preferences to clear.
 */
@Composable
private fun ClearSettingsButton(
    @StringRes buttonText: Int,
    sharedPreferences: SharedPreferences,
    modifier: Modifier = Modifier
) {
    ClearButton(
        buttonText,
        valueLoad = { true },
        deleteAction = { sharedPreferences.edit().clear().apply() },
        modifier = modifier
    )
}

/**
 * A button that clears all the cdata stored in the search session.
 * @author Arnau Mora
 * @since 20211214
 * @param buttonText The text of the button.
 * @param searchSession The search session to clear.
 */
@Composable
private fun ClearSearchSessionButton(
    @StringRes buttonText: Int,
    searchSession: AppSearchSession,
    modifier: Modifier = Modifier
) {
    ClearButton(
        buttonText,
        valueLoad = { true },
        deleteAction = {
            val searchSpec = SearchSpec.Builder()
                .addFilterPackageNames("com.arnyminerz.escalaralcoiaicomtat")
                .setResultCountPerPage(10000)
                .build()
            searchSession.remove("", searchSpec).await()
            PREF_INDEXED_SEARCH.put(false)
        },
        modifier = modifier
    )
}

/**
 * A button that uninstalls the data module when clicked.
 * @author Arnau Mora
 * @since 20211227
 */
@Composable
fun UninstallDataModuleButton(context: Context) {
    val splitInstallManager = SplitInstallManagerFactory.create(context)
    val dataModuleInstalled = splitInstallManager.installedModules.contains(DATA_MODULE_NAME)
    var uninstallingDataModule by remember { mutableStateOf(!dataModuleInstalled) }
    Timber.i("Data module installed: $dataModuleInstalled")
    Button(
        enabled = !uninstallingDataModule,
        onClick = {
            uninstallingDataModule = true
            Timber.i("Uninstalling data module...")
            splitInstallManager.deferredUninstall(listOf(DATA_MODULE_NAME))

            uninstallingDataModule = false
            context.toast(R.string.toast_data_uninstalled)
        },
        modifier = Modifier
            .fillMaxWidth(1f)
    ) {
        Text(stringResource(R.string.action_uninstall_data))
    }
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

    val app = context.applicationContext as App
    val searchSession = app.searchSession

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
                filesDir(context),
                modifier = Modifier
                    .fillMaxWidth(.5f)
                    .padding(end = 4.dp)
            )
            ClearSettingsButton(
                R.string.action_clear_settings,
                sharedPreferences,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(start = 4.dp)
            )
        }

        ClearSearchSessionButton(
            buttonText = R.string.action_clear_data, searchSession = searchSession,
            modifier = Modifier
                .fillMaxWidth(1f)
        )

        UninstallDataModuleButton(context)

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
