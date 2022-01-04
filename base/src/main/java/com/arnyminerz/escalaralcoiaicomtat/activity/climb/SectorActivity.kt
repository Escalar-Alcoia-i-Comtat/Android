package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivitySectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.SectorFragment
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import org.json.JSONObject
import timber.log.Timber

@ExperimentalBadgeUtils
@Deprecated("Should be converted to Jetpack Compose")
class SectorActivity : LanguageAppCompatActivity() {
    companion object {
        /**
         * Launches the [SectorActivity] with the specified arguments.
         * @author Arnau Mora
         * @since 20210820
         * @param context The [Context] that wants to launch the Intent
         * @param zoneId The id of the zone to display.
         * @param sectorId The id of the sector to select by default.
         */
        fun intent(context: Context, zoneId: String, sectorId: String): Intent =
            Intent(context, SectorActivity::class.java).apply {
                putExtra(EXTRA_ZONE, zoneId)
                putExtra(EXTRA_SECTOR, sectorId)
            }
    }

    private var transitionName: String? = null

    lateinit var zoneId: String
        private set
    lateinit var sectorId: String
        private set

    var currentPage: Int = 0
        private set

    /**
     * Stores the initialized [SectorFragment]s that should be displayed to the user.
     * The key represents the [Sector.objectId] to display, the value is the [SectorFragment].
     * @author Arnau Mora
     * @since 20210820
     */
    private val fragments = arrayListOf<SectorFragment>()

    private lateinit var binding: ActivitySectorBinding
    lateinit var firestore: FirebaseFirestore
    lateinit var storage: FirebaseStorage

    /**
     * The currectly loaded zone.
     * @author Arnau Mora
     * @since 20210826
     */
    private lateinit var zone: Zone

    /**
     * The list of sectors contained in [zone].
     * @author Arnau Mora
     * @since 20210826
     */
    private lateinit var sectors: List<Sector>

    /**
     * Tells if the content has been loaded correctly
     * @author Arnau Mora
     * @since 20210324
     */
    private var loadComplete = false

    /**
     * Updates the Activity's title
     * @author Arnau Mora
     * @since 20210314
     * @param newTitle The new title to set
     */
    @UiThread
    fun updateTitle(newTitle: String? = null, isDownloaded: Boolean = false) {
        if (newTitle == null)
            binding.toolbar.title = null
        else {
            binding.toolbar.title = newTitle
            binding.toolbar.transitionName = transitionName
        }
        val item: MenuItem? = null // binding.toolbar.menu.getItem(0)
        item?.apply {
            if (isDownloaded) {
                setIcon(R.drawable.cloud_check)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    tooltipText = getString(R.string.status_downloaded)
                show()
            } else if (!appNetworkState.hasInternet) {
                setIcon(R.drawable.ic_round_signal_cellular_off_24)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    tooltipText = getString(R.string.status_no_internet)
                show()
            } else
                hide()
        }
    }

