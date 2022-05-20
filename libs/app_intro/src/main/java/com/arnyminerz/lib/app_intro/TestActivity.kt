package com.arnyminerz.lib.app_intro

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import com.arnyminerz.lib.app_intro.action.IntroAction
import com.arnyminerz.lib.app_intro.action.IntroActionType
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import timber.log.Timber

class TestActivity : ComponentActivity() {
    companion object {
        const val INTRO_PAGE_1_TITLE = "Intro Page 1"

        const val INTRO_PAGE_2_TITLE = "Intro Page 2"

        const val INTRO_PAGE_3_TITLE = "Intro Page 3"

        const val INTRO_PAGE_3_SWITCH = "Switch"

        const val INTRO_PAGE_4_TITLE = "Final Page"
    }

    private val switch1State = mutableStateOf(false)

    private val pages = listOf(
        IntroPageData(
            title = INTRO_PAGE_1_TITLE,
            content = "The content of the first page."
        ),
        IntroPageData(
            title = INTRO_PAGE_2_TITLE,
            content = "The content of the second page."
        ),
        IntroPageData(
            title = INTRO_PAGE_3_TITLE,
            content = "This page will request some permissions from the user",
            IntroAction(
                text = INTRO_PAGE_3_SWITCH,
                switch1State,
                IntroActionType.SWITCH,
                { },
            ),
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
        ),
        IntroPageData<Any?>(
            title = INTRO_PAGE_4_TITLE,
            content = "This is the last page, the arrow icon should be shown in fab"
        )
    )

    @OptIn(
        ExperimentalPagerApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalPermissionsApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())

        setContent {
            IntroWindow(pages) {
                finishAffinity()
            }
        }
    }
}
