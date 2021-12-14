package com.arnyminerz.escalaralcoiaicomtat.core.ui.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The data required to display an [IntroPage].
 * @author Arnau Mora
 * @since 20211214
 * @param title The string resource of the title of the page
 * @param content The string resource of the content description of the page
 */
data class IntroPageData(val title: String, val content: String)

@Composable
fun IntroPage(data: IntroPageData) {
    Column(
        modifier = Modifier
            .fillMaxHeight(1f)
            .fillMaxWidth(1f)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(start = 50.dp, end = 50.dp, top = 120.dp),
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            text = data.title
        )
        Text(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(top = 16.dp, start = 40.dp, end = 40.dp),
            textAlign = TextAlign.Center,
            fontSize = 19.sp,
            text = data.content
        )
    }
}
