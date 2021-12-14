package com.arnyminerz.escalaralcoiaicomtat.core.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Creates a [TranslatableString] for using inside Composable functions.
 * @author Arnau Mora
 * @since 20211214
 * @param res The resource string id of the string
 * @param placeholders If wanted, the placeholders for replacing parts of the string
 */
@Composable
fun translatableString(@StringRes res: Int, vararg placeholders: String): TranslatableString =
    TranslatableString(stringResource(res).format(placeholders))

class TranslatableString(private val string: String) {
    constructor(string: String, vararg placeholders: String) : this(string.format(placeholders))
    constructor(context: Context, @StringRes res: Int) : this(context.getString(res))
    constructor(context: Context, @StringRes res: Int, vararg placeholders: String) :
            this(context.getString(res).format(placeholders))
    constructor(context: Context, @StringRes res: Int, placeholders: ArrayList<String>) :
            this(context.getString(res).format(placeholders))

    override fun toString(): String {
        return string
    }
}
