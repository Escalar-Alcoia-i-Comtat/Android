package com.arnyminerz.escalaralcoiaicomtat.core.ui.intro

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arnyminerz.escalaralcoiaicomtat.core.R

/**
 * A composable for displaying a set of [IntroPage]s.
 * @author Arnau Mora
 * @since 20211214
 * @param pages All the [IntroPageData] to display.
 * @param finishListener What to run when the intro reaches its end.
 */
@Composable
@ExperimentalMaterial3Api
fun IntroWindow(pages: List<IntroPageData>, finishListener: () -> Unit) {
    var currentPage: Int by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentPage + 1 >= pages.size) {
                        // Reached the end, exit and enter MainActivity
                        finishListener()
                    } else
                        currentPage++
                }
            ) {
                if (currentPage + 1 >= pages.size)
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "" // TODO: Add content description
                    )
                else
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "" // TODO: Add content description
                    )
            }
        }
    ) {
        val page = pages[currentPage]

        IntroPage(page)
    }
}

@ExperimentalMaterial3Api
@Preview(showSystemUi = true)
@Composable
fun IntroWindowDemo() {
    IntroWindow(
        pages = listOf(
            IntroPageData(
                stringResource(R.string.intro_main_title, "Escalar Alcoià i Comtat"),
                stringResource(R.string.intro_main_message)
            )
        )
    ) { }
}