    /**
     * Sets whether or not the slider can be controlled
     * @author Arnau Mora
     * @since 20210314
     * @param enabled If true, the view pager would be able to slide
     */
    @UiThread
    fun userInputEnabled(enabled: Boolean) {
        binding.sectorViewPager.isUserInputEnabled = enabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = Firebase.firestore
        storage = Firebase.storage
        binding = ActivitySectorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadExtras(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.toolbar.menu.getItem(0).setOnMenuItemClickListener {
            it.actionView.performLongClick()
            true
        }

        updateTitle()

        doAsync {
            zone = app.getZone(zoneId) ?: run {
                Timber.e("Could not find zone $zoneId.")
                uiContext { onBackPressed() }
                finish()
                return@doAsync
            }
            Timber.v("Loading sectors from $zone...")
            sectors = zone.getChildren(app.searchSession)
            val sectorCount = sectors.size

            Timber.v("Getting position extra...")
            var positionExtra = savedInstanceState?.getInt(EXTRA_POSITION.key)

            Timber.v("There are $sectorCount sectors.")
            Timber.d("Initializing fragments...")
            fragments.clear()
            val sectorNames = arrayListOf<String>()
            for ((i, sector) in sectors.withIndex()) {
                fragments.add(SectorFragment.newInstance(sector.objectId))
                sectorNames.add(sector.displayName)
                if (positionExtra == null && sector.objectId == sectorId) {
                    Timber.v("The desired sector S/$sectorId in at #$i.")
                    positionExtra = i
                }
            }

            val defaultPosition = positionExtra ?: 0
            currentPage = defaultPosition
            if (fragments.size >= defaultPosition)
                fragments[currentPage].load()

            uiContext {
                Timber.v("Adding listener for sectors dialog")
                binding.toolbar.setOnClickListener {
                    MaterialAlertDialogBuilder(this@SectorActivity)
                        .setTitle(getString(R.string.dialog_sectors_title))
                        .setSingleChoiceItems(
                            sectorNames.toTypedArray(),
                            currentPage
                        ) { dialog, which ->
                            dialog.dismiss()
                            binding.sectorViewPager.setCurrentItem(which, true)
                        }
                        .setNegativeButton(R.string.action_close) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

                Timber.v("Initializing view pager...")
                Timber.d("  Initializing adapter for ${fragments.size} pages...")
                val adapter = object : FragmentStateAdapter(this@SectorActivity) {
                    override fun getItemCount(): Int = fragments.size
                    override fun createFragment(position: Int): Fragment {
                        Timber.d("Creating fragment #$position")
                        return fragments[position]
                    }
                }
                Timber.d("  Initializing page change callback...")
                val callback = object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        Timber.d("Selected page #$position")
                        currentPage = position

                        // Minimize all fragments
                        for (fragment in fragments)
                            fragment.minimize()

                        doAsync {
                            fragments[position].load()
                        }
                    }
                }
                Timber.d("  Setting adapter...")
                binding.sectorViewPager.adapter = adapter
                Timber.d("  Registering callback...")
                binding.sectorViewPager.registerOnPageChangeCallback(callback)
                // If there's an stored position, load it
                Timber.d("  Setting position")
                binding.sectorViewPager.setCurrentItem(defaultPosition, false)

                loadComplete = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (this::zoneId.isInitialized) {
            outState.putString(EXTRA_ZONE.key, zoneId)
            outState.putInt(EXTRA_POSITION.key, binding.sectorViewPager.currentItem)
        }
        super.onSaveInstanceState(outState)
    }

    /**
     * Provides context for improving the user experience.
     * @author Arnau Mora
     * @since 20210826
     */
    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)

        if (!this::zone.isInitialized || !this::sectors.isInitialized)
            return

        val sector = sectors[currentPage]
        val webUrl = sector.webUrl

        if (webUrl != null)
            outContent.webUri = Uri.parse(sector.metadata.webURL)
        outContent.structuredData = JSONObject().apply {
            put("@type", sector.namespace)
            put("name", sector.displayName)
            if (webUrl != null)
                put("url", webUrl)
        }.toString()
    }

    /**
     * Initializes all the variables from the Activity's intent's extras.
     * @author Arnau Mora
     * @since 20210527
     * @param savedInstanceState The savedInstanceState from the `onCreate` method.
     * @return True if everything was loaded correctly, false otherwise.
     */
    private fun loadExtras(savedInstanceState: Bundle?): Boolean {
        val extras = intent.extras
        return if (extras == null && savedInstanceState == null) {
            Timber.e("Extras is null and there's no savedInstanceState")
            onBackPressed()
            false
        } else {
            val zoneIdExtra = intent.getExtra(EXTRA_ZONE)
            val sectorIdExtra = intent.getExtra(EXTRA_SECTOR)
            val zoneIdInstanceState = savedInstanceState?.getString(EXTRA_ZONE.key, null)
            val sectorIdInstanceState = savedInstanceState?.getString(EXTRA_SECTOR.key, null)
            val zoneIdBothInvalid = zoneIdInstanceState == null && zoneIdExtra == null
            val sectorIdBothInvalid = sectorIdInstanceState == null && sectorIdExtra == null
            if (zoneIdBothInvalid || sectorIdBothInvalid) {
                Timber.e("No loaded data for activity")
                // TODO: Should be transferred using intent result
                //errorNotStored = true
                onBackPressed()
                false
            } else {
                zoneId = zoneIdExtra ?: run {
                    Timber.v("Loading zoneId from savedInstanceState")
                    zoneIdInstanceState!!
                }
                sectorId = sectorIdExtra ?: run {
                    Timber.v("Loading sectorId from savedInstanceState")
                    sectorIdInstanceState!!
                }
                Timber.d("Loading sectors from zone $zoneId...")

                transitionName = intent.getExtra(EXTRA_SECTOR_TRANSITION_NAME)
                if (transitionName == null)
                    Timber.w("Transition name is null")

                true
            }
        }
    }
}
