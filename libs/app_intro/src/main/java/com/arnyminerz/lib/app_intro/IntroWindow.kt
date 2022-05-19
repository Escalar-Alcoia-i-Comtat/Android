package com.arnyminerz.lib.app_intro

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A composable for displaying a set of [IntroPage]s.
 * @author Arnau Mora
 * @since 20211214
 * @param pages All the [IntroPageData] to display.
 * @param finishListener What to run when the intro reaches its end.
 */
@Composable
@ExperimentalPagerApi
@ExperimentalMaterial3Api
@ExperimentalPermissionsApi
fun IntroWindow(
    pages: List<IntroPageData<*>>,
    finishListener: () -> Unit,
) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    var blockPage by remember { mutableStateOf(-1) }
    var currentPage by remember { mutableStateOf(0) }

    val permissionsStatesMap = hashMapOf<Int, MultiplePermissionsState>().apply {
        for ((index, page) in pages.withIndex()) {
            val permissions = page.permissions
            if (permissions != null)
                put(index, rememberMultiplePermissionsState(permissions.toList()))
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                Timber.v("Current page: $page. Blocked: $blockPage")
                currentPage = page

                // Checks if page is greater than blockPage
                if (blockPage != -1 && page > blockPage) {
                    Timber.i("Going back to page $blockPage")
                    pagerState.scrollToPage(blockPage)
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val page = pages[currentPage]
                    val currentValue = page.action.currentValue as MutableState<*>
                    val permissionsState = permissionsStatesMap[currentPage]

                    if (currentValue.value as? Boolean == true && permissionsState?.allPermissionsGranted == false)
                        permissionsState.launchMultiplePermissionRequest()
                    else if (pagerState.currentPage + 1 >= pages.size)
                    // Reached the end, exit and enter MainActivity
                        finishListener()
                    else scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            ) {
                val page = pages[currentPage]
                val currentValue = page.action.currentValue as MutableState<*>
                val permissionsState = permissionsStatesMap[currentPage]

                if (currentValue.value as? Boolean == true && permissionsState?.allPermissionsGranted != true) {
                    if (blockPage < 0) {
                        blockPage = pagerState.currentPage
                        Timber.i("Blocked current page to $blockPage")
                    }

                    Icon(
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        imageVector = Icons.Rounded.Security,
                        contentDescription = stringResource(R.string.fab_desc_grant)
                    )
                } else {
                    blockPage = -1
                    Timber.d("Unlocked current page")
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
        }
    ) { padding ->
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier
                .padding(padding),
        ) { currentPage ->
            val page = pages[currentPage]

            IntroPage(page)
        }
    }
}

@Composable
@Preview(showSystemUi = true, name = "Intro Window Preview")
@ExperimentalPagerApi
@ExperimentalMaterial3Api
@ExperimentalPermissionsApi
fun IntroWindowDemo() {
    IntroWindow(
        pages = listOf(
            IntroPageData<Any?>(
                "Welcome to Escalar Alcoia i Comtat",
                "We are going to walk you through a tiny configuration process. Don't worry it's fast and easy."
            ),
            IntroPageData(
                "Attention",
                "Climbing is considered a dangerous sport, even though well done can be really safe to perform. Please, inform yourself before going out, and don't use this guide as an absolute reference over what's outside. Have caution, and use the appropriate equipment, check not only yourself, but also your partner's equipment and preparation, and of course, have fun."
            )
        )
    ) { }
}

/*
@ExperimentalMaterial3Api
@ExperimentalPagerApi
@Preview(showSystemUi = true, name = "Intro Window Dark Preview")
@Composable
fun IntroWindowDarkDemo() {
    AppTheme(useDarkTheme = true) {
        IntroWindow(
            pages = listOf(
                IntroPageData<Any?>(
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
*/
