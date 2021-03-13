package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

fun generateUUID(): String = UUID.randomUUID().toString()

@Suppress("DEPRECATION")
fun getDisplaySize(activity: Activity): Pair<Int, Int> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        Pair(bounds.width(), bounds.height())
    } else {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

fun Int.drawable(context: Context) = ContextCompat.getDrawable(context, this)

fun runAsync(call: () -> Unit) =
    CoroutineScope(Dispatchers.IO).launch {
        runCatching(call)
    }

/**
 * Runs the action in the UI thread.
 * @author Arnau Mora
 * @since 20210311
 * @param action The runnable to execute
 * @see Activity.runOnUiThread
 * @see Fragment.getActivity
 * @throws IllegalStateException If not currently associated with an activity or if associated only with a context
 */
@Throws(IllegalStateException::class)
fun Fragment.runOnUiThread(action: Activity.() -> Unit) =
    requireActivity().runOnUiThread { action(requireActivity()) }

fun String.fixTildes(): String =
    replace("Â¡", "¡")
        .replace("Â¢", "¢")
        .replace("Â£", "£")
        .replace("Â¤", "¤")
        .replace("Â¥", "¥")
        .replace("Â¦", "¦")
        .replace("Â§", "§")
        .replace("Â¨", "¨")
        .replace("Â©", "©")
        .replace("Âª", "ª")
        .replace("Â«", "«")
        .replace("Â®", "®")
        .replace("Â¯", "¯")
        .replace("Â°", "°")
        .replace("Â±", "±")
        .replace("Â²", "²")
        .replace("Â³", "³")
        .replace("Â´", "´")
        .replace("Âµ", "µ")
        .replace("Â·", "·")
        .replace("Â¸", "¸")
        .replace("Â¹", "¹")
        .replace("Âº", "º")
        .replace("Â»", "»")
        .replace("Â¼", "¼")
        .replace("Â½", "½")
        .replace("Â¾", "¾")
        .replace("Â¿", "¿")
        .replace("Ã€", "À")
        .replace("Ã", "Á")
        .replace("Ã‚", "Â")
        .replace("Ãƒ", "Ã")
        .replace("Ã„", "Ä")
        .replace("Ã…", "Å")
        .replace("Ã†", "Æ")
        .replace("Ã‡", "Ç")
        .replace("Ãˆ", "È")
        .replace("Ã‰", "É")
        .replace("ÃŠ", "Ê")
        .replace("Ã‹", "Ë")
        .replace("ÃŒ", "Ì")
        .replace("Ã", "Í")
        .replace("ÃŽ", "Î")
        .replace("Ã", "Ï")
        .replace("Ã", "Ð")
        .replace("Ã‘", "Ñ")
        .replace("Ã’", "Ò")
        .replace("Ã“", "Ó")
        .replace("Ã”", "Ô")
        .replace("Ã•", "Õ")
        .replace("Ã–", "Ö")
        .replace("Ã—", "×")
        .replace("Ã˜", "Ø")
        .replace("Ã™", "Ù")
        .replace("Ãš", "Ú")
        .replace("Ã›", "Û")
        .replace("Ãœ", "Ü")
        .replace("Ã", "Ý")
        .replace("Ãž", "Þ")
        .replace("ÃŸ", "ß")
        .replace("Ã", "à")
        .replace("Ã¡", "á")
        .replace("Ã¢", "â")
        .replace("Ã£", "ã")
        .replace("Ã¤", "ä")
        .replace("Ã¥", "å")
        .replace("Ã¦", "æ")
        .replace("Ã§", "ç")
        .replace("Ã¨", "è")
        .replace("Ã©", "é")
        .replace("Ãª", "ê")
        .replace("Ã«", "ë")
        .replace("Ã", "ì")
        .replace("Ã­", "í")
        .replace("Ã®", "î")
        .replace("Ã¯", "ï")
        .replace("Ã°", "ð")
        .replace("Ã±", "ñ")
        .replace("Ã²", "ò")
        .replace("Ã³", "ó")
        .replace("Ã´", "ô")
        .replace("Ãµ", "õ")
        .replace("Ã", "ö")
        .replace("Ã·", "÷")
        .replace("Ã¸", "ø")
        .replace("Ã¹", "ù")
        .replace("Ãº", "ú")
        .replace("Ã»", "û")
        .replace("Ã¼", "ü")
        .replace("Ã½", "ý")
        .replace("Ã¾", "þ")
        .replace("Ã¿", "ÿ")
