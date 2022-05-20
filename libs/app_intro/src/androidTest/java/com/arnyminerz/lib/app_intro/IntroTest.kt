package com.arnyminerz.lib.app_intro

import android.Manifest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arnyminerz.lib.app_intro.action.IntroAction
import com.arnyminerz.lib.app_intro.action.IntroActionType
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val INTRO_PAGE_1_TITLE = "Intro Page 1"

private const val INTRO_PAGE_2_TITLE = "Intro Page 2"

private const val INTRO_PAGE_3_TITLE = "Permissions request page"

class IntroTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val pages = arrayListOf<IntroPageData<*>>()

    private val switch1State = mutableStateOf(false)

    @Before
    fun initializeIntroPages() {
        pages.add(
            IntroPageData<Any?>(
                title = INTRO_PAGE_1_TITLE,
                content = "The content of the first page."
            )
        )
        pages.add(
            IntroPageData<Any?>(
                title = INTRO_PAGE_2_TITLE,
                content = "The content of the second page."
            )
        )
        pages.add(
            IntroPageData(
                title = INTRO_PAGE_3_TITLE,
                content = "This page will request some permissions from the user",
                IntroAction(
                    text = "Enable or disable",
                    switch1State,
                    IntroActionType.SWITCH,
                    { },
                ),
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
            )
        )
    }

    /**
     * Performs click on the fab for the intro page.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun nextPage() {
        composeTestRule.onNodeWithTag("intro_fab").performClick()
    }

    @Test
    @OptIn(
        ExperimentalPagerApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalPermissionsApi::class,
    )
    fun startIntroTest() {
        var finished = false

        composeTestRule.setContent {
            IntroWindow(pages) {
                finished = true
            }
        }

        // Check if the first page is being displayed
        composeTestRule.onNodeWithText(INTRO_PAGE_1_TITLE).assertIsDisplayed()

        // Try to go to the next page
        nextPage()

        // Ensure the second page is being displayed
        composeTestRule.onNodeWithText(INTRO_PAGE_2_TITLE).assertIsDisplayed()

        // Go to the next page
        nextPage()

        // Ensure the third page is being displayed
        composeTestRule.onNodeWithText(INTRO_PAGE_3_TITLE).assertIsDisplayed()

        // Ensure that the default value was correctly set to false
        assert(!switch1State.value)

        // Change the switch state by clicking
        composeTestRule.onNodeWithTag("intro_switch").performClick()
        assert(switch1State.value)

        // Change the switch state by updating the state
        switch1State.value = false
        assert(!switch1State.value)

        // TODO: Test permission request
        /*
        // Force the switch to be enabled
        composeTestRule.onNodeWithTag("intro_switch").performClick()

        // Make sure the permission icon is being shown
        composeTestRule.onNodeWithTag("intro_icon_permission").assertIsDisplayed()
        */

        nextPage()

        assert(finished)
    }
}
