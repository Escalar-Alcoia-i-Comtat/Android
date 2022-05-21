package com.arnyminerz.escalaralcoiaicomtat

import android.app.Activity
import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
import android.widget.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.utils.DeviceProvider
import com.arnyminerz.escalaralcoiaicomtat.utils.classNameSelector
import com.arnyminerz.escalaralcoiaicomtat.utils.descriptionSelector
import com.arnyminerz.escalaralcoiaicomtat.utils.regexTextSelector
import com.arnyminerz.escalaralcoiaicomtat.utils.textSelector
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import timber.log.Timber


@RunWith(MockitoJUnitRunner::class)
class MainActivityTest : DeviceProvider() {
    private lateinit var instrumentation: Instrumentation

    private lateinit var loadingActivityMonitor: ActivityMonitor

    private lateinit var introActivityMonitor: ActivityMonitor

    private lateinit var mainActivityMonitor: ActivityMonitor

    private lateinit var dataClassActivityMonitor: ActivityMonitor

    private var activity: Activity? = null

    /**
     * Launches the main activity, so it's ready for tasks to interact with it.
     * @author Arnau Mora
     * @since 20220520
     */
    @Before
    fun startMainActivityFromHomeScreen() {
        instrumentation = InstrumentationRegistry.getInstrumentation()

        // Initialize UiDevice instance
        device = UiDevice.getInstance(instrumentation)
        ViewMatchers.assertThat(device, CoreMatchers.notNullValue())

        // Start from the home screen
        device.pressHome()

        val launcherPackage = device.launcherPackageName
        ViewMatchers.assertThat(launcherPackage, CoreMatchers.notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            5_000
        )
    }

    private fun introRoutine() {
        runBlocking {
            Timber.i("Marking intro as not shown...")
            PreferencesModule.systemPreferencesRepository.markIntroAsShown(false)

            Timber.i("Enabling nearby zones...")
            PreferencesModule.userPreferencesRepository.setNearbyZonesEnabled(true)
        }


        val intent = Intent(instrumentation.targetContext, LoadingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        instrumentation.startActivitySync(intent)


        val loadingActivity = instrumentation.waitForMonitor(loadingActivityMonitor)
        assertNotNull(loadingActivity)
        activity = loadingActivity

        var activityName = activity?.javaClass?.simpleName
        Timber.i("Activity class name: $activityName")


        Timber.d("Waiting for intro activity...")
        val introActivity = instrumentation.waitForMonitor(introActivityMonitor)
        assertNotNull(introActivity)
        activity = introActivity

        activityName = introActivity?.javaClass?.simpleName
        Timber.i("Activity class name: $activityName")

        Timber.d("Waiting for device to be idle")
        device.waitForIdle()


        val context = instrumentation.targetContext

        val nextFab = descriptionSelector(context.getString(R.string.fab_desc_next))

        regexTextSelector(context.getString(R.string.intro_main_title))
        nextFab.click()

        regexTextSelector(context.getString(R.string.intro_warning_title))
        nextFab.click()

        regexTextSelector(context.getString(R.string.intro_nearbyzones_title))

        val switch = classNameSelector(Switch::class.java)
        switch.click()

        if (BuildConfig.DEBUG) {
            // If in debug, skip the beta slide
            nextFab.click()
            regexTextSelector(context.getString(R.string.intro_beta_title))
        }

        val finishFab = descriptionSelector(context.getString(R.string.fab_desc_finish))
        finishFab.click()


        Timber.d("Waiting for MainActivity...")
        val mainActivity = instrumentation.waitForMonitor(mainActivityMonitor)
        assertNotNull(mainActivity)
        activity = mainActivity

        Timber.i("Intro works correctly!")
    }

    private fun sectorDisplayRoutine() {
        val context = instrumentation.targetContext

        val exploreNavMenu = descriptionSelector(context.getString(R.string.item_explore))
        exploreNavMenu.click()

        val areaItem = regexTextSelector(
            context.getString(R.string.downloads_zones_title).replace("%d", "\\d*")
        )
        areaItem.clickAndWaitForNewWindow()

        Timber.d("Waiting for DataClassActivity (Zones)...")
        val dataClassActivity = instrumentation.waitForMonitor(dataClassActivityMonitor)
        assertNotNull(dataClassActivity)
        activity = dataClassActivity

        val zoneItem = regexTextSelector(
            context.getString(R.string.downloads_sectors_title).replace("%d", "\\d*")
        )
        zoneItem.clickAndWaitForNewWindow()


        val sectorItem = regexTextSelector(
            context.getString(R.string.downloads_paths_title).replace("%d", "\\d*")
        )
        sectorItem.clickAndWaitForNewWindow()


        textSelector(context.getString(R.string.activity_summary_grades))
    }

    private fun sectorBackNavigationRoutine() {
        val context = instrumentation.targetContext

        device.pressBack()
        device.pressBack()
        device.pressBack()

        descriptionSelector(context.getString(R.string.item_explore))

        Timber.i("Sector back navigation works correctly.")
    }

    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun initActivity() {
        instrumentation = InstrumentationRegistry.getInstrumentation()

        loadingActivityMonitor = ActivityMonitor(LoadingActivity::class.java.name, null, false)
        instrumentation.addMonitor(loadingActivityMonitor)

        introActivityMonitor = ActivityMonitor(IntroActivity::class.java.name, null, false)
        instrumentation.addMonitor(introActivityMonitor)

        mainActivityMonitor = ActivityMonitor(MainActivity::class.java.name, null, false)
        instrumentation.addMonitor(mainActivityMonitor)

        dataClassActivityMonitor = ActivityMonitor(DataClassActivity::class.java.name, null, false)
        instrumentation.addMonitor(dataClassActivityMonitor)


        introRoutine()

        sectorDisplayRoutine()

        sectorBackNavigationRoutine()


        Timber.d("Hello :)")
        /*mockLoadingViewModel = mock()
        whenever(
            mockLoadingViewModel.startLoading(
                any() as String,
                any() as FirebaseRemoteConfig,
                any() as FirebaseMessaging,
                any() as FirebaseAnalytics,
            )
        ).thenAnswer {
            mockLoadingViewModel.progressMessageResource.value = 0
            null
        }*/
    }
}