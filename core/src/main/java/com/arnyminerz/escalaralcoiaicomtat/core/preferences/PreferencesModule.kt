package com.arnyminerz.escalaralcoiaicomtat.core.preferences

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREFERENCES_NAME
import com.arnyminerz.lib.app_intro.Value
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

/**
 * The module for modifying preferences.
 * @author Arnau Mora
 * @since 20211229
 */
object PreferencesModule {
    /**
     * Clears all the preferences from the system.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    /**
     * Gets the last value stored at [key], or returns [default] if there are no stored values.
     * @author Arnau Mora
     * @since 20220829
     * @param context The context that is requesting the data.
     * @param key The preferences key of the preference.
     * @param default What to return if there's no stored data at [key].
     * @return The data stored at [key] or [default] if any.
     */
    @WorkerThread
    suspend fun <T> get(context: Context, key: Preferences.Key<T>, default: T): T =
        context
            .dataStore
            .getTheFlow(key, default)
            .first()

    /**
     * Updates the value at [key] with [value].
     * @author Arnau Mora
     * @since 20220829
     * @param context The context that is requesting the data.
     * @param key The preferences key to update.
     * @param value The value to set at [key].
     */
    @WorkerThread
    suspend fun <T> set(context: Context, key: Preferences.Key<T>, value: T) {
        context
            .dataStore
            .edit { it[key] = value }
    }
}

class PreferenceValue<Kt>(
    context: Context,
    private val key: Preferences.Key<Kt>,
    private val default: Kt,
) : Value<Kt> {
    private val dataStore = context.dataStore

    private val flow = dataStore
        .getTheFlow(key, default)

    override suspend fun setValue(value: Kt) {
        dataStore.edit {
            it[key] = value
        }
    }

    override val value: State<Kt>
        @Composable
        get() = flow
            .collectAsState(initial = default)
}

fun <T> Context.PreferenceValue(key: Preferences.Key<T>, default: T) =
    PreferenceValue(this, key, default)
