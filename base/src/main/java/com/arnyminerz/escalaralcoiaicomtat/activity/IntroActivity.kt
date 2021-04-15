package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.data.IntroShowReason
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityIntroBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.BetaIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.MainIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.WarningIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_SHOWN_INTRO
import timber.log.Timber

class IntroActivity : LanguageAppCompatActivity() {
    companion object {
        var shouldChange = false

        fun shouldShow(): IntroShowReason {
            var result: IntroShowReason? = null
            if (!PREF_SHOWN_INTRO.get())
                result = IntroShowReason.PREF_FALSE
            return result ?: IntroShowReason.OK
        }
    }

    var adapterViewPager: IntroPagerAdapter? = null
        private set

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        adapterViewPager = IntroPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = adapterViewPager

        binding.introNextFAB.setOnClickListener {
            next()
        }
    }

    fun fabStatus(enabled: Boolean) {
        binding.introNextFAB.isEnabled = enabled
    }

    fun next() {
        val position = binding.viewPager.currentItem

        if (position + 1 >= adapterViewPager!!.fragments.size) {
            Timber.v("Finished showing intro pages. Loading LoadingActivity")
            PREF_SHOWN_INTRO.put(true)
            startActivity(Intent(this, LoadingActivity()::class.java))
        } else {
            if (binding.viewPager.currentItem == adapterViewPager!!.fragments.size - 2)
                binding.introNextFAB.setImageResource(R.drawable.round_check_24)
            binding.viewPager.currentItem++
            Timber.v("Showing intro page ${binding.viewPager.currentItem}")
        }
    }

    class IntroPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        val fragments = arrayListOf<Fragment>()

        val mainIntroFragment = MainIntroFragment()
        val warningIntroFragment = WarningIntroFragment()
        val betaIntroFragment = BetaIntroFragment()

        init {
            fragments.add(mainIntroFragment)
            fragments.add(warningIntroFragment)
            if (BuildConfig.DEBUG)
                fragments.add(betaIntroFragment)
        }

        override fun getCount() = fragments.size

        override fun getItem(position: Int): Fragment = fragments[position]
    }
}
