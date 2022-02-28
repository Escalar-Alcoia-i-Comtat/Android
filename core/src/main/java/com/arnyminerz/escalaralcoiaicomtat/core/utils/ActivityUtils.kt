package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.app.Activity
import android.os.Bundle

/**
 * Gets the specified [extra] parameter, or if null, gets from [savedInstanceState].
 * @author Arnau Mora
 * @since 20220228
 * @param extra The [DataExtra] to fetch.
 * @param savedInstanceState The [savedInstanceState] to use as backup.
 */
inline fun <reified T : Any?> Activity.getExtraOrSavedInstanceState(
    extra: DataExtra<T>,
    savedInstanceState: Bundle?
) = intent.extras?.getExtra(extra) ?: savedInstanceState?.getExtra(extra)
