package com.arnyminerz.escalaralcoiaicomtat.core.data

import android.content.Context
import androidx.annotation.StringRes

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
