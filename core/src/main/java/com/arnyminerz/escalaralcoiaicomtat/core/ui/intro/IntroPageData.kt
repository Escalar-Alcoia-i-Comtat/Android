package com.arnyminerz.escalaralcoiaicomtat.core.ui.intro

import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.action.IntroAction

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
    val action: IntroAction<R> = IntroAction.None
)
