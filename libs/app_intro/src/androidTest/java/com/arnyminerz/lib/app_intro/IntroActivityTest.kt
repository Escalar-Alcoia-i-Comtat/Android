package com.arnyminerz.lib.app_intro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Switch
import androidx.core.content.ContextCompat
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.Until
import com.arnyminerz.lib.app_intro.TestActivity.Companion.INTRO_PAGE_1_TITLE
import com.arnyminerz.lib.app_intro.TestActivity.Companion.INTRO_PAGE_2_TITLE
import com.arnyminerz.lib.app_intro.TestActivity.Companion.INTRO_PAGE_4_TITLE
import com.arnyminerz.lib.app_intro.utils.TestDeviceProvider
import com.arnyminerz.lib.app_intro.utils.assertExists
import com.arnyminerz.lib.app_intro.utils.classNameSelector
import com.arnyminerz.lib.app_intro.utils.descriptionSelector
import com.arnyminerz.lib.app_intro.utils.textSelector
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class IntroActivityTest : TestDeviceProvider() {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    /**
     * Launches the test activity, so it's ready for tasks to interact with it.
     * @author Arnau Mora
     * @since 20220520
     */
    @Before
    fun startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(device, notNullValue())

        // Start from the home screen
        device.pressHome()

        val launcherPackage = device.launcherPackageName
        assertThat(launcherPackage, notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            5_000
        )

        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = context
            .packageManager
            .getLaunchIntentForPackage(BuildConfig.LIBRARY_PACKAGE_NAME + ".test")
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        assertThat(intent, notNullValue())
        context.startActivity(intent)

        device.wait(
            Until.hasObject(By.pkg(BuildConfig.LIBRARY_PACKAGE_NAME + ".test").depth(0)),
            1_000,
        )
    }

    /**
     * Makes sure the finish fab is being displayed, and returns it.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun assertFinishFab() =
        descriptionSelector(R.string.fab_desc_finish)
            .assertExists(5_000)

    /**
     * Makes sure the next fab is being displayed, and returns it.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun assertNextFab() =
        descriptionSelector(R.string.fab_desc_next)
            .assertExists(5_000)

    /**
     * Performs click on the fab for the intro page.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun nextPage() {
        assertNextFab()
            .click()
    }

    /**
     * Makes sure that an element with text [title] exists.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun testPageDisplay(title: String) {
        // Check if the page is being displayed
        textSelector(title)
            .assertExists(5_000)
    }

    /**
     * Makes sure that the switch on the third page has the checked state set to [checked].
     * @author Arnau Mora
     * @since 20220520
     */
    private fun assertThirdPageSwitchState(checked: Boolean) {
        // Check that the initial switch value was set correctly to false
        val switch = classNameSelector(Switch::class.java)
            .assertExists(5_000)

        val isChecked = switch.isChecked
        Timber.w("Checked: $isChecked")
        assert(isChecked == checked)
    }

    /**
     * Makes sure that the grant permission fab is being displayed.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun assertFabPermission(): UiObject =
        descriptionSelector(R.string.fab_desc_grant)
            .assertExists(5_000)

    /**
     * Clicks the grant permissions fab.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun tapFabPermission() {
        assertFabPermission()
            .click()
    }

    /**
     * Clicks the switch on the third page.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun thirdPageSwitchClick() {
        // Check that the initial switch value was set correctly to false
        val switch = classNameSelector(Switch::class.java)
            .assertExists(5_000)
        switch.click()

        Timber.i("Clicked switch.")
    }

    /**
     * Checks that the permission [permission] is granted or not based on [granted].
     * @author Arnau Mora
     * @since 20220520
     */
    private fun assertPermission(permission: String, granted: Boolean) {
        assert(
            ContextCompat.checkSelfPermission(
                InstrumentationRegistry
                    .getInstrumentation()
                    .context,
                permission,
            ) == if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        )
    }

    /**
     * Runs actions on the location dialog to grant precise location access only this time.
     * @author Arnau Mora
     * @since 20220520
     */
    private fun locationPermissionDialogRoutine() {
        val preciseLocationLabel = textSelector("Precise")
            .assertExists(5_000)
        preciseLocationLabel.click()

        val onlyThisTimeButton = textSelector("Only this time")
            .assertExists(5_000)
        onlyThisTimeButton.clickAndWaitForNewWindow(5_000)
    }

    @Test
    fun runUiTests() {
        Timber.d("Checking if first page is being displayed...")
        testPageDisplay(INTRO_PAGE_1_TITLE)
        nextPage()


        Timber.d("Checking if second page is being displayed...")
        testPageDisplay(INTRO_PAGE_2_TITLE)
        nextPage()


        Timber.d("Checking if third page's switch is off...")
        assertThirdPageSwitchState(false)

        Timber.d("Clicking third page's switch...")
        thirdPageSwitchClick()

        Timber.d("Checking if third page's switch is on...")
        assertThirdPageSwitchState(true)

        Timber.d("Making sure the permission is not granted...")
        assertPermission(Manifest.permission.ACCESS_FINE_LOCATION, false)

        Timber.d("Tapping permission fab...")
        tapFabPermission()

        Timber.d("Accepting location permission...")
        locationPermissionDialogRoutine()

        Timber.d("Making sure the permission has been granted...")
        assertPermission(Manifest.permission.ACCESS_FINE_LOCATION, true)

        nextPage()


        Timber.d("Checking if third page is being displayed...")
        testPageDisplay(INTRO_PAGE_4_TITLE)
        assertFinishFab().click()
    }
}
