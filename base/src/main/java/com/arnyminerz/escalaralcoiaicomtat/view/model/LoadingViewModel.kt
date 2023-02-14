package com.arnyminerz.escalaralcoiaicomtat.view.model

import android.app.Application
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.WarningActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.get
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.arnyminerz.escalaralcoiaicomtat.core.worker.BlockStatusWorker
import kotlinx.coroutines.launch
import org.json.JSONException
import timber.log.Timber

class LoadingViewModel(application: Application) : AndroidViewModel(application) {
    var progressMessageResource = mutableStateOf(R.string.status_loading)
    var progressMessageAttributes = mutableStateOf(listOf<Any?>())

    @get:StringRes
    var errorMessage = mutableStateOf<Int?>(null)

    var errorCode = mutableStateOf<Int?>(null)

    /**
     * Stores whether or not the user was using the app before the migration to DataStore. If so,
     * show a warning, and try to migrate.
     * @author Arnau Mora
     * @since 20220327
     */
    var migratedFromSharedPreferences = false

    /**
     * Stores whether or not the device is compatible with MD5 encryption. If not, updates may not
     * work correctly.
     * @author Arnau Mora
     * @since 20220327
     */
    var md5Compatible = true

    private var isLoading = false

    private var deepLinkPath by mutableStateOf<String?>(null)

    /**
     * Requests the view model to start loading
     * @author Arnau Mora
     * @since 20211225
     * @throws SecurityException When the version given by the server and the expected by the app
     * do not match.
     */
    @Throws(SecurityException::class)
    fun startLoading(
        deepLinkPath: String?,
    ) {
        viewModelScope.launch {
            loadingRoutine(deepLinkPath)
        }
    }

    /**
     * Tries calling [load] again, when an error occurred. Only gets called when [errorMessage] is
     * not null, this is, when an error occurred.
     * @author Arnau Mora
     * @since 20220322
     * @throws SecurityException When the version given by the server and the expected by the app
     * do not match.
     */
    @Throws(SecurityException::class)
    fun tryLoading() {
        if (errorMessage.value != null)
            viewModelScope.launch {
                Timber.v("Trying to load data again...")
                performLoad()
            }
    }

    @UiThread
    @Throws(SecurityException::class)
    private suspend fun loadingRoutine(
        deepLinkPath: String?,
    ) {
        this.deepLinkPath = deepLinkPath

        progressMessageResource.value = R.string.status_loading_checks
        ioContext {
            checkMD5Support()
        }

        progressMessageResource.value = R.string.status_loading_workers
        ioContext {
            val alreadyScheduled = BlockStatusWorker.isScheduled(context)
            if (!alreadyScheduled)
                BlockStatusWorker.schedule(context)
        }

        /*progressMessageResource.value = R.string.status_loading_data
        ioContext {
            load(app, deepLinkPath) { stringResource, errorResource ->
                stringResource?.let { progressMessageResource.value = it }
                errorResource?.let { errorMessage.value = it }
            }
        }*/

        performLoad()
    }

    /**
     * Calls [load] including callbacks. Also resets [errorMessage] and sets [progressMessageResource]
     * to "Loading data".
     * @author Arnau Mora
     * @since 20220322
     * @throws SecurityException When the version given by the server and the expected by the app
     * do not match.
     */
    @UiThread
    @Throws(SecurityException::class)
    private suspend fun performLoad() {
        val app = getApplication<App>()

        errorMessage.value = null
        errorCode.value = null
        progressMessageResource.value = R.string.status_loading_data

        if (!isLoading)
            ioContext {
                load(app, deepLinkPath) { stringResource, errorResource, errorCode_ ->
                    stringResource?.let { progressMessageResource.value = it }
                    errorResource?.let { errorMessage.value = it }
                    errorCode_?.let { errorCode.value = it }
                }
            }
        else
            Timber.w("Tried to run performLoad() that is already loading.")
    }

