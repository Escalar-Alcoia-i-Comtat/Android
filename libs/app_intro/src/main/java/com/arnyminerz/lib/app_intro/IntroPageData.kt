package com.arnyminerz.lib.app_intro

import com.arnyminerz.lib.app_intro.action.IntroAction

/**
 * The data required to display an [IntroPage].
 * @author Arnau Mora
 * @since 20211214
 * @param title The string resource of the title of the page
 * @param content The string resource of the content description of the page
 */
data class IntroPageData<R>(
    val title: String,
    val content: String,
    val action: IntroAction<R> = IntroAction.None,
    val permissions: Array<String>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntroPageData<*>

        if (title != other.title) return false
        if (content != other.content) return false
        if (action != other.action) return false
        if (permissions != null) {
            if (other.permissions == null) return false
            if (!permissions.contentEquals(other.permissions)) return false
        } else if (other.permissions != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + (permissions?.contentHashCode() ?: 0)
        return result
    }
}
