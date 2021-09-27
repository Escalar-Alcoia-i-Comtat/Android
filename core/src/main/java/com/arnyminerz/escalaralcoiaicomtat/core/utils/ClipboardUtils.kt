package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Gets a new instance of the [ClipboardManager] from [Context.CLIPBOARD_SERVICE].
 * @author Arnau Mora
 * @since 20210927
 */
val Context.clipboard: ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

/**
 * Copies the [String] into the clipboard using [ClipboardManager].
 * @author Arnau Mora
 * @since 20210927
 * @param clipboardManager The [ClipboardManager] for copying the [String].
 * @param label A developer-aimed label used for describing the contents of what's in the clipboard.
 */
fun String.copy(clipboardManager: ClipboardManager, label: String = "Copied text") {
    val clip = ClipData.newPlainText(label, this)
    clipboardManager.setPrimaryClip(clip)
}
