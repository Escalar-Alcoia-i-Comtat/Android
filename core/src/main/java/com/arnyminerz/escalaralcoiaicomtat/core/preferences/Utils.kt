package com.arnyminerz.escalaralcoiaicomtat.core.preferences

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ioContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

fun <R> DataStore<Preferences>.getTheFlow(
    key: Preferences.Key<R>,
    default: R,
): Flow<R> = data
    .catch {
        if (it is IOException) {
            emit(emptyPreferences())
        } else {
            throw it
        }
    }
    .map {
        it[key] ?: default
    }
    .distinctUntilChanged()

/**
 * Gets the last value stored at [key], or returns [default] if there are no stored values.
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key of the preference.
 * @param default What to return if there's no stored data at [key].
 * @return The data stored at [key] or [default] if any.
 * @see PreferencesModule.get
 */
@WorkerThread
suspend fun <T> Context.get(key: Preferences.Key<T>, default: T) =
    PreferencesModule.get(this, key, default)

/**
 * Gets the last value stored at [key], or returns [default] if there are no stored values.
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key of the preference.
 * @param default What to return if there's no stored data at [key].
 * @return The data stored at [key] or [default] if any.
 * @see PreferencesModule.get
 */
@WorkerThread
suspend fun <T> AndroidViewModel.get(key: Preferences.Key<T>, default: T) =
    PreferencesModule.get(context, key, default)

/**
 * Updates the value at [key] with [value].
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key to update.
 * @param value The value to set at [key].
 * @see PreferencesModule.set
 */
@WorkerThread
suspend fun <T> Context.set(key: Preferences.Key<T>, value: T) =
    PreferencesModule.set(this, key, value)

/**
 * Updates the value at [key] with [value] using the scope of the view model.
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key to update.
 * @param value The value to set at [key].
 * @see PreferencesModule.set
 */
fun <T> AndroidViewModel.set(key: Preferences.Key<T>, value: T) =
    viewModelScope.launch {
        ioContext { PreferencesModule.set(getApplication(), key, value) }
    }

/**
 * Updates the value at [key] with [value] in the IO thread.
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key to update.
 * @param value The value to set at [key].
 * @see PreferencesModule.set
 * @see set
 * @see doAsync
 */
fun <T> Context.setAsync(
    key: Preferences.Key<T>,
    value: T,
) = doAsync { set(key, value) }

/**
 * Observes the values sent to [key] while the Activity is started. Will pause `onPause`, and restart
 * after `onStart`.
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key to observe.
 * @param default The value to return if no value is stored at [key].
 * @param callback Will get called whenever the value at [key] is updated.
 */
fun <T> ComponentActivity.observe(
    key: Preferences.Key<T>,
    default: T,
    @WorkerThread callback: suspend (value: T) -> Unit,
) = lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        dataStore.getTheFlow(key, default)
            .collect { callback(it) }
    }
}

/**
 * Collects the the value of [key] as a state.
 * @author Arnau Mora
 * @since 20220829
 * @param key The preferences key to observe.
 * @param default The value to return if no value is stored at [key], and before the stored value is
 * fetched.
 * @return An state that gets updated with new values if set to the preference.
 * @see Flow.collectAsState
 * @see getTheFlow
 */
@Composable
fun <T> collectAsState(key: Preferences.Key<T>, default: T) =
    LocalContext
        .current
        .dataStore
        .getTheFlow(key, default)
        .collectAsState(initial = default)
