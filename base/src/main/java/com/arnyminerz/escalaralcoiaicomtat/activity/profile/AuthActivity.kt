package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityAuthBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.auth.LoginFragment

class AuthActivity : LanguageFragmentActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authPages = listOf(
            LoginFragment.newInstance()
        )

        binding.authViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = authPages.size

            override fun createFragment(position: Int): Fragment = authPages[position]
        }
    }
}
