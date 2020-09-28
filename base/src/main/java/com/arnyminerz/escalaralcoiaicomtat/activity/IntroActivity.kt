package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.analytics
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.BetaIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.DownloadAreasIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.MainIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.StorageIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.StorageIntroFragment.Companion.STORAGE_PERMISSION_REQUEST
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_SHOWN_INTRO
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_intro.*
import org.jetbrains.anko.toast
import java.io.File


@ExperimentalUnsignedTypes
class IntroActivity : AppCompatActivity() {
    companion object {
        var shouldChange = false

        fun cacheFile(context: Context) = File(context.filesDir, "cache.json")

        fun hasStoragePermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED

        fun hasLocationPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        fun hasDownloaded(context: Context): Boolean = cacheFile(context).exists()

        fun shouldShow(context: Context): Boolean =
            !hasStoragePermission(context) || !hasDownloaded(context)
    }

    var adapterViewPager: IntroPagerAdapter? = null
    val bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val isBeta = BuildConfig.VERSION_NAME.contains("pre", true)

        bundle.clear()
        bundle.putString("os_version", android.os.Build.VERSION.RELEASE)
        bundle.putInt("api_level", android.os.Build.VERSION.SDK_INT)
        analytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, bundle)

        adapterViewPager = IntroPagerAdapter(supportFragmentManager, isBeta, this)
        view_pager.adapter = adapterViewPager
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                val storageIntroFragmentIndex =
                    adapterViewPager!!.fragments.indexOf(adapterViewPager!!.storageIntroFragment)
                if (position - 1 == storageIntroFragmentIndex)
                    if (!hasStoragePermission(this@IntroActivity)) {
                        view_pager.currentItem = storageIntroFragmentIndex
                        intro_next_FAB.setImageResource(R.drawable.round_chevron_right_24)
                        shouldChange = false
                        ActivityCompat.requestPermissions(
                            this@IntroActivity,
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ),
                            STORAGE_PERMISSION_REQUEST
                        )
                        return
                    }
            }

            override fun onPageSelected(position: Int) {
            }
        })

        intro_next_FAB.setOnClickListener {
            next()
        }
    }

    fun fabStatus(enabled: Boolean) = intro_next_FAB.setEnabled(enabled)

    fun next() {
        if (view_pager.currentItem + 1 >= adapterViewPager!!.fragments.size) {
            sharedPreferences?.let {
                PREF_SHOWN_INTRO.put(sharedPreferences!!, true)
                analytics.logEvent(
                    FirebaseAnalytics.Event.TUTORIAL_COMPLETE,
                    bundle
                )
                startActivity(Intent(this@IntroActivity, MainActivity()::class.java))
            }
        } else {
            if (view_pager.currentItem == adapterViewPager!!.fragments.size - 2)
                intro_next_FAB.setImageResource(R.drawable.round_check_24)
            view_pager.currentItem++
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    findViewById<MaterialButton?>(R.id.grant_storage_permission_button)
                        ?.apply {
                            setText(R.string.status_permission_granted)
                            isEnabled = false
                        }
                } else {
                    toast(R.string.toast_permission_required)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class IntroPagerAdapter(fragmentManager: FragmentManager, isBeta: Boolean, context: Context) :
        FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        val fragments = arrayListOf<Fragment>()

        val mainIntroFragment = MainIntroFragment()
        val betaIntroFragment = BetaIntroFragment()
        val storageIntroFragment = StorageIntroFragment()
        val downloadIntroFragment = DownloadAreasIntroFragment()

        init {
            if (!hasStoragePermission(context))
                fragments.add(mainIntroFragment)
            if (isBeta && !hasStoragePermission(context))
                fragments.add(betaIntroFragment)
            if (!hasStoragePermission(context))
                fragments.add(storageIntroFragment)
            if (!hasDownloaded(context))
                fragments.add(downloadIntroFragment)
        }

        override fun getCount() = fragments.size

        override fun getItem(position: Int): Fragment = fragments[position]
    }
}