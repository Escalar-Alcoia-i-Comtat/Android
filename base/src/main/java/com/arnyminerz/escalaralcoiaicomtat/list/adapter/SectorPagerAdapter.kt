package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SectorPagerAdapter(fa: FragmentActivity, private val items: ArrayList<Fragment>): FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment = items[position]
}