    /**
     * Checks if the device supports MD5 hashing. If it isn't supported, it shows a warning to the
     * user.
     * @author Arnau Mora
     * @since 20210929
     */
    @WorkerThread
    private fun checkMD5Support() {
        md5Compatible = md5Compatible()
        Timber.i("Is MD5 hashing supported: $md5Compatible")
        if (!md5Compatible)
            Timber.w("MD5 hashing is not compatible")
    }

    /**
     * Loads and processes all the data from the data module.
     * Note: The data module needs to have been installed.
     * @author Arnau Mora
     * @since 20211225
     * @throws SecurityException When the version given by the server and the expected by the app
     * do not match.
     */
    @WorkerThread
    @Throws(SecurityException::class)
    private suspend fun load(
        app: App,
        deepLinkPath: String?,
        @UiThread progressUpdater: (textResource: Int?, errorResource: Int?, errorCode: Int?) -> Unit
    ) {
        try {
            isLoading = true

            Timber.v("Fetching areas data...")
            val jsonData = context.getJson("$REST_API_DATA_LIST/*")

            Timber.v("Fetching server info...")
            val serverInfo = context.getJson(REST_API_INFO_ENDPOINT)

            // Check if the response contains a "result" field
            // if (jsonData.has("result"))
            //     throw IllegalStateException("Server's JSON data does not contain a field named \"result\".")

            Timber.i("Data fetched from data module!")
            val areas = loadAreas(app, jsonData.getJSONObject("result"), serverInfo)

            Timber.v("Finished loading areas.")
            if (areas.isNotEmpty()) {
                Timber.v("Getting intent from deep link...")
                val intent = deepLinkPath?.let { path ->
                    uiContext {
                        progressUpdater(R.string.status_loading_deep_link, null, null)
                    }

                    DataClass.getIntent(
                        app,
                        DataClassActivity::class.java,
                        path
                    )
                }

                val shownPrefWarning = get(Keys.shownPreferencesWarning, false)
                val shownMd5Warning = get(Keys.shownMd5Warning, false)

                Timber.v("Launching activity (intent==null? ${intent == null})...")
                uiContext {
                    if ((migratedFromSharedPreferences && !shownPrefWarning) ||
                        (!md5Compatible) && !shownMd5Warning
                    ) {
                        Timber.i("Displaying warnings...")
                        app.launch(WarningActivity::class.java) {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                            if (intent != null)
                                putExtra(EXTRA_WARNING_INTENT, intent)
                            putExtra(EXTRA_WARNING_MD5, md5Compatible)
                            putExtra(EXTRA_WARNING_PREFERENCE, migratedFromSharedPreferences)
                        }
                    } else if (intent != null) {
                        Timber.v("Launching url intent...")
                        app.launch(intent) {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        Timber.v("Launching MainActivity...")
                        app.launch(MainActivity::class.java) {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }
            } else Timber.v("Areas is empty, but no handle was called.")
        } catch (e: VolleyError) {
            Timber.e(e, "An error occurred while loading areas from the server.")
            uiContext {
                when (e) {
                    is NoConnectionError -> progressUpdater(
                        null,
                        R.string.status_loading_error_connection,
                        e.networkResponse?.statusCode,
                    )
                    is TimeoutError -> progressUpdater(
                        null,
                        R.string.status_loading_error_server,
                        e.networkResponse?.statusCode,
                    )
                    else -> progressUpdater(
                        null,
                        R.string.status_loading_error_server,
                        e.networkResponse?.statusCode,
                    )
                }
            }
        } catch (e: JSONException) {
            Timber.e(e, "Could not parse the server's response.")
            ioContext { progressUpdater(null, R.string.status_loading_error_format, 1000) }
        } catch (e: IllegalStateException) {
            Timber.e("The server response does not have a \"result\" field.")
            ioContext { progressUpdater(null, R.string.status_loading_error_format, 1001) }
        } catch (e: ExceptionInInitializerError) {
            Timber.e(e, "Could not initialize a field.")
            ioContext { progressUpdater(null, R.string.status_loading_error_internal, 1002) }
        } finally {
            isLoading = false
        }
    }
}