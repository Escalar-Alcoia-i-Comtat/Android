package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REQUEST_CODE_LOGIN
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityAuthBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.auth.LoginFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.auth.RegisterFragment

/**
 * Allows the user to get logged into the app, register, or get some help on a lost account.
 * Note that this returns a result once an action has been completed. See the also see section of
 * the javadoc.
 * @author Arnau Mora
 * @since 20210425
 * @see REQUEST_CODE_LOGIN
 */
class AuthActivity : LanguageFragmentActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authPages = mapOf(
            PAGE_REGISTER to RegisterFragment.newInstance(),
            PAGE_LOGIN to LoginFragment.newInstance()
        )

        binding.authViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = authPages.size

            override fun createFragment(position: Int): Fragment = authPages[position]!!
        }
        binding.authViewPager.isUserInputEnabled = false
        changePage(PAGE_LOGIN, false)
    }

    /**
     * Changes the current page on the [ActivityAuthBinding.authViewPager].
     * @author Arnau Mora
     * @since 20210424
     * @param page The new page
     * @param animate If the change should be animated
     */
    fun changePage(page: Int, animate: Boolean = true) {
        binding.authViewPager.setCurrentItem(page, animate)
    }

    companion object {
        const val PAGE_REGISTER = 0
        const val PAGE_LOGIN = 1
    }
}
