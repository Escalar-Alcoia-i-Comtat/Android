package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityAuthBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.auth.LoginFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.auth.RegisterFragment
import com.arnyminerz.escalaralcoiaicomtat.shared.LOGGED_IN_REQUEST_CODE

/**
 * Allows the user to get logged into the app, register, or get some help on a lost account.
 * Note that this returns a result once an action has been completed. See the also see section of
 * the javadoc.
 * @author Arnau Mora
 * @since 20210425
 * @see LOGGED_IN_REQUEST_CODE
 */
class AuthActivity : LanguageFragmentActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authPages = listOf(
            RegisterFragment.newInstance(),
            LoginFragment.newInstance()
        )

        binding.authViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = authPages.size

            override fun createFragment(position: Int): Fragment = authPages[position]
        }
    }
}
