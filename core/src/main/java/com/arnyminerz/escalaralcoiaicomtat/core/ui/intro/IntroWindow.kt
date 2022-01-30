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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

/**
 * A composable for displaying a set of [IntroPage]s.
 * @author Arnau Mora
 * @since 20211214
 * @param pages All the [IntroPageData] to display.
 * @param finishListener What to run when the intro reaches its end.
 */
@Composable
@ExperimentalMaterial3Api
@ExperimentalPagerApi
fun IntroWindow(pages: List<IntroPageData<Any>>, finishListener: () -> Unit) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage + 1 >= pages.size) {
                        // Reached the end, exit and enter MainActivity
                        finishListener()
                    } else scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            ) {
                if (pagerState.currentPage + 1 >= pages.size)
                    Icon(
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.fab_desc_finish)
                    )
                else
                    Icon(
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.fab_desc_next)
                    )
            }
        }
    ) {
        HorizontalPager(count = pages.size, state = pagerState) { currentPage ->
            val page = pages[currentPage]

            IntroPage(page)
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalPagerApi
@Preview(showSystemUi = true, name = "Intro Window Preview")
@Composable
fun IntroWindowDemo() {
    AppTheme {
        IntroWindow(
            pages = listOf(
                IntroPageData(
                    stringResource(R.string.intro_main_title, "Escalar Alcoià i Comtat"),
                    stringResource(R.string.intro_main_message)
                ),
                IntroPageData(
                    stringResource(R.string.intro_warning_title),
                    stringResource(R.string.intro_warning_message)
                )
            )
        ) { }
    }
}

@ExperimentalMaterial3Api
@ExperimentalPagerApi
@Preview(showSystemUi = true, name = "Intro Window Dark Preview")
@Composable
fun IntroWindowDarkDemo() {
    AppTheme(useDarkTheme = true) {
        IntroWindow(
            pages = listOf(
                IntroPageData(
                    stringResource(R.string.intro_main_title, "Escalar Alcoià i Comtat"),
                    stringResource(R.string.intro_main_message)
                ),
                IntroPageData(
                    stringResource(R.string.intro_warning_title),
                    stringResource(R.string.intro_warning_message)
                )
            )
        ) { }
    }
}